package com.orbis.stream.service;

import com.orbis.stream.component.LoggerMessageComponent;
import com.orbis.stream.enums.*;
import com.orbis.stream.exceptions.FileReadingException;
import com.orbis.stream.exceptions.LiveException;
import com.orbis.stream.exceptions.NotFoundCustomException;
import com.orbis.stream.handler.ResponseHandler;
import com.orbis.stream.mapping.mapperGeneric.FromVideoRecordToStartLiveRecordMapper;
import com.orbis.stream.mapping.mapperRECORD.VideoLiveHistoryRecordMapper;
import com.orbis.stream.mapping.mapperRECORD.VideoSettingRecordMapper;
import com.orbis.stream.model.Video;
import com.orbis.stream.model.VideoLiveHistory;
import com.orbis.stream.model.VideoSetting;
import com.orbis.stream.model.VideoSettingsOption;
import com.orbis.stream.record.StartLiveRecord;
import com.orbis.stream.record.VideoLiveHistoryRecord;
import com.orbis.stream.record.VideoRecord;
import com.orbis.stream.record.VideoSettingsRecord;
import com.orbis.stream.repository.VideoLiveHistoryRepository;
import com.orbis.stream.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import java.util.concurrent.locks.LockSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamService {

    private final ResponseHandler responseHandler;
    private final LoggerMessageComponent loggerMessageComponent;

    private final VideoSettingRecordMapper videoSettingRecordMapper;
    private final FromVideoRecordToStartLiveRecordMapper fromVideoRecordToStartLiveRecordMapper;
    private final VideoLiveHistoryRecordMapper videoLiveHistoryRecordMapper;

    private final VideoRepository videoRepository;
    private final VideoLiveHistoryRepository videoLiveHistoryRepository;

    private final TaskExecutor taskExecutor;

    private static final long TWO_MILLISECONDS = 2_000_000L;
    private static final long ONE_MILLISECONDS = 1_000_000L;

    private void cleanup(FFmpegFrameGrabber grabber, FFmpegFrameRecorder recorder) {
        try {
            if (recorder != null) recorder.stop();
            if (grabber != null) grabber.stop();
        } catch (Exception ignored) {}
    }

    public ResponseEntity<Map<String, String>> startLive(StartLiveRecord startLiveRecord){

        String channelName = startLiveRecord.channelName();
        String platformStreamName = startLiveRecord.platformStreamName();

        checkIfALiveAlreadyStreamingForAChannel(channelName, platformStreamName);

        String videoPathFolder = startLiveRecord.videoPath();
        String streamKey = startLiveRecord.streamKey();
        String streamUrl = startLiveRecord.streamUrl();


        LocalDateTime timeStartLive = ZonedDateTime.now().toLocalDateTime();
        saveVideoLiveHistory(videoPathFolder, timeStartLive, streamUrl, streamKey, platformStreamName);

        VideoLiveHistory videoLiveHistory = retrievedVideoLiveHistorySaved(videoPathFolder, timeStartLive);

        saveVideoPaths(videoPathFolder, videoLiveHistory, startLiveRecord.videoSettingsRecord(), channelName);

        String streamingUrl;

        streamingUrl = streamUrl.endsWith("/")
                ? streamUrl.concat(streamKey)
                : streamUrl.concat("/").concat(streamKey);

        return streamingVideo(videoLiveHistory, streamingUrl);
    }

    @Transactional(readOnly = true)
    protected void checkIfALiveAlreadyStreamingForAChannel(String channelName, String platformStreamName) {
        List<Video> videoList = videoRepository
                .findByLiveStatusAndChannelName(LiveStatusEnum.LIVE, channelName);

        if (videoList.isEmpty()) {
            return;
        }

        List<Video> videoListParsed = videoList.stream()
                .filter((video)-> {
                    boolean isCorrectedPlatform = video
                            .getVideoLiveHistory()
                            .getPlatformStreamName()
                            .equalsIgnoreCase(platformStreamName);
                    return isCorrectedPlatform;
                })
                .toList();

        if (videoListParsed.isEmpty()) {
            return;
        }

        throw new LiveException("channel.has.already.a.live.active", new Object[]{channelName});
    }

    private ResponseEntity<Map<String, String>> streamingVideo(VideoLiveHistory videoLiveHistory, String streamingUrl) {
        Long videoLiveHistoryId = videoLiveHistory.getPkid();
        List<Video> videoList = findAllVideoToStreamOnDirectory(videoLiveHistoryId);

        taskExecutor.execute(() -> {

            videoList.forEach((video -> {
                String inputPath = video.getVideoPath();
                Integer videoKey = video.getPkid();
                VideoSetting videoSetting = video.getVideoSetting();
                startVideoStreaming(streamingUrl, inputPath, videoSetting, videoLiveHistoryId, videoKey);
            }));
        });

        return responseHandler.buildResponse("live.started", HttpStatus.ACCEPTED);

    }

    @Transactional
    private List<Video> findAllVideoToStreamOnDirectory(Long videoLiveHistoryId) {
        return videoRepository.findByVideoLiveHistory_pkid(videoLiveHistoryId)
                .filter(videos -> !videos.isEmpty())
                .orElseThrow(()-> {
                    log.error("video.streaming.not.found");
                    return new NotFoundCustomException("video.streaming.not.found");
                });
    }

    private void startVideoStreaming(String twitchUrl,
                                     String inputPath,
                                     VideoSetting videoSetting,
                                     Long videoLiveHistoryId,
                                     Integer videoKey
                                     ){
        try {
            org.hibernate.Hibernate.initialize(videoSetting.getVideoSettingsOptions());
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath);
            log.info("INPUT: working directory = {}", inputPath);
            log.info("FORMAT: of grabber on running machine = {}",  grabber.getPixelFormat());

            //for windows device
            Integer pixelFormatOfGrabber = grabber.getPixelFormat();
            Integer pixelFormatOfVideoSetting = videoSetting.getPixelFormat();

            if(!pixelFormatOfGrabber.equals(pixelFormatOfVideoSetting) ){
                grabber.setPixelFormat(pixelFormatOfVideoSetting);
            }

            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            int audioChannels = grabber.getAudioChannels();
            double fps = grabber.getFrameRate();

            boolean skipIsLiveEnded = false;

            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(twitchUrl, width, height, audioChannels);
            settingFFmpegFrameRecorder(recorder, videoSetting, fps);

            recorder.start();
            log.info(loggerMessageComponent.printMessage("video.live.started", new Object[]{inputPath}));
            LocalDateTime dateTime = ZonedDateTime.now().toLocalDateTime();
            saveMessageOnVideoLiveHistory(loggerMessageComponent.printMessage("video.live.started", new Object[]{inputPath}), videoLiveHistoryId, inputPath, "LIVE", dateTime);

            try {
                Frame frame;
                long startTimeNanos = System.nanoTime();
                log.info("start time->{}", startTimeNanos);
                boolean shouldBeStopped = false;
                int frameCounter = 0;

                while ((frame = grabber.grab()) != null) {
                    long framePtsMicros = grabber.getTimestamp();
                    long targetTimeNanos = startTimeNanos + (framePtsMicros * 1000L);
                    // i chceked if the video should be stopped each 50 fps
                    if (frameCounter % 50 == 0) {
                        shouldBeStopped = checkIfFlagStopExists(videoKey);
                    }
                    frameCounter++;

                    if (shouldBeStopped) {
                        recorder.stop();
                        String message = loggerMessageComponent.printMessage("live.stopped");
                        log.info(message);
                        resetFlag(videoKey);
                        saveMessageOnVideoLiveHistory(message, videoLiveHistoryId, inputPath, "STOPPED", null);
                        skipIsLiveEnded = true;
                        break;
                    }

                    long currentTimeNanos = System.nanoTime();
                    long differenceNanos = targetTimeNanos - currentTimeNanos;

                    if (differenceNanos > 0) {
                       //i use a buffer to correct system imprecision
                        if (differenceNanos > TWO_MILLISECONDS) {
                            LockSupport.parkNanos(differenceNanos - ONE_MILLISECONDS);
                        }

                        /*
                        *    Spin‑wait loop for the final millisecond/microsecond.
                        *    Thread.onSpinWait() in Java 23 hints the CPU that this is a busy‑wait loop, reducing power consumption and wake‑up latency.
                        */
                        while (System.nanoTime() < targetTimeNanos) {
                            Thread.onSpinWait();
                        }
                    }

                    recorder.setTimestamp(framePtsMicros);
                    recorder.record(frame);
                }

                if(!skipIsLiveEnded){
                    String message = loggerMessageComponent.printMessage("video.live.ended");
                    log.info(message);
                    saveMessageOnVideoLiveHistory(message, videoLiveHistoryId, inputPath, "ENDED", null);
                }

            } catch (Exception e) {
                String errorMessage = loggerMessageComponent.printMessage("error.during.streaming.video", new Object[]{inputPath})
                        + "\n"
                        + e.getMessage();
                log.error(errorMessage);
                dateTime = ZonedDateTime.now().toLocalDateTime();
                saveMessageOnVideoLiveHistory(errorMessage, videoLiveHistoryId, inputPath, "ERROR", dateTime);
            } finally {
                cleanup(grabber, recorder);
            }

        }catch(Exception e){
            String errorMessage = loggerMessageComponent.printMessage("error.during.starting.video", new Object[]{inputPath})
                    + "\n"
                    + e.getMessage();
            log.error(errorMessage);
            LocalDateTime dateTime = ZonedDateTime.now().toLocalDateTime();
            saveMessageOnVideoLiveHistory(errorMessage, videoLiveHistoryId, inputPath, "ERROR", dateTime);
        }
    }

    private void settingFFmpegFrameRecorder(FFmpegFrameRecorder recorder, VideoSetting videoSetting, double fps){

        recorder.setFormat(videoSetting.getVideoFormat());
        recorder.setVideoCodec(videoSetting.getVideoCodec());
        recorder.setAudioCodec(videoSetting.getAudioSetting().getAudioCodec());
        recorder.setPixelFormat(videoSetting.getPixelFormat());
        recorder.setFrameRate(fps);
        recorder.setVideoBitrate(videoSetting.getVideoBitrate());
        recorder.setAudioBitrate(videoSetting.getAudioSetting().getAudioBitrate());
        //gop size is the double of fps
        //Twitch strictly requires sending a keyframe every 2 seconds; otherwise, the stream becomes unstable.
        recorder.setGopSize((int) (fps * videoSetting.getGopSize()));
        recorder.setVideoCodecName(videoSetting.getVideoCodecName());

        List<VideoSettingsOption> videoSettingsOptionList = videoSetting.getVideoSettingsOptions();

        videoSettingsOptionList.forEach((videoSettingsOption)->{
            recorder.setVideoOption(videoSettingsOption.getKey(), videoSettingsOption.getValue());
        });
    }

    @Transactional(readOnly = true)
    private boolean checkIfFlagStopExists(Integer videoKey) {
        Video video = videoRepository.findByPkid(videoKey);
        return video.getShouldBeStop();
    }

    @Transactional
    private void saveMessageOnVideoLiveHistory(String message, Long videoLiveHistoryId, String inputPath, String statusLive, LocalDateTime dateTime) {
        Video video = videoRepository.findByVideoPathAndVideoLiveHistory_pkid(inputPath, videoLiveHistoryId);

        if(video == null){
            log.error(loggerMessageComponent.printMessage("error.during.retrieved.video", new Object[]{inputPath, videoLiveHistoryId}));
            return;
        }
        if(dateTime != null)
            video.setStartDateLive(dateTime);

        video.setMessage(message);

        video.setLiveStatus(LiveStatusEnum.valueOf(statusLive));
        videoRepository.save(video);
    }

    @Transactional
    private void saveVideoPaths(String videoPathFolder, VideoLiveHistory videoLiveHistory, VideoSettingsRecord videoSettingsRecord, String channelName) {
        File videoFile = Paths.get(videoPathFolder).toFile();

        VideoSetting videoSetting = videoSettingRecordMapper.toModel(videoSettingsRecord);

        if(videoFile.isDirectory()) {
            saveAllVideoPaths(videoFile, videoLiveHistory, videoSetting, channelName);
        }else{
            saveOneVideoPaths(videoFile, videoLiveHistory, videoSetting, channelName);
        }
    }

   
    private void saveAllVideoPaths(File videoFile, VideoLiveHistory videoLiveHistory, VideoSetting videoSetting, String channelName){

        File[] videoList = videoFile.listFiles(pathname -> {
            if(pathname.isDirectory()){
                return false;
            }
            String fileName = pathname.getName();
            log.info(loggerMessageComponent.printMessage("file.name.found", new Object[]{fileName}));
            int lastDotIndex = fileName.lastIndexOf(".");

            if (lastDotIndex == -1) {
                log.error(loggerMessageComponent.printMessage("extension.not.found.with.param", new Object[]{lastDotIndex}));
                return false;
            }

            String extension = fileName.substring(lastDotIndex + 1);
            return VideoExtensionEnum.isVideoExtensionPresent(extension);
        });


        if(videoList == null || videoList.length == 0){
            log.error(loggerMessageComponent.printMessage("folder.empty", new Object[]{ videoFile.getAbsolutePath()}));
            throw new NotFoundCustomException("folder.empty", new Object[]{ videoFile.getAbsolutePath()});
        }

        for(File file : videoList){
            Video video = Video
                    .builder()
                    .name(file.getName())
                    .videoPath(file.getAbsolutePath())
                    .extension(extractExtensionFile(file.getName()))
                    .lastTimeStampBeforeStop(0L)
                    .liveStatus(LiveStatusEnum.OFFLINE)
                    .videoLiveHistory(videoLiveHistory)
                    .videoSetting(videoSetting)
                    .shouldBeStop(false)
                    .startDateLive(ZonedDateTime.now().toLocalDateTime())
                    .channelName(channelName)
                    .build();
            saveOnModelVideo(video);
        }
    }

    @Transactional(readOnly = true)
    private VideoLiveHistory retrievedVideoLiveHistorySaved(String videoFileAbsolutePath, LocalDateTime timeStartLive) {
        return videoLiveHistoryRepository.findByFolderOfVideoToStreamAndLocalDateTimeStartLive(videoFileAbsolutePath, timeStartLive)
                .orElseThrow( ()->{
                    log.error(loggerMessageComponent.printMessage("video.history.not.found"));
                    return new NotFoundCustomException("video.history.not.found");
                });
    }

    // for now user is always Mario
    @Transactional
    private void saveVideoLiveHistory(String videoFileAbsolutePath,
                                      LocalDateTime zoneIdTime,
                                      String streamUrl,
                                      String streamKey,
                                      String platformStreamingName
                                      ) {
        VideoLiveHistory videoLiveHistory = VideoLiveHistory
                .builder()
                .userName("Mario")
                .folderOfVideoToStream(videoFileAbsolutePath)
                .localDateTimeStartLive(zoneIdTime)
                .streamUrl(streamUrl)
                .streamKey(streamKey)
                .platformStreamName(platformStreamingName)
                .build();

        videoLiveHistoryRepository.save(videoLiveHistory);
    }

    private void saveOneVideoPaths(File videoFile, VideoLiveHistory videoLiveHistory, VideoSetting videoSetting, String channelName){
       

        Video video = Video
                .builder()
                .name(videoFile.getName())
                .videoPath(videoFile.getAbsolutePath())
                .extension(extractExtensionFile(videoFile.getName()))
                .lastTimeStampBeforeStop(0L)
                .liveStatus(LiveStatusEnum.OFFLINE)
                .videoLiveHistory(videoLiveHistory)
                .videoSetting(videoSetting)
                .shouldBeStop(false)
                .startDateLive(ZonedDateTime.now().toLocalDateTime())
                .channelName(channelName)
                .build();
        saveOnModelVideo(video);

    }

    private void saveOnModelVideo(Video video){
        videoRepository.save(video);
    }

    private String extractExtensionFile(String fileName){
        if(fileName == null){
            log.error(loggerMessageComponent.printMessage("extension.not.found"));
            throw new NotFoundCustomException("extension.not.found");
        }

        String[] files = fileName.split("\\.");

        log.info("nome file" + fileName);

        if(files.length < 2){
            throw new FileReadingException("video.not.valid", new Object[]{fileName});
        }

        return files[files.length - 1];
    }

    public void stopVideoStreamingByPkid(Integer videoLivePkid) {
        Video video = checkIfExistsAndReturnEntity(videoLivePkid);
        video.setShouldBeStop(true);
        saveFlagToStopLive(video);
    }

    public void resetFlag(Integer videoLivePkid) {
        Video video = checkIfExistsAndReturnEntity(videoLivePkid);
        video.setShouldBeStop(false);
        saveFlagToStopLive(video);
    }

    @Transactional(readOnly = true)
    protected Video checkIfExistsAndReturnEntity(Integer pkid){
        Video video =  videoRepository.findByPkid(pkid);
        if(video == null){
            log.warn(loggerMessageComponent.printMessage("video.not.found"));
            throw new NotFoundCustomException("video.not.found");
        }
        return video;
    }

    @Transactional
    protected void saveFlagToStopLive(Video video){
        videoRepository.save(video);
    }

    public ResponseEntity<Map<String, String>> startVideo(VideoRecord videoRecord){
        VideoLiveHistoryRecord videoLiveHistoryPkid = videoRecord.videoLiveHistory();
        StartLiveRecord startLiveRecord = fromVideoRecordToStartLiveRecordMapper.toStartLiveRecord(videoRecord);
        return startLiveWithoutSaveOnModel(startLiveRecord, videoLiveHistoryPkid);
    }

    /*
    / this method is used for play o restart a single video , with video options
      and video details already saved on db
     */
    private ResponseEntity<Map<String, String>> startLiveWithoutSaveOnModel(StartLiveRecord startLiveRecord, VideoLiveHistoryRecord videoLiveHistoryRecord){

        String channelName = startLiveRecord.channelName();
        String platformStreamName = startLiveRecord.platformStreamName();
        checkIfALiveAlreadyStreamingForAChannel(channelName, platformStreamName);

        String videoPathFolder = startLiveRecord.videoPath();
        String streamKey = startLiveRecord.streamKey();
        String streamUrl = startLiveRecord.streamUrl();


        LocalDateTime timeStartLive = ZonedDateTime.now().toLocalDateTime();
        saveVideoLiveHistory(videoPathFolder, timeStartLive, streamUrl, streamKey, platformStreamName);

        VideoLiveHistory videoLiveHistory = videoLiveHistoryRecordMapper.toModel(videoLiveHistoryRecord);

        String streamingUrl;

        if(streamUrl.endsWith("/")){
            streamingUrl =  streamUrl.concat(streamKey);
        }else{
            streamingUrl =  streamUrl.concat("/").concat(streamKey);
        }

        return streamingVideo(videoLiveHistory, streamingUrl);
    }

    //to_DO see if if possible
    public List<Video> stopLives(String channelName, String platformStream) {

        var videoToStoppedList = getAllVideoLiveForChannelNameAndPlatformName(channelName, platformStream);
        if(videoToStoppedList == null || videoToStoppedList.isEmpty())
            return null;
        videoToStoppedList.forEach(this::saveFlagToStopLive);

        return videoToStoppedList;
    }

    @Transactional(readOnly = true)
    protected List<Video> getAllVideoLiveForChannelNameAndPlatformName(String channelName, String platformStreamName) {
        List<Video> videoList = videoRepository
                .findByLiveStatusAndChannelName(LiveStatusEnum.LIVE, channelName);

        if (videoList.isEmpty()) {
            return null;
        }

        List<Video> videoListParsed = videoList.stream()
                .filter((video)-> {
                    boolean isCorrectedPlatform = video
                            .getVideoLiveHistory()
                            .getPlatformStreamName()
                            .equalsIgnoreCase(platformStreamName);
                    return isCorrectedPlatform;
                })
                .toList();

        if (videoListParsed.isEmpty()) {
            return null;
        }

        return videoListParsed;
    }

}

