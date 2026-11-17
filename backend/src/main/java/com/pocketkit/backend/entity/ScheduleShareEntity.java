package com.pocketkit.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schedule_shares")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduleShareEntity {

    @Id
    @Column(length = 12)
    private String code;

    private Long ownerUserId;

    @Column(length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String slotsJson;

    @Column(nullable = false)
    private long createdAtEpoch;

    @Column(nullable = false)
    private long expiresAtEpoch;
}
