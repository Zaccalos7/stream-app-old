package com.orbis.stream.repository;

import com.orbis.stream.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Integer>, JpaSpecificationExecutor<Setting> {
    Optional<Setting> findByStreamUrlAndStreamKey(String streamUrl, String StreamKey);
}
