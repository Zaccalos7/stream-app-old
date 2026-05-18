package com.orbis.stream.record;

import jakarta.validation.constraints.NotNull;

public record StartLiveRecord(
        @NotNull(message = "not.valid.input")
        String streamUrl,
        @NotNull(message = "not.valid.input")
        String streamKey,
        @NotNull(message = "not.valid.input")
        String videoPath,
        @NotNull(message = "input.not.valid")
        String platformStreamName,
        @NotNull(message = "input.not.valid")
        String channelName,
        VideoSettingsRecord videoSettingsRecord
) {
}
