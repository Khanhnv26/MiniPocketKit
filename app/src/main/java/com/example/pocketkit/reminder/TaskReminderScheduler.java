package com.example.pocketkit.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.StudyTask;

import java.util.List;

public final class TaskReminderScheduler {

    private static final String PREFS_NAME = "pocketkit_fired_reminders";
    private static final String KEY_PREFIX_TASK = "fired_task_";

    private TaskReminderScheduler() {
    }

    public static void scheduleOrCancel(@NonNull Context context, @NonNull StudyTask task) {
        if (!TaskReminderHelper.isReminderConfigured(task)) {
            cancelReminder(context, task.getId());
            clearFiredMarker(context, task.getId());
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long triggerAtMillis = TaskReminderHelper.computeTriggerAtMillis(
                task.getDueAtMillis(),
                task.getRemindBeforeMinutes(),
                task.isDone(),
                nowMillis
        );
        if (triggerAtMillis <= 0L) {
            cancelReminder(context, task.getId());
            return;
        }

        boolean inWindow = TaskReminderHelper.isInsidePastOffsetWindow(
                task.getDueAtMillis(),
                task.getRemindBeforeMinutes(),
                nowMillis
        );
        if (inWindow && hasFiredForToken(context, task)) {
            cancelAlarmOnly(context, task.getId());
            return;
        }

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildAlarmPendingIntent(context, task.getId());
        alarmManager.cancel(pendingIntent);
        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent);
    }

    public static void cancelReminder(@NonNull Context context, int taskId) {
        if (taskId <= 0) {
            return;
        }
        cancelAlarmOnly(context, taskId);
        TaskNotificationHelper.cancelTaskNotification(context, taskId);
    }

    public static void clearFiredMarker(@NonNull Context context, int taskId) {
        if (taskId <= 0) {
            return;
        }
        getPrefs(context).edit().remove(KEY_PREFIX_TASK + taskId).apply();
    }

    public static void markReminderFired(@NonNull Context context, @NonNull StudyTask task) {
        if (task.getId() <= 0) {
            return;
        }
        String token = TaskReminderHelper.buildFiredToken(
                task.getDueAtMillis(),
                task.getRemindBeforeMinutes()
        );
        getPrefs(context).edit().putString(KEY_PREFIX_TASK + task.getId(), token).apply();
    }

    public static void rescheduleAllPendingReminders(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        DatabaseHelper dbHelper = new DatabaseHelper(appContext);
        List<StudyTask> pendingTasks = dbHelper.getTasksWithPendingReminders();
        for (StudyTask task : pendingTasks) {
            scheduleOrCancel(appContext, task);
        }
    }

    private static void cancelAlarmOnly(@NonNull Context context, int taskId) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = buildAlarmPendingIntent(context, taskId);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private static void scheduleAlarm(
            @NonNull AlarmManager alarmManager,
            long triggerAtMillis,
            @NonNull PendingIntent pendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            return;
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    @NonNull
    private static PendingIntent buildAlarmPendingIntent(@NonNull Context context, int taskId) {
        Intent intent = new Intent(context, TaskReminderReceiver.class)
                .setAction(TaskReminderHelper.ACTION_REMIND_TASK)
                .putExtra(TaskReminderHelper.EXTRA_TASK_ID, taskId);
        return PendingIntent.getBroadcast(
                context,
                TaskReminderHelper.buildRemindRequestCode(taskId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static boolean hasFiredForToken(@NonNull Context context, @NonNull StudyTask task) {
        String saved = getPrefs(context).getString(KEY_PREFIX_TASK + task.getId(), "");
        String expected = TaskReminderHelper.buildFiredToken(
                task.getDueAtMillis(),
                task.getRemindBeforeMinutes()
        );
        return expected.equals(saved);
    }

    @NonNull
    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
