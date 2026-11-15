package com.example.pocketkit.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.R;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudyTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class HomeSummaryHelper {

    public static final long MILLIS_PER_MINUTE = 60_000L;
    public static final long MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE;
    public static final long MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR;

    public enum SpotlightKind {
        DEADLINE_TASK,
        SCHEDULE_SLOT,
        PENDING_TASK,
        ALL_CLEAR
    }

    public enum DeadlineUrgency {
        NONE,
        OVERDUE,
        SOON_HOURS,
        UPCOMING_DAYS
    }

    public static final class DeadlineProgress {
        public final int doneCount;
        public final int totalCount;
        public final int remainingCount;
        public final float fraction;

        public DeadlineProgress(int doneCount, int totalCount) {
            this.doneCount = Math.max(0, doneCount);
            this.totalCount = Math.max(0, totalCount);
            this.remainingCount = Math.max(0, this.totalCount - this.doneCount);
            this.fraction = this.totalCount == 0 ? 0f : (float) this.doneCount / (float) this.totalCount;
        }
    }

    public static final class SpotlightCandidate {
        public final SpotlightKind kind;
        @Nullable
        public final StudyTask task;
        @Nullable
        public final ScheduleSlot slot;
        public final boolean happeningNow;
        public final boolean today;
        public final DeadlineUrgency urgency;
        public final long remainingMillis;

        public SpotlightCandidate(
                @NonNull SpotlightKind kind,
                @Nullable StudyTask task,
                @Nullable ScheduleSlot slot,
                boolean happeningNow,
                boolean today,
                @NonNull DeadlineUrgency urgency,
                long remainingMillis
        ) {
            this.kind = kind;
            this.task = task;
            this.slot = slot;
            this.happeningNow = happeningNow;
            this.today = today;
            this.urgency = urgency;
            this.remainingMillis = remainingMillis;
        }
    }

    private HomeSummaryHelper() {
    }

    @NonNull
    public static DeadlineProgress computeDeadlineProgress(@Nullable List<StudyTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return new DeadlineProgress(0, 0);
        }
        int doneCount = 0;
        for (StudyTask task : tasks) {
            if (task != null && task.isDone()) {
                doneCount++;
            }
        }
        return new DeadlineProgress(doneCount, tasks.size());
    }

    public static float computeClassProgress(@Nullable List<ScheduleSlot> allSchedules, long nowMillis) {
        if (allSchedules == null || allSchedules.isEmpty()) {
            return 0f;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMillis);
        int androidDay = cal.get(Calendar.DAY_OF_WEEK);
        int nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

        if (androidDay >= Calendar.MONDAY && androidDay <= Calendar.SATURDAY) {
            int todayCount = 0;
            int completed = 0;
            for (ScheduleSlot slot : allSchedules) {
                if (slot != null && slot.getDayOfWeek() == androidDay) {
                    todayCount++;
                    int endMin = ScheduleSlot.toMinutes(slot.getEndTime());
                    if (endMin >= 0 && endMin <= nowMinutes) {
                        completed++;
                    }
                }
            }
            if (todayCount > 0) {
                return (float) completed / (float) todayCount;
            }
        }
        return 1.0f;
    }

    @NonNull
    public static SpotlightCandidate findBestSpotlight(
            @Nullable List<StudyTask> tasks,
            @Nullable List<ScheduleSlot> allSchedules,
            long nowMillis
    ) {
        StudyTask earliestDeadline = null;
        StudyTask firstPendingTask = null;

        if (tasks != null) {
            for (StudyTask task : tasks) {
                if (task == null || task.isDone()) {
                    continue;
                }
                if (task.hasDueAt()) {
                    if (earliestDeadline == null
                            || task.getDueAtMillis() < earliestDeadline.getDueAtMillis()
                            || (task.getDueAtMillis() == earliestDeadline.getDueAtMillis()
                            && task.getId() < earliestDeadline.getId())) {
                        earliestDeadline = task;
                    }
                } else if (firstPendingTask == null) {
                    firstPendingTask = task;
                }
            }
        }

        if (earliestDeadline != null) {
            long remaining = earliestDeadline.getDueAtMillis() - nowMillis;
            DeadlineUrgency urgency;
            if (remaining < 0L) {
                urgency = DeadlineUrgency.OVERDUE;
            } else if (remaining <= MILLIS_PER_DAY) {
                urgency = DeadlineUrgency.SOON_HOURS;
            } else {
                urgency = DeadlineUrgency.UPCOMING_DAYS;
            }
            return new SpotlightCandidate(
                    SpotlightKind.DEADLINE_TASK,
                    earliestDeadline,
                    null,
                    false,
                    isSameDay(earliestDeadline.getDueAtMillis(), nowMillis),
                    urgency,
                    remaining
            );
        }

        SpotlightCandidate nextSchedule = findNextScheduleCandidate(allSchedules, nowMillis);
        if (nextSchedule != null) {
            return nextSchedule;
        }

        if (firstPendingTask != null) {
            return new SpotlightCandidate(
                    SpotlightKind.PENDING_TASK,
                    firstPendingTask,
                    null,
                    false,
                    true,
                    DeadlineUrgency.NONE,
                    0L
            );
        }

        return new SpotlightCandidate(
                SpotlightKind.ALL_CLEAR,
                null,
                null,
                false,
                false,
                DeadlineUrgency.NONE,
                0L
        );
    }

    @Nullable
    private static SpotlightCandidate findNextScheduleCandidate(
            @Nullable List<ScheduleSlot> allSchedules,
            long nowMillis
    ) {
        if (allSchedules == null || allSchedules.isEmpty()) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMillis);
        int androidDay = cal.get(Calendar.DAY_OF_WEEK);
        int nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

        if (androidDay >= Calendar.MONDAY && androidDay <= Calendar.SATURDAY) {
            ScheduleSlot bestToday = null;
            int bestTodayStart = Integer.MAX_VALUE;
            boolean bestTodayActive = false;

            for (ScheduleSlot slot : allSchedules) {
                if (slot == null || slot.getDayOfWeek() != androidDay) {
                    continue;
                }
                int startMin = ScheduleSlot.toMinutes(slot.getStartTime());
                int endMin = ScheduleSlot.toMinutes(slot.getEndTime());
                if (startMin >= 0 && endMin > nowMinutes && startMin < bestTodayStart) {
                    bestToday = slot;
                    bestTodayStart = startMin;
                    bestTodayActive = startMin <= nowMinutes;
                }
            }
            if (bestToday != null) {
                return new SpotlightCandidate(
                        SpotlightKind.SCHEDULE_SLOT,
                        null,
                        bestToday,
                        bestTodayActive,
                        true,
                        DeadlineUrgency.NONE,
                        0L
                );
            }
        }

        int startSearchDay = (androidDay >= Calendar.MONDAY && androidDay <= Calendar.SATURDAY)
                ? androidDay
                : ScheduleSlot.DAY_MIN;
        for (int offset = 0; offset < 6; offset++) {
            int candidateDay = ScheduleSlot.DAY_MIN + ((startSearchDay - ScheduleSlot.DAY_MIN + offset) % 6);
            if (androidDay >= Calendar.MONDAY && androidDay <= Calendar.SATURDAY && offset == 0) {
                continue;
            }
            ScheduleSlot bestDaySlot = findEarliestSlotForDay(allSchedules, candidateDay);
            if (bestDaySlot != null) {
                return new SpotlightCandidate(
                        SpotlightKind.SCHEDULE_SLOT,
                        null,
                        bestDaySlot,
                        false,
                        false,
                        DeadlineUrgency.NONE,
                        0L
                );
            }
        }
        return null;
    }

    @Nullable
    private static ScheduleSlot findEarliestSlotForDay(@NonNull List<ScheduleSlot> allSchedules, int dayOfWeek) {
        ScheduleSlot best = null;
        int bestStart = Integer.MAX_VALUE;
        for (ScheduleSlot slot : allSchedules) {
            if (slot == null || slot.getDayOfWeek() != dayOfWeek) {
                continue;
            }
            int startMin = ScheduleSlot.toMinutes(slot.getStartTime());
            int effectiveStart = startMin >= 0 ? startMin : Integer.MAX_VALUE;
            if (best == null || effectiveStart < bestStart) {
                best = slot;
                bestStart = effectiveStart;
            }
        }
        return best;
    }

    public static long buildUpcomingDueAtMillis(int dayOffset, int hourOfDay, int minute, long nowMillis, boolean shiftIfPast) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(nowMillis);
        target.add(Calendar.DAY_OF_YEAR, dayOffset);
        target.set(Calendar.HOUR_OF_DAY, hourOfDay);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (shiftIfPast && target.getTimeInMillis() <= nowMillis + (30L * MILLIS_PER_MINUTE)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        return target.getTimeInMillis();
    }

    @NonNull
    public static String buildTaskMeta(
            @NonNull Context context,
            @NonNull StudyTask task,
            long nowMillis
    ) {
        String subject = hasText(task.getSubject()) ? task.getSubject().trim() : "";
        if (task.hasDueAt()) {
            String dueText = formatDueDateTime(context, task.getDueAtMillis(), nowMillis);
            if (!subject.isEmpty()) {
                return context.getString(R.string.home_spotlight_due_remaining_format, subject, dueText);
            }
            return dueText;
        }
        if (!subject.isEmpty()) {
            return subject;
        }
        return context.getString(R.string.task_quick_meta);
    }

    @NonNull
    public static String formatDueDateTime(@NonNull Context context, long dueAtMillis, long nowMillis) {
        if (dueAtMillis <= 0L) {
            return context.getString(R.string.task_no_due_label);
        }
        Locale locale = Locale.getDefault();
        String timeStr = new SimpleDateFormat("HH:mm", locale).format(new Date(dueAtMillis));
        long dayDiff = computeDayDiff(nowMillis, dueAtMillis);
        if (dayDiff == 0L) {
            return context.getString(R.string.task_due_prefix_today, timeStr);
        }
        if (dayDiff == 1L) {
            return context.getString(R.string.task_due_prefix_tomorrow, timeStr);
        }
        String dateStr = new SimpleDateFormat("dd/MM", locale).format(new Date(dueAtMillis));
        return context.getString(R.string.task_due_prefix_date, timeStr, dateStr);
    }

    @NonNull
    public static String formatRelativeDelta(@NonNull Context context, long dueAtMillis, long nowMillis) {
        long diff = dueAtMillis - nowMillis;
        if (diff < 0L) {
            long totalMinutes = Math.max(1L, ((-diff) + MILLIS_PER_MINUTE - 1L) / MILLIS_PER_MINUTE);
            if (totalMinutes < 60L) {
                return context.getString(R.string.task_overdue_minutes, (int) totalMinutes);
            }
            long totalHours = totalMinutes / 60L;
            if (totalHours < 24L) {
                return context.getString(R.string.task_overdue_hours, (int) totalHours);
            }
            long totalDays = Math.max(1L, totalHours / 24L);
            return context.getString(R.string.task_overdue_days, (int) totalDays);
        }
        long totalMinutes = Math.max(1L, (diff + MILLIS_PER_MINUTE - 1L) / MILLIS_PER_MINUTE);
        if (totalMinutes < 60L) {
            return context.getString(R.string.task_remaining_minutes, (int) totalMinutes);
        }
        long totalHours = totalMinutes / 60L;
        if (totalHours < 24L) {
            return context.getString(R.string.task_remaining_hours, (int) totalHours);
        }
        long totalDays = Math.max(1L, totalHours / 24L);
        return context.getString(R.string.task_remaining_days, (int) totalDays);
    }

    private static boolean isSameDay(long firstMillis, long secondMillis) {
        return computeDayDiff(firstMillis, secondMillis) == 0L;
    }

    private static long computeDayDiff(long fromMillis, long toMillis) {
        long startFrom = startOfDayMillis(fromMillis);
        long startTo = startOfDayMillis(toMillis);
        return Math.round((double) (startTo - startFrom) / (double) MILLIS_PER_DAY);
    }

    private static long startOfDayMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static boolean hasText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }
}
