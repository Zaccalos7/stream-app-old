package com.orbis.stream.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DialectOverride;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"streamUrl", "streamKey"})
        }
)
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 512)
    private String streamUrl;

    @Column(nullable = false, length = 512)
    private String streamKey;

    private String platformStreamName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ColumnDefault("'/'")
    @Column(nullable = false)
    private String videoFolder;

    @ColumnDefault("false")
    private Boolean isActive;

    @Column(nullable = false, columnDefinition = "TEXT")
    @ColumnDefault("Zingy")
    private String channelName;

    @ManyToOne
    private User user;
}
