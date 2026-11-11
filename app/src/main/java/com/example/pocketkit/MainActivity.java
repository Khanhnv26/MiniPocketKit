package com.example.pocketkit;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pocketkit.databinding.ActivityMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_NIGHT_MODE = "dashboard_night_mode";
    private static final String STATE_SELECTED_TAB = "dashboard_selected_tab";
    private static final String STATE_FOCUS_END = "dashboard_focus_end";
    private static final String STATE_FOCUS_REMAINING = "dashboard_focus_remaining";
    private static final long FOCUS_DURATION = 25 * 60_000L;

    private ActivityMainBinding binding;
    private TextView[] tabs;
    private int selectedTab = 0;
    private long focusEnd;
    private long focusRemaining = FOCUS_DURATION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        Locale vietnamese = Locale.forLanguageTag("vi-VN");
        binding.dashboardDate.setText(new SimpleDateFormat("EEEE, d 'tháng' M", vietnamese)
                .format(new Date()).toUpperCase(vietnamese));
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(STATE_SELECTED_TAB);
            focusEnd = savedInstanceState.getLong(STATE_FOCUS_END);
            focusRemaining = savedInstanceState.getLong(STATE_FOCUS_REMAINING, FOCUS_DURATION);
        }
        setupClickListeners();
        adaptDashboard();
        if (savedInstanceState == null) {
            animateDashboard();
        } else if (savedInstanceState.getInt(STATE_NIGHT_MODE) != getNightMode()
                && animationsEnabled()) {
            binding.getRoot().setAlpha(0f);
            binding.getRoot().animate().alpha(1f).setDuration(200).start();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateFocusTimer();
    }

    @Override
    protected void onStop() {
        binding.focusTimer.stop();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_NIGHT_MODE, getNightMode());
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
        outState.putLong(STATE_FOCUS_END, focusEnd);
        outState.putLong(STATE_FOCUS_REMAINING, focusRemaining);
        super.onSaveInstanceState(outState);
    }

    private int getNightMode() {
        return getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    private boolean animationsEnabled() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled();
    }

    private void animateDashboard() {
        if (!animationsEnabled()) {
            return;
        }
        binding.dashboardRings.animateReveal();
        View[] sections = {binding.dashboardHeader, binding.dashboardSummary, binding.dashboardDeadlineCard,
                binding.cardToolTimetable, binding.dashboardQuickTools, binding.dashboardDocuments};
        float offset = 12 * getResources().getDisplayMetrics().density;
        Interpolator easing = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in);
        for (int index = 0; index < sections.length; index++) {
            View section = sections[index];
            section.setAlpha(0f);
            section.setTranslationY(offset);
            section.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(index * 35L)
                    .setDuration(220)
                    .setInterpolator(easing)
                    .start();
        }
    }

    private void adaptDashboard() {
        Configuration configuration = getResources().getConfiguration();
        if (configuration.fontScale > 1.25f || configuration.screenWidthDp < 350) {
            stackTiles(binding.dashboardQuickTools);
            stackTiles(binding.dashboardDocuments);
        }
        if (configuration.fontScale > 1.4f) {
            binding.dashboardHeader.setOrientation(LinearLayout.VERTICAL);
            binding.dashboardHeader.setGravity(Gravity.START);
            binding.dashboardHeader.getChildAt(0).setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private void stackTiles(LinearLayout row) {
        row.setOrientation(LinearLayout.VERTICAL);
        row.getChildAt(1).setLayoutParams(new LinearLayout.LayoutParams(1,
                Math.round(10 * getResources().getDisplayMetrics().density)));
        for (int index : new int[]{0, 2}) {
            View tile = row.getChildAt(index);
            tile.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private void selectTab(int index, boolean scroll) {
        selectedTab = index;
        for (int tab = 0; tab < tabs.length; tab++) {
            int color = getColor(tab == index ? R.color.dashboard_accent : R.color.dashboard_secondary);
            tabs[tab].setSelected(tab == index);
            tabs[tab].setTextColor(color);
            tabs[tab].setCompoundDrawableTintList(ColorStateList.valueOf(color));
        }
        if (scroll) {
            View[] destinations = {null, binding.cardToolTimetable,
                    binding.dashboardUtilities, binding.dashboardDocuments};
            int y = index == 0 ? 0 : destinations[index].getTop();
            if (animationsEnabled()) {
                binding.mainScrollView.smoothScrollTo(0, y);
            } else {
                binding.mainScrollView.scrollTo(0, y);
            }
        }
    }

    private void updateTaskCount() {
        int count = (binding.taskLab.isChecked() ? 0 : 1)
                + (binding.taskSlides.isChecked() ? 0 : 1)
                + (binding.taskAttendance.isChecked() ? 0 : 1);
        binding.taskCount.setText(getString(R.string.dashboard_tasks_left, count));
    }

    private void updateFocusTimer() {
        long now = SystemClock.elapsedRealtime();
        if (focusEnd != 0 && focusEnd <= now) {
            focusEnd = 0;
            focusRemaining = 0;
        }
        if (focusEnd != 0) {
            binding.focusTimer.setBase(focusEnd);
            binding.focusTimer.start();
            binding.cardToolFocus.setText("Tạm dừng");
        } else {
            binding.focusTimer.stop();
            long seconds = (focusRemaining + 999) / 1000;
            binding.focusTimer.setText(String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60));
            binding.cardToolFocus.setText(focusRemaining == FOCUS_DURATION ? "Bắt đầu"
                    : focusRemaining == 0 ? "Bắt đầu lại" : "Tiếp tục");
        }
    }

    private void toggleFocusTimer() {
        long now = SystemClock.elapsedRealtime();
        if (focusEnd == 0) {
            focusEnd = now + (focusRemaining == 0 ? FOCUS_DURATION : focusRemaining);
        } else {
            focusRemaining = Math.max(0, focusEnd - now);
            focusEnd = 0;
        }
        updateFocusTimer();
    }

    private void showAppearanceOptions() {
        PocketKitApplication application = (PocketKitApplication) getApplication();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dashboard_appearance)
                .setSingleChoiceItems(R.array.dashboard_theme_options, application.getThemeChoice(),
                        (dialog, choice) -> {
                            dialog.dismiss();
                            application.setThemeChoice(choice);
                        })
                .show();
    }

    private void setupClickListeners() {
        PocketKitApplication application = (PocketKitApplication) getApplication();
        binding.themeLight.setOnClickListener(v -> application.setThemeChoice(1));
        binding.themeDark.setOnClickListener(v -> application.setThemeChoice(2));
        boolean dark = getNightMode() == Configuration.UI_MODE_NIGHT_YES;
        TextView activeTheme = dark ? binding.themeDark : binding.themeLight;
        activeTheme.setSelected(true);
        activeTheme.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.dashboard_surface)));
        View.OnLongClickListener appearanceOptions = v -> {
            showAppearanceOptions();
            return true;
        };
        binding.themeLight.setOnLongClickListener(appearanceOptions);
        binding.themeDark.setOnLongClickListener(appearanceOptions);

        tabs = new TextView[]{binding.tabToday, binding.tabSchedule, binding.tabTools, binding.tabDocuments};
        for (int index = 0; index < tabs.length; index++) {
            int destination = index;
            tabs[index].setOnClickListener(v -> selectTab(destination, true));
        }
        selectTab(selectedTab, false);

        binding.taskLab.setOnCheckedChangeListener((button, checked) -> updateTaskCount());
        binding.taskSlides.setOnCheckedChangeListener((button, checked) -> updateTaskCount());
        binding.taskAttendance.setOnCheckedChangeListener((button, checked) -> updateTaskCount());
        binding.taskCount.post(this::updateTaskCount);

        binding.cardToolNotes.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotesActivity.class);
            if (animationsEnabled()) {
                ActivityOptionsCompat transition = ActivityOptionsCompat.makeCustomAnimation(
                        this, R.anim.dashboard_enter, R.anim.dashboard_exit);
                startActivity(intent, transition.toBundle());
            } else {
                startActivity(intent);
            }
        });

        binding.cardToolDeadline.setOnClickListener(v ->
                showToast("Mở Hạn nộp bài (Deadline Reminder)")
        );

        binding.cardToolTimetable.setOnClickListener(v ->
                showToast("Mở Thời khóa biểu (Timetable)")
        );

        binding.cardToolGpa.setOnClickListener(v ->
                showToast("Mở Tính điểm GPA & Dự đoán qua môn")
        );

        binding.cardToolFocus.setOnClickListener(v -> toggleFocusTimer());
        binding.focusTimer.setOnChronometerTickListener(timer -> {
            if (focusEnd != 0 && SystemClock.elapsedRealtime() >= focusEnd) {
                updateFocusTimer();
            }
        });

        binding.cardToolCompress.setOnClickListener(v ->
                showToast("Mở Nén ảnh hồ sơ & CCCD (< 100KB, 500KB)")
        );

        binding.cardToolOcr.setOnClickListener(v ->
                showToast("Mở Quét slide & Trích xuất chữ tiếng Việt")
        );

        binding.cardToolPdf.setOnClickListener(v ->
                showToast("Mở Bộ tiện ích PDF (Gộp ảnh thành PDF)")
        );

        binding.cardToolSplitBill.setOnClickListener(v ->
                showToast("Mở Chia tiền phòng trọ / Điện nước EVN")
        );

        binding.cardToolRandom.setOnClickListener(v ->
                showToast("Mở Vòng quay chọn người thuyết trình")
        );

        binding.cardToolQr.setOnClickListener(v ->
                showToast("Mở Quét & Tạo mã QR tài liệu / WiFi")
        );
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
