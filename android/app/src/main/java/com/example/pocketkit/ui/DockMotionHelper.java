package com.example.pocketkit.ui;

public final class DockMotionHelper {

    private static final double SPRING_DAMPING = 6.5;
    private static final double SPRING_FREQ_PI = 1.45 * Math.PI;
    private static final double SPRING_END_VALUE =
            1.0 - Math.exp(-SPRING_DAMPING) * Math.cos(SPRING_FREQ_PI);

    private DockMotionHelper() {
    }

    public static int resolveTabIndex(float touchXInTrack, float trackWidth, int tabCount) {
        if (trackWidth <= 0f || tabCount <= 1) {
            return 0;
        }
        float clampedX = Math.max(0f, Math.min(touchXInTrack, trackWidth - 0.001f));
        int index = (int) (clampedX * tabCount / trackWidth);
        return Math.max(0, Math.min(tabCount - 1, index));
    }

    public static float clampIndicatorTranslation(float desiredX, float indicatorWidth, float trackWidth) {
        float maxX = Math.max(0f, trackWidth - Math.max(0f, indicatorWidth));
        return Math.max(0f, Math.min(desiredX, maxX));
    }

    public static float evaluateSpringProgress(float fraction) {
        if (fraction <= 0f) {
            return 0f;
        }
        if (fraction >= 1f) {
            return 1f;
        }
        double raw = 1.0 - Math.exp(-SPRING_DAMPING * fraction) * Math.cos(SPRING_FREQ_PI * fraction);
        return (float) (raw / SPRING_END_VALUE);
    }

    public static float computeStretchScaleX(float fraction, int tabDistance) {
        if (fraction <= 0f || fraction >= 1f || tabDistance <= 0) {
            return 1f;
        }
        float amplitude = Math.min(0.16f, 0.05f + 0.035f * Math.min(3, tabDistance));
        return 1f + amplitude * (float) Math.sin(Math.PI * fraction);
    }

    public static float evaluateIconBounceScale(float fraction) {
        if (fraction <= 0f || fraction >= 1f) {
            return 1f;
        }
        return 1f + 0.08f * (float) Math.sin(Math.PI * fraction);
    }

    public static float computeContentSlideOffsetPx(int fromIndex, int toIndex, float slideDistancePx) {
        if (toIndex > fromIndex) {
            return slideDistancePx;
        }
        if (toIndex < fromIndex) {
            return -slideDistancePx;
        }
        return 0f;
    }
}
