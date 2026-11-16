package com.pocketkit.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public final class ScheduleShareDtos {

    private ScheduleShareDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateShareRequest {
        private String title;

        @Valid
        @NotEmpty(message = "Danh sách ca học không được trống")
        private List<BackupDtos.ScheduleSlotDto> slots;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreateShareResponse {
        private final String code;
        private final long expiresAtEpoch;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ShareDetailResponse {
        private final String code;
        private final String title;
        private final long expiresAtEpoch;
        private final List<BackupDtos.ScheduleSlotDto> slots;
    }
}
