package com.orbis.stream.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VideoSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "varchar(255) default 'Title'")
    private String title;

    private Integer videoCodec;
    private String videoCodecName;
    private Integer pixelFormat;
    private Integer videoBitrate;
    private String videoFormat;
    private LocalDateTime lastModified;

    @NotNull
    @ColumnDefault("2")
    private Integer gopSize;



    @NotNull
    @ColumnDefault("False")
    private Boolean isVideoAndAudioSettingActive;

    @ElementCollection
    @CollectionTable(
            name = "video_settings_options",
            joinColumns = @JoinColumn(name = "video_setting_id")
    )
    private List<VideoSettingsOption> videoSettingsOptions;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "audio_setting_id")
    private AudioSetting audioSetting;

}
