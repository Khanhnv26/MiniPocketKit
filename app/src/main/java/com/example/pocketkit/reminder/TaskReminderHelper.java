package com.example.pocketkit.reminder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.model.StudyTask;

public final class TaskReminderHelper {

    public static final String CHANNEL_ID = "deadline_reminders";
    public static final String ACTION_REMIND_TASK = "com.example.pocketkit.action.REMIND_TASK";
    public static final String ACTION_MARK_DONE = "com.example.pocketkit.action.MARK_TASK_DONE";
    public static final String ACTION_TASK_UPDATED = "com.example.pocketkit.action.TASK_UPDATED";
    public static final String EXTRA_TASK_ID = "extra_task_id";

    public static final int REMIND_NONE = -1;
    public static final int REMIND_AT_DUE = 0;
    public static final int REMIND_30_MIN = 30;
    public static final int REMIND_60_MIN = 60;
    public static final int REMIND_1_DAY = 1440;

    public static final long MILLIS_PER_MINUTE = 60_000L;
    public static final long IMMEDIATE_WINDOW_DELAY_MS = 5_000L;

    private static final int REMIND_REQUEST_OFFSET = 10_000;
    private static final int ACTION_REQUEST_OFFSET = 20_000;
    private static final int NOTIFICATION_ID_OFFSET = 30_000;

    private TaskReminderHelper() {
    }

    public static boolean isReminderConfigured(@Nullable StudyTask task) {
        return task != null
                && task.getId() > 0
                && !task.isDone()
                && task.hasDueAt()
                && task.getRemindBeforeMinutes() >= 0;
    }

    public static boolean isSupportedOffset(int remindBeforeMinutes) {
        return remindBeforeMinutes == REMIND_NONE
                || remindBeforeMinutes == REMIND_AT_DUE
                || remindBeforeMinutes == REMIND_30_MIN
                || remindBeforeMinutes == REMIND_60_MIN
                || remindBeforeMinutes == REMIND_1_DAY;
    }

    public static int normalizeRemindBeforeMinutes(int remindBeforeMinutes) {
        return isSupportedOffset(remindBeforeMinutes) ? remindBeforeMinutes : REMIND_NONE;
    }

    public static long computeTriggerAtMillis(
            long dueAtMillis,
            int remindBeforeMinutes,
            boolean done,
            long nowMillis
    ) {
        if (done || dueAtMillis <= 0L || remindBeforeMinutes < 0) {
            return -1L;
        }
        if (dueAtMillis <= nowMillis) {
            return -1L;
        }
        long rawTrigger = dueAtMillis - (remindBeforeMinutes * MILLIS_PER_MINUTE);
        if (rawTrigger > nowMillis) {
            return rawTrigger;
        }
        return nowMillis + IMMEDIATE_WINDOW_DELAY_MS;
    }

    public static boolean isInsidePastOffsetWindow(long dueAtMillis, int remindBeforeMinutes, long nowMillis) {
        if (dueAtMillis <= nowMillis || remindBeforeMinutes < 0) {
            return false;
        }
        long rawTrigger = dueAtMillis - (remindBeforeMinutes * MILLIS_PER_MINUTE);
        return rawTrigger <= nowMillis;
    }

    @NonNull
    public static String buildFiredToken(long dueAtMillis, int remindBeforeMinutes) {
        return dueAtMillis + ":" + remindBeforeMinutes;
    }

    public static int buildRemindRequestCode(int taskId) {
        return REMIND_REQUEST_OFFSET + Math.max(0, taskId);
    }

    public static int buildActionRequestCode(int taskId) {
        return ACTION_REQUEST_OFFSET + Math.max(0, taskId);
    }

    public static int buildNotificationId(int taskId) {
        return NOTIFICATION_ID_OFFSET + Math.max(0, taskId);
    }
}
