package com.orbis.stream.record;

import com.orbis.stream.enums.LiveStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
public record VideoRecord(
        @NotNull(message = "input.not.valid") Integer pkid,
        @NotNull(message = "input.not.valid")  String name,
        @NotNull(message = "input.not.valid")  String videoPath,
        @NotNull(message = "input.not.valid")  String extension,
        @NotNull(message = "input.not.valid")  LiveStatusEnum liveStatus,
        Long lastTimeStampBeforeStop,
        String message,
        @NotNull(message = "input.not.valid") VideoLiveHistoryRecord videoLiveHistory,
        @NotNull(message = "input.not.valid") VideoSettingsRecord videoSetting,
        Boolean shouldBeStop,
        LocalDateTime startDateLive,
        @NotNull(message = "input.not.valid") String channelName
) {
}
