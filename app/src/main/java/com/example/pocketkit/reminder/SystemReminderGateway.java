package com.example.pocketkit.reminder;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.pocketkit.model.StudyTask;

public final class SystemReminderGateway implements ReminderGateway {

    private final Context appContext;

    public SystemReminderGateway(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void scheduleOrCancel(@NonNull StudyTask task) {
        TaskReminderScheduler.scheduleOrCancel(appContext, task);
    }

    @Override
    public void cancelReminder(int taskId) {
        TaskReminderScheduler.cancelReminder(appContext, taskId);
    }

    @Override
    public void clearFiredMarker(int taskId) {
        TaskReminderScheduler.clearFiredMarker(appContext, taskId);
    }

    @Override
    public void rescheduleAllPendingReminders() {
        TaskReminderScheduler.rescheduleAllPendingReminders(appContext);
    }
}
