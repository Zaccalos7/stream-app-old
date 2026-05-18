package com.orbis.stream.model;

import com.orbis.stream.enums.LiveStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pkid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String videoPath;

    @Column(nullable = false)
    private String extension;

    @ColumnDefault("1")
    private LiveStatusEnum liveStatus;

    @ColumnDefault("0")
    private Long lastTimeStampBeforeStop;

    @Column(columnDefinition = "TEXT")
    private String message;

    @ColumnDefault("false")
    private Boolean shouldBeStop;

    private LocalDateTime startDateLive;

    @Column(nullable = false, length = 512)
    @ColumnDefault("Zingy")
    private String channelName;

    @ManyToOne
    private VideoLiveHistory videoLiveHistory;


    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private VideoSetting videoSetting;
}
