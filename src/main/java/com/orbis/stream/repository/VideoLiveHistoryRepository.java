package com.orbis.stream.repository;

import com.orbis.stream.model.VideoLiveHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoLiveHistoryRepository extends JpaRepository<VideoLiveHistory, Long> {
    Optional<VideoLiveHistory> findByFolderOfVideoToStreamAndLocalDateTimeStartLive(String folder, LocalDateTime startLiveDate);

}
