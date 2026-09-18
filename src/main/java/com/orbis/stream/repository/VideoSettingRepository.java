package com.orbis.stream.repository;

import com.orbis.stream.model.VideoSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;


public interface VideoSettingRepository extends JpaRepository<VideoSetting, Integer>, JpaSpecificationExecutor<VideoSetting> {
    Optional<VideoSetting> findByDefaultPlatformConfiguration(String defaultPlatformConfiguration);
}
