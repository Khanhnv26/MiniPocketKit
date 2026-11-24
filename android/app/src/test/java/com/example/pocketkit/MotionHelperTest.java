package com.example.pocketkit;

import com.example.pocketkit.ui.MotionHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MotionHelperTest {

    @Test
    public void computeStaggerDelayMs_capsAtMaxStaggerItems() {
        assertEquals(0L, MotionHelper.computeStaggerDelayMs(-1));
        assertEquals(0L, MotionHelper.computeStaggerDelayMs(0));
        assertEquals(25L, MotionHelper.computeStaggerDelayMs(1));
        assertEquals(50L, MotionHelper.computeStaggerDelayMs(2));
        assertEquals(75L, MotionHelper.computeStaggerDelayMs(3));
        assertEquals(75L, MotionHelper.computeStaggerDelayMs(4));
        assertEquals(75L, MotionHelper.computeStaggerDelayMs(10));
    }

    @Test
    public void computeDaySlideDirection_matchesNavigationDirection() {
        assertEquals(0, MotionHelper.computeDaySlideDirection(-1, 2));
        assertEquals(0, MotionHelper.computeDaySlideDirection(2, 2));
        assertEquals(1, MotionHelper.computeDaySlideDirection(0, 3));
        assertEquals(-1, MotionHelper.computeDaySlideDirection(4, 1));
    }

    @Test
    public void computeShakeKeyframesPx_decaysSymmetricallyAndEndsAtZero() {
        float[] frames = MotionHelper.computeShakeKeyframesPx(12f);
        assertEquals(7, frames.length);
        assertEquals(0f, frames[0], 0.001f);
        assertEquals(-12f, frames[1], 0.001f);
        assertEquals(12f, frames[2], 0.001f);
        assertEquals(-7.8f, frames[3], 0.001f);
        assertEquals(7.8f, frames[4], 0.001f);
        assertEquals(-3f, frames[5], 0.001f);
        assertEquals(0f, frames[6], 0.001f);
    }

    @Test
    public void clampProgressTarget_clampsBetweenZeroAndMax() {
        assertEquals(0, MotionHelper.clampProgressTarget(-20, 400));
        assertEquals(275, MotionHelper.clampProgressTarget(275, 400));
        assertEquals(400, MotionHelper.clampProgressTarget(550, 400));
        assertEquals(0, MotionHelper.clampProgressTarget(50, -5));
    }
}
