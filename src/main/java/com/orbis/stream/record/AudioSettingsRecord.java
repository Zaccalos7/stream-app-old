package com.orbis.stream.record;

public record AudioSettingsRecord(
        Integer audioCodec,
        Integer audioBitrate
) {
}
