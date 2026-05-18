package com.orbis.stream.dto;

import com.orbis.stream.enums.LiveStatusEnum;
import lombok.*;

import java.time.LocalDateTime;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDto {
    private Integer pkid;

    private String name;

    private String videoPath;

    private String extension;

    private LiveStatusEnum liveStatus;

    private Long lastTimeStampBeforeStop;

    private String message;

    private Boolean shouldBeStop;

    private LocalDateTime startDateLive;

    private String channelName;
}
