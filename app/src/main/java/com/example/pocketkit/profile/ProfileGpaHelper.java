package com.example.pocketkit.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class ProfileGpaHelper {

    public static final int DEFAULT_EARNED_CREDITS = 114;
    public static final int DEFAULT_TOTAL_CREDITS = 145;
    public static final int DEFAULT_ATTENDANCE_PERCENT = 96;
    public static final float DEFAULT_BASELINE_GPA = 3.44f;
    public static final int DEFAULT_TERM_CREDITS = 20;
    public static final int DEFAULT_SIMULATED_PROGRESS = 85;
    public static final int MAX_SIMULATED_PROGRESS = 100;
    public static final int MAX_CREDITS_LIMIT = 400;
    public static final int MAX_TERM_CREDITS_LIMIT = 60;
    public static final float GPA_MAX = 4.0f;
    public static final float RANK_EXCELLENT_MIN = 3.60f;
    public static final float RANK_GOOD_MIN = 3.20f;
    public static final float RANK_FAIR_MIN = 2.50f;

    private static final Locale VI_LOCALE = Locale.forLanguageTag("vi-VN");

    public enum GpaRank {
        EXCELLENT,
        GOOD,
        FAIR,
        AVERAGE
    }

    public enum ValidationError {
        NONE,
        EMPTY_NAME,
        INVALID_EARNED_CREDITS,
        INVALID_TOTAL_CREDITS,
        EARNED_EXCEEDS_TOTAL,
        INVALID_ATTENDANCE,
        INVALID_BASELINE_GPA,
        INVALID_TERM_CREDITS
    }

    private ProfileGpaHelper() {
    }

    @NonNull
    public static String buildInitials(@Nullable String fullName, @NonNull String fallback) {
        if (fullName == null) {
            return fallback.toUpperCase(VI_LOCALE);
        }
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) {
            return fallback.toUpperCase(VI_LOCALE);
        }
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 1) {
            String token = tokens[0];
            int end = Math.min(2, token.length());
            return token.substring(0, end).toUpperCase(VI_LOCALE);
        }
        String first = tokens[0].substring(0, 1);
        String last = tokens[tokens.length - 1].substring(0, 1);
        return (first + last).toUpperCase(VI_LOCALE);
    }

    public static int clampSimulatedProgress(int progress) {
        return Math.max(0, Math.min(MAX_SIMULATED_PROGRESS, progress));
    }

    public static float progressToScoreTen(int progress) {
        return clampSimulatedProgress(progress) / 10f;
    }

    public static float scoreTenToFour(float scoreTen) {
        if (Float.isNaN(scoreTen)) {
            return 0f;
        }
        return clampGpa(scoreTen * 0.4f);
    }

    public static float calculateProjectedGpa(
            float baselineGpa,
            int earnedCredits,
            int currentTermCredits,
            float scoreTen
    ) {
        float safeBaseline = clampGpa(baselineGpa);
        int safeEarned = Math.max(0, earnedCredits);
        int safeTerm = Math.max(0, currentTermCredits);
        float scoreFour = scoreTenToFour(scoreTen);

        if (safeEarned == 0 && safeTerm == 0) {
            return safeBaseline;
        }
        if (safeTerm == 0) {
            return safeBaseline;
        }
        if (safeEarned == 0) {
            return scoreFour;
        }
        float weighted = (safeBaseline * safeEarned + scoreFour * safeTerm)
                / (float) (safeEarned + safeTerm);
        return clampGpa(weighted);
    }

    @NonNull
    public static GpaRank classifyGpaRank(float projectedGpa) {
        if (projectedGpa >= RANK_EXCELLENT_MIN) {
            return GpaRank.EXCELLENT;
        }
        if (projectedGpa >= RANK_GOOD_MIN) {
            return GpaRank.GOOD;
        }
        if (projectedGpa >= RANK_FAIR_MIN) {
            return GpaRank.FAIR;
        }
        return GpaRank.AVERAGE;
    }

    @NonNull
    public static ValidationError validateProfile(
            @Nullable String fullName,
            int earnedCredits,
            int totalCredits,
            int attendancePercent,
            float baselineGpa,
            int currentTermCredits
    ) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return ValidationError.EMPTY_NAME;
        }
        if (earnedCredits < 0 || earnedCredits > MAX_CREDITS_LIMIT) {
            return ValidationError.INVALID_EARNED_CREDITS;
        }
        if (totalCredits <= 0 || totalCredits > MAX_CREDITS_LIMIT) {
            return ValidationError.INVALID_TOTAL_CREDITS;
        }
        if (earnedCredits > totalCredits) {
            return ValidationError.EARNED_EXCEEDS_TOTAL;
        }
        if (attendancePercent < 0 || attendancePercent > 100) {
            return ValidationError.INVALID_ATTENDANCE;
        }
        if (Float.isNaN(baselineGpa) || Float.isInfinite(baselineGpa) || baselineGpa < 0f || baselineGpa > GPA_MAX) {
            return ValidationError.INVALID_BASELINE_GPA;
        }
        if (currentTermCredits < 0 || currentTermCredits > MAX_TERM_CREDITS_LIMIT) {
            return ValidationError.INVALID_TERM_CREDITS;
        }
        return ValidationError.NONE;
    }

    private static float clampGpa(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0f;
        }
        return Math.max(0f, Math.min(GPA_MAX, value));
    }
}
