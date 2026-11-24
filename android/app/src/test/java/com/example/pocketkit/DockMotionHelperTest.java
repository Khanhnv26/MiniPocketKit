package com.example.pocketkit;

import com.example.pocketkit.ui.DockMotionHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DockMotionHelperTest {

    @Test
    public void resolveTabIndex_mapsTrackPositionsAndClampsBounds() {
        assertEquals(0, DockMotionHelper.resolveTabIndex(-20f, 400f, 4));
        assertEquals(0, DockMotionHelper.resolveTabIndex(0f, 400f, 4));
        assertEquals(0, DockMotionHelper.resolveTabIndex(99f, 400f, 4));
        assertEquals(1, DockMotionHelper.resolveTabIndex(100f, 400f, 4));
        assertEquals(2, DockMotionHelper.resolveTabIndex(250f, 400f, 4));
        assertEquals(3, DockMotionHelper.resolveTabIndex(399f, 400f, 4));
        assertEquals(3, DockMotionHelper.resolveTabIndex(520f, 400f, 4));
        assertEquals(0, DockMotionHelper.resolveTabIndex(150f, 0f, 4));
        assertEquals(0, DockMotionHelper.resolveTabIndex(150f, 400f, 1));
    }

    @Test
    public void clampIndicatorTranslation_keepsIndicatorInsideTrack() {
        assertEquals(0f, DockMotionHelper.clampIndicatorTranslation(-15f, 90f, 360f), 0.001f);
        assertEquals(135f, DockMotionHelper.clampIndicatorTranslation(135f, 90f, 360f), 0.001f);
        assertEquals(270f, DockMotionHelper.clampIndicatorTranslation(320f, 90f, 360f), 0.001f);
        assertEquals(0f, DockMotionHelper.clampIndicatorTranslation(50f, 120f, 100f), 0.001f);
    }

    @Test
    public void evaluateSpringProgress_reachesExactEndpointsWithGentleOvershoot() {
        assertEquals(0f, DockMotionHelper.evaluateSpringProgress(-0.1f), 0.0001f);
        assertEquals(0f, DockMotionHelper.evaluateSpringProgress(0f), 0.0001f);
        assertEquals(1f, DockMotionHelper.evaluateSpringProgress(1f), 0.0001f);
        assertEquals(1f, DockMotionHelper.evaluateSpringProgress(1.2f), 0.0001f);

        float mid = DockMotionHelper.evaluateSpringProgress(0.55f);
        assertTrue(mid > 1.0f);
        assertTrue(mid < 1.08f);
    }

    @Test
    public void computeStretchScaleX_pulsesAtMidpointAndScalesWithDistance() {
        assertEquals(1f, DockMotionHelper.computeStretchScaleX(0f, 2), 0.0001f);
        assertEquals(1f, DockMotionHelper.computeStretchScaleX(1f, 2), 0.0001f);
        assertEquals(1f, DockMotionHelper.computeStretchScaleX(0.5f, 0), 0.0001f);

        float oneTab = DockMotionHelper.computeStretchScaleX(0.5f, 1);
        float threeTabs = DockMotionHelper.computeStretchScaleX(0.5f, 3);
        assertTrue(oneTab > 1.05f && oneTab < 1.12f);
        assertTrue(threeTabs > oneTab && threeTabs <= 1.16f);
    }

    @Test
    public void evaluateIconBounceScale_pulsesSmoothly() {
        assertEquals(1f, DockMotionHelper.evaluateIconBounceScale(0f), 0.0001f);
        assertEquals(1.08f, DockMotionHelper.evaluateIconBounceScale(0.5f), 0.001f);
        assertEquals(1f, DockMotionHelper.evaluateIconBounceScale(1f), 0.0001f);
    }

    @Test
    public void computeContentSlideOffsetPx_followsNavigationDirection() {
        assertEquals(14f, DockMotionHelper.computeContentSlideOffsetPx(0, 2, 14f), 0.001f);
        assertEquals(-14f, DockMotionHelper.computeContentSlideOffsetPx(3, 1, 14f), 0.001f);
        assertEquals(0f, DockMotionHelper.computeContentSlideOffsetPx(1, 1, 14f), 0.001f);
    }
}
