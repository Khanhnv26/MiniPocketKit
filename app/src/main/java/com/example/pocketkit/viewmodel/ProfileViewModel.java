package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketkit.R;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.profile.ProfileGpaHelper;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;

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

    private final PocketKitRepository repository;
    private final MutableLiveData<ProfileUiState> profileState = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();

    private StudentProfile currentProfile;
    private int currentProgress;

    public ProfileViewModel(@NonNull PocketKitRepository repository) {
        this.repository = repository;
        loadProfile();
    }

    @NonNull
    public LiveData<ProfileUiState> getProfileState() {
        return profileState;
    }

    @NonNull
    public LiveData<UiEvent<UiMessage>> getToastEvent() {
        return toastEvent;
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

    private void emitState() {
        float scoreTen = ProfileGpaHelper.progressToScoreTen(currentProgress);
        float projectedGpa = ProfileGpaHelper.calculateProjectedGpa(
                currentProfile.getBaselineGpa(),
                currentProfile.getEarnedCredits(),
                currentProfile.getCurrentTermCredits(),
                scoreTen
        );
        ProfileGpaHelper.GpaRank rank = ProfileGpaHelper.classifyGpaRank(projectedGpa);
        profileState.setValue(new ProfileUiState(
                currentProfile,
                currentProgress,
                scoreTen,
                projectedGpa,
                rank
        ));
    }
}
