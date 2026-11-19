package com.example.pocketkit.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;

import java.util.List;

public interface PocketKitRepository {

    @NonNull
    List<StudyTask> getAllTasks();

    long insertTask(@NonNull StudyTask task);

    int updateTask(@NonNull StudyTask task);

    int setTaskDone(int taskId, boolean done);

    int deleteTask(int taskId);

    @NonNull
    List<ScheduleSlot> getAllSchedules();

    @NonNull
    List<ScheduleSlot> getSchedulesByDay(int dayOfWeek);

    int getTotalScheduleCount();

    long insertSchedule(@NonNull ScheduleSlot slot);

    int updateSchedule(@NonNull ScheduleSlot slot);

    int deleteSchedule(int slotId);

    @NonNull
    DatabaseHelper.ImportSummary importSchedules(@NonNull List<ScheduleSlot> slots, boolean replaceExisting);

    @NonNull
    List<Note> getNotesFiltered(@NonNull String query, @NonNull String filter);

    @Nullable
    Note getHighlightedHomeNote();

    int getTotalNotesCount();

    long insertNote(@NonNull Note note);

    int updateNote(@NonNull Note note);

    int deleteNote(int noteId);

    @NonNull
    List<String> getAllCategories();

    void insertCategory(@NonNull String name);

    void deleteCategory(@NonNull String name);

    @NonNull
    StudentProfile loadProfile();

    void saveProfile(@NonNull StudentProfile profile);

    int loadSimulatedScoreProgress();

    void saveSimulatedScoreProgress(int progress);

    void replaceAllBackupData(
            @NonNull List<String> categories,
            @NonNull List<ScheduleSlot> schedules,
            @NonNull List<StudyTask> tasks,
            @NonNull List<Note> notes
    );
}
