package com.example.pocketkit;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_NIGHT_MODE = "dashboard_night_mode";
    private static final String STATE_SELECTED_TAB = "dashboard_selected_tab";

    private ActivityMainBinding binding;
    private DatabaseHelper dbHelper;
    private Note currentPinnedNote = null;
    private TextView[] tabs;
    private int selectedTab = 0;

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
    private View[] dayDots;

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
        dayDots = new View[]{
                binding.dotDay0, binding.dotDay1, binding.dotDay2,
                binding.dotDay3, binding.dotDay4, binding.dotDay5, binding.dotDay6
        };

        for (int i = 0; i < dayButtons.length; i++) {
            final int dayIdx = i;
            dayButtons[i].setOnClickListener(v -> selectCalendarDay(dayIdx));
        }
        selectCalendarDay(3);
    }

    private void selectCalendarDay(int dayIdx) {
        int activeColor = ContextCompat.getColor(this, R.color.dashboard_capsule_active_text);
        int inactiveColor = ContextCompat.getColor(this, R.color.dashboard_secondary);
        int textColor = ContextCompat.getColor(this, R.color.dashboard_text);

        for (int i = 0; i < dayButtons.length; i++) {
            if (i == dayIdx) {
                dayButtons[i].setBackgroundResource(R.drawable.shape_calendar_day_selected);
                dayNames[i].setTextColor(activeColor);
                dayNums[i].setTextColor(activeColor);
                if (dayDots[i] != null) dayDots[i].setVisibility(View.VISIBLE);
            } else {
                dayButtons[i].setBackgroundResource(R.drawable.shape_calendar_day_normal);
                dayNames[i].setTextColor(inactiveColor);
                dayNums[i].setTextColor(textColor);
                if (dayDots[i] != null) dayDots[i].setVisibility(View.GONE);
            }
        }

        DaySchedule schedule = weekSchedules[dayIdx];
        binding.tvTimetableBadge.setText(schedule.badge);
        binding.tvTimetableTime.setText(schedule.time);
        binding.tvTimetableSubject.setText(schedule.subject);
        binding.tvTimetableRoom.setText(schedule.room);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_NIGHT_MODE, getNightMode());
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
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
        View[] sections = {
                binding.dashboardHeader,
                binding.dashboardCalendarCard,
                binding.cardToolTimetable,
                binding.cardPinnedNote,
                binding.cardBentoProject,
                binding.dashboardTasks
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

    private void showToolsBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_all_tools, null);
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.btnSheetFocus).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Đồng hồ Pomodoro 25 phút");
        });
        sheetView.findViewById(R.id.btnSheetGpa).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Tính điểm GPA & Dự đoán qua môn");
        });
        sheetView.findViewById(R.id.btnSheetDeadline).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Hạn nộp bài Assignment 1 (Còn 2 ngày)");
        });
        sheetView.findViewById(R.id.btnSheetOcr).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Quét slide & Trích xuất tiếng Việt");
        });
        sheetView.findViewById(R.id.btnSheetCompress).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Nén ảnh CCCD & Thẻ SV (< 500KB)");
        });
        sheetView.findViewById(R.id.btnSheetPdf).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Bộ tiện ích PDF");
        });
        sheetView.findViewById(R.id.btnSheetSplitBill).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Chia tiền trọ & Điện nước");
        });
        sheetView.findViewById(R.id.btnSheetRandom).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Vòng quay bốc thăm nhóm");
        });
        sheetView.findViewById(R.id.btnSheetQr).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Mã QR thanh toán & Wi-Fi");
        });

        dialog.show();
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
        binding.tabToday.setOnClickListener(v -> selectTab(0, true));
        binding.tabSchedule.setOnClickListener(v -> {
            selectTab(1, true);
            showToast("Thời khóa biểu tuần 7 · Block 5");
        });
        binding.tabTools.setOnClickListener(v -> {
            selectTab(2, true);
            showToolsBottomSheet();
        });
        binding.tabDocuments.setOnClickListener(v -> {
            selectTab(3, true);
            showToast("Hồ sơ sinh viên FPT University");
        });
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

        binding.cardBentoProject.setOnClickListener(v ->
                showToast("Dự án Khởi nghiệp EXE201 · Nhóm 4")
        );

        binding.btnDockAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteEditorActivity.class);
            startActivity(intent);
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

        binding.cardToolTimetable.setOnClickListener(v ->
                showToast("Thời khóa biểu: Ca 2 · PRM392 (Phòng 302)")
        );
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
