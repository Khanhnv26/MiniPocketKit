package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketkit.R;
import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.network.exception.CloudException;
import com.example.pocketkit.network.gateway.CloudGateway;
import com.example.pocketkit.network.utils.BackupPayloadSanitizer;
import com.example.pocketkit.profile.ProfileGpaHelper;
import com.example.pocketkit.reminder.ReminderGateway;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public class ProfileViewModel extends ViewModel {

    public static final class ProfileUiState {
        @NonNull
        public final StudentProfile profile;
        public final int simulatedProgress;
        public final float scoreTen;
        public final float projectedGpa;
        @NonNull
        public final ProfileGpaHelper.GpaRank rank;

        public ProfileUiState(
                @NonNull StudentProfile profile,
                int simulatedProgress,
                float scoreTen,
                float projectedGpa,
                @NonNull ProfileGpaHelper.GpaRank rank
        ) {
            this.profile = profile;
            this.simulatedProgress = simulatedProgress;
            this.scoreTen = scoreTen;
            this.projectedGpa = projectedGpa;
            this.rank = rank;
        }
    }

    public static final class CloudUiState {
        public final boolean loggedIn;
        @NonNull
        public final String email;
        @NonNull
        public final String fullName;
        public final long lastBackupEpoch;
        public final boolean busy;

        public CloudUiState(
                boolean loggedIn,
                @NonNull String email,
                @NonNull String fullName,
                long lastBackupEpoch,
                boolean busy
        ) {
            this.loggedIn = loggedIn;
            this.email = email;
            this.fullName = fullName;
            this.lastBackupEpoch = lastBackupEpoch;
            this.busy = busy;
        }
    }

    @FunctionalInterface
    private interface CloudAuthAction {
        void run() throws CloudException;
    }

    private final PocketKitRepository repository;
    private final CloudGateway cloudGateway;
    private final ReminderGateway reminderGateway;
    private final Executor ioExecutor;
    private final Executor networkExecutor;

    private final MutableLiveData<ProfileUiState> profileState = new MutableLiveData<>();
    private final MutableLiveData<CloudUiState> cloudState = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Boolean>> dataRestoredEvent = new MutableLiveData<>();

    private StudentProfile currentProfile;
    private int currentProgress;
    private volatile boolean cloudBusy = false;

    public ProfileViewModel(
            @NonNull PocketKitRepository repository,
            @NonNull CloudGateway cloudGateway,
            @NonNull ReminderGateway reminderGateway,
            @NonNull Executor ioExecutor,
            @NonNull Executor networkExecutor
    ) {
        this.repository = repository;
        this.cloudGateway = cloudGateway;
        this.reminderGateway = reminderGateway;
        this.ioExecutor = ioExecutor;
        this.networkExecutor = networkExecutor;
        loadProfile();
        postCloudState();
    }

    @NonNull
    public LiveData<ProfileUiState> getProfileState() {
        return profileState;
    }

    @NonNull
    public LiveData<CloudUiState> getCloudState() {
        return cloudState;
    }

    @NonNull
    public LiveData<UiEvent<UiMessage>> getToastEvent() {
        return toastEvent;
    }

    @NonNull
    public LiveData<UiEvent<Boolean>> getDataRestoredEvent() {
        return dataRestoredEvent;
    }

    @NonNull
    public StudentProfile getCurrentProfileSnapshot() {
        return currentProfile;
    }

    public void loadProfile() {
        currentProfile = repository.loadProfile();
        currentProgress = ProfileGpaHelper.clampSimulatedProgress(repository.loadSimulatedScoreProgress());
        emitState();
    }

    public void saveProfile(@NonNull StudentProfile updatedProfile) {
        repository.saveProfile(updatedProfile);
        currentProfile = updatedProfile;
        emitState();
        toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.profile_toast_updated)));
    }

    public void updateSimulatedProgress(int progressValue, boolean persist) {
        currentProgress = ProfileGpaHelper.clampSimulatedProgress(progressValue);
        if (persist) {
            repository.saveSimulatedScoreProgress(currentProgress);
        }
        emitState();
    }

    public void persistCurrentSimulatedProgress(int progressValue) {
        currentProgress = ProfileGpaHelper.clampSimulatedProgress(progressValue);
        repository.saveSimulatedScoreProgress(currentProgress);
    }

    public void login(@NonNull String email, @NonNull String password) {
        executeAuth(R.string.cloud_auth_login_success, () -> cloudGateway.login(email, password));
    }

    public void register(@NonNull String fullName, @NonNull String email, @NonNull String password) {
        executeAuth(R.string.cloud_auth_register_success, () -> cloudGateway.register(fullName, email, password));
    }

    public void logout() {
        if (cloudBusy) {
            return;
        }
        cloudGateway.logout();
        postCloudState();
        toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_logout_success)));
    }

    public void backupNow() {
        if (cloudBusy) {
            return;
        }
        if (!cloudGateway.isLoggedIn()) {
            postCloudState();
            toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_error_login_required)));
            return;
        }
        cloudBusy = true;
        postCloudState();
        ioExecutor.execute(() -> {
            ApiModels.BackupPayload payload = BackupPayloadSanitizer.sanitize(
                    repository.loadSimulatedScoreProgress(),
                    repository.loadProfile(),
                    repository.getAllCategories(),
                    repository.getAllSchedules(),
                    repository.getAllTasks(),
                    repository.getNotesFiltered("", DatabaseHelper.FILTER_ALL)
            );
            networkExecutor.execute(() -> {
                try {
                    cloudGateway.saveBackup(payload);
                    cloudBusy = false;
                    postCloudState();
                    toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_backup_success)));
                } catch (CloudException ex) {
                    cloudBusy = false;
                    postCloudState();
                    toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_message_raw, ex.getMessage())));
                }
            });
        });
    }

    public void restoreLatest() {
        if (cloudBusy) {
            return;
        }
        if (!cloudGateway.isLoggedIn()) {
            postCloudState();
            toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_error_login_required)));
            return;
        }
        cloudBusy = true;
        postCloudState();
        networkExecutor.execute(() -> {
            try {
                ApiModels.BackupSnapshotResponse snapshot = cloudGateway.getLatestBackup();
                ApiModels.BackupPayload payload = snapshot.getPayload();
                if (payload == null || payload.getProfile() == null) {
                    cloudBusy = false;
                    postCloudState();
                    toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_restore_empty_error)));
                    return;
                }
                ioExecutor.execute(() -> applyRestoredSnapshot(payload));
            } catch (CloudException ex) {
                cloudBusy = false;
                postCloudState();
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_message_raw, ex.getMessage())));
            }
        });
    }

    private void executeAuth(@StringRes int successMessageRes, @NonNull CloudAuthAction authAction) {
        if (cloudBusy) {
            return;
        }
        cloudBusy = true;
        postCloudState();
        networkExecutor.execute(() -> {
            try {
                authAction.run();
                cloudBusy = false;
                postCloudState();
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(successMessageRes)));
            } catch (CloudException ex) {
                cloudBusy = false;
                postCloudState();
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_message_raw, ex.getMessage())));
            }
        });
    }

    private void applyRestoredSnapshot(@NonNull ApiModels.BackupPayload payload) {
        try {
            List<StudyTask> oldTasks = repository.getAllTasks();
            for (StudyTask oldTask : oldTasks) {
                reminderGateway.cancelReminder(oldTask.getId());
            }

            List<String> categories = payload.getCategories() != null ? payload.getCategories() : Collections.emptyList();
            List<ScheduleSlot> schedules = payload.getSchedules() != null ? payload.getSchedules() : Collections.emptyList();
            List<StudyTask> tasks = payload.getTasks() != null ? payload.getTasks() : Collections.emptyList();
            List<Note> notes = payload.getNotes() != null ? payload.getNotes() : Collections.emptyList();

            repository.replaceAllBackupData(categories, schedules, tasks, notes);
            repository.saveProfile(payload.getProfile());
            repository.saveSimulatedScoreProgress(
                    ProfileGpaHelper.clampSimulatedProgress(payload.getSimulatedScoreProgress())
            );

            reminderGateway.rescheduleAllPendingReminders();

            currentProfile = repository.loadProfile();
            currentProgress = ProfileGpaHelper.clampSimulatedProgress(repository.loadSimulatedScoreProgress());
            emitState();
            cloudBusy = false;
            postCloudState();
            dataRestoredEvent.postValue(new UiEvent<>(true));
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_restore_success)));
        } catch (RuntimeException ex) {
            cloudBusy = false;
            postCloudState();
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_restore_empty_error)));
        }
    }

    private void emitState() {
        float scoreTen = ProfileGpaHelper.progressToScoreTen(currentProgress);
        float projectedGpa = ProfileGpaHelper.calculateProjectedGpa(
                currentProfile.getBaselineGpa(),
                currentProfile.getEarnedCredits(),
                currentProfile.getCurrentTermCredits(),
                scoreTen
        );
        ProfileGpaHelper.GpaRank rank = ProfileGpaHelper.classifyGpaRank(projectedGpa);
        profileState.postValue(new ProfileUiState(
                currentProfile,
                currentProgress,
                scoreTen,
                projectedGpa,
                rank
        ));
    }

    private void postCloudState() {
        cloudState.postValue(new CloudUiState(
                cloudGateway.isLoggedIn(),
                cloudGateway.getEmail(),
                cloudGateway.getFullName(),
                cloudGateway.getLastBackupEpoch(),
                cloudBusy
        ));
    }
}
