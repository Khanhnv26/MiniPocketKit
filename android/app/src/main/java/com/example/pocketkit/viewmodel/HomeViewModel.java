package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketkit.R;
import com.example.pocketkit.home.HomeSummaryHelper;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.reminder.ReminderGateway;
import com.example.pocketkit.reminder.TaskReminderHelper;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class HomeViewModel extends ViewModel {

    public static final long FOCUS_DURATION_MS = 25L * 60L * 1000L;
    private static final long TIMER_TICK_MS = 1000L;

    public interface TimeProvider {
        long nowMillis();
    }

    public static final class HomeDashboardUiState {
        @NonNull
        public final List<StudyTask> tasks;
        @NonNull
        public final HomeSummaryHelper.DeadlineProgress deadlineProgress;
        public final float classProgress;
        @NonNull
        public final HomeSummaryHelper.SpotlightCandidate spotlightCandidate;
        @Nullable
        public final Note pinnedNote;
        public final long snapshotNowMillis;
        public final boolean animateProgress;

        public HomeDashboardUiState(
                @NonNull List<StudyTask> tasks,
                @NonNull HomeSummaryHelper.DeadlineProgress deadlineProgress,
                float classProgress,
                @NonNull HomeSummaryHelper.SpotlightCandidate spotlightCandidate,
                @Nullable Note pinnedNote,
                long snapshotNowMillis,
                boolean animateProgress
        ) {
            this.tasks = tasks;
            this.deadlineProgress = deadlineProgress;
            this.classProgress = classProgress;
            this.spotlightCandidate = spotlightCandidate;
            this.pinnedNote = pinnedNote;
            this.snapshotNowMillis = snapshotNowMillis;
            this.animateProgress = animateProgress;
        }
    }

    public static final class TimerUiState {
        public final long remainingMs;
        public final boolean running;
        public final boolean started;
        public final float focusProgress;
        public final boolean animateRing;

        public TimerUiState(
                long remainingMs,
                boolean running,
                boolean started,
                float focusProgress,
                boolean animateRing
        ) {
            this.remainingMs = remainingMs;
            this.running = running;
            this.started = started;
            this.focusProgress = focusProgress;
            this.animateRing = animateRing;
        }
    }

    private final PocketKitRepository repository;
    private final ReminderGateway reminderGateway;
    private final Executor ioExecutor;
    private final TimeProvider timeProvider;

    private final MutableLiveData<HomeDashboardUiState> dashboardState = new MutableLiveData<>();
    private final MutableLiveData<TimerUiState> timerState = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<StudyTask>> notificationPermissionEvent = new MutableLiveData<>();

    private final ScheduledExecutorService timerScheduler;
    private ScheduledFuture<?> timerFuture;
    private long remainingTimerMs = FOCUS_DURATION_MS;
    private long timerEndAtMillis = 0L;
    private boolean timerRunning = false;
    private boolean timerStarted = false;

    public HomeViewModel(
            @NonNull PocketKitRepository repository,
            @NonNull ReminderGateway reminderGateway,
            @NonNull Executor ioExecutor
    ) {
        this(repository, reminderGateway, ioExecutor, System::currentTimeMillis);
    }

    public HomeViewModel(
            @NonNull PocketKitRepository repository,
            @NonNull ReminderGateway reminderGateway,
            @NonNull Executor ioExecutor,
            @NonNull TimeProvider timeProvider
    ) {
        this.repository = repository;
        this.reminderGateway = reminderGateway;
        this.ioExecutor = ioExecutor;
        this.timeProvider = timeProvider;
        this.timerScheduler = Executors.newSingleThreadScheduledExecutor();
        emitTimerState(false);
    }

    @NonNull
    public LiveData<HomeDashboardUiState> getDashboardState() {
        return dashboardState;
    }

    @NonNull
    public LiveData<TimerUiState> getTimerState() {
        return timerState;
    }

    @NonNull
    public LiveData<UiEvent<UiMessage>> getToastEvent() {
        return toastEvent;
    }

    @NonNull
    public LiveData<UiEvent<StudyTask>> getNotificationPermissionEvent() {
        return notificationPermissionEvent;
    }

    public void reschedulePendingRemindersOnStart() {
        ioExecutor.execute(reminderGateway::rescheduleAllPendingReminders);
    }

    public void loadDashboard(boolean animate) {
        ioExecutor.execute(() -> postDashboardSnapshot(animate));
    }

    public void addQuickTask(@NonNull String rawTitle, @NonNull String defaultTag) {
        String title = rawTitle.trim();
        if (title.isEmpty()) {
            return;
        }
        ioExecutor.execute(() -> {
            StudyTask task = StudyTask.builder()
                    .title(title)
                    .subject("")
                    .tag(defaultTag)
                    .dueAtMillis(0L)
                    .done(false)
                    .createdAtMillis(timeProvider.nowMillis())
                    .remindBeforeMinutes(TaskReminderHelper.REMIND_NONE)
                    .build();
            repository.insertTask(task);
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.task_toast_created)));
            postDashboardSnapshot(true);
        });
    }

    public void saveTask(@NonNull StudyTask savedTask, boolean notificationPermissionGranted) {
        ioExecutor.execute(() -> {
            if (savedTask.getId() > 0) {
                repository.updateTask(savedTask);
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.task_toast_updated)));
            } else {
                long insertedId = repository.insertTask(savedTask);
                if (insertedId > 0L) {
                    savedTask.setId((int) insertedId);
                }
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.task_toast_created)));
            }
            reminderGateway.clearFiredMarker(savedTask.getId());
            handleReminderScheduling(savedTask, notificationPermissionGranted);
            postDashboardSnapshot(true);
        });
    }

    public void deleteTask(@NonNull StudyTask targetTask) {
        ioExecutor.execute(() -> {
            repository.deleteTask(targetTask.getId());
            reminderGateway.cancelReminder(targetTask.getId());
            reminderGateway.clearFiredMarker(targetTask.getId());
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.task_toast_deleted)));
            postDashboardSnapshot(true);
        });
    }

    public void toggleTaskCompletion(
            @NonNull StudyTask task,
            boolean nextDone,
            boolean notificationPermissionGranted
    ) {
        final StudyTask updated = StudyTask.builder()
                .id(task.getId())
                .title(task.getTitle())
                .subject(task.getSubject())
                .tag(task.getTag())
                .dueAtMillis(task.getDueAtMillis())
                .done(nextDone)
                .createdAtMillis(task.getCreatedAtMillis())
                .remindBeforeMinutes(task.getRemindBeforeMinutes())
                .build();
        ioExecutor.execute(() -> {
            repository.setTaskDone(updated.getId(), nextDone);
            reminderGateway.clearFiredMarker(updated.getId());
            if (nextDone) {
                reminderGateway.cancelReminder(updated.getId());
            } else {
                handleReminderScheduling(updated, notificationPermissionGranted);
            }
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(
                    nextDone ? R.string.task_toast_completed : R.string.task_toast_reopened,
                    updated.getTitle()
            )));
            postDashboardSnapshot(true);
        });
    }

    public void onNotificationPermissionResult(boolean granted, @Nullable StudyTask pendingTask) {
        if (pendingTask == null) {
            return;
        }
        if (!granted) {
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.task_reminder_permission_denied)));
            return;
        }
        ioExecutor.execute(() -> reminderGateway.scheduleOrCancel(pendingTask));
    }

    public synchronized void toggleFocusTimer() {
        if (timerRunning) {
            pauseTimerInternal();
        } else {
            startTimerInternal();
        }
    }

    public synchronized void resetFocusTimer() {
        cancelTimerFuture();
        timerRunning = false;
        timerStarted = false;
        remainingTimerMs = FOCUS_DURATION_MS;
        timerEndAtMillis = 0L;
        emitTimerState(true);
    }

    public synchronized void stepFocusTimerForTick() {
        if (!timerRunning) {
            return;
        }
        long left = Math.max(0L, timerEndAtMillis - timeProvider.nowMillis());
        remainingTimerMs = left;
        if (left == 0L) {
            cancelTimerFuture();
            timerRunning = false;
            timerStarted = false;
            timerEndAtMillis = 0L;
            emitTimerState(true);
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.home_timer_finished_toast)));
            return;
        }
        emitTimerState(false);
    }

    @Override
    protected void onCleared() {
        cancelTimerFuture();
        timerScheduler.shutdownNow();
    }

    private void startTimerInternal() {
        cancelTimerFuture();
        if (remainingTimerMs <= 0L) {
            remainingTimerMs = FOCUS_DURATION_MS;
        }
        timerRunning = true;
        timerStarted = true;
        timerEndAtMillis = timeProvider.nowMillis() + remainingTimerMs;
        emitTimerState(false);
        timerFuture = timerScheduler.scheduleWithFixedDelay(
                this::stepFocusTimerForTick,
                TIMER_TICK_MS,
                TIMER_TICK_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void pauseTimerInternal() {
        remainingTimerMs = Math.max(0L, timerEndAtMillis - timeProvider.nowMillis());
        cancelTimerFuture();
        timerRunning = false;
        emitTimerState(false);
    }

    private void cancelTimerFuture() {
        if (timerFuture != null) {
            timerFuture.cancel(false);
            timerFuture = null;
        }
    }

    private void emitTimerState(boolean animateRing) {
        float elapsedFraction = 1f - ((float) remainingTimerMs / (float) FOCUS_DURATION_MS);
        float focusRingProgress = Math.max(0.40f, Math.min(1.0f, 0.40f + 0.60f * elapsedFraction));
        timerState.postValue(new TimerUiState(
                remainingTimerMs,
                timerRunning,
                timerStarted,
                focusRingProgress,
                animateRing
        ));
    }

    private void handleReminderScheduling(@NonNull StudyTask task, boolean permissionGranted) {
        if (!TaskReminderHelper.isReminderConfigured(task)) {
            reminderGateway.cancelReminder(task.getId());
            return;
        }
        if (!permissionGranted) {
            notificationPermissionEvent.postValue(new UiEvent<>(task));
            return;
        }
        reminderGateway.scheduleOrCancel(task);
    }

    private void postDashboardSnapshot(boolean animate) {
        long nowMillis = timeProvider.nowMillis();
        List<StudyTask> tasks = repository.getAllTasks();
        List<ScheduleSlot> schedules = repository.getAllSchedules();
        HomeSummaryHelper.DeadlineProgress deadlineProgress = HomeSummaryHelper.computeDeadlineProgress(tasks);
        float classProgress = HomeSummaryHelper.computeClassProgress(schedules, nowMillis);
        HomeSummaryHelper.SpotlightCandidate spotlight = HomeSummaryHelper.findBestSpotlight(tasks, schedules, nowMillis);
        Note pinnedNote = repository.getHighlightedHomeNote();

        dashboardState.postValue(new HomeDashboardUiState(
                tasks,
                deadlineProgress,
                classProgress,
                spotlight,
                pinnedNote,
                nowMillis,
                animate
        ));
    }
}
