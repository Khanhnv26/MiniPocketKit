package com.example.pocketkit;

import com.example.pocketkit.home.HomeSummaryHelper;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudyTask;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HomeSummaryHelperTest {

    private long fixedTime(int year, int monthZeroBased, int dayOfMonth, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthZeroBased);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Test
    public void computeDeadlineProgress_handlesEmptyPartialAndCompleteLists() {
        HomeSummaryHelper.DeadlineProgress empty = HomeSummaryHelper.computeDeadlineProgress(Collections.emptyList());
        assertEquals(0, empty.doneCount);
        assertEquals(0, empty.totalCount);
        assertEquals(0, empty.remainingCount);
        assertEquals(0f, empty.fraction, 0.0001f);

        List<StudyTask> partialList = Arrays.asList(
                StudyTask.builder().id(1).title("Task A").done(true).build(),
                StudyTask.builder().id(2).title("Task B").done(false).build(),
                StudyTask.builder().id(3).title("Task C").done(false).build(),
                StudyTask.builder().id(4).title("Task D").done(false).build()
        );
        HomeSummaryHelper.DeadlineProgress partial = HomeSummaryHelper.computeDeadlineProgress(partialList);
        assertEquals(1, partial.doneCount);
        assertEquals(4, partial.totalCount);
        assertEquals(3, partial.remainingCount);
        assertEquals(0.25f, partial.fraction, 0.0001f);

        List<StudyTask> doneList = Arrays.asList(
                StudyTask.builder().id(1).title("Task A").done(true).build(),
                StudyTask.builder().id(2).title("Task B").done(true).build()
        );
        HomeSummaryHelper.DeadlineProgress allDone = HomeSummaryHelper.computeDeadlineProgress(doneList);
        assertEquals(2, allDone.doneCount);
        assertEquals(0, allDone.remainingCount);
        assertEquals(1.0f, allDone.fraction, 0.0001f);
    }

    @Test
    public void findBestSpotlight_prioritizesEarliestPendingDeadlineAndUrgency() {
        long now = fixedTime(2026, Calendar.NOVEMBER, 16, 10, 0);
        long overdueAt = fixedTime(2026, Calendar.NOVEMBER, 16, 8, 30);
        long soonAt = fixedTime(2026, Calendar.NOVEMBER, 16, 17, 0);
        long laterAt = fixedTime(2026, Calendar.NOVEMBER, 19, 23, 59);

        List<StudyTask> tasks = new ArrayList<>(Arrays.asList(
                StudyTask.builder().id(1).title("Completed Deadline").dueAtMillis(overdueAt).done(true).build(),
                StudyTask.builder().id(2).title("Far Deadline").dueAtMillis(laterAt).done(false).build(),
                StudyTask.builder().id(3).title("Soon Deadline").dueAtMillis(soonAt).done(false).build()
        ));
        List<ScheduleSlot> schedules = Collections.singletonList(
                ScheduleSlot.builder().id(10).dayOfWeek(Calendar.MONDAY).startTime("13:00").endTime("15:00").subject("Mobile").build()
        );

        HomeSummaryHelper.SpotlightCandidate soon = HomeSummaryHelper.findBestSpotlight(tasks, schedules, now);
        assertEquals(HomeSummaryHelper.SpotlightKind.DEADLINE_TASK, soon.kind);
        assertNotNull(soon.task);
        assertEquals(3, soon.task.getId());
        assertEquals(HomeSummaryHelper.DeadlineUrgency.SOON_HOURS, soon.urgency);
        assertTrue(soon.today);

        tasks.get(2).setDone(true);
        HomeSummaryHelper.SpotlightCandidate far = HomeSummaryHelper.findBestSpotlight(tasks, schedules, now);
        assertEquals(HomeSummaryHelper.SpotlightKind.DEADLINE_TASK, far.kind);
        assertNotNull(far.task);
        assertEquals(2, far.task.getId());
        assertEquals(HomeSummaryHelper.DeadlineUrgency.UPCOMING_DAYS, far.urgency);
        assertFalse(far.today);

        tasks.add(StudyTask.builder().id(4).title("Overdue Task").dueAtMillis(overdueAt).done(false).build());
        HomeSummaryHelper.SpotlightCandidate overdue = HomeSummaryHelper.findBestSpotlight(tasks, schedules, now);
        assertEquals(HomeSummaryHelper.DeadlineUrgency.OVERDUE, overdue.urgency);
        assertEquals(4, overdue.task.getId());
    }

    @Test
    public void findBestSpotlight_fallsBackToScheduleThenPendingTaskThenAllClear() {
        long nowMonday = fixedTime(2026, Calendar.NOVEMBER, 16, 14, 0);
        List<StudyTask> tasks = Arrays.asList(
                StudyTask.builder().id(1).title("Done Deadline").dueAtMillis(nowMonday).done(true).build(),
                StudyTask.builder().id(2).title("Quick Checklist").dueAtMillis(0L).done(false).build()
        );
        List<ScheduleSlot> schedules = Arrays.asList(
                ScheduleSlot.builder().id(11).dayOfWeek(Calendar.MONDAY).startTime("07:30").endTime("09:45").subject("Finished Class").build(),
                ScheduleSlot.builder().id(12).dayOfWeek(Calendar.MONDAY).startTime("13:30").endTime("15:45").subject("Active Class").build(),
                ScheduleSlot.builder().id(13).dayOfWeek(Calendar.TUESDAY).startTime("08:00").endTime("10:00").subject("Tomorrow Class").build()
        );

        HomeSummaryHelper.SpotlightCandidate activeClass = HomeSummaryHelper.findBestSpotlight(tasks, schedules, nowMonday);
        assertEquals(HomeSummaryHelper.SpotlightKind.SCHEDULE_SLOT, activeClass.kind);
        assertNotNull(activeClass.slot);
        assertEquals("Active Class", activeClass.slot.getSubject());
        assertTrue(activeClass.happeningNow);
        assertTrue(activeClass.today);

        HomeSummaryHelper.SpotlightCandidate pendingOnly = HomeSummaryHelper.findBestSpotlight(tasks, Collections.emptyList(), nowMonday);
        assertEquals(HomeSummaryHelper.SpotlightKind.PENDING_TASK, pendingOnly.kind);
        assertNotNull(pendingOnly.task);
        assertEquals("Quick Checklist", pendingOnly.task.getTitle());

        tasks.get(1).setDone(true);
        HomeSummaryHelper.SpotlightCandidate clear = HomeSummaryHelper.findBestSpotlight(tasks, Collections.emptyList(), nowMonday);
        assertEquals(HomeSummaryHelper.SpotlightKind.ALL_CLEAR, clear.kind);
        assertNull(clear.task);
        assertNull(clear.slot);
    }

    @Test
    public void computeClassProgress_tracksFinishedTodaySlots() {
        long nowMonday = fixedTime(2026, Calendar.NOVEMBER, 16, 14, 0);
        List<ScheduleSlot> schedules = Arrays.asList(
                ScheduleSlot.builder().id(1).dayOfWeek(Calendar.MONDAY).startTime("07:30").endTime("09:45").subject("Morning").build(),
                ScheduleSlot.builder().id(2).dayOfWeek(Calendar.MONDAY).startTime("13:30").endTime("15:45").subject("Afternoon").build()
        );

        assertEquals(0.5f, HomeSummaryHelper.computeClassProgress(schedules, nowMonday), 0.0001f);
        assertEquals(0f, HomeSummaryHelper.computeClassProgress(Collections.emptyList(), nowMonday), 0.0001f);
    }
}
