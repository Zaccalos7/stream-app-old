package com.orbis.stream.model;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;


@Embeddable
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoSettingsOption {

    @Column(name = "option_key")
    private String key;

    @Column(name = "option_value")
    private String value;
}
