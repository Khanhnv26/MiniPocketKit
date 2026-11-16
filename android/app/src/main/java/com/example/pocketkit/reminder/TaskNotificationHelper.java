package com.example.pocketkit.reminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.pocketkit.MainActivity;
import com.example.pocketkit.R;
import com.example.pocketkit.home.HomeSummaryHelper;
import com.example.pocketkit.model.StudyTask;

public final class TaskNotificationHelper {

    private TaskNotificationHelper() {
    }

    public static void createNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                TaskReminderHelper.CHANNEL_ID,
                context.getString(R.string.task_reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.task_reminder_channel_desc));
        manager.createNotificationChannel(channel);
    }

    public static boolean hasNotificationPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void showTaskReminderNotification(
            @NonNull Context context,
            @NonNull StudyTask task,
            long nowMillis
    ) {
        createNotificationChannel(context);
        if (!hasNotificationPermission(context)) {
            return;
        }

        String subject = task.getSubject() != null ? task.getSubject().trim() : "";
        String contentTitle = subject.isEmpty()
                ? task.getTitle()
                : context.getString(R.string.task_reminder_notif_title_subject, task.getTitle(), subject);

        String duePrefix = HomeSummaryHelper.formatDueDateTime(context, task.getDueAtMillis(), nowMillis);
        String relativeDelta = HomeSummaryHelper.formatRelativeDelta(context, task.getDueAtMillis(), nowMillis);
        String contentBody = context.getString(
                R.string.home_spotlight_due_remaining_format,
                duePrefix,
                relativeDelta
        );

        Intent openAppIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                TaskReminderHelper.buildRemindRequestCode(task.getId()),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent doneIntent = new Intent(context, TaskReminderReceiver.class)
                .setAction(TaskReminderHelper.ACTION_MARK_DONE)
                .putExtra(TaskReminderHelper.EXTRA_TASK_ID, task.getId());
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context,
                TaskReminderHelper.buildActionRequestCode(task.getId()),
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TaskReminderHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setColor(ContextCompat.getColor(context, R.color.accent_sapphire))
                .setContentTitle(contentTitle)
                .setContentText(contentBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(
                        R.drawable.ic_check_white,
                        context.getString(R.string.task_reminder_notif_action_done),
                        donePendingIntent
                );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (!notificationManager.areNotificationsEnabled()) {
            return;
        }
        notificationManager.notify(TaskReminderHelper.buildNotificationId(task.getId()), builder.build());
    }

    public static void cancelTaskNotification(@NonNull Context context, int taskId) {
        if (taskId <= 0) {
            return;
        }
        NotificationManagerCompat.from(context).cancel(TaskReminderHelper.buildNotificationId(taskId));
    }
}
