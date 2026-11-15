package com.example.pocketkit;

import com.example.pocketkit.profile.ProfileGpaHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProfileGpaHelperTest {

    @Test
    public void buildInitials_multiWordVietnameseName_returnsFirstAndLastInitials() {
        assertEquals("NK", ProfileGpaHelper.buildInitials("Nguyễn Khánh", "PK"));
        assertEquals("NĐ", ProfileGpaHelper.buildInitials("  Nguyễn   Văn   Đức ", "PK"));
    }

    @Test
    public void buildInitials_singleWordOrBlank_usesPrefixOrFallback() {
        assertEquals("MI", ProfileGpaHelper.buildInitials("Minh", "NK"));
        assertEquals("A", ProfileGpaHelper.buildInitials("A", "NK"));
        assertEquals("NK", ProfileGpaHelper.buildInitials("   ", "nk"));
        assertEquals("NK", ProfileGpaHelper.buildInitials(null, "NK"));
    }

    @Test
    public void scoreConversions_clampOutOfRangeValues() {
        assertEquals(0, ProfileGpaHelper.clampSimulatedProgress(-10));
        assertEquals(100, ProfileGpaHelper.clampSimulatedProgress(150));
        assertEquals(8.5f, ProfileGpaHelper.progressToScoreTen(85), 0.001f);
        assertEquals(3.4f, ProfileGpaHelper.scoreTenToFour(8.5f), 0.001f);
        assertEquals(4.0f, ProfileGpaHelper.scoreTenToFour(12.0f), 0.001f);
        assertEquals(0f, ProfileGpaHelper.scoreTenToFour(Float.NaN), 0.001f);
    }

    @Test
    public void calculateProjectedGpa_handlesWeightedAndEdgeCases() {
        float defaultWeighted = ProfileGpaHelper.calculateProjectedGpa(3.44f, 114, 20, 8.5f);
        float expected = (3.44f * 114 + 3.4f * 20) / 134f;
        assertEquals(expected, defaultWeighted, 0.001f);

        assertEquals(3.44f, ProfileGpaHelper.calculateProjectedGpa(3.44f, 114, 0, 10.0f), 0.001f);
        assertEquals(3.6f, ProfileGpaHelper.calculateProjectedGpa(0f, 0, 18, 9.0f), 0.001f);
        assertEquals(4.0f, ProfileGpaHelper.calculateProjectedGpa(4.0f, 100, 20, 10.0f), 0.001f);
    }

    @Test
    public void classifyGpaRank_coversAllBoundaries() {
        assertEquals(ProfileGpaHelper.GpaRank.EXCELLENT, ProfileGpaHelper.classifyGpaRank(3.60f));
        assertEquals(ProfileGpaHelper.GpaRank.GOOD, ProfileGpaHelper.classifyGpaRank(3.59f));
        assertEquals(ProfileGpaHelper.GpaRank.GOOD, ProfileGpaHelper.classifyGpaRank(3.20f));
        assertEquals(ProfileGpaHelper.GpaRank.FAIR, ProfileGpaHelper.classifyGpaRank(3.19f));
        assertEquals(ProfileGpaHelper.GpaRank.FAIR, ProfileGpaHelper.classifyGpaRank(2.50f));
        assertEquals(ProfileGpaHelper.GpaRank.AVERAGE, ProfileGpaHelper.classifyGpaRank(2.49f));
    }

    @Test
    public void validateProfile_reportsValidAndAllInvalidFields() {
        assertEquals(
                ProfileGpaHelper.ValidationError.NONE,
                ProfileGpaHelper.validateProfile("Nguyễn Khánh", 114, 145, 96, 3.44f, 20)
        );
        assertEquals(
                ProfileGpaHelper.ValidationError.EMPTY_NAME,
                ProfileGpaHelper.validateProfile("  ", 114, 145, 96, 3.44f, 20)
        );
        assertEquals(
                ProfileGpaHelper.ValidationError.INVALID_EARNED_CREDITS,
                ProfileGpaHelper.validateProfile("An", -1, 145, 96, 3.44f, 20)
        );
        assertEquals(
                ProfileGpaHelper.ValidationError.INVALID_TOTAL_CREDITS,
                ProfileGpaHelper.validateProfile("An", 0, 0, 96, 3.44f, 20)
        );
        assertEquals(
                ProfileGpaHelper.ValidationError.EARNED_EXCEEDS_TOTAL,
                ProfileGpaHelper.validateProfile("An", 150, 145, 96, 3.44f, 20)
        );
        assertEquals(
                ProfileGpaHelper.ValidationError.INVALID_ATTENDANCE,
                ProfileGpaHelper.validateProfile("An", 114, 145, 101, 3.44f, 20)
        );
        assertEquals(
                ProfileGpaHelper.ValidationError.INVALID_BASELINE_GPA,
                ProfileGpaHelper.validateProfile("An", 114, 145, 96, 4.1f, 20)
        );
        assertEquals(
                ProfileGpaHelper.ValidationError.INVALID_TERM_CREDITS,
                ProfileGpaHelper.validateProfile("An", 114, 145, 96, 3.44f, 65)
        );
    }
}
