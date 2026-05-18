package com.orbis.stream.record;

import java.time.LocalDateTime;


public record VideoSettingsRecord(
        Integer id,
        String title,
        Integer videoCodec,
        String videoCodecName,
        Integer pixelFormat,
        Integer videoBitrate,
        LocalDateTime lastModified,
        Boolean isVideoAndAudioSettingActive,
        Integer gopSize,
        VideoOptionRecord[] videoOptions,
        String videoFormat,
        AudioSettingsRecord audioSettingRecord
) {
}
