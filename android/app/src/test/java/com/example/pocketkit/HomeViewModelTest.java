package com.example.pocketkit;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.reminder.ReminderGateway;
import com.example.pocketkit.reminder.TaskReminderHelper;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;
import com.example.pocketkit.viewmodel.HomeViewModel;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HomeViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    private static final class RecordingReminderGateway implements ReminderGateway {
        final List<Integer> scheduledTaskIds = new ArrayList<>();
        final List<Integer> cancelledTaskIds = new ArrayList<>();
        final List<Integer> clearedTaskIds = new ArrayList<>();
        int rescheduleAllCalls = 0;

        @Override
        public void scheduleOrCancel(@NonNull StudyTask task) {
            scheduledTaskIds.add(task.getId());
        }

        @Override
        public void cancelReminder(int taskId) {
            cancelledTaskIds.add(taskId);
        }

        @Override
        public void clearFiredMarker(int taskId) {
            clearedTaskIds.add(taskId);
        }

        @Override
        public void rescheduleAllPendingReminders() {
            rescheduleAllCalls++;
        }
    }

    @Test
    public void saveTask_withReminderAndPermission_schedulesAndUpdatesDashboard() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        RecordingReminderGateway gateway = new RecordingReminderGateway();
        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        HomeViewModel viewModel = new HomeViewModel(repository, gateway, Runnable::run, clock::get);

        StudyTask newTask = StudyTask.builder()
                .title("Nop do an")
                .subject("PRM392")
                .tag("Deadline")
                .dueAtMillis(clock.get() + 7_200_000L)
                .remindBeforeMinutes(TaskReminderHelper.REMIND_60_MIN)
                .done(false)
                .build();

        viewModel.saveTask(newTask, true);

        HomeViewModel.HomeDashboardUiState state = viewModel.getDashboardState().getValue();
        assertNotNull(state);
        assertEquals(1, state.tasks.size());
        assertEquals(1, gateway.scheduledTaskIds.size());
        assertNull(viewModel.getNotificationPermissionEvent().getValue());
    }

    @Test
    public void saveTask_withoutNotificationPermission_requestsPermissionOnce() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        RecordingReminderGateway gateway = new RecordingReminderGateway();
        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        HomeViewModel viewModel = new HomeViewModel(repository, gateway, Runnable::run, clock::get);

        StudyTask newTask = StudyTask.builder()
                .title("Nop lab")
                .dueAtMillis(clock.get() + 3_600_000L)
                .remindBeforeMinutes(TaskReminderHelper.REMIND_30_MIN)
                .build();

        viewModel.saveTask(newTask, false);

        assertTrue(gateway.scheduledTaskIds.isEmpty());
        UiEvent<StudyTask> permEvent = viewModel.getNotificationPermissionEvent().getValue();
        assertNotNull(permEvent);
        StudyTask pending = permEvent.getContentIfNotHandled();
        assertNotNull(pending);
        assertNull(permEvent.getContentIfNotHandled());

        viewModel.onNotificationPermissionResult(true, pending);
        assertEquals(1, gateway.scheduledTaskIds.size());
    }

    @Test
    public void toggleTaskDone_cancelsReminderWhenCompletedAndReschedulesWhenReopened() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        RecordingReminderGateway gateway = new RecordingReminderGateway();
        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        HomeViewModel viewModel = new HomeViewModel(repository, gateway, Runnable::run, clock::get);

        StudyTask task = StudyTask.builder()
                .title("Bai tap")
                .dueAtMillis(clock.get() + 3_600_000L)
                .remindBeforeMinutes(TaskReminderHelper.REMIND_30_MIN)
                .done(false)
                .build();
        repository.insertTask(task);

        viewModel.toggleTaskCompletion(task, true, true);
        assertTrue(repository.getAllTasks().get(0).isDone());
        assertTrue(gateway.cancelledTaskIds.contains(task.getId()));

        viewModel.toggleTaskCompletion(repository.getAllTasks().get(0), false, true);
        assertFalse(repository.getAllTasks().get(0).isDone());
        assertTrue(gateway.scheduledTaskIds.contains(task.getId()));
    }

    @Test
    public void focusTimer_tracksCountdownFinishAndResetCleanly() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        RecordingReminderGateway gateway = new RecordingReminderGateway();
        AtomicLong clock = new AtomicLong(1_000_000L);
        HomeViewModel viewModel = new HomeViewModel(repository, gateway, Runnable::run, clock::get);

        HomeViewModel.TimerUiState initial = viewModel.getTimerState().getValue();
        assertNotNull(initial);
        assertEquals(HomeViewModel.FOCUS_DURATION_MS, initial.remainingMs);
        assertFalse(initial.running);

        viewModel.toggleFocusTimer();
        HomeViewModel.TimerUiState running = viewModel.getTimerState().getValue();
        assertNotNull(running);
        assertTrue(running.running);

        clock.addAndGet(HomeViewModel.FOCUS_DURATION_MS);
        viewModel.stepFocusTimerForTick();

        HomeViewModel.TimerUiState finished = viewModel.getTimerState().getValue();
        assertNotNull(finished);
        assertEquals(0L, finished.remainingMs);
        assertFalse(finished.running);
        UiEvent<UiMessage> toastEvent = viewModel.getToastEvent().getValue();
        assertNotNull(toastEvent);
        assertEquals(R.string.home_timer_finished_toast, toastEvent.peekContent().resId);

        viewModel.resetFocusTimer();
        HomeViewModel.TimerUiState reset = viewModel.getTimerState().getValue();
        assertNotNull(reset);
        assertEquals(HomeViewModel.FOCUS_DURATION_MS, reset.remainingMs);
    }
}
