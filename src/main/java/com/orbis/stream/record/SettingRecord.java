package com.orbis.stream.record;

import jakarta.validation.constraints.NotNull;

public record SettingRecord(
        @NotNull(message = "not.valid.input")  String streamUrl,
        @NotNull(message = "not.valid.input")  String streamKey,
        @NotNull(message = "not.valid.input")  String platformStreamName,
        @NotNull(message = "not.valid.input")  String description,
        @NotNull(message = "not.valid.input")  String videoFolder,
        Boolean isActive,
        @NotNull(message = "not.valid.input") String channelName) {
}
