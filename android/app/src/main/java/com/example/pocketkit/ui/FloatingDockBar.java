package com.example.pocketkit.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FloatingDockBar extends FrameLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(int index, boolean fromUser);
    }

    private static final long SLIDE_DURATION_MS = 280L;

    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private final int touchSlop;

    @Nullable
    private View indicatorView;
    @Nullable
    private ViewGroup tabsRow;
    private View[] tabViews = new View[0];
    private ImageView[] tabIcons = new ImageView[0];
    private TextView[] tabLabels = new TextView[0];
    private int[] currentColors = new int[0];

    private int activeColor;
    private int inactiveColor;
    private int selectedIndex = 0;

    @Nullable
    private OnTabSelectedListener tabSelectedListener;
    @Nullable
    private ValueAnimator slideAnimator;

    private float downX;
    private float downY;
    private boolean dragging = false;
    private float dragOffsetInIndicator = 0f;
    private int dragHoverIndex = -1;

    public FloatingDockBar(@NonNull Context context) {
        this(context, null);
    }

    public FloatingDockBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingDockBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void configure(
            @NonNull View indicatorView,
            @NonNull ViewGroup tabsRow,
            @NonNull View[] tabViews,
            @NonNull ImageView[] tabIcons,
            @NonNull TextView[] tabLabels,
            int activeColor,
            int inactiveColor,
            @Nullable OnTabSelectedListener listener
    ) {
        this.indicatorView = indicatorView;
        this.tabsRow = tabsRow;
        this.tabViews = tabViews;
        this.tabIcons = tabIcons;
        this.tabLabels = tabLabels;
        this.activeColor = activeColor;
        this.inactiveColor = inactiveColor;
        this.tabSelectedListener = listener;
        this.currentColors = new int[tabViews.length];

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dockShadow = Color.argb(44, 15, 23, 42);
            setOutlineSpotShadowColor(dockShadow);
            setOutlineAmbientShadowColor(dockShadow);
            int pillShadow = Color.argb(34, 15, 23, 42);
            indicatorView.setOutlineSpotShadowColor(pillShadow);
            indicatorView.setOutlineAmbientShadowColor(pillShadow);
        }

        for (int i = 0; i < tabViews.length; i++) {
            final int tabIndex = i;
            tabViews[i].setOnClickListener(v -> {
                if (tabIndex == selectedIndex) {
                    if (tabSelectedListener != null) {
                        tabSelectedListener.onTabSelected(tabIndex, true);
                    }
                } else {
                    selectTab(tabIndex, true, true);
                }
            });
        }

        tabsRow.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            syncIndicatorGeometry();
            if (!dragging && (slideAnimator == null || !slideAnimator.isRunning())) {
                placeIndicatorImmediate(selectedIndex);
            }
        });

        applyStaticTabVisuals(selectedIndex);
    }

    public void selectTab(int targetIndex, boolean animate, boolean notifyListener) {
        if (tabViews.length == 0 || indicatorView == null) {
            return;
        }
        int clampedTarget = Math.max(0, Math.min(tabViews.length - 1, targetIndex));
        int previousIndex = selectedIndex;
        int visualOriginIndex = dragHoverIndex >= 0 ? dragHoverIndex : previousIndex;
        selectedIndex = clampedTarget;
        dragHoverIndex = -1;

        cancelAnimations();
        syncIndicatorGeometry();

        if (notifyListener && clampedTarget != previousIndex) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }

        if (tabsRow == null || tabsRow.getWidth() == 0) {
            applyStaticTabVisuals(clampedTarget);
            if (notifyListener && tabSelectedListener != null) {
                tabSelectedListener.onTabSelected(clampedTarget, true);
            }
            return;
        }

        float targetX = resolveTargetTranslationX(clampedTarget);
        if (!animate || !FrostUiHelper.areAnimationsEnabled(getContext())) {
            indicatorView.setScaleX(1f);
            indicatorView.setScaleY(1f);
            indicatorView.setTranslationX(targetX);
            applyStaticTabVisuals(clampedTarget);
            if (notifyListener && tabSelectedListener != null) {
                tabSelectedListener.onTabSelected(clampedTarget, true);
            }
            return;
        }

        float startX = indicatorView.getTranslationX();
        int[] startColors = currentColors.clone();
        int tabDistance = Math.max(1, Math.abs(clampedTarget - visualOriginIndex));
        resetIconScales();

        if (notifyListener && tabSelectedListener != null) {
            tabSelectedListener.onTabSelected(clampedTarget, true);
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(SLIDE_DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(anim -> {
            float fraction = (Float) anim.getAnimatedValue();
            float spring = DockMotionHelper.evaluateSpringProgress(fraction);
            indicatorView.setTranslationX(startX + (targetX - startX) * spring);

            float stretchX = DockMotionHelper.computeStretchScaleX(fraction, tabDistance);
            indicatorView.setScaleX(stretchX);
            indicatorView.setScaleY(1f - (stretchX - 1f) * 0.25f);

            float colorFraction = Math.min(1f, Math.max(0f, fraction * 1.15f));
            for (int i = 0; i < tabViews.length; i++) {
                int endColor = i == clampedTarget ? activeColor : inactiveColor;
                int blended = (Integer) argbEvaluator.evaluate(colorFraction, startColors[i], endColor);
                setTabColor(i, blended);
            }

            float iconScale = DockMotionHelper.evaluateIconBounceScale(fraction);
            tabIcons[clampedTarget].setScaleX(iconScale);
            tabIcons[clampedTarget].setScaleY(iconScale);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled = false;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (slideAnimator == animation) {
                    slideAnimator = null;
                }
                resetIconScales();
                if (cancelled) {
                    return;
                }
                indicatorView.setScaleX(1f);
                indicatorView.setScaleY(1f);
                indicatorView.setTranslationX(targetX);
                applyStaticTabVisuals(clampedTarget);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
                if (slideAnimator == animation) {
                    slideAnimator = null;
                }
                resetIconScales();
            }
        });
        slideAnimator = animator;
        animator.start();
    }

    public void cancelAnimations() {
        if (slideAnimator != null) {
            slideAnimator.cancel();
            slideAnimator = null;
        }
        if (indicatorView != null) {
            indicatorView.animate().cancel();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        if (tabsRow == null || indicatorView == null || tabViews.length <= 1 || tabsRow.getWidth() <= 0) {
            return super.onInterceptTouchEvent(ev);
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                dragging = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - downX);
                float dy = Math.abs(ev.getY() - downY);
                if (dx > touchSlop && dx >= dy) {
                    beginDragging(downX);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return false;
            default:
                return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        if (!dragging) {
            return super.onTouchEvent(ev);
        }
        if (tabsRow == null || indicatorView == null || tabsRow.getWidth() <= 0) {
            dragging = false;
            return false;
        }
        float trackWidth = tabsRow.getWidth();
        float trackX = ev.getX() - tabsRow.getLeft();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                float desiredX = trackX - dragOffsetInIndicator;
                indicatorView.setTranslationX(
                        DockMotionHelper.clampIndicatorTranslation(desiredX, indicatorView.getWidth(), trackWidth)
                );
                int hover = DockMotionHelper.resolveTabIndex(trackX, trackWidth, tabViews.length);
                if (hover != dragHoverIndex) {
                    dragHoverIndex = hover;
                    applyStaticTabVisuals(hover);
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
                return true;
            case MotionEvent.ACTION_UP:
                int releaseTarget = dragHoverIndex >= 0
                        ? dragHoverIndex
                        : DockMotionHelper.resolveTabIndex(trackX, trackWidth, tabViews.length);
                dragging = false;
                selectTab(releaseTarget, true, releaseTarget != selectedIndex);
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                selectTab(selectedIndex, true, false);
                return true;
            default:
                return super.onTouchEvent(ev);
        }
    }

    private void beginDragging(float startXInParent) {
        if (tabsRow == null || indicatorView == null) {
            return;
        }
        dragging = true;
        cancelAnimations();
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }

        float trackX = startXInParent - tabsRow.getLeft();
        float currentTx = indicatorView.getTranslationX();
        float indicatorW = indicatorView.getWidth();
        if (trackX >= currentTx && trackX <= currentTx + indicatorW) {
            dragOffsetInIndicator = trackX - currentTx;
        } else {
            dragOffsetInIndicator = indicatorW * 0.5f;
        }

        dragHoverIndex = DockMotionHelper.resolveTabIndex(trackX, tabsRow.getWidth(), tabViews.length);
        applyStaticTabVisuals(dragHoverIndex);
        if (FrostUiHelper.areAnimationsEnabled(getContext())) {
            indicatorView.animate().scaleX(1.03f).scaleY(1.03f).setDuration(90L).start();
        }
    }

    private void syncIndicatorGeometry() {
        if (indicatorView == null || tabsRow == null || tabViews.length == 0) {
            return;
        }
        int rowWidth = tabsRow.getWidth();
        int rowHeight = tabsRow.getHeight();
        if (rowWidth <= 0 || rowHeight <= 0) {
            return;
        }
        int targetW = Math.max(1, Math.round((float) rowWidth / tabViews.length));
        ViewGroup.LayoutParams lp = indicatorView.getLayoutParams();
        if (lp.width != targetW || lp.height != rowHeight) {
            lp.width = targetW;
            lp.height = rowHeight;
            indicatorView.setLayoutParams(lp);
        }
    }

    private void placeIndicatorImmediate(int index) {
        if (indicatorView == null || tabsRow == null || tabsRow.getWidth() == 0) {
            return;
        }
        indicatorView.setTranslationX(resolveTargetTranslationX(index));
    }

    private float resolveTargetTranslationX(int index) {
        if (tabsRow == null || indicatorView == null || tabViews.length == 0) {
            return 0f;
        }
        int safeIndex = Math.max(0, Math.min(tabViews.length - 1, index));
        float tabLeft = tabViews[safeIndex].getLeft();
        return DockMotionHelper.clampIndicatorTranslation(
                tabLeft,
                indicatorView.getWidth(),
                tabsRow.getWidth()
        );
    }

    private void applyStaticTabVisuals(int activeIndex) {
        for (int i = 0; i < tabViews.length; i++) {
            boolean active = i == activeIndex;
            setTabColor(i, active ? activeColor : inactiveColor);
            tabLabels[i].setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void setTabColor(int index, int color) {
        currentColors[index] = color;
        tabIcons[index].setColorFilter(color);
        tabLabels[index].setTextColor(color);
    }

    private void resetIconScales() {
        for (ImageView icon : tabIcons) {
            icon.setScaleX(1f);
            icon.setScaleY(1f);
        }
    }
}
