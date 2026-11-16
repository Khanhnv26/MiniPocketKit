package com.example.pocketkit.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.profile.StudentProfileStore;

import java.util.List;

public class LocalPocketKitRepository implements PocketKitRepository {

    private final DatabaseHelper dbHelper;
    private final StudentProfileStore profileStore;

    public LocalPocketKitRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(appContext);
        this.profileStore = new StudentProfileStore(appContext);
    }

    @NonNull
    @Override
    public List<StudyTask> getAllTasks() {
        return dbHelper.getAllTasks();
    }

    @Override
    public long insertTask(@NonNull StudyTask task) {
        return dbHelper.insertTask(task);
    }

    @Override
    public int updateTask(@NonNull StudyTask task) {
        return dbHelper.updateTask(task);
    }

    @Override
    public int setTaskDone(int taskId, boolean done) {
        return dbHelper.setTaskDone(taskId, done);
    }

    @Override
    public int deleteTask(int taskId) {
        return dbHelper.deleteTask(taskId);
    }

    @NonNull
    @Override
    public List<ScheduleSlot> getAllSchedules() {
        return dbHelper.getAllSchedules();
    }

    @NonNull
    @Override
    public List<ScheduleSlot> getSchedulesByDay(int dayOfWeek) {
        return dbHelper.getSchedulesByDay(dayOfWeek);
    }

    @Override
    public int getTotalScheduleCount() {
        return dbHelper.getTotalScheduleCount();
    }

    @Override
    public long insertSchedule(@NonNull ScheduleSlot slot) {
        return dbHelper.insertSchedule(slot);
    }

    @Override
    public int updateSchedule(@NonNull ScheduleSlot slot) {
        return dbHelper.updateSchedule(slot);
    }

    @Override
    public int deleteSchedule(int slotId) {
        return dbHelper.deleteSchedule(slotId);
    }

    @NonNull
    @Override
    public DatabaseHelper.ImportSummary importSchedules(@NonNull List<ScheduleSlot> slots, boolean replaceExisting) {
        return dbHelper.importSchedules(slots, replaceExisting);
    }

    @NonNull
    @Override
    public List<Note> getNotesFiltered(@NonNull String query, @NonNull String filter) {
        return dbHelper.getNotesFiltered(query, filter);
    }

    @Nullable
    @Override
    public Note getHighlightedHomeNote() {
        List<Note> pinned = dbHelper.getNotesFiltered("", DatabaseHelper.FILTER_PINNED);
        if (!pinned.isEmpty()) {
            return pinned.get(0);
        }
        List<Note> all = dbHelper.getNotesFiltered("", DatabaseHelper.FILTER_ALL);
        return all.isEmpty() ? null : all.get(0);
    }

    @Override
    public int getTotalNotesCount() {
        return dbHelper.getNotesFiltered("", DatabaseHelper.FILTER_ALL).size();
    }

    @Override
    public long insertNote(@NonNull Note note) {
        return dbHelper.insertNote(note);
    }

    @Override
    public int updateNote(@NonNull Note note) {
        return dbHelper.updateNote(note);
    }

    @Override
    public int deleteNote(int noteId) {
        return dbHelper.deleteNote(noteId);
    }

    @NonNull
    @Override
    public List<String> getAllCategories() {
        return dbHelper.getAllCategories();
    }

    @Override
    public void insertCategory(@NonNull String name) {
        dbHelper.insertCategory(name);
    }

    @Override
    public void deleteCategory(@NonNull String name) {
        dbHelper.deleteCategory(name);
    }

    @NonNull
    @Override
    public StudentProfile loadProfile() {
        return profileStore.loadProfile();
    }

    @Override
    public void saveProfile(@NonNull StudentProfile profile) {
        profileStore.saveProfile(profile);
    }

    @Override
    public int loadSimulatedScoreProgress() {
        return profileStore.loadSimulatedProgress();
    }

    @Override
    public void saveSimulatedScoreProgress(int progress) {
        profileStore.saveSimulatedProgress(progress);
    }
}
