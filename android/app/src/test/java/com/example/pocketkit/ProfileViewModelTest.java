package com.example.pocketkit;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.profile.ProfileGpaHelper;
import com.example.pocketkit.reminder.ReminderGateway;
import com.example.pocketkit.viewmodel.ProfileViewModel;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProfileViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    private static final class RecordingReminderGateway implements ReminderGateway {
        final List<Integer> cancelledIds = new ArrayList<>();
        int rescheduleCalls = 0;

        @Override
        public void scheduleOrCancel(@NonNull StudyTask task) {
        }

        @Override
        public void cancelReminder(int taskId) {
            cancelledIds.add(taskId);
        }

        @Override
        public void clearFiredMarker(int taskId) {
        }

        @Override
        public void rescheduleAllPendingReminders() {
            rescheduleCalls++;
        }
    }

    private ProfileViewModel createViewModel(
            @NonNull FakePocketKitRepository repository,
            @NonNull FakeCloudGateway cloudGateway,
            @NonNull ReminderGateway reminderGateway
    ) {
        return new ProfileViewModel(
                repository,
                cloudGateway,
                reminderGateway,
                Runnable::run,
                Runnable::run
        );
    }

    @Test
    public void updateSimulatedProgress_recalculatesProjectedGpaAndPersists() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        ProfileViewModel viewModel = createViewModel(repository, new FakeCloudGateway(), new RecordingReminderGateway());

        viewModel.updateSimulatedProgress(100, true);
        ProfileViewModel.ProfileUiState state = viewModel.getProfileState().getValue();
        assertNotNull(state);
        assertEquals(10.0f, state.scoreTen, 0.001f);
        assertEquals(100, repository.loadSimulatedScoreProgress());
        assertEquals(
                ProfileGpaHelper.calculateProjectedGpa(3.44f, 114, 20, 10.0f),
                state.projectedGpa,
                0.001f
        );
    }

    @Test
    public void saveProfile_updatesStateAndEmitsSingleToastEvent() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        ProfileViewModel viewModel = createViewModel(repository, new FakeCloudGateway(), new RecordingReminderGateway());

        StudentProfile updated = StudentProfile.builder()
                .fullName("Trần Minh Đức")
                .majorAndCohort("An toàn thông tin · K19")
                .earnedCredits(120)
                .totalCredits(145)
                .attendancePercent(98)
                .baselineGpa(3.70f)
                .currentTermCredits(20)
                .build();

        viewModel.saveProfile(updated);

        ProfileViewModel.ProfileUiState state = viewModel.getProfileState().getValue();
        assertNotNull(state);
        assertEquals("Trần Minh Đức", state.profile.getFullName());
        assertEquals(ProfileGpaHelper.GpaRank.EXCELLENT, state.rank);
        assertNotNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());
        assertNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void login_updatesCloudState() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        ProfileViewModel viewModel = createViewModel(repository, cloudGateway, new RecordingReminderGateway());

        viewModel.login("student@fpt.edu.vn", "123456");

        ProfileViewModel.CloudUiState cloudState = viewModel.getCloudState().getValue();
        assertNotNull(cloudState);
        assertTrue(cloudState.loggedIn);
        assertFalse(cloudState.busy);
        assertEquals("student@fpt.edu.vn", cloudState.email);
    }

    @Test
    public void register_updatesCloudStateWithEmailAndName() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        ProfileViewModel viewModel = createViewModel(repository, cloudGateway, new RecordingReminderGateway());

        viewModel.register("Nguyễn Khánh", "khanh@fpt.edu.vn", "123456");

        ProfileViewModel.CloudUiState cloudState = viewModel.getCloudState().getValue();
        assertNotNull(cloudState);
        assertTrue(cloudState.loggedIn);
        assertEquals("khanh@fpt.edu.vn", cloudState.email);
        assertEquals("Nguyễn Khánh", cloudState.fullName);
    }

    @Test
    public void backupNow_whenLoggedIn_sanitizesAndUploadsPayload() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        repository.insertCategory("PRM392");
        repository.insertSchedule(ScheduleSlot.builder()
                .dayOfWeek(2)
                .startTime("7:30")
                .endTime("9:45")
                .subject("PRM392")
                .build());
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        cloudGateway.loggedIn = true;
        cloudGateway.email = "student@fpt.edu.vn";
        ProfileViewModel viewModel = createViewModel(repository, cloudGateway, new RecordingReminderGateway());

        viewModel.backupNow();

        assertNotNull(cloudGateway.lastSavedPayload);
        assertEquals(1, cloudGateway.lastSavedPayload.getSchedules().size());
        assertEquals("07:30", cloudGateway.lastSavedPayload.getSchedules().get(0).getStartTime());
        assertEquals(1795000000000L, viewModel.getCloudState().getValue().lastBackupEpoch);
        assertNotNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void backupNow_whenNotLoggedIn_emitsToastWithoutUploading() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        ProfileViewModel viewModel = createViewModel(repository, cloudGateway, new RecordingReminderGateway());

        viewModel.backupNow();

        assertNull(cloudGateway.lastSavedPayload);
        assertNotNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void restoreLatest_replacesDataAndReschedulesReminders() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        repository.insertTask(StudyTask.builder().title("Việc cũ").build());

        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        cloudGateway.loggedIn = true;
        cloudGateway.email = "student@fpt.edu.vn";
        cloudGateway.latestBackup = new ApiModels.BackupSnapshotResponse(
                1795000000000L,
                ApiModels.BackupPayload.builder()
                        .simulatedScoreProgress(92)
                        .profile(StudentProfile.builder()
                                .fullName("Lê Cloud")
                                .majorAndCohort("SE1901")
                                .earnedCredits(120)
                                .totalCredits(145)
                                .attendancePercent(99)
                                .baselineGpa(3.60f)
                                .currentTermCredits(18)
                                .build())
                        .categories(Collections.singletonList("PRM392"))
                        .schedules(Collections.singletonList(ScheduleSlot.builder()
                                .dayOfWeek(2)
                                .startTime("07:30")
                                .endTime("09:45")
                                .subject("PRM392")
                                .build()))
                        .tasks(Collections.singletonList(StudyTask.builder()
                                .title("Việc mới")
                                .dueAtMillis(1796000000000L)
                                .remindBeforeMinutes(60)
                                .build()))
                        .notes(Collections.singletonList(Note.builder()
                                .title("Ghi chú Cloud")
                                .content("Nội dung")
                                .subject("PRM392")
                                .isPinned(1)
                                .isDone(0)
                                .updatedAt("08:30")
                                .build()))
                        .build()
        );

        RecordingReminderGateway reminderGateway = new RecordingReminderGateway();
        ProfileViewModel viewModel = createViewModel(repository, cloudGateway, reminderGateway);

        viewModel.restoreLatest();

        assertEquals(Collections.singletonList(1), reminderGateway.cancelledIds);
        assertEquals(1, reminderGateway.rescheduleCalls);
        assertEquals(1, repository.getAllTasks().size());
        assertEquals("Việc mới", repository.getAllTasks().get(0).getTitle());
        assertEquals(1, repository.getTotalNotesCount());
        assertEquals("Lê Cloud", viewModel.getProfileState().getValue().profile.getFullName());
        assertEquals(1795000000000L, viewModel.getCloudState().getValue().lastBackupEpoch);
        assertNotNull(viewModel.getDataRestoredEvent().getValue().getContentIfNotHandled());
    }
}
