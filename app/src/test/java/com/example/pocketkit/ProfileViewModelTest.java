package com.example.pocketkit;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.profile.ProfileGpaHelper;
import com.example.pocketkit.viewmodel.ProfileViewModel;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ProfileViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    @Test
    public void updateSimulatedProgress_recalculatesProjectedGpaAndPersists() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        ProfileViewModel viewModel = new ProfileViewModel(repository);

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
        ProfileViewModel viewModel = new ProfileViewModel(repository);

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
}
