package com.example.pocketkit.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.pocketkit.R;

public final class FrostUiHelper {

    public interface OnCategoryCreatedListener {
        void onCategoryCreated(@NonNull String categoryName);
    }

    private FrostUiHelper() {
    }

    public static void setupEdgeToEdgeAndBackdrop(
            @NonNull AppCompatActivity activity,
            @NonNull View rootContainer,
            @NonNull View topContentView,
            @Nullable View backdropView,
            @Nullable View bottomBarView
    ) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(activity.getWindow(), rootContainer);
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }
        if (backdropView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            backdropView.setRenderEffect(
                    RenderEffect.createBlurEffect(110f, 110f, Shader.TileMode.CLAMP)
            );
        }

        final int bottomPadH = activity.getResources().getDimensionPixelSize(R.dimen.dock_horizontal_padding);
        final int bottomPadB = activity.getResources().getDimensionPixelSize(R.dimen.dock_bottom_padding);
        final int topPadLeft = topContentView.getPaddingLeft();
        final int topPadTop = topContentView.getPaddingTop();
        final int topPadRight = topContentView.getPaddingRight();
        final int topPadBottom = topContentView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            topContentView.setPadding(
                    topPadLeft + bars.left,
                    topPadTop + bars.top,
                    topPadRight + bars.right,
                    bottomBarView == null ? topPadBottom + bars.bottom : topPadBottom
            );
            if (bottomBarView != null) {
                bottomBarView.setPadding(
                        bottomPadH + bars.left,
                        bottomBarView.getPaddingTop(),
                        bottomPadH + bars.right,
                        bottomPadB + bars.bottom
                );
            }
            return windowInsets;
        });
    }

    public static TextView createCategoryChip(
            @NonNull Context context,
            @NonNull ViewGroup parent,
            @NonNull String label,
            boolean selected,
            boolean accentAction
    ) {
        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.text_size_body));
        chip.setTypeface(null, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(context.getResources().getDimensionPixelSize(R.dimen.touch_target_min));

        int padH = context.getResources().getDimensionPixelSize(R.dimen.chip_padding_h);
        int padV = context.getResources().getDimensionPixelSize(R.dimen.chip_padding_v);
        chip.setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (parent.getChildCount() > 0) {
            params.setMarginStart(context.getResources().getDimensionPixelSize(R.dimen.chip_gap));
        }
        chip.setLayoutParams(params);

        if (accentAction) {
            chip.setBackgroundResource(R.drawable.bg_pill_accent_soft);
            chip.setTextColor(ContextCompat.getColor(context, R.color.accent_sapphire));
        } else if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(context, R.color.ink_primary));
        }
        return chip;
    }

    public static void showAddCategoryDialog(
            @NonNull Context context,
            @NonNull OnCategoryCreatedListener listener
    ) {
        AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_frost_input, null);
        dialog.setContentView(content);

        TextView tvTitle = content.findViewById(R.id.tvDialogTitle);
        EditText edtInput = content.findViewById(R.id.edtDialogInput);
        TextView btnCancel = content.findViewById(R.id.btnDialogCancel);
        TextView btnConfirm = content.findViewById(R.id.btnDialogConfirm);

        tvTitle.setText(R.string.notes_dialog_add_category_title);
        edtInput.setHint(R.string.notes_dialog_add_category_hint);

        Runnable submit = () -> {
            String name = edtInput.getText().toString().trim();
            if (name.isEmpty()) {
                edtInput.setError(context.getString(R.string.notes_dialog_empty_category_error));
                edtInput.requestFocus();
                return;
            }
            dialog.dismiss();
            listener.onCategoryCreated(name);
        };

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> submit.run());
        edtInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit.run();
                return true;
            }
            return false;
        });

        configureFrostDialogWindow(dialog, true);
        edtInput.requestFocus();
        dialog.show();
    }

    public static void showConfirmDialog(
            @NonNull Context context,
            @StringRes int titleRes,
            @NonNull String message,
            @StringRes int confirmTextRes,
            boolean destructive,
            @NonNull Runnable onConfirm
    ) {
        AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_frost_confirm, null);
        dialog.setContentView(content);

        TextView tvTitle = content.findViewById(R.id.tvConfirmTitle);
        TextView tvMessage = content.findViewById(R.id.tvConfirmMessage);
        TextView btnCancel = content.findViewById(R.id.btnConfirmCancel);
        TextView btnConfirm = content.findViewById(R.id.btnConfirmOk);

        tvTitle.setText(titleRes);
        tvMessage.setText(message);
        btnConfirm.setText(confirmTextRes);

        if (destructive) {
            btnConfirm.setBackgroundResource(R.drawable.bg_pill_danger_soft);
            btnConfirm.setTextColor(ContextCompat.getColor(context, R.color.accent_danger));
        } else {
            btnConfirm.setBackgroundResource(R.drawable.bg_pill_primary);
            btnConfirm.setTextColor(ContextCompat.getColor(context, R.color.white));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        configureFrostDialogWindow(dialog, false);
        dialog.show();
    }

    private static void configureFrostDialogWindow(@NonNull AppCompatDialog dialog, boolean showKeyboard) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setDimAmount(0.28f);
        if (showKeyboard) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    public static boolean areAnimationsEnabled(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        }
        float scale = Settings.Global.getFloat(
                context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
        );
        return scale != 0f;
    }

    public static int dpToPx(@NonNull Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
