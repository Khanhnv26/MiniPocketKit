package com.example.pocketkit;

import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.reminder.TaskReminderHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TaskReminderHelperTest {

    private static final long NOW = 1_700_000_000_000L;

    @Test
    public void computeTriggerAtMillis_futureOffset_subtractsMinutesCleanly() {
        long dueAt = NOW + (120L * TaskReminderHelper.MILLIS_PER_MINUTE);
        long trigger = TaskReminderHelper.computeTriggerAtMillis(
                dueAt,
                TaskReminderHelper.REMIND_30_MIN,
                false,
                NOW
        );
        assertEquals(dueAt - (30L * TaskReminderHelper.MILLIS_PER_MINUTE), trigger);
        assertFalse(TaskReminderHelper.isInsidePastOffsetWindow(dueAt, TaskReminderHelper.REMIND_30_MIN, NOW));
    }

    @Test
    public void computeTriggerAtMillis_atDue_triggersExactlyAtDueTime() {
        long dueAt = NOW + (45L * TaskReminderHelper.MILLIS_PER_MINUTE);
        long trigger = TaskReminderHelper.computeTriggerAtMillis(
                dueAt,
                TaskReminderHelper.REMIND_AT_DUE,
                false,
                NOW
        );
        assertEquals(dueAt, trigger);
    }

    @Test
    public void computeTriggerAtMillis_insideWindow_schedulesImmediateDelay() {
        long dueAt = NOW + (15L * TaskReminderHelper.MILLIS_PER_MINUTE);
        long trigger = TaskReminderHelper.computeTriggerAtMillis(
                dueAt,
                TaskReminderHelper.REMIND_60_MIN,
                false,
                NOW
        );
        assertEquals(NOW + TaskReminderHelper.IMMEDIATE_WINDOW_DELAY_MS, trigger);
        assertTrue(TaskReminderHelper.isInsidePastOffsetWindow(dueAt, TaskReminderHelper.REMIND_60_MIN, NOW));
    }

    @Test
    public void computeTriggerAtMillis_doneOrOverdueOrDisabled_returnsMinusOne() {
        long futureDue = NOW + (90L * TaskReminderHelper.MILLIS_PER_MINUTE);
        long pastDue = NOW - 1_000L;

        assertEquals(-1L, TaskReminderHelper.computeTriggerAtMillis(futureDue, TaskReminderHelper.REMIND_30_MIN, true, NOW));
        assertEquals(-1L, TaskReminderHelper.computeTriggerAtMillis(futureDue, TaskReminderHelper.REMIND_NONE, false, NOW));
        assertEquals(-1L, TaskReminderHelper.computeTriggerAtMillis(0L, TaskReminderHelper.REMIND_AT_DUE, false, NOW));
        assertEquals(-1L, TaskReminderHelper.computeTriggerAtMillis(pastDue, TaskReminderHelper.REMIND_30_MIN, false, NOW));
    }

    @Test
    public void normalizeAndConfiguredChecks_validateInputsProperly() {
        assertEquals(TaskReminderHelper.REMIND_1_DAY, TaskReminderHelper.normalizeRemindBeforeMinutes(1440));
        assertEquals(TaskReminderHelper.REMIND_NONE, TaskReminderHelper.normalizeRemindBeforeMinutes(999));

        StudyTask validTask = StudyTask.builder()
                .id(12)
                .title("Nop bao cao")
                .dueAtMillis(NOW + 3_600_000L)
                .done(false)
                .remindBeforeMinutes(TaskReminderHelper.REMIND_60_MIN)
                .build();
        assertTrue(TaskReminderHelper.isReminderConfigured(validTask));

        validTask.setDone(true);
        assertFalse(TaskReminderHelper.isReminderConfigured(validTask));

        validTask.setDone(false);
        validTask.setRemindBeforeMinutes(TaskReminderHelper.REMIND_NONE);
        assertFalse(TaskReminderHelper.isReminderConfigured(validTask));
        assertFalse(validTask.hasReminder());
    }

    @Test
    public void requestAndNotificationIds_stayDistinctPerTask() {
        assertNotEquals(
                TaskReminderHelper.buildRemindRequestCode(5),
                TaskReminderHelper.buildActionRequestCode(5)
        );
        assertNotEquals(
                TaskReminderHelper.buildRemindRequestCode(5),
                TaskReminderHelper.buildRemindRequestCode(6)
        );
        assertNotEquals(
                TaskReminderHelper.buildNotificationId(5),
                TaskReminderHelper.buildNotificationId(6)
        );
    }
}
