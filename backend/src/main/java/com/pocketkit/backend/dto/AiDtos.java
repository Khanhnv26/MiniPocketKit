package com.pocketkit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

public final class AiDtos {

    private AiDtos () {

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class NoteSummarizeRequest {
        @Size(max = 200, message = "Tiêu đề ghi chú không được vượt quá 200 ký tự")
        private String title;
        @NotBlank(message = "Nội dung cần tóm tắt không được để trống")
        @Size(max = 20000, message = "Nội dung ghi chú không được vượt quá 20000 ký tự")
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    private static class NoteSummarizeResponse {
        private final String title;
        private final String summary;
        private final String formattedContent;
        private final List<String> keypoints;
        private final boolean aiPowered;

    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class ScheduleScanResponse{
        private final List<ScheduleShareDtos> slots;
        private final int skippedCount;
        private final boolean aiPowered;
    }


}
