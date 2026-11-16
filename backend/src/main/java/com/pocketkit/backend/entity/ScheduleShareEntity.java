package com.pocketkit.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schedule_shares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
