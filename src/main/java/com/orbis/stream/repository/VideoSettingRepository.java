package com.orbis.stream.repository;

import com.orbis.stream.model.VideoSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;


public interface VideoSettingRepository extends JpaRepository<VideoSetting, Integer>, JpaSpecificationExecutor<VideoSetting> {
    List<VideoSetting> findByIsDefaultConfigurationTrueAndDefaultPlatformConfiguration(String defaultPlatformConfiguration);
}
