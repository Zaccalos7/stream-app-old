package com.orbis.stream.service;

import com.orbis.stream.component.LoggerMessageComponent;
import com.orbis.stream.controller.filter.DynamicSpecificationBuilder;
import com.orbis.stream.dto.VideoDto;
import com.orbis.stream.enums.LiveStatusEnum;
import com.orbis.stream.exceptions.NotFoundCustomException;
import com.orbis.stream.handler.ResponseHandler;
import com.orbis.stream.mapping.mapperDTO.VideoMapper;
import com.orbis.stream.mapping.mapperRECORD.VideoRecordMapper;
import com.orbis.stream.model.Video;
import com.orbis.stream.record.VideoRecord;
import com.orbis.stream.record.output.ChannelRecord;
import com.orbis.stream.record.output.VideoPathAndKey;
import com.orbis.stream.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;

    private final VideoMapper videoMapper;
    private final VideoRecordMapper videoRecordMapper;

    private final TaskExecutor taskExecutor;

    private final ResponseHandler responseHandler;

    private final LoggerMessageComponent loggerMessageComponent;

    @Transactional(readOnly = true)
    public Page<VideoDto> getVideoList (Pageable pageable){
        Page<VideoDto> videoDtoPage = videoRepository
                .findAll(pageable)
                .map(videoMapper::toDto);
        log.info(loggerMessageComponent.printMessage("recovered.video.page"));
        return videoDtoPage;
    }

    public Page<VideoRecord> getAllVideoList(Map<String, String> filtersMap, Pageable pageable) {
        Page<VideoRecord> videoList = getAllVideoByFilters(filtersMap, pageable);
        log.info(loggerMessageComponent.printMessage("recovered.video"));
        return videoList;
    }

    @Transactional(readOnly = true)
    private Page<VideoRecord> getAllVideoByFilters(Map<String, String> filtersMap, Pageable pageable){
        Specification<Video> dynamicSpecification = DynamicSpecificationBuilder.buildSpecification(filtersMap);
        return videoRepository.findAll(dynamicSpecification, pageable)
                .map(videoRecordMapper::toDto);
    }

    public List<ChannelRecord> getAllChannelOnline() {
        List<Video> videoToStopList = findByLiveStatus();
        return mappingToChannelRecord(videoToStopList);
    }

    @Transactional(readOnly = true)
    protected List<Video> findByLiveStatus(){
        List<Video> videoList = videoRepository.findByLiveStatus(LiveStatusEnum.LIVE)
                .orElseThrow(()-> new NotFoundCustomException("nothing.to.stop"));
        log.info("found video to stop");
        return videoList;
    }

    private List<ChannelRecord> mappingToChannelRecord(List<Video> videoList){
        Set<String> videoChannelNameSet = videoList.stream()
                .map(Video::getChannelName)
                .collect(Collectors.toSet());

        return createChannelRecordList(videoChannelNameSet, videoList);
    }

    private List<ChannelRecord> createChannelRecordList(Set<String> videoChannelNameSet, List<Video> videoList){
        List<ChannelRecord> channelRecordList = new ArrayList<>();

        for(String channelName: videoChannelNameSet){
            ChannelRecord channelRecord = ChannelRecord.builder()
                    .channelName(channelName)
                    .videoPathAndKeyList(createVideoPathArray(channelName, videoList))
                    .build();

            channelRecordList.add(channelRecord);
        }
        log.info("created list of ChannelRecord");
        return channelRecordList;
    }

    private List<VideoPathAndKey> createVideoPathArray(String channelName, List<Video> videoList){
        return videoList.stream()
                .filter((video)-> Objects.equals(video.getChannelName(), channelName))
                .map((video)-> {
                    VideoPathAndKey videoPathAndKey = VideoPathAndKey.builder()
                            .videoPath(video.getVideoPath())
                            .videoKey(video.getPkid())
                            .build();
                    return videoPathAndKey;
                })
                .toList();

    }

    public ResponseEntity<Map<String, String>> unlockVideo(Integer videoKey) {
        Video video = findVideoToUnlock(videoKey);

        taskExecutor.execute(() -> {
            tryToStopVideoWithFlag(video);
            //i tried to stop, if the user unlock a live in streaming
            boolean videoUnlocked = checkThreeTimes(video.getPkid());
            if(videoUnlocked){
                return;
            }
            //if video is really unlock i made this
            changeStatusLiveToOffline(video);
        });

        return responseHandler.build("unlock.in.progress", HttpStatus.ACCEPTED, null);
    }


    @Transactional(readOnly = true)
    protected Video findVideoToUnlock(Integer videoKey) {
        Video video = videoRepository.findByPkid(videoKey);
        if(video == null){
            log.error(loggerMessageComponent.printMessage("video.not.found"));
            throw new NotFoundCustomException("video.not.found");
        }
        return video;
    }

    @Transactional
    private void tryToStopVideoWithFlag(Video video) {
        video.setShouldBeStop(true);
        videoRepository.save(video);
    }

    private boolean checkThreeTimes(Integer videoPkid)   {
        boolean isStooped;
        for(int i=0 ; i<3 ; i++){
            isStooped = checkIfVideoGoOffline(videoPkid);

            if(isStooped)
                return true;

            try{
                Thread.sleep(1000);
            }catch (InterruptedException ex){
                log.error("Thread spleep failed {}", ex.getMessage());
            }

        }

        return false;
    }

    @Transactional(readOnly = true)
    protected boolean checkIfVideoGoOffline(Integer videoPkid){
        Video video = videoRepository.findByPkid(videoPkid);

        if(video == null){
            log.error(loggerMessageComponent.printMessage("video.not.found"));
            throw new NotFoundCustomException("video.not.found");
        }

        return Objects.equals(video.getLiveStatus().getValue(), "OFFLINE");
    }

    @Transactional
    private void changeStatusLiveToOffline(Video video) {
        video.setLiveStatus(LiveStatusEnum.OFFLINE);
        videoRepository.save(video);
    }
}
