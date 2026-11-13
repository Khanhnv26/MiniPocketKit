package com.example.pocketkit;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.databinding.ActivityMainBinding;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.ui.FrostUiHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "state_selected_tab";
    private static final String STATE_SELECTED_DAY = "state_selected_day";
    private static final String STATE_URGENT_SUBMITTED = "state_urgent_submitted";
    private static final String STATE_COMPRESS_PRESET = "state_compress_preset";
    private static final String STATE_OCR_SAMPLE = "state_ocr_sample";
    private static final String STATE_TIMER_MS = "state_timer_ms";
    private static final long FOCUS_DURATION_MS = 25L * 60L * 1000L;

    private ActivityMainBinding binding;
    private DatabaseHelper dbHelper;
    private Note currentPinnedNote;

    private int selectedTab = 0;
    private int selectedDayIndex = 2;
    private boolean urgentSubmitted = false;
    private int selectedCompressPreset = 0;
    private int selectedOcrSample = 0;
    private long remainingTimerMs = FOCUS_DURATION_MS;
    private boolean timerRunning = false;
    private CountDownTimer focusTimer;

    private final List<TaskItem> taskItems = new ArrayList<>();
    private final Map<Integer, List<ScheduleSlot>> scheduleMap = new LinkedHashMap<>();

    private static class TaskItem {
        final String title;
        final String meta;
        final String tag;
        boolean done;

        TaskItem(String title, String meta, String tag, boolean done) {
            this.title = title;
            this.meta = meta;
            this.tag = tag;
            this.done = done;
        }
    }

    private static class ScheduleSlot {
        final String time;
        final String room;
        final String subject;
        final String lecturer;

        ScheduleSlot(String time, String room, String subject, String lecturer) {
            this.time = time;
            this.room = room;
            this.subject = subject;
            this.lecturer = lecturer;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        FrostUiHelper.setupEdgeToEdgeAndBackdrop(
                this,
                binding.rootContainer,
                binding.mainScrollView,
                binding.ambientBackdrop.ambientBackdropView,
                binding.bottomDockContainer
        );
        restoreState(savedInstanceState);
        seedData();
        bindHeaderDate();
        bindHeroCard();
        bindFocusTimer();
        bindChecklist();
        bindCalendarTab();
        bindToolsTab();
        bindProfileTab();
        bindDockNavigation();
        applySavedSelections();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotesIntegration();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
        outState.putInt(STATE_SELECTED_DAY, selectedDayIndex);
        outState.putBoolean(STATE_URGENT_SUBMITTED, urgentSubmitted);
        outState.putInt(STATE_COMPRESS_PRESET, selectedCompressPreset);
        outState.putInt(STATE_OCR_SAMPLE, selectedOcrSample);
        outState.putLong(STATE_TIMER_MS, remainingTimerMs);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (focusTimer != null) {
            focusTimer.cancel();
            focusTimer = null;
        }
        super.onDestroy();
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        selectedTab = savedInstanceState.getInt(STATE_SELECTED_TAB, 0);
        selectedDayIndex = savedInstanceState.getInt(STATE_SELECTED_DAY, 2);
        urgentSubmitted = savedInstanceState.getBoolean(STATE_URGENT_SUBMITTED, false);
        selectedCompressPreset = savedInstanceState.getInt(STATE_COMPRESS_PRESET, 0);
        selectedOcrSample = savedInstanceState.getInt(STATE_OCR_SAMPLE, 0);
        remainingTimerMs = savedInstanceState.getLong(STATE_TIMER_MS, FOCUS_DURATION_MS);
    }

    private void seedData() {
        taskItems.clear();
        taskItems.add(new TaskItem("Hoàn thiện giao diện báo cáo tuần", "Cơ sở dữ liệu · Hôm nay", "Bài tập", false));
        taskItems.add(new TaskItem("Ôn tập chương 4 Quản lý bộ nhớ", "Hệ điều hành · 30 phút", "Ôn thi", false));
        taskItems.add(new TaskItem("Gửi biên bản họp nhóm", "Dự án phần mềm · Xong sáng nay", "Nhóm", true));

        scheduleMap.clear();
        List<ScheduleSlot> t2 = new ArrayList<>();
        t2.add(new ScheduleSlot("07:30 - 09:45", "Phòng A204", "Cơ sở dữ liệu nâng cao", "Giảng viên Nguyễn Minh Anh"));
        t2.add(new ScheduleSlot("13:30 - 15:45", "Phòng Lab 02", "Lập trình Di động", "Giảng viên Trần Quốc Bảo"));
        scheduleMap.put(0, t2);

        List<ScheduleSlot> t3 = new ArrayList<>();
        t3.add(new ScheduleSlot("09:55 - 12:10", "Phòng B301", "Hệ điều hành", "Giảng viên Lê Thu Hà"));
        scheduleMap.put(1, t3);

        List<ScheduleSlot> t4 = new ArrayList<>();
        t4.add(new ScheduleSlot("07:30 - 09:45", "Phòng A302", "Lập trình Di động", "Giảng viên Trần Quốc Bảo"));
        t4.add(new ScheduleSlot("13:30 - 15:45", "Phòng C105", "Phát triển Web hiện đại", "Giảng viên Phạm Đức Huy"));
        scheduleMap.put(2, t4);

        List<ScheduleSlot> t5 = new ArrayList<>();
        t5.add(new ScheduleSlot("09:55 - 12:10", "Phòng A401", "Thiết kế giao diện người dùng", "Giảng viên Vũ Ngọc Lan"));
        scheduleMap.put(3, t5);

        List<ScheduleSlot> t6 = new ArrayList<>();
        t6.add(new ScheduleSlot("13:30 - 15:45", "Phòng Lab 04", "Thực hành Dự án Phần mềm", "Giảng viên Nguyễn Minh Anh"));
        scheduleMap.put(4, t6);

        List<ScheduleSlot> t7 = new ArrayList<>();
        t7.add(new ScheduleSlot("08:30 - 10:30", "Thư viện tầng 2", "Tổng hợp đề cương ôn tập cuối kỳ", "Tự học theo nhóm"));
        scheduleMap.put(5, t7);
    }

    private void bindHeaderDate() {
        Locale vi = Locale.forLanguageTag("vi-VN");
        String formatted = new SimpleDateFormat("EEEE, d 'tháng' M", vi).format(new Date());
        binding.tvHeaderDate.setText(formatted.toUpperCase(vi));
        binding.btnHeaderProfile.setOnClickListener(v -> switchTab(3));
    }

    private void bindHeroCard() {
        updateSpotlightUi(false);
        binding.tabHome.btnSpotlightAction.setOnClickListener(v -> {
            urgentSubmitted = !urgentSubmitted;
            updateSpotlightUi(true);
            String msg = urgentSubmitted ? "Đã đánh dấu hoàn thành Lab 04" : "Đã khôi phục trạng thái chưa nộp";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSpotlightUi(boolean animate) {
        if (urgentSubmitted) {
            binding.tabHome.tvSpotlightBadge.setText("TIẾT HỌC TIẾP THEO");
            binding.tabHome.tvSpotlightBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_mint_text));
            binding.tabHome.tvSpotlightTitle.setText("Lập trình Di động · Phòng A302");
            binding.tabHome.tvSpotlightSub.setText("Bắt đầu lúc 13:30 · Đã hoàn thành Lab 04");
            binding.tabHome.btnSpotlightAction.setText("Hoàn tác");
            binding.tabHome.btnSpotlightAction.setBackgroundResource(R.drawable.bg_pill_secondary);
            binding.tabHome.btnSpotlightAction.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
            binding.tabHome.ringsView.setDeadlineProgress(1.0f, animate);
            binding.tabHome.tvRingDeadlineValue.setText("100%");
        } else {
            binding.tabHome.tvSpotlightBadge.setText("NGAY LÚC NÀY");
            binding.tabHome.tvSpotlightBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_sapphire));
            binding.tabHome.tvSpotlightTitle.setText("Nộp Lab 04 · Lập trình Di động");
            binding.tabHome.tvSpotlightSub.setText("Hạn 17:00 hôm nay · Còn 2 giờ");
            binding.tabHome.btnSpotlightAction.setText("Nộp ngay");
            binding.tabHome.btnSpotlightAction.setBackgroundResource(R.drawable.bg_pill_primary);
            binding.tabHome.btnSpotlightAction.setTextColor(ContextCompat.getColor(this, R.color.white));
            binding.tabHome.ringsView.setDeadlineProgress(0.68f, animate);
            binding.tabHome.tvRingDeadlineValue.setText("68%");
        }
        binding.tabHome.ringsView.setClassProgress(0.85f, animate);
        binding.tabHome.tvRingClassValue.setText("85%");
    }

    private void bindFocusTimer() {
        updateTimerUi(false);
        binding.tabHome.btnTimerToggle.setOnClickListener(v -> {
            if (timerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });
        binding.tabHome.btnTimerReset.setOnClickListener(v -> resetTimer());
    }

    private void startTimer() {
        if (focusTimer != null) {
            focusTimer.cancel();
        }
        timerRunning = true;
        binding.tabHome.btnTimerToggle.setText("Tạm dừng");
        binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_dark);
        focusTimer = new CountDownTimer(remainingTimerMs, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimerMs = millisUntilFinished;
                updateTimerUi(false);
            }

            @Override
            public void onFinish() {
                remainingTimerMs = 0L;
                timerRunning = false;
                binding.tabHome.btnTimerToggle.setText("Bắt đầu");
                binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_primary);
                updateTimerUi(true);
                Toast.makeText(MainActivity.this, "Hoàn thành phiên tập trung 25 phút", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void pauseTimer() {
        if (focusTimer != null) {
            focusTimer.cancel();
            focusTimer = null;
        }
        timerRunning = false;
        binding.tabHome.btnTimerToggle.setText("Tiếp tục");
        binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_primary);
    }

    private void resetTimer() {
        if (focusTimer != null) {
            focusTimer.cancel();
            focusTimer = null;
        }
        timerRunning = false;
        remainingTimerMs = FOCUS_DURATION_MS;
        binding.tabHome.btnTimerToggle.setText("Bắt đầu");
        binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_primary);
        updateTimerUi(true);
    }

    private void updateTimerUi(boolean animate) {
        long totalSeconds = remainingTimerMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        binding.tabHome.tvTimerDisplay.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
        float elapsedFraction = 1f - ((float) remainingTimerMs / (float) FOCUS_DURATION_MS);
        float focusRingProgress = Math.max(0.40f, Math.min(1.0f, 0.40f + 0.60f * elapsedFraction));
        binding.tabHome.ringsView.setFocusProgress(focusRingProgress, animate);
        binding.tabHome.tvRingFocusValue.setText(Math.round(focusRingProgress * 100f) + "%");
    }

    private void bindChecklist() {
        renderChecklist();
        binding.tabHome.btnHomeOpenNotes.setOnClickListener(v -> openNotesActivity());
        binding.tabHome.btnAddTask.setOnClickListener(v -> addNewTaskFromInput());
        binding.tabHome.edtNewTask.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addNewTaskFromInput();
                return true;
            }
            return false;
        });
        binding.tabHome.layoutPinnedNoteBar.setOnClickListener(v -> {
            if (currentPinnedNote != null) {
                Intent intent = new Intent(this, NoteEditorActivity.class);
                intent.putExtra(Note.EXTRA_NOTE_ITEM, currentPinnedNote);
                startActivity(intent);
            }
        });
    }

    private void addNewTaskFromInput() {
        String text = binding.tabHome.edtNewTask.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        taskItems.add(0, new TaskItem(text, "Thêm nhanh · Hôm nay", "Cá nhân", false));
        binding.tabHome.edtNewTask.setText("");
        renderChecklist();
    }

    private void renderChecklist() {
        LinearLayout container = binding.tabHome.checklistContainer;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int remainingCount = 0;

        for (TaskItem item : taskItems) {
            if (!item.done) {
                remainingCount++;
            }
            View row = inflater.inflate(R.layout.item_checklist_task, container, false);
            View checkBox = row.findViewById(R.id.taskCheckBox);
            ImageView ivCheck = row.findViewById(R.id.ivTaskCheck);
            TextView tvTitle = row.findViewById(R.id.tvTaskTitle);
            TextView tvMeta = row.findViewById(R.id.tvTaskMeta);
            TextView tvTag = row.findViewById(R.id.tvTaskTag);

            tvTitle.setText(item.title);
            tvMeta.setText(item.meta);
            tvTag.setText(item.tag);

            applyTaskRowStyle(checkBox, ivCheck, tvTitle, tvTag, item.done);

            row.setOnClickListener(v -> {
                item.done = !item.done;
                renderChecklist();
            });
            container.addView(row);
        }

        binding.tabHome.tvChecklistCount.setText(remainingCount + "/" + taskItems.size() + " việc");
    }

    private void applyTaskRowStyle(View checkBox, ImageView ivCheck, TextView tvTitle, TextView tvTag, boolean done) {
        if (done) {
            checkBox.setBackgroundResource(R.drawable.bg_check_checked);
            ivCheck.setVisibility(View.VISIBLE);
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.ink_muted));
            tvTag.setBackgroundResource(R.drawable.bg_pill_success_soft);
            tvTag.setTextColor(ContextCompat.getColor(this, R.color.accent_mint_text));
        } else {
            checkBox.setBackgroundResource(R.drawable.bg_check_unchecked);
            ivCheck.setVisibility(View.GONE);
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
            tvTag.setBackgroundResource(R.drawable.bg_pill_neutral_soft);
            tvTag.setTextColor(ContextCompat.getColor(this, R.color.ink_secondary));
        }
    }

    private void refreshNotesIntegration() {
        if (dbHelper == null) {
            return;
        }
        List<Note> pinned = dbHelper.getNotesFiltered("", DatabaseHelper.FILTER_PINNED);
        List<Note> all = dbHelper.getNotesFiltered("", DatabaseHelper.FILTER_ALL);

        if (!pinned.isEmpty()) {
            currentPinnedNote = pinned.get(0);
        } else if (!all.isEmpty()) {
            currentPinnedNote = all.get(0);
        } else {
            currentPinnedNote = null;
        }

        if (currentPinnedNote != null) {
            binding.tabHome.layoutPinnedNoteBar.setVisibility(View.VISIBLE);
            String subject = currentPinnedNote.getSubject() != null && !currentPinnedNote.getSubject().trim().isEmpty()
                    ? currentPinnedNote.getSubject()
                    : getString(R.string.note_default_category);
            binding.tabHome.tvPinnedNoteSummary.setText(
                    getString(R.string.notes_summary_pinned_bar, subject, currentPinnedNote.getTitle())
            );
        } else {
            binding.tabHome.layoutPinnedNoteBar.setVisibility(View.GONE);
        }

        binding.tabTools.tvToolNotesCount.setText(getString(R.string.tools_notes_count_format, all.size()));
    }

    private void bindCalendarTab() {
        TextView[] chips = new TextView[]{
                binding.tabCalendar.chipDayT2,
                binding.tabCalendar.chipDayT3,
                binding.tabCalendar.chipDayT4,
                binding.tabCalendar.chipDayT5,
                binding.tabCalendar.chipDayT6,
                binding.tabCalendar.chipDayT7
        };
        for (int i = 0; i < chips.length; i++) {
            final int index = i;
            chips[i].setOnClickListener(v -> selectScheduleDay(index));
        }
    }

    private void selectScheduleDay(int dayIndex) {
        selectedDayIndex = dayIndex;
        TextView[] chips = new TextView[]{
                binding.tabCalendar.chipDayT2,
                binding.tabCalendar.chipDayT3,
                binding.tabCalendar.chipDayT4,
                binding.tabCalendar.chipDayT5,
                binding.tabCalendar.chipDayT6,
                binding.tabCalendar.chipDayT7
        };
        String[] dayLabels = new String[]{
                "Lịch học Thứ Hai",
                "Lịch học Thứ Ba",
                "Lịch học Thứ Tư",
                "Lịch học Thứ Năm",
                "Lịch học Thứ Sáu",
                "Lịch học Thứ Bảy"
        };

        for (int i = 0; i < chips.length; i++) {
            boolean active = i == dayIndex;
            chips[i].setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chips[i].setTextColor(ContextCompat.getColor(this, active ? R.color.white : R.color.ink_primary));
        }

        binding.tabCalendar.tvCalendarDayTitle.setText(dayLabels[dayIndex]);
        List<ScheduleSlot> slots = scheduleMap.get(dayIndex);
        int count = slots != null ? slots.size() : 0;
        binding.tabCalendar.tvCalendarCountBadge.setText(count + " ca học");

        LinearLayout container = binding.tabCalendar.scheduleContainer;
        container.removeAllViews();
        if (slots == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ScheduleSlot slot : slots) {
            View card = inflater.inflate(R.layout.item_schedule_slot, container, false);
            ((TextView) card.findViewById(R.id.tvSlotTime)).setText(slot.time);
            ((TextView) card.findViewById(R.id.tvSlotRoom)).setText(slot.room);
            ((TextView) card.findViewById(R.id.tvSlotSubject)).setText(slot.subject);
            ((TextView) card.findViewById(R.id.tvSlotLecturer)).setText(slot.lecturer);
            container.addView(card);
        }
    }

    private void bindToolsTab() {
        binding.tabTools.chipCompress100.setOnClickListener(v -> selectCompressPreset(0));
        binding.tabTools.chipCompress500.setOnClickListener(v -> selectCompressPreset(1));
        binding.tabTools.chipCompress3x4.setOnClickListener(v -> selectCompressPreset(2));
        binding.tabTools.btnRunCompress.setOnClickListener(v -> {
            String presetName = selectedCompressPreset == 0 ? "dưới 100 KB"
                    : (selectedCompressPreset == 1 ? "dưới 500 KB" : "chuẩn ảnh thẻ 3x4");
            Toast.makeText(this, "Đã sẵn sàng cấu hình nén ảnh " + presetName, Toast.LENGTH_SHORT).show();
        });

        binding.tabTools.chipOcrSample1.setOnClickListener(v -> selectOcrSample(0));
        binding.tabTools.chipOcrSample2.setOnClickListener(v -> selectOcrSample(1));
        binding.tabTools.btnCopyOcr.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("ocr_text", binding.tabTools.tvOcrOutput.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Đã sao chép nội dung OCR vào bộ nhớ tạm", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tabTools.btnToolOpenNotes.setOnClickListener(v -> openNotesActivity());
        binding.tabTools.btnToolNewNote.setOnClickListener(v ->
                startActivity(new Intent(this, NoteEditorActivity.class))
        );
    }

    private void selectCompressPreset(int preset) {
        selectedCompressPreset = preset;
        TextView[] chips = new TextView[]{
                binding.tabTools.chipCompress100,
                binding.tabTools.chipCompress500,
                binding.tabTools.chipCompress3x4
        };
        for (int i = 0; i < chips.length; i++) {
            boolean active = i == preset;
            chips[i].setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chips[i].setTextColor(ContextCompat.getColor(this, active ? R.color.white : R.color.ink_primary));
        }

        if (preset == 0) {
            binding.tabTools.tvCompressSpec.setText("Định dạng JPEG · Chất lượng 82% · 1280 x 960 px");
            binding.tabTools.tvCompressEstimate.setText("Ảnh gốc 3.4 MB giảm còn 94 KB");
        } else if (preset == 1) {
            binding.tabTools.tvCompressSpec.setText("Định dạng JPEG · Chất lượng 92% · 2048 x 1536 px");
            binding.tabTools.tvCompressEstimate.setText("Ảnh gốc 3.4 MB giảm còn 420 KB");
        } else {
            binding.tabTools.tvCompressSpec.setText("Ảnh chân dung 3x4 · 300 DPI · Nền sáng đồng nhất");
            binding.tabTools.tvCompressEstimate.setText("Kích thước chuẩn 354 x 472 px · 148 KB");
        }
    }

    private void selectOcrSample(int sample) {
        selectedOcrSample = sample;
        boolean s1 = sample == 0;
        binding.tabTools.chipOcrSample1.setBackgroundResource(s1 ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        binding.tabTools.chipOcrSample1.setTextColor(ContextCompat.getColor(this, s1 ? R.color.white : R.color.ink_primary));
        binding.tabTools.chipOcrSample2.setBackgroundResource(!s1 ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        binding.tabTools.chipOcrSample2.setTextColor(ContextCompat.getColor(this, !s1 ? R.color.white : R.color.ink_primary));

        if (s1) {
            binding.tabTools.tvOcrOutput.setText(
                    "Chương 5: Thiết kế giao diện thích ứng\n"
                            + "1. Đảm bảo vùng chạm tối thiểu 48dp cho mọi nút tương tác.\n"
                            + "2. Giữ tỷ lệ tương phản chữ trên nền kính tối thiểu 4.5:1."
            );
        } else {
            binding.tabTools.tvOcrOutput.setText(
                    "Biên bản họp nhóm tuần 07\n"
                            + "Hoàn thiện module nén ảnh hồ sơ trước thứ Sáu.\n"
                            + "Kiểm thử tích hợp SQLite trên thiết bị API 24 trở lên."
            );
        }
    }

    private void bindProfileTab() {
        updateGpaSimulation(binding.tabProfile.seekSimulatedScore.getProgress());
        binding.tabProfile.seekSimulatedScore.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateGpaSimulation(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        binding.tabProfile.btnProfileNotes.setOnClickListener(v -> openNotesActivity());
    }

    private void updateGpaSimulation(int progressValue) {
        float scoreTen = progressValue / 10f;
        float scoreFour = scoreTen * 0.4f;
        float projectedGpa = Math.min(4.0f, (3.44f * 0.85f) + (scoreFour * 0.15f));

        binding.tabProfile.tvSimulatedScore.setText(String.format(Locale.US, "Điểm dự kiến học phần hiện tại: %.1f / 10", scoreTen));
        binding.tabProfile.tvGpaProjected.setText(String.format(Locale.US, "%.2f / 4.0", projectedGpa));
        binding.tabProfile.pbGpaMeter.setProgress(Math.round(projectedGpa * 100f));

        if (projectedGpa >= 3.60f) {
            binding.tabProfile.tvGpaRankBadge.setText("Xuất sắc");
            binding.tabProfile.tvGpaRankBadge.setBackgroundResource(R.drawable.bg_pill_success_soft);
            binding.tabProfile.tvGpaRankBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_mint_text));
        } else if (projectedGpa >= 3.20f) {
            binding.tabProfile.tvGpaRankBadge.setText("Giỏi");
            binding.tabProfile.tvGpaRankBadge.setBackgroundResource(R.drawable.bg_pill_accent_soft);
            binding.tabProfile.tvGpaRankBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_sapphire));
        } else {
            binding.tabProfile.tvGpaRankBadge.setText("Khá");
            binding.tabProfile.tvGpaRankBadge.setBackgroundResource(R.drawable.bg_pill_neutral_soft);
            binding.tabProfile.tvGpaRankBadge.setTextColor(ContextCompat.getColor(this, R.color.ink_secondary));
        }
    }

    private void bindDockNavigation() {
        binding.dockTabHome.setOnClickListener(v -> switchTab(0));
        binding.dockTabCalendar.setOnClickListener(v -> switchTab(1));
        binding.dockTabTools.setOnClickListener(v -> switchTab(2));
        binding.dockTabProfile.setOnClickListener(v -> switchTab(3));
    }

    private void applySavedSelections() {
        selectScheduleDay(selectedDayIndex);
        selectCompressPreset(selectedCompressPreset);
        selectOcrSample(selectedOcrSample);
        switchTab(selectedTab);
    }

    private void switchTab(int index) {
        selectedTab = index;

        View[] containers = new View[]{
                binding.tabHome.getRoot(),
                binding.tabCalendar.getRoot(),
                binding.tabTools.getRoot(),
                binding.tabProfile.getRoot()
        };
        LinearLayout[] dockTabs = new LinearLayout[]{
                binding.dockTabHome,
                binding.dockTabCalendar,
                binding.dockTabTools,
                binding.dockTabProfile
        };
        ImageView[] dockIcons = new ImageView[]{
                binding.ivDockHome,
                binding.ivDockCalendar,
                binding.ivDockTools,
                binding.ivDockProfile
        };
        TextView[] dockLabels = new TextView[]{
                binding.tvDockHome,
                binding.tvDockCalendar,
                binding.tvDockTools,
                binding.tvDockProfile
        };
        String[] headers = new String[]{
                "Mini Pocket Kit",
                "Lịch học tuần",
                "Tiện ích nhanh",
                "Hồ sơ học tập"
        };

        int activeColor = ContextCompat.getColor(this, R.color.accent_sapphire);
        int inactiveColor = ContextCompat.getColor(this, R.color.ink_secondary);

        binding.tvHeaderTitle.setText(headers[index]);

        for (int i = 0; i < containers.length; i++) {
            boolean active = i == index;
            containers[i].setVisibility(active ? View.VISIBLE : View.GONE);
            if (active) {
                dockTabs[i].setBackgroundResource(R.drawable.bg_dock_active_pill);
                dockIcons[i].setColorFilter(activeColor);
                dockLabels[i].setTextColor(activeColor);
                dockLabels[i].setTypeface(null, Typeface.BOLD);
            } else {
                dockTabs[i].setBackgroundResource(android.R.color.transparent);
                dockIcons[i].setColorFilter(inactiveColor);
                dockLabels[i].setTextColor(inactiveColor);
                dockLabels[i].setTypeface(null, Typeface.NORMAL);
            }
        }
        binding.mainScrollView.scrollTo(0, 0);
    }

    private void openNotesActivity() {
        Intent intent = new Intent(this, NotesActivity.class);
        if (FrostUiHelper.areAnimationsEnabled(this)) {
            ActivityOptionsCompat transition = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.dashboard_enter, R.anim.dashboard_exit);
            startActivity(intent, transition.toBundle());
        } else {
            startActivity(intent);
        }
    }
}
