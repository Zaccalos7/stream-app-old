package com.orbis.stream.record;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record VideoLiveHistoryRecord(
        Long pkid,
        String folderOfVideoToStream,
        LocalDateTime localDateTimeStartLive,
        String streamUrl,
        String streamKey,
        String platformStreamName,
        String userName) {
}
