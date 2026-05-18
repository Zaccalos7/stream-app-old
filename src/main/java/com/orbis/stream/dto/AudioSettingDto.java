package com.orbis.stream.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AudioSettingDto {
    private Integer audioCodec;
    private Integer audioBitrate;

}
