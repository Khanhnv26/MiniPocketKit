package com.example.pocketkit;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.databinding.ActivityMainBinding;
import com.example.pocketkit.model.Note;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_NIGHT_MODE = "dashboard_night_mode";
    private static final String STATE_SELECTED_TAB = "dashboard_selected_tab";
    private static final String STATE_FOCUS_END = "dashboard_focus_end";
    private static final String STATE_FOCUS_REMAINING = "dashboard_focus_remaining";
    private static final long FOCUS_DURATION = 25 * 60_000L;

    private ActivityMainBinding binding;
    private DatabaseHelper dbHelper;
    private Note currentPinnedNote = null;
    private TextView[] tabs;
    private int selectedTab = 0;
    private long focusEnd;
    private long focusRemaining = FOCUS_DURATION;

    private static class DaySchedule {
        final String badge;
        final String time;
        final String subject;
        final String room;

        DaySchedule(String badge, String time, String subject, String room) {
            this.badge = badge;
            this.time = time;
            this.subject = subject;
            this.room = room;
        }
    }

    private final DaySchedule[] weekSchedules = new DaySchedule[]{
            new DaySchedule("● ĐÃ KẾT THÚC", "Ca 1 · 07:30 - 09:00", "SWP391 · Phát triển dự án phần mềm", "Phòng 401 (Tòa Beta) · GV Cô Mai"),
            new DaySchedule("● ĐÃ KẾT THÚC", "Ca 3 · 13:00 - 15:30", "MAS291 · Thống kê ứng dụng", "Phòng 204 (Tòa Alpha) · GV Thầy Dũng"),
            new DaySchedule("● ĐÃ KẾT THÚC", "Ca 4 · 15:45 - 18:00", "PRN211 · Lập trình C# .NET", "Phòng Lab 3 (Tòa Gamma) · GV Thầy Long"),
            new DaySchedule("● TIẾT HỌC KẾ TIẾP", "Ca 2 · 09:15 - 11:45", "PRM392 · Lập trình Android", "Phòng 302 (Tòa Alpha) · GV Thầy Hoàng"),
            new DaySchedule("● NGÀY MAI", "Ca 2 · 09:15 - 11:45", "EXE201 · Khởi nghiệp sáng tạo", "Hội trường A (Tòa Delta) · GV Thầy Tuấn"),
            new DaySchedule("● CUỐI TUẦN", "Ca 3 · 14:00 - 16:00", "SWP391 · Họp Sprint 2 Review", "Online Google Meet · Nhóm trưởng"),
            new DaySchedule("● NGÀY NGHỈ", "Cả ngày", "Tự học & Chuẩn bị bài tuần mới", "Thư viện Tòa Alpha · Tự do")
    };

    private View[] dayButtons;
    private TextView[] dayNames;
    private TextView[] dayNums;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

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
        setupCalendarStrip();
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
    protected void onResume() {
        super.onResume();
        loadNotesFromDatabase();
    }

    private void loadNotesFromDatabase() {
        if (dbHelper == null) return;
        List<Note> pinnedNotes = dbHelper.getNotesFiltered("", "PINNED");
        if (!pinnedNotes.isEmpty()) {
            currentPinnedNote = pinnedNotes.get(0);
            binding.cardPinnedNote.setVisibility(View.VISIBLE);
            binding.tvPinnedSubject.setText("● " + currentPinnedNote.getSubject());
            binding.tvPinnedTitle.setText(currentPinnedNote.getTitle());
            binding.tvPinnedContent.setText(currentPinnedNote.getContent());
            binding.tvPinnedTime.setText(currentPinnedNote.getUpdatedAt());
        } else {
            List<Note> allNotes = dbHelper.getNotesFiltered("", "ALL");
            if (!allNotes.isEmpty()) {
                currentPinnedNote = allNotes.get(0);
                binding.cardPinnedNote.setVisibility(View.VISIBLE);
                binding.tvPinnedSubject.setText("● " + currentPinnedNote.getSubject());
                binding.tvPinnedTitle.setText(currentPinnedNote.getTitle());
                binding.tvPinnedContent.setText(currentPinnedNote.getContent());
                binding.tvPinnedTime.setText(currentPinnedNote.getUpdatedAt());
            } else {
                binding.cardPinnedNote.setVisibility(View.GONE);
            }
        }
    }

    private void setupCalendarStrip() {
        dayButtons = new View[]{
                binding.dayBtn0, binding.dayBtn1, binding.dayBtn2,
                binding.dayBtn3, binding.dayBtn4, binding.dayBtn5, binding.dayBtn6
        };
        dayNames = new TextView[]{
                binding.tvDayName0, binding.tvDayName1, binding.tvDayName2,
                binding.tvDayName3, binding.tvDayName4, binding.tvDayName5, binding.tvDayName6
        };
        dayNums = new TextView[]{
                binding.tvDayNum0, binding.tvDayNum1, binding.tvDayNum2,
                binding.tvDayNum3, binding.tvDayNum4, binding.tvDayNum5, binding.tvDayNum6
        };

        for (int i = 0; i < dayButtons.length; i++) {
            final int dayIdx = i;
            dayButtons[i].setOnClickListener(v -> selectCalendarDay(dayIdx));
        }
        selectCalendarDay(3);
    }

    private void selectCalendarDay(int dayIdx) {
        int activeColor = ContextCompat.getColor(this, R.color.dashboard_surface);
        int inactiveColor = ContextCompat.getColor(this, R.color.dashboard_secondary);
        int textColor = ContextCompat.getColor(this, R.color.dashboard_text);

        for (int i = 0; i < dayButtons.length; i++) {
            if (i == dayIdx) {
                dayButtons[i].setBackgroundResource(R.drawable.shape_calendar_day_selected);
                dayNames[i].setTextColor(activeColor);
                dayNums[i].setTextColor(activeColor);
            } else {
                dayButtons[i].setBackgroundResource(R.drawable.shape_calendar_day_normal);
                dayNames[i].setTextColor(inactiveColor);
                dayNums[i].setTextColor(textColor);
            }
        }

        DaySchedule schedule = weekSchedules[dayIdx];
        binding.tvTimetableBadge.setText(schedule.badge);
        binding.tvTimetableTime.setText(schedule.time);
        binding.tvTimetableSubject.setText(schedule.subject);
        binding.tvTimetableRoom.setText(schedule.room);
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
        View[] sections = {
                binding.dashboardHeader,
                binding.dashboardCalendarCard,
                binding.cardToolTimetable,
                binding.dashboardTasks,
                binding.dashboardSummary,
                binding.dashboardQuickTools,
                binding.dashboardDeadlineCard,
                binding.dashboardDocuments,
                binding.dashboardUtilities
        };
        float offset = 12 * getResources().getDisplayMetrics().density;
        long stagger = 30L;
        for (int index = 0; index < sections.length; index++) {
            View section = sections[index];
            if (section == null) continue;
            section.setAlpha(0f);
            section.setTranslationY(offset);
            section.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(320)
                    .setStartDelay(index * stagger)
                    .start();
        }
    }

    private void adaptDashboard() {
        boolean dark = getNightMode() == Configuration.UI_MODE_NIGHT_YES;
        binding.dashboardDate.setTextColor(getColor(dark ? R.color.dashboard_secondary : R.color.dashboard_accent));
    }

    private void updateTaskCount() {
        int count = 0;
        if (!binding.taskLab.isChecked()) count++;
        if (!binding.taskSlides.isChecked()) count++;
        if (!binding.taskAttendance.isChecked()) count++;
        binding.taskCount.setText(getString(R.string.dashboard_tasks_left, count));
    }

    private void selectTab(int index, boolean animate) {
        selectedTab = index;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = i == index;
            tabs[i].setSelected(active);
            int color = getColor(active ? R.color.dashboard_accent : R.color.dashboard_secondary);
            tabs[i].setTextColor(color);
            tabs[i].getCompoundDrawablesRelative()[1].setTint(color);
            if (active && animate && animationsEnabled()) {
                tabs[i].setScaleX(0.92f);
                tabs[i].setScaleY(0.92f);
                tabs[i].animate().scaleX(1f).scaleY(1f).setDuration(160).start();
            }
        }
    }

    private void updateFocusTimer() {
        long remaining = focusEnd == 0 ? focusRemaining : Math.max(0, focusEnd - SystemClock.elapsedRealtime());
        long base = SystemClock.elapsedRealtime() + remaining;
        binding.focusTimer.setBase(base);
        if (focusEnd == 0) {
            binding.focusTimer.stop();
            binding.cardToolFocus.setText("Bắt đầu");
            binding.cardToolFocus.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.dashboard_segment)));
            binding.cardToolFocus.setTextColor(getColor(R.color.dashboard_text));
        } else {
            binding.focusTimer.start();
            binding.cardToolFocus.setText("Tạm dừng");
            binding.cardToolFocus.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.dashboard_accent)));
            binding.cardToolFocus.setTextColor(getColor(R.color.dashboard_surface));
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

        binding.cardPinnedNote.setOnClickListener(v -> {
            if (currentPinnedNote != null) {
                Intent intent = new Intent(this, NoteEditorActivity.class);
                intent.putExtra("note_item", currentPinnedNote);
                startActivity(intent);
            }
        });

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
