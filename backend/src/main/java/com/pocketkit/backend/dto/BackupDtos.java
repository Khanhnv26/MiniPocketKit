package com.pocketkit.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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

        @Min(value = 0, message = "Tiến độ điểm mô phỏng phải từ 0 đến 100")
        @Max(value = 100, message = "Tiến độ điểm mô phỏng phải từ 0 đến 100")
        private int simulatedScoreProgress;

        @Valid
        @NotNull(message = "Thiếu thông tin hồ sơ")
        private ProfileDto profile;

        @NotNull(message = "Danh sách danh mục không được null")
        @Size(max = 100, message = "Số lượng danh mục tối đa là 100")
        @Builder.Default
        private List<
                @NotBlank(message = "Tên danh mục không được để trống")
                @Size(max = 60, message = "Tên danh mục không được vượt quá 60 ký tự")
                String
        > categories = new ArrayList<>();

        @Valid
        @NotNull(message = "Danh sách lịch học không được null")
        @Size(max = 500, message = "Số lượng lịch học tối đa là 500")
        @Builder.Default
        private List<@NotNull(message = "Thông tin lịch học không được null") @Valid ScheduleSlotDto> schedules = new ArrayList<>();

        @Valid
        @NotNull(message = "Danh sách công việc không được null")
        @Size(max = 1000, message = "Số lượng công việc tối đa là 1000")
        @Builder.Default
        private List<@NotNull(message = "Thông tin công việc không được null") @Valid StudyTaskDto> tasks = new ArrayList<>();

        @Valid
        @NotNull(message = "Danh sách ghi chú không được null")
        @Size(max = 1000, message = "Số lượng ghi chú tối đa là 1000")
        @Builder.Default
        private List<@NotNull(message = "Thông tin ghi chú không được null") @Valid NoteDto> notes = new ArrayList<>();
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
    @AllArgsConstructor
    @Builder
    public static class ProfileDto {

        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 120, message = "Họ và tên không được vượt quá 120 ký tự")
        private String fullName;

        @Size(max = 120, message = "Ngành và khóa học không được vượt quá 120 ký tự")
        private String majorAndCohort;

        @Min(value = 0, message = "Số tín chỉ đã tích lũy không được âm")
        @Max(value = 400, message = "Số tín chỉ đã tích lũy không được vượt quá 400")
        private int earnedCredits;

        @Min(value = 1, message = "Tổng số tín chỉ tối thiểu phải là 1")
        @Max(value = 400, message = "Tổng số tín chỉ không được vượt quá 400")
        private int totalCredits;

        @Min(value = 0, message = "Phần trăm điểm danh không được âm")
        @Max(value = 100, message = "Phần trăm điểm danh không được vượt quá 100")
        private int attendancePercent;

        private float baselineGpa;

        @Min(value = 0, message = "Số tín chỉ học kỳ hiện tại không được âm")
        @Max(value = 60, message = "Số tín chỉ học kỳ hiện tại không được vượt quá 60")
        private int currentTermCredits;

        @JsonIgnore
        @AssertTrue(message = "GPA tích lũy phải nằm trong khoảng từ 0.0 đến 4.0")
        public boolean isBaselineGpaValid() {
            return !Float.isNaN(baselineGpa)
                    && !Float.isInfinite(baselineGpa)
                    && baselineGpa >= 0.0f
                    && baselineGpa <= 4.0f;
        }

        @JsonIgnore
        @AssertTrue(message = "Số tín chỉ đã tích lũy không được vượt quá tổng số tín chỉ")
        public boolean isCreditProgressValid() {
            if (totalCredits < 1) {
                return true;
            }
            return earnedCredits <= totalCredits;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleSlotDto {

        private static final String TIME_24H_REGEX = "^([01]\\d|2[0-3]):[0-5]\\d$";

        @Min(value = 2, message = "Ngày trong tuần phải từ thứ 2 đến thứ 7")
        @Max(value = 7, message = "Ngày trong tuần phải từ thứ 2 đến thứ 7")
        private int dayOfWeek;

        @NotBlank(message = "Thời gian bắt đầu không được để trống")
        @Pattern(regexp = TIME_24H_REGEX, message = "startTime phải có định dạng HH:mm")
        private String startTime;

        @NotBlank(message = "Thời gian kết thúc không được để trống")
        @Pattern(regexp = TIME_24H_REGEX, message = "endTime phải có định dạng HH:mm")
        private String endTime;

        @NotBlank(message = "Môn học không được để trống")
        @Size(max = 150, message = "Môn học không được vượt quá 150 ký tự")
        private String subject;

        @Size(max = 80, message = "Phòng học không được vượt quá 80 ký tự")
        private String room;

        @Size(max = 120, message = "Tên giảng viên không được vượt quá 120 ký tự")
        private String lecturer;

        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
        private String note;

        @JsonIgnore
        @AssertTrue(message = "Thời gian kết thúc phải sau thời gian bắt đầu")
        public boolean isTimeRangeValid() {
            LocalTime start = parseStrictTime(startTime);
            LocalTime end = parseStrictTime(endTime);
            if (start == null || end == null) {
                return true;
            }
            return end.isAfter(start);
        }

        private static LocalTime parseStrictTime(String value) {
            if (value == null || !value.matches(TIME_24H_REGEX)) {
                return null;
            }
            try {
                return LocalTime.parse(value);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudyTaskDto {

        @NotBlank(message = "Tiêu đề công việc không được để trống")
        @Size(max = 200, message = "Tiêu đề công việc không được vượt quá 200 ký tự")
        private String title;

        @Size(max = 120, message = "Môn học không được vượt quá 120 ký tự")
        private String subject;

        @Size(max = 50, message = "Nhãn công việc không được vượt quá 50 ký tự")
        private String tag;

        @Min(value = 0, message = "Thời gian đến hạn không được âm")
        private long dueAtMillis;

        private boolean done;

        @Min(value = 0, message = "Thời gian tạo không được âm")
        private long createdAtMillis;

        @Min(value = -1, message = "Thời gian nhắc trước không hợp lệ")
        @Max(value = 10080, message = "Thời gian nhắc trước tối đa là 10080 phút")
        @Builder.Default
        private int remindBeforeMinutes = -1;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoteDto {

        @NotBlank(message = "Tiêu đề ghi chú không được để trống")
        @Size(max = 200, message = "Tiêu đề ghi chú không được vượt quá 200 ký tự")
        private String title;

        @Size(max = 20000, message = "Nội dung ghi chú không được vượt quá 20000 ký tự")
        private String content;

        @Size(max = 100, message = "Chủ đề ghi chú không được vượt quá 100 ký tự")
        private String subject;

        @Min(value = 0, message = "Trạng thái ghim chỉ nhận giá trị 0 hoặc 1")
        @Max(value = 1, message = "Trạng thái ghim chỉ nhận giá trị 0 hoặc 1")
        private int isPinned;

        @Min(value = 0, message = "Trạng thái hoàn thành chỉ nhận giá trị 0 hoặc 1")
        @Max(value = 1, message = "Trạng thái hoàn thành chỉ nhận giá trị 0 hoặc 1")
        private int isDone;

        @Size(max = 64, message = "Thời gian cập nhật không được vượt quá 64 ký tự")
        private String updatedAt;
    }
}
