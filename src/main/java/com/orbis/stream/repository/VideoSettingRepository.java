package com.orbis.stream.repository;

import com.orbis.stream.model.VideoSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VideoSettingRepository extends JpaRepository<VideoSetting, Integer>, JpaSpecificationExecutor<VideoSetting> {

}
