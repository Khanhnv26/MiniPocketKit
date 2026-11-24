package com.example.pocketkit.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;

import com.example.pocketkit.R;

public final class MotionHelper {

    public static final float PRESS_SCALE_BUTTON = 0.96f;
    public static final float PRESS_SCALE_CARD = 0.985f;
    public static final long DURATION_PRESS_DOWN_MS = 90L;
    public static final long DURATION_PRESS_UP_MS = 160L;
    public static final int MAX_STAGGER_ITEMS = 4;
    public static final long STAGGER_STEP_MS = 25L;

    private MotionHelper() {
    }

    public static long computeStaggerDelayMs(int position) {
        if (position <= 0) {
            return 0L;
        }
        return Math.min(position, MAX_STAGGER_ITEMS - 1) * STAGGER_STEP_MS;
    }

    public static int computeDaySlideDirection(int previousDayIndex, int newDayIndex) {
        if (previousDayIndex < 0 || previousDayIndex == newDayIndex) {
            return 0;
        }
        return newDayIndex > previousDayIndex ? 1 : -1;
    }

    @NonNull
    public static float[] computeShakeKeyframesPx(float amplitudePx) {
        return new float[]{
                0f,
                -amplitudePx,
                amplitudePx,
                -0.65f * amplitudePx,
                0.65f * amplitudePx,
                -0.25f * amplitudePx,
                0f
        };
    }

    public static int clampProgressTarget(int target, int max) {
        int safeMax = Math.max(0, max);
        return Math.max(0, Math.min(target, safeMax));
    }

    public static void attachButtonPress(@NonNull View... views) {
        for (View view : views) {
            if (view != null) {
                attachPressScale(view, PRESS_SCALE_BUTTON);
            }
        }
    }

    public static void attachCardPress(@NonNull View... views) {
        for (View view : views) {
            if (view != null) {
                attachPressScale(view, PRESS_SCALE_CARD);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void attachPressScale(@NonNull View view, float pressedScale) {
        view.setOnTouchListener((v, event) -> {
            if (!v.isEnabled() || !FrostUiHelper.areAnimationsEnabled(v.getContext())) {
                if (v.getScaleX() != 1f || v.getScaleY() != 1f) {
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                }
                return false;
            }
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().cancel();
                v.animate()
                        .scaleX(pressedScale)
                        .scaleY(pressedScale)
                        .setDuration(DURATION_PRESS_DOWN_MS)
                        .setInterpolator(standardEaseOut())
                        .start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().cancel();
                v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(DURATION_PRESS_UP_MS)
                        .setInterpolator(standardEaseOut())
                        .start();
            }
            return false;
        });
    }

    public static void attachDialogTactilePress(@Nullable View root) {
        if (root == null) {
            return;
        }
        if (root.isClickable() && !(root instanceof EditText)) {
            attachPressScale(root, root instanceof ViewGroup ? PRESS_SCALE_CARD : PRESS_SCALE_BUTTON);
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                attachDialogTactilePress(group.getChildAt(i));
            }
        }
    }

    public static void performStateHaptic(@NonNull View view) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    public static void startActivityPush(@NonNull Activity activity, @NonNull Intent intent) {
        if (FrostUiHelper.areAnimationsEnabled(activity)) {
            ActivityOptionsCompat transition = ActivityOptionsCompat.makeCustomAnimation(
                    activity,
                    R.anim.activity_push_enter,
                    R.anim.activity_push_exit
            );
            activity.startActivity(intent, transition.toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    public static void registerClosePopTransition(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && FrostUiHelper.areAnimationsEnabled(activity)) {
            activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_CLOSE,
                    R.anim.activity_pop_enter,
                    R.anim.activity_pop_exit
            );
        }
    }

    @SuppressWarnings("deprecation")
    public static void applyLegacyClosePopTransition(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && FrostUiHelper.areAnimationsEnabled(activity)) {
            activity.overridePendingTransition(R.anim.activity_pop_enter, R.anim.activity_pop_exit);
        }
    }

    public static void shakeError(@NonNull View view) {
        if (!FrostUiHelper.areAnimationsEnabled(view.getContext())) {
            return;
        }
        view.animate().cancel();
        float[] keyframes = computeShakeKeyframesPx(dpToPxFloat(view, 6f));
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, keyframes);
        animator.setDuration(260L);
        animator.start();
    }

    public static void pulseChip(@NonNull View chip) {
        if (!FrostUiHelper.areAnimationsEnabled(chip.getContext())) {
            return;
        }
        chip.animate().cancel();
        chip.setScaleX(1f);
        chip.setScaleY(1f);
        chip.animate()
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(85L)
                .setInterpolator(standardEaseOut())
                .withEndAction(() -> chip.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100L)
                        .setInterpolator(standardEaseOut())
                        .start())
                .start();
    }

    public static void animateCheckPop(@NonNull View checkBox) {
        if (!FrostUiHelper.areAnimationsEnabled(checkBox.getContext())) {
            checkBox.setScaleX(1f);
            checkBox.setScaleY(1f);
            return;
        }
        checkBox.animate().cancel();
        checkBox.setScaleX(0.68f);
        checkBox.setScaleY(0.68f);
        checkBox.animate()
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(105L)
                .setInterpolator(standardEaseOut())
                .withEndAction(() -> checkBox.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(105L)
                        .setInterpolator(standardEaseOut())
                        .start())
                .start();
    }

    public static void animateCardEntrance(@NonNull View card, int index, int direction) {
        card.animate().cancel();
        if (!FrostUiHelper.areAnimationsEnabled(card.getContext()) || direction == 0) {
            card.setAlpha(1f);
            card.setTranslationX(0f);
            return;
        }
        float offsetPx = direction * dpToPxFloat(card, 12f);
        card.setAlpha(0f);
        card.setTranslationX(offsetPx);
        card.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(200L)
                .setStartDelay(computeStaggerDelayMs(index))
                .setInterpolator(standardEaseOut())
                .start();
    }

    public static void animateSectionReveal(@NonNull View view) {
        view.animate().cancel();
        if (!FrostUiHelper.areAnimationsEnabled(view.getContext())) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            return;
        }
        view.setAlpha(0f);
        view.setTranslationY(dpToPxFloat(view, 10f));
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(190L)
                .setInterpolator(standardEaseOut())
                .start();
    }

    public static void animateTextCrossFade(@NonNull TextView view) {
        view.animate().cancel();
        if (!FrostUiHelper.areAnimationsEnabled(view.getContext())) {
            view.setAlpha(1f);
            return;
        }
        view.setAlpha(0.35f);
        view.animate()
                .alpha(1f)
                .setDuration(180L)
                .setInterpolator(standardEaseOut())
                .start();
    }

    public static void animateProgress(@NonNull ProgressBar progressBar, int targetProgress, boolean animate) {
        Object existingTag = progressBar.getTag(R.id.tag_progress_animator);
        if (existingTag instanceof Animator) {
            ((Animator) existingTag).cancel();
            progressBar.setTag(R.id.tag_progress_animator, null);
        }
        int clamped = clampProgressTarget(targetProgress, progressBar.getMax());
        if (!animate || !FrostUiHelper.areAnimationsEnabled(progressBar.getContext())) {
            progressBar.setProgress(clamped);
            return;
        }
        int current = progressBar.getProgress();
        if (current == clamped) {
            return;
        }
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", current, clamped);
        animator.setDuration(240L);
        animator.setInterpolator(standardEaseOut());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (progressBar.getTag(R.id.tag_progress_animator) == animation) {
                    progressBar.setTag(R.id.tag_progress_animator, null);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (progressBar.getTag(R.id.tag_progress_animator) == animation) {
                    progressBar.setTag(R.id.tag_progress_animator, null);
                }
            }
        });
        progressBar.setTag(R.id.tag_progress_animator, animator);
        animator.start();
    }

    @NonNull
    private static Interpolator standardEaseOut() {
        return new PathInterpolator(0.2f, 0f, 0f, 1f);
    }

    private static float dpToPxFloat(@NonNull View view, float dp) {
        return dp * view.getResources().getDisplayMetrics().density;
    }
}
