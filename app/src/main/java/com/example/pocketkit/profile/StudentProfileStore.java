package com.example.pocketkit.profile;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.pocketkit.R;
import com.example.pocketkit.model.StudentProfile;

public final class StudentProfileStore {

    private static final String PREFS_NAME = "pocketkit_profile_prefs";
    private static final String KEY_FULL_NAME = "key_full_name";
    private static final String KEY_MAJOR_COHORT = "key_major_cohort";
    private static final String KEY_EARNED_CREDITS = "key_earned_credits";
    private static final String KEY_TOTAL_CREDITS = "key_total_credits";
    private static final String KEY_ATTENDANCE_PERCENT = "key_attendance_percent";
    private static final String KEY_BASELINE_GPA = "key_baseline_gpa";
    private static final String KEY_CURRENT_TERM_CREDITS = "key_current_term_credits";
    private static final String KEY_SIMULATED_PROGRESS = "key_simulated_progress";

    private final Context context;
    private final SharedPreferences prefs;

    public StudentProfileStore(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public StudentProfile loadProfile() {
        String defaultName = context.getString(R.string.profile_default_name);
        String defaultMajor = context.getString(R.string.profile_default_major);

        return StudentProfile.builder()
                .fullName(prefs.getString(KEY_FULL_NAME, defaultName))
                .majorAndCohort(prefs.getString(KEY_MAJOR_COHORT, defaultMajor))
                .earnedCredits(prefs.getInt(KEY_EARNED_CREDITS, ProfileGpaHelper.DEFAULT_EARNED_CREDITS))
                .totalCredits(prefs.getInt(KEY_TOTAL_CREDITS, ProfileGpaHelper.DEFAULT_TOTAL_CREDITS))
                .attendancePercent(prefs.getInt(KEY_ATTENDANCE_PERCENT, ProfileGpaHelper.DEFAULT_ATTENDANCE_PERCENT))
                .baselineGpa(prefs.getFloat(KEY_BASELINE_GPA, ProfileGpaHelper.DEFAULT_BASELINE_GPA))
                .currentTermCredits(prefs.getInt(KEY_CURRENT_TERM_CREDITS, ProfileGpaHelper.DEFAULT_TERM_CREDITS))
                .build();
    }

    public void saveProfile(@NonNull StudentProfile profile) {
        prefs.edit()
                .putString(KEY_FULL_NAME, profile.getFullName())
                .putString(KEY_MAJOR_COHORT, profile.getMajorAndCohort())
                .putInt(KEY_EARNED_CREDITS, profile.getEarnedCredits())
                .putInt(KEY_TOTAL_CREDITS, profile.getTotalCredits())
                .putInt(KEY_ATTENDANCE_PERCENT, profile.getAttendancePercent())
                .putFloat(KEY_BASELINE_GPA, profile.getBaselineGpa())
                .putInt(KEY_CURRENT_TERM_CREDITS, profile.getCurrentTermCredits())
                .apply();
    }

    public int loadSimulatedProgress() {
        int saved = prefs.getInt(KEY_SIMULATED_PROGRESS, ProfileGpaHelper.DEFAULT_SIMULATED_PROGRESS);
        return ProfileGpaHelper.clampSimulatedProgress(saved);
    }

    public void saveSimulatedProgress(int progress) {
        prefs.edit()
                .putInt(KEY_SIMULATED_PROGRESS, ProfileGpaHelper.clampSimulatedProgress(progress))
                .apply();
    }
}
