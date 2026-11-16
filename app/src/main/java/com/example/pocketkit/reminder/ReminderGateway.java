package com.example.pocketkit.reminder;

import androidx.annotation.NonNull;

import com.example.pocketkit.model.StudyTask;

public interface ReminderGateway {

    void scheduleOrCancel(@NonNull StudyTask task);

    void cancelReminder(int taskId);

    void clearFiredMarker(int taskId);

    void rescheduleAllPendingReminders();
}
