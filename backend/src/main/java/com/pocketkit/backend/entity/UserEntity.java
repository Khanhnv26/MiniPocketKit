package com.pocketkit.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = true)
    private String passwordHash;

    @Column(unique = true, length = 128)
    private String googleSub;

    @Column(length = 32)
    private String authProvider;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false)
    private long createdAtEpoch;
}
