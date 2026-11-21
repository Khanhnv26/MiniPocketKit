package com.example.pocketkit;

import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.network.utils.BackupPayloadSanitizer;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class BackupPayloadSanitizerTest {

    @Test
    public void sanitize_preservesValidGpaAndNormalizesFields() {
        StudentProfile profile = StudentProfile.builder()
                .fullName("  Nguyễn Khánh  ")
                .majorAndCohort("SE1901")
                .earnedCredits(114)
                .totalCredits(145)
                .attendancePercent(96)
                .baselineGpa(3.44f)
                .currentTermCredits(20)
                .build();
        ScheduleSlot looseTimeSlot = ScheduleSlot.builder()
                .dayOfWeek(2)
                .startTime("7:30")
                .endTime("9:5")
                .subject("PRM392")
                .build();
        ScheduleSlot invalidOrderSlot = ScheduleSlot.builder()
                .dayOfWeek(2)
                .startTime("10:00")
                .endTime("09:00")
                .subject("Bỏ qua")
                .build();
        StudyTask blankTask = StudyTask.builder().title("   ").build();
        StudyTask validTask = StudyTask.builder()
                .title("Nộp Lab")
                .remindBeforeMinutes(20000)
                .build();
        Note note = Note.builder()
                .title("Ôn thi")
                .isPinned(5)
                .isDone(-1)
                .build();

        ApiModels.BackupPayload sanitized = BackupPayloadSanitizer.sanitize(
                85,
                profile,
                Arrays.asList("  PRM392  ", "PRM392", "   "),
                Arrays.asList(looseTimeSlot, invalidOrderSlot),
                Arrays.asList(blankTask, validTask),
                Collections.singletonList(note)
        );

        assertEquals(85, sanitized.getSimulatedScoreProgress());
        assertEquals("Nguyễn Khánh", sanitized.getProfile().getFullName());
        assertEquals(3.44f, sanitized.getProfile().getBaselineGpa(), 0.001f);
        assertEquals(1, sanitized.getCategories().size());
        assertEquals("PRM392", sanitized.getCategories().get(0));
        assertEquals(1, sanitized.getSchedules().size());
        assertEquals("07:30", sanitized.getSchedules().get(0).getStartTime());
        assertEquals("09:05", sanitized.getSchedules().get(0).getEndTime());
        assertEquals(1, sanitized.getTasks().size());
        assertEquals(10080, sanitized.getTasks().get(0).getRemindBeforeMinutes());
        assertEquals(1, sanitized.getNotes().size());
        assertEquals(0, sanitized.getNotes().get(0).getIsPinned());
        assertEquals(0, sanitized.getNotes().get(0).getIsDone());
    }

    @Test
    public void sanitize_clampsOutOfRangeProfileValues() {
        StudentProfile invalidProfile = StudentProfile.builder()
                .fullName(" ")
                .earnedCredits(200)
                .totalCredits(145)
                .attendancePercent(120)
                .baselineGpa(Float.NaN)
                .currentTermCredits(80)
                .build();

        ApiModels.BackupPayload sanitized = BackupPayloadSanitizer.sanitize(
                150,
                invalidProfile,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertEquals(100, sanitized.getSimulatedScoreProgress());
        assertEquals("Sinh viên", sanitized.getProfile().getFullName());
        assertEquals(145, sanitized.getProfile().getEarnedCredits());
        assertEquals(100, sanitized.getProfile().getAttendancePercent());
        assertEquals(0f, sanitized.getProfile().getBaselineGpa(), 0.001f);
        assertEquals(60, sanitized.getProfile().getCurrentTermCredits());
    }
}
