package com.orbis.stream.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoSettingsOptionDto {
    private String key;

    private String value;
}
