package com.orbis.stream.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AudioSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer audioCodec;
    private Integer audioBitrate;


    @OneToOne(mappedBy = "audioSetting")
    private VideoSetting videoSetting;
}
