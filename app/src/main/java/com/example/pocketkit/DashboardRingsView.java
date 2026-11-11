package com.example.pocketkit;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DashboardRingsView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colors;
    private float reveal = 1f;

    public DashboardRingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        colors = new int[]{context.getColor(R.color.dashboard_red),
                context.getColor(R.color.dashboard_green), context.getColor(R.color.dashboard_teal)};
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    void animateReveal() {
        ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
        animation.setDuration(540);
        animation.addUpdateListener(frame -> {
            reveal = (float) frame.getAnimatedValue();
            invalidate();
        });
        animation.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float unit = Math.min(getWidth(), getHeight()) / 112f;
        paint.setStrokeWidth(8.5f * unit);
        for (int index = 0; index < colors.length; index++) {
            float radius = (46 - index * 11) * unit;
            paint.setColor(colors[index]);
            paint.setAlpha(42);
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setAlpha(255);
            canvas.drawArc(centerX - radius, centerY - radius, centerX + radius,
                    centerY + radius, -90, 240 * reveal, false, paint);
        }
    }
}
