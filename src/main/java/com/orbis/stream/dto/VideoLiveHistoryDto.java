package com.orbis.stream.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoLiveHistoryDto {
    private Long pkid;

    private String folderOfVideoToStream;

    private LocalDateTime localDateTimeStartLive;

    private String userName;

    private String streamUrl;

    private String streamKey;

    private String platformStreamName;

}
