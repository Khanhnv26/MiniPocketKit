package com.example.pocketkit.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.StudyTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskReminderReceiver extends BroadcastReceiver {

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) {
            return;
        }
        int taskId = intent.getIntExtra(TaskReminderHelper.EXTRA_TASK_ID, -1);
        if (taskId <= 0) {
            return;
        }

        String action = intent.getAction();
        Context appContext = context.getApplicationContext();
        PendingResult pendingResult = goAsync();
        IO_EXECUTOR.execute(() -> {
            try {
                handleAction(appContext, action, taskId);
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void handleAction(Context appContext, String action, int taskId) {
        DatabaseHelper dbHelper = new DatabaseHelper(appContext);
        if (TaskReminderHelper.ACTION_REMIND_TASK.equals(action)) {
            StudyTask task = dbHelper.getTaskById(taskId);
            if (!TaskReminderHelper.isReminderConfigured(task)) {
                TaskReminderScheduler.cancelReminder(appContext, taskId);
                return;
            }
            TaskReminderScheduler.markReminderFired(appContext, task);
            TaskNotificationHelper.showTaskReminderNotification(
                    appContext,
                    task,
                    System.currentTimeMillis()
            );
            return;
        }

        if (TaskReminderHelper.ACTION_MARK_DONE.equals(action)) {
            dbHelper.setTaskDone(taskId, true);
            TaskReminderScheduler.cancelReminder(appContext, taskId);
            TaskReminderScheduler.clearFiredMarker(appContext, taskId);
            Intent updateIntent = new Intent(TaskReminderHelper.ACTION_TASK_UPDATED)
                    .setPackage(appContext.getPackageName());
            appContext.sendBroadcast(updateIntent);
        }
    }
}
