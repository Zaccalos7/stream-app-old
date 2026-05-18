package com.orbis.stream.model;

import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.time.ZoneId;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class VideoLiveHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pkid;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String folderOfVideoToStream;

    @Column(nullable = false)
    private LocalDateTime localDateTimeStartLive;

    @Column(nullable = false, length = 512)
    @ColumnDefault("N/A")
    private String streamUrl;

    @Column(nullable = false, length = 512)
    @ColumnDefault("N/A")
    private String streamKey;

    @Column(nullable = false)
    @ColumnDefault("N/A")
    private String platformStreamName;

    private String userName;
}
