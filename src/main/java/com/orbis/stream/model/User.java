package com.orbis.stream.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pkid;

    @Column(nullable = false, unique = true)
    private String nickName;

    @OneToOne(cascade = CascadeType.ALL)
    private Credential credential;
}

