package com.example.pocketkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.profile.ProfileGpaHelper;
import com.example.pocketkit.repository.PocketKitRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class FakePocketKitRepository implements PocketKitRepository {

    final List<StudyTask> tasks = new ArrayList<>();
    final List<ScheduleSlot> schedules = new ArrayList<>();
    final List<Note> notes = new ArrayList<>();
    final Set<String> categories = new LinkedHashSet<>();

    StudentProfile profile = StudentProfile.builder()
            .fullName("Nguyễn Khánh")
            .majorAndCohort("Kỹ thuật Phần mềm · Khóa 19")
            .earnedCredits(ProfileGpaHelper.DEFAULT_EARNED_CREDITS)
            .totalCredits(ProfileGpaHelper.DEFAULT_TOTAL_CREDITS)
            .attendancePercent(ProfileGpaHelper.DEFAULT_ATTENDANCE_PERCENT)
            .baselineGpa(ProfileGpaHelper.DEFAULT_BASELINE_GPA)
            .currentTermCredits(ProfileGpaHelper.DEFAULT_TERM_CREDITS)
            .build();
    int simulatedProgress = ProfileGpaHelper.DEFAULT_SIMULATED_PROGRESS;
    private int nextTaskId = 1;
    private int nextScheduleId = 1;
    private int nextNoteId = 1;

    @NonNull
    @Override
    public List<StudyTask> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    @Override
    public long insertTask(@NonNull StudyTask task) {
        int id = nextTaskId++;
        task.setId(id);
        tasks.add(task);
        return id;
    }

    @Override
    public int updateTask(@NonNull StudyTask task) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == task.getId()) {
                tasks.set(i, task);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int setTaskDone(int taskId, boolean done) {
        for (StudyTask t : tasks) {
            if (t.getId() == taskId) {
                t.setDone(done);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int deleteTask(int taskId) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == taskId) {
                tasks.remove(i);
                return 1;
            }
        }
        return 0;
    }

    @NonNull
    @Override
    public List<ScheduleSlot> getAllSchedules() {
        return new ArrayList<>(schedules);
    }

    @NonNull
    @Override
    public List<ScheduleSlot> getSchedulesByDay(int dayOfWeek) {
        List<ScheduleSlot> out = new ArrayList<>();
        for (ScheduleSlot s : schedules) {
            if (s.getDayOfWeek() == dayOfWeek) {
                out.add(s);
            }
        }
        return out;
    }

    @Override
    public int getTotalScheduleCount() {
        return schedules.size();
    }

    @Override
    public long insertSchedule(@NonNull ScheduleSlot slot) {
        int id = nextScheduleId++;
        slot.setId(id);
        schedules.add(slot);
        return id;
    }

    @Override
    public int updateSchedule(@NonNull ScheduleSlot slot) {
        for (int i = 0; i < schedules.size(); i++) {
            if (schedules.get(i).getId() == slot.getId()) {
                schedules.set(i, slot);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int deleteSchedule(int slotId) {
        for (int i = 0; i < schedules.size(); i++) {
            if (schedules.get(i).getId() == slotId) {
                schedules.remove(i);
                return 1;
            }
        }
        return 0;
    }

    @NonNull
    @Override
    public DatabaseHelper.ImportSummary importSchedules(@NonNull List<ScheduleSlot> slots, boolean replaceExisting) {
        if (replaceExisting) {
            schedules.clear();
        }
        for (ScheduleSlot slot : slots) {
            insertSchedule(slot);
        }
        return new DatabaseHelper.ImportSummary(slots.size(), 0);
    }

    @NonNull
    @Override
    public List<Note> getNotesFiltered(@NonNull String query, @NonNull String filter) {
        String q = query.trim().toLowerCase();
        List<Note> out = new ArrayList<>();
        for (Note n : notes) {
            if (DatabaseHelper.FILTER_PINNED.equals(filter) && n.getIsPinned() != 1) {
                continue;
            }
            if (!DatabaseHelper.FILTER_ALL.equals(filter)
                    && !DatabaseHelper.FILTER_PINNED.equals(filter)
                    && !filter.equals(n.getSubject())) {
                continue;
            }
            if (!q.isEmpty()) {
                String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";
                String subject = n.getSubject() != null ? n.getSubject().toLowerCase() : "";
                if (!title.contains(q) && !subject.contains(q)) {
                    continue;
                }
            }
            out.add(n);
        }
        return out;
    }

    @Nullable
    @Override
    public Note getHighlightedHomeNote() {
        for (Note n : notes) {
            if (n.getIsPinned() == 1) {
                return n;
            }
        }
        return notes.isEmpty() ? null : notes.get(0);
    }

    @Override
    public int getTotalNotesCount() {
        return notes.size();
    }

    @Override
    public long insertNote(@NonNull Note note) {
        int id = nextNoteId++;
        note.setId(id);
        notes.add(note);
        return id;
    }

    @Override
    public int updateNote(@NonNull Note note) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId() == note.getId()) {
                notes.set(i, note);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int deleteNote(int noteId) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId() == noteId) {
                notes.remove(i);
                return 1;
            }
        }
        return 0;
    }

    @NonNull
    @Override
    public List<String> getAllCategories() {
        return new ArrayList<>(categories);
    }

    @Override
    public void insertCategory(@NonNull String name) {
        categories.add(name.trim());
    }

    @Override
    public void deleteCategory(@NonNull String name) {
        categories.remove(name);
    }

    @NonNull
    @Override
    public StudentProfile loadProfile() {
        return profile;
    }

    @Override
    public void saveProfile(@NonNull StudentProfile profile) {
        this.profile = profile;
    }

    @Override
    public int loadSimulatedScoreProgress() {
        return simulatedProgress;
    }

    @Override
    public void saveSimulatedScoreProgress(int progress) {
        this.simulatedProgress = progress;
    }
}
