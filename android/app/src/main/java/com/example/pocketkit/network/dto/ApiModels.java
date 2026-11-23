package com.example.pocketkit.network.dto;

import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class ApiModels {

    private ApiModels() {
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RegisterRequest {
        private String email;
        private String password;
        private String fullName;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LoginRequest {
        private String email;
        private String password;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String accessToken;
        private long expiresInMs;
        private UserSummary user;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String email;
        private String fullName;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BackupPayload {
        private int simulatedScoreProgress;
        private StudentProfile profile;
        private List<String> categories;
        private List<ScheduleSlot> schedules;
        private List<StudyTask> tasks;
        private List<Note> notes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackupSnapshotResponse {
        private long backedUpAtEpoch;
        private BackupPayload payload;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OcrResponse {
        private String text;
        private String language;
        private String fileName;
        private long sizeBytes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoteSummarizeRequest {
        private String title;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteSummarizeResponse {
        private String title;
        private String summary;
        private String formattedContent;
        private List<String> keyPoints;
        private boolean aiPowered;
    }

}
