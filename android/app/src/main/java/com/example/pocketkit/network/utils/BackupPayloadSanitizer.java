package com.example.pocketkit.network.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.profile.ProfileGpaHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BackupPayloadSanitizer {

    private static final int MAX_CATEGORIES = 100;
    private static final int MAX_SCHEDULES = 500;
    private static final int MAX_TASKS = 1000;
    private static final int MAX_NOTES = 1000;
    private static final int MAX_REMIND_MINUTES = 10080;
    private BackupPayloadSanitizer() {
    }

    @NonNull
    public static ApiModels.BackupPayload sanitize(
            int simulatedScoreProgress,
            @Nullable StudentProfile rawProfile,
            @Nullable List<String> rawCategories,
            @Nullable List<ScheduleSlot> rawSchedules,
            @Nullable List<StudyTask> rawTasks,
            @Nullable List<Note> rawNotes
    ) {
        return ApiModels.BackupPayload.builder()
                .simulatedScoreProgress(ProfileGpaHelper.clampSimulatedProgress(simulatedScoreProgress))
                .profile(sanitizeProfile(rawProfile))
                .schedules(sanitizeSchedules(rawSchedules))
                .tasks(sanitizeTasks(rawTasks))
                .notes(sanitizeNotes(rawNotes))
                .categories(sanitizeCategories(rawCategories))
                .build();
    }

    @NonNull
    private static StudentProfile sanitizeProfile(@Nullable StudentProfile raw) {
        if (raw == null) {
            return StudentProfile.builder()
                    .fullName("Sinh viên")
                    .majorAndCohort("")
                    .earnedCredits(ProfileGpaHelper.DEFAULT_EARNED_CREDITS)
                    .totalCredits(ProfileGpaHelper.DEFAULT_TOTAL_CREDITS)
                    .attendancePercent(ProfileGpaHelper.DEFAULT_ATTENDANCE_PERCENT)
                    .baselineGpa(ProfileGpaHelper.DEFAULT_BASELINE_GPA)
                    .currentTermCredits(ProfileGpaHelper.DEFAULT_TERM_CREDITS)
                    .build();
        }

        String name = trimMax(raw.getFullName(), 120);
        if (name.isEmpty()) {
            name = "Sinh viên";
        }

        int total = clamp(raw.getTotalCredits(), 1, ProfileGpaHelper.MAX_CREDITS_LIMIT);
        int earned = clamp(raw.getEarnedCredits(), 0, total);
        int attendance = clamp(raw.getAttendancePercent(), 0, 100);
        int termCredits = clamp(raw.getCurrentTermCredits(), 0, ProfileGpaHelper.MAX_TERM_CREDITS_LIMIT);
        float gpa = raw.getBaselineGpa();

        if (Float.isNaN(gpa) || Float.isInfinite(gpa)) {
            gpa = 0f;
        } else {
            gpa = Math.max(0f, Math.min(ProfileGpaHelper.GPA_MAX, gpa));
        }

        return StudentProfile.builder()
                .fullName(name)
                .majorAndCohort(trimMax(raw.getMajorAndCohort(), 120))
                .earnedCredits(earned)
                .totalCredits(total)
                .attendancePercent(attendance)
                .baselineGpa(gpa)
                .currentTermCredits(termCredits)
                .build();
    }

    @NonNull
    private static List<String> sanitizeCategories(@Nullable List<String> raw) {
        Set<String> unique = new LinkedHashSet<>();
        if(raw != null) {
            for(String item : raw) {
                if(unique.size() >= MAX_CATEGORIES) {
                    break;
                }

                String clean = trimMax(item, 60);
                if(!clean.isEmpty()) {
                    unique.add(clean);
                }
            }
        }
        return new ArrayList<>(unique);
    }

    @NonNull
    public static List<ScheduleSlot> sanitizeSchedules(@Nullable List<ScheduleSlot> raw) {
        return sanitizeSchedules(raw, MAX_SCHEDULES);
    }

    @NonNull
    public static List<ScheduleSlot> sanitizeSchedules(@Nullable List<ScheduleSlot> raw, int maxCount) {
        List<ScheduleSlot> out = new ArrayList<>();
        if (raw == null || maxCount <= 0) {
            return out;
        }
        for (ScheduleSlot slot : raw) {
            if (out.size() >= maxCount) {
                break;
            }
            if (slot == null) {
                continue;
            }
            int day = slot.getDayOfWeek();
            if (day < ScheduleSlot.DAY_MIN || day > ScheduleSlot.DAY_MAX) {
                continue;
            }
            int startMinutes = ScheduleSlot.toMinutes(slot.getStartTime());
            int endMinutes = ScheduleSlot.toMinutes(slot.getEndTime());
            if (startMinutes < 0 || endMinutes <= startMinutes) {
                continue;
            }
            String subject = trimMax(slot.getSubject(), 150);
            if (subject.isEmpty()) {
                continue;
            }
            out.add(ScheduleSlot.builder()
                    .dayOfWeek(day)
                    .startTime(formatMinutes(startMinutes))
                    .endTime(formatMinutes(endMinutes))
                    .subject(subject)
                    .room(trimMax(slot.getRoom(), 80))
                    .lecturer(trimMax(slot.getLecturer(), 120))
                    .note(trimMax(slot.getNote(), 500))
                    .build());
        }
        return out;
    }
    @NonNull
    private static List<StudyTask> sanitizeTasks(@Nullable List<StudyTask> raw) {
        List<StudyTask> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (StudyTask task : raw) {
            if (out.size() >= MAX_TASKS) {
                break;
            }
            if (task == null) {
                continue;
            }
            String title = trimMax(task.getTitle(), 200);
            if (title.isEmpty()) {
                continue;
            }
            out.add(StudyTask.builder()
                    .title(title)
                    .subject(trimMax(task.getSubject(), 120))
                    .tag(trimMax(task.getTag(), 50))
                    .dueAtMillis(Math.max(0L, task.getDueAtMillis()))
                    .done(task.isDone())
                    .createdAtMillis(Math.max(0L, task.getCreatedAtMillis()))
                    .remindBeforeMinutes(clamp(task.getRemindBeforeMinutes(), -1, MAX_REMIND_MINUTES))
                    .build());
        }
        return out;
    }
    @NonNull
    private static List<Note> sanitizeNotes(@Nullable List<Note> raw) {
        List<Note> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (Note note : raw) {
            if (out.size() >= MAX_NOTES) {
                break;
            }
            if (note == null) {
                continue;
            }
            String title = trimMax(note.getTitle(), 200);
            if (title.isEmpty()) {
                continue;
            }
            out.add(Note.builder()
                    .title(title)
                    .content(trimMax(note.getContent(), 20000))
                    .subject(trimMax(note.getSubject(), 100))
                    .isPinned(note.getIsPinned() == 1 ? 1 : 0)
                    .isDone(note.getIsDone() == 1 ? 1 : 0)
                    .updatedAt(trimMax(note.getUpdatedAt(), 64))
                    .build());
        }
        return out;
    }


    @NonNull
    private static String formatMinutes(int totalMinutes) {
        return String.format(Locale.US, "%02d:%02d", totalMinutes / 60, totalMinutes % 60);
    }
    @NonNull
    private static String trimMax(@Nullable String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLen ? trimmed.substring(0, maxLen) : trimmed;
    }
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
