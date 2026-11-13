package com.example.pocketkit;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class DashboardRingsView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final int[] ringColors = new int[3];
    private final float[] progressValues = new float[]{0.68f, 0.85f, 0.40f};
    private final ValueAnimator[] animators = new ValueAnimator[3];

    public DashboardRingsView(Context context) {
        this(context, null);
    }

    public DashboardRingsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DashboardRingsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ringColors[0] = ContextCompat.getColor(context, R.color.accent_sapphire);
        ringColors[1] = ContextCompat.getColor(context, R.color.accent_sky);
        ringColors[2] = ContextCompat.getColor(context, R.color.accent_mint);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void animateReveal() {
        setProgress(0, progressValues[0], true);
        setProgress(1, progressValues[1], true);
        setProgress(2, progressValues[2], true);
    }

    public void setDeadlineProgress(float progress, boolean animate) {
        setProgress(0, progress, animate);
    }

    public void setClassProgress(float progress, boolean animate) {
        setProgress(1, progress, animate);
    }

    public void setFocusProgress(float progress, boolean animate) {
        setProgress(2, progress, animate);
    }

    public void setProgress(int ringIndex, float targetProgress, boolean animate) {
        if (ringIndex < 0 || ringIndex >= progressValues.length) {
            return;
        }
        float clamped = Math.max(0f, Math.min(1f, targetProgress));
        if (animators[ringIndex] != null) {
            animators[ringIndex].cancel();
        }
        if (!animate || !areAnimationsEnabled()) {
            progressValues[ringIndex] = clamped;
            invalidate();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(progressValues[ringIndex], clamped);
        animator.setDuration(420L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            progressValues[ringIndex] = (float) animation.getAnimatedValue();
            invalidate();
        });
        animators[ringIndex] = animator;
        animator.start();
    }

    private boolean areAnimationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        }
        float scale = Settings.Global.getFloat(
                getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
        );
        return scale != 0f;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float size = Math.min(width, height);
        if (size <= 0f) {
            return;
        }
        float centerX = width / 2f;
        float centerY = height / 2f;
        float strokeWidth = size * 0.088f;
        float gap = size * 0.032f;

        trackPaint.setStrokeWidth(strokeWidth);
        ringPaint.setStrokeWidth(strokeWidth);

        float outerRadius = (size / 2f) - (strokeWidth * 0.7f);
        for (int i = 0; i < 3; i++) {
            float radius = outerRadius - i * (strokeWidth + gap);
            if (radius <= 0f) {
                continue;
            }
            arcBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

            trackPaint.setColor(ringColors[i]);
            trackPaint.setAlpha(40);
            canvas.drawCircle(centerX, centerY, radius, trackPaint);

            ringPaint.setColor(ringColors[i]);
            ringPaint.setAlpha(255);
            float sweep = Math.max(4f, 360f * progressValues[i]);
            canvas.drawArc(arcBounds, -90f, sweep, false, ringPaint);
        }
    }
}
