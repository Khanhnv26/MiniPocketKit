package com.example.pocketkit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {
    private String fullName;
    private String majorAndCohort;
    private int earnedCredits;
    private int totalCredits;
    private int attendancePercent;
    private float baselineGpa;
    private int currentTermCredits;
}
