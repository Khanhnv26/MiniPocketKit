package com.pocketkit.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public final class BackupDtos {

    private BackupDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BackupPayload {
        @Min(0)
        @Max(100)
        private int simulatedScoreProgress;

        @Valid
        @NotNull(message = "Thiếu thông tin hồ sơ")
        private ProfileDto profile;

        @NotNull
        @Builder.Default
        private List<String> categories = new ArrayList<>();

        @Valid
        @NotNull
        @Builder.Default
        private List<ScheduleSlotDto> schedules = new ArrayList<>();

        @Valid
        @NotNull
        @Builder.Default
        private List<StudyTaskDto> tasks = new ArrayList<>();

        @Valid
        @NotNull
        @Builder.Default
        private List<NoteDto> notes = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BackupSnapshotResponse {
        private long backedUpAtEpoch;
        private BackupPayload payload;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProfileDto {
        @NotBlank
        private String fullName;
        private String majorAndCohort;
        @Min(0)
        private int earnedCredits;
        @Min(1)
        private int totalCredits;
        @Min(0)
        @Max(100)
        private int attendancePercent;
        private float baselineGpa;
        @Min(0)
        private int currentTermCredits;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ScheduleSlotDto {
        @Min(2)
        @Max(7)
        private int dayOfWeek;

        @NotBlank
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "startTime phải có định dạng HH:mm")
        private String startTime;

        @NotBlank
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "endTime phải có định dạng HH:mm")
        private String endTime;

        @NotBlank
        private String subject;
        private String room;
        private String lecturer;
        private String note;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StudyTaskDto {
        @NotBlank
        private String title;
        private String subject;
        private String tag;
        private long dueAtMillis;
        private boolean done;
        private long createdAtMillis;
        private int remindBeforeMinutes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class NoteDto {
        @NotBlank
        private String title;
        private String content;
        private String subject;
        private int isPinned;
        private int isDone;
        private String updatedAt;
    }
}
