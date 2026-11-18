package com.pocketkit.backend.dto;

import com.pocketkit.backend.dto.BackupDtos.ScheduleSlotDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @AllArgsConstructor
    @Builder
    public static class CreateShareRequest {

        @Size(max = 120, message = "Tiêu đề chia sẻ không được vượt quá 120 ký tự")
        private String title;

        @Valid
        @NotEmpty(message = "Danh sách ca học không được trống")
        @Size(max = 200, message = "Số lượng ca học chia sẻ tối đa là 200")
        private List<@NotNull(message = "Thông tin ca học không được null") @Valid ScheduleSlotDto> slots;
    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class CreateShareResponse {
        private final String code;
        private final long expiresAtEpoch;
    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class ShareDetailResponse {
        private final String code;
        private final String title;
        private final long expiresAtEpoch;
        private final List<ScheduleSlotDto> slots;
    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class ShareSummaryResponse {
        private final String code;
        private final String title;
        private final long createdAtEpoch;
        private final long expiresAtEpoch;
        private final boolean expired;
    }
}
