package com.orbis.stream.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
public class SettingDto {
    private Integer id;

    private String streamUrl;

    private String streamKey;

    private String platformStreamName;

    private String description;

    private String videoFolder;

    private Integer gopSize;

    private LocalDateTime lastModified;

    private Boolean isActive;

    private String channelName;

}
