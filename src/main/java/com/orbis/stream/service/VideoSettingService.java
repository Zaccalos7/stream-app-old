package com.orbis.stream.service;

import com.orbis.stream.component.LoggerMessageComponent;
import com.orbis.stream.controller.filter.DynamicSpecificationBuilder;
import com.orbis.stream.exceptions.NotFoundCustomException;
import com.orbis.stream.handler.ResponseHandler;
import com.orbis.stream.mapping.mapperRECORD.VideoSettingRecordMapper;
import com.orbis.stream.model.Video;
import com.orbis.stream.model.VideoSetting;
import com.orbis.stream.record.VideoSettingsRecord;
import com.orbis.stream.repository.VideoRepository;
import com.orbis.stream.repository.VideoSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Service
@Slf4j
public class VideoSettingService {

    private final VideoSettingRecordMapper videoSettingRecordMapper;

    private final VideoSettingRepository videoSettingRepository;
    private final VideoRepository videoRepository;

    private final LoggerMessageComponent loggerMessageComponent;
    private final ResponseHandler responseHandler;

    public ResponseEntity<Map<String,String>> saveSettingsVideo(VideoSettingsRecord videoSettingsRecord){
        mappingAndSaveValue(videoSettingsRecord);
        log.info(loggerMessageComponent.printMessage("video.settings.saved"));

        return responseHandler.build("video.settings.saved", HttpStatus.CREATED, null);
    }

    @Transactional
    private void mappingAndSaveValue(VideoSettingsRecord videoSettingsRecord){
        VideoSetting videoSetting =  videoSettingRecordMapper.toModel(videoSettingsRecord);
        LocalDateTime localDate = ZonedDateTime.now().toLocalDateTime();
        videoSetting.setLastModified(localDate);
        videoSettingRepository.save(videoSetting);
    }

    public ResponseEntity<Map<String,String>> linkAndSaveSettingsVideo(VideoSettingsRecord videoSettingsRecord, Integer videoPkidToLink){
        Video video = findVideoByPkid(videoPkidToLink);
        log.info(loggerMessageComponent.printMessage("video.to.link.found"));

        mappingAndSaveValueAndLinkVideo(videoSettingsRecord, video);
        log.info(loggerMessageComponent.printMessage("video.settings.saved"));

        log.info(loggerMessageComponent.printMessage("success.operations"));
        return responseHandler.build("success.operations", HttpStatus.CREATED, null);
    }


    @Transactional(readOnly = true)
    protected Video findVideoByPkid(Integer videoPkidToLink){
        Video video = videoRepository.findByPkid(videoPkidToLink);
        if(video == null){
            throw new NotFoundCustomException("video.to.link.not.found");
        }
        return video;
    }

    @Transactional
    private void mappingAndSaveValueAndLinkVideo(VideoSettingsRecord videoSettingsRecord, Video videoToLink){
        VideoSetting videoSetting =  videoSettingRecordMapper.toModel(videoSettingsRecord);
        LocalDateTime localDate = ZonedDateTime.now().toLocalDateTime();
        videoSetting.setLastModified(localDate);

        VideoSetting videoSettingSaved = videoSettingRepository.save(videoSetting);
        videoToLink.setVideoSetting(videoSettingSaved);
        videoRepository.save(videoToLink);
    }

    public List<VideoSettingsRecord> getAllVideoSettings(Map<String, String> filtersMap) {
       return retrivesVideoSettings(filtersMap);
    }

    @Transactional(readOnly = true)
    private List<VideoSettingsRecord> retrivesVideoSettings(Map<String, String> filtersMap) {
        Specification<VideoSetting> dynamicFilteringSpecification =
                DynamicSpecificationBuilder.buildSpecification(filtersMap);

        return videoSettingRepository.findAll(dynamicFilteringSpecification)
                .stream()
                .map(videoSettingRecordMapper::toDto)
                .toList();
    }

    public ResponseEntity<Map<String,String>> editSettingsVideo(VideoSettingsRecord videoSettingsRecord, Integer id){
        editVideoSetting(videoSettingsRecord, id);
        log.info(loggerMessageComponent.printMessage("video.settings.modified"));

        return responseHandler.build("video.settings.modified", HttpStatus.CREATED, null);
    }

    @Transactional
    private void editVideoSetting(VideoSettingsRecord videoSettingsRecord, Integer id) {
        checkIfVideoSettingExist(id);

        VideoSetting videoSetting = videoSettingRecordMapper.toModel(videoSettingsRecord);
        LocalDateTime localDate = ZonedDateTime.now().toLocalDateTime();
        videoSetting.setLastModified(localDate);
        videoSettingRepository.save(videoSetting);
    }

    private void checkIfVideoSettingExist(Integer id){
        videoSettingRepository.findById(id).orElseThrow(()->{
            log.error(loggerMessageComponent.printMessage("video.settings.not.found"));
            return new NotFoundCustomException("video.settings.not.found");
        });
    }

    public ResponseEntity<Map<String, String>> deleteVideoSetting(Integer id) {
        deleteById(id);
        log.info(loggerMessageComponent.printMessage("delete.successful"));
        return responseHandler.build("delete.successful", HttpStatus.OK, null);
    }

    private void deleteById(Integer id){
        videoSettingRepository.deleteById(id);
    }
}
