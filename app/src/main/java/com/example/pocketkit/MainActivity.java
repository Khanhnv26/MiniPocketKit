package com.example.pocketkit;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.databinding.ActivityMainBinding;
import com.example.pocketkit.home.HomeSummaryHelper;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.profile.ProfileGpaHelper;
import com.example.pocketkit.profile.StudentProfileStore;
import com.example.pocketkit.reminder.TaskReminderHelper;
import com.example.pocketkit.reminder.TaskReminderScheduler;
import com.example.pocketkit.transfer.ScheduleTransferHelper;
import com.example.pocketkit.ui.FrostUiHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "state_selected_tab";
    private static final String STATE_SELECTED_DAY = "state_selected_day";
    private static final String STATE_COMPRESS_PRESET = "state_compress_preset";
    private static final String STATE_OCR_SAMPLE = "state_ocr_sample";
    private static final String STATE_TIMER_MS = "state_timer_ms";
    private static final String STATE_EXPORT_FORMAT = "state_export_format";
    private static final String STATE_EXPORT_TEMPLATE = "state_export_template";
    private static final long FOCUS_DURATION_MS = 25L * 60L * 1000L;

    private ActivityMainBinding binding;
    private DatabaseHelper dbHelper;
    private StudentProfileStore profileStore;
    private StudentProfile currentProfile;
    private Note currentPinnedNote;
    private HomeSummaryHelper.SpotlightCandidate currentSpotlight;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private int selectedTab = 0;
    private int selectedDayIndex = getInitialDayIndex();
    private int selectedCompressPreset = 0;
    private int selectedOcrSample = 0;
    private long remainingTimerMs = FOCUS_DURATION_MS;
    private boolean timerRunning = false;
    private CountDownTimer focusTimer;

    private ScheduleTransferHelper.TransferFormat pendingExportFormat = ScheduleTransferHelper.TransferFormat.XLSX;
    private boolean pendingExportTemplate = false;

    private List<StudyTask> taskItems = new ArrayList<>();
    private StudyTask pendingReminderTask;
    private boolean taskReceiverRegistered = false;

    private final BroadcastReceiver taskUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshHomeDashboard(false);
        }
    };

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                StudyTask pending = pendingReminderTask;
                pendingReminderTask = null;
                if (granted && pending != null) {
                    TaskReminderScheduler.scheduleOrCancel(this, pending);
                } else if (!granted && pending != null) {
                    Toast.makeText(this, R.string.task_reminder_permission_denied, Toast.LENGTH_SHORT).show();
                }
            }
    );

    private static final class ExportDocumentSpec {
        final String fileName;
        final ScheduleTransferHelper.TransferFormat format;

        ExportDocumentSpec(String fileName, ScheduleTransferHelper.TransferFormat format) {
            this.fileName = fileName;
            this.format = format;
        }
    }

    private final ActivityResultLauncher<String[]> importScheduleLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    handleImportUri(uri);
                }
            }
    );

    private final ActivityResultLauncher<ExportDocumentSpec> exportScheduleLauncher = registerForActivityResult(
            new ActivityResultContract<ExportDocumentSpec, Uri>() {
                @NonNull
                @Override
                public Intent createIntent(@NonNull Context context, ExportDocumentSpec input) {
                    return new Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType(input.format.mimeType)
                            .putExtra(Intent.EXTRA_TITLE, input.fileName);
                }

                @Override
                public Uri parseResult(int resultCode, @Nullable Intent intent) {
                    return resultCode == RESULT_OK && intent != null ? intent.getData() : null;
                }
            },
            uri -> {
                if (uri != null) {
                    handleExportUri(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        profileStore = new StudentProfileStore(this);
        currentProfile = profileStore.loadProfile();

        FrostUiHelper.setupEdgeToEdgeAndBackdrop(
                this,
                binding.rootContainer,
                binding.mainScrollView,
                binding.ambientBackdropView,
                binding.dockBar
        );
        restoreState(savedInstanceState);
        bindHeaderDate();
        bindHeroCard();
        bindFocusTimer();
        bindChecklist();
        bindCalendarTab();
        bindToolsTab();
        bindProfileTab();
        bindDockNavigation();
        applySavedSelections();
        refreshHomeDashboard(false);
        ioExecutor.execute(() -> TaskReminderScheduler.rescheduleAllPendingReminders(getApplicationContext()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!taskReceiverRegistered) {
            ContextCompat.registerReceiver(
                    this,
                    taskUpdatedReceiver,
                    new IntentFilter(TaskReminderHelper.ACTION_TASK_UPDATED),
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
            taskReceiverRegistered = true;
        }
    }

    @Override
    protected void onStop() {
        if (taskReceiverRegistered) {
            unregisterReceiver(taskUpdatedReceiver);
            taskReceiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotesIntegration();
        selectScheduleDay(selectedDayIndex);
        refreshHomeDashboard(false);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
        outState.putInt(STATE_SELECTED_DAY, selectedDayIndex);
        outState.putInt(STATE_COMPRESS_PRESET, selectedCompressPreset);
        outState.putInt(STATE_OCR_SAMPLE, selectedOcrSample);
        outState.putLong(STATE_TIMER_MS, remainingTimerMs);
        outState.putString(STATE_EXPORT_FORMAT, pendingExportFormat.name());
        outState.putBoolean(STATE_EXPORT_TEMPLATE, pendingExportTemplate);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (focusTimer != null) {
            focusTimer.cancel();
            focusTimer = null;
        }
        ioExecutor.shutdown();
        super.onDestroy();
    }

    private static int getInitialDayIndex() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (day >= Calendar.MONDAY && day <= Calendar.SATURDAY) {
            return day - Calendar.MONDAY;
        }
        return 0;
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        selectedTab = savedInstanceState.getInt(STATE_SELECTED_TAB, 0);
        selectedDayIndex = savedInstanceState.getInt(STATE_SELECTED_DAY, getInitialDayIndex());
        if (selectedDayIndex < 0 || selectedDayIndex > 5) {
            selectedDayIndex = 0;
        }
        selectedCompressPreset = savedInstanceState.getInt(STATE_COMPRESS_PRESET, 0);
        selectedOcrSample = savedInstanceState.getInt(STATE_OCR_SAMPLE, 0);
        remainingTimerMs = savedInstanceState.getLong(STATE_TIMER_MS, FOCUS_DURATION_MS);
        pendingExportTemplate = savedInstanceState.getBoolean(STATE_EXPORT_TEMPLATE, false);
        String formatName = savedInstanceState.getString(STATE_EXPORT_FORMAT);
        if (formatName != null) {
            try {
                pendingExportFormat = ScheduleTransferHelper.TransferFormat.valueOf(formatName);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void bindHeaderDate() {
        Locale vi = Locale.forLanguageTag("vi-VN");
        String formatted = new SimpleDateFormat(getString(R.string.home_header_date_pattern), vi).format(new Date());
        binding.tvHeaderDate.setText(formatted.toUpperCase(vi));
        binding.btnHeaderProfile.setOnClickListener(v -> switchTab(3));
    }

    private void bindHeroCard() {
        binding.tabHome.btnSpotlightAction.setOnClickListener(v -> {
            if (currentSpotlight == null) {
                return;
            }
            if ((currentSpotlight.kind == HomeSummaryHelper.SpotlightKind.DEADLINE_TASK
                    || currentSpotlight.kind == HomeSummaryHelper.SpotlightKind.PENDING_TASK)
                    && currentSpotlight.task != null) {
                toggleTaskCompletion(currentSpotlight.task, true);
            } else if (currentSpotlight.kind == HomeSummaryHelper.SpotlightKind.SCHEDULE_SLOT
                    && currentSpotlight.slot != null) {
                int dayIdx = currentSpotlight.slot.getDayOfWeek() - ScheduleSlot.DAY_MIN;
                if (dayIdx >= 0 && dayIdx <= 5) {
                    selectScheduleDay(dayIdx);
                }
                switchTab(1);
            } else {
                openTaskEditor(null);
            }
        });
    }

    private void refreshHomeDashboard(boolean animate) {
        if (dbHelper == null) {
            return;
        }
        long nowMillis = System.currentTimeMillis();
        taskItems = dbHelper.getAllTasks();
        List<ScheduleSlot> allSchedules = dbHelper.getAllSchedules();

        renderChecklist(nowMillis);

        HomeSummaryHelper.DeadlineProgress deadlineProgress = HomeSummaryHelper.computeDeadlineProgress(taskItems);
        binding.tabHome.ringsView.setDeadlineProgress(deadlineProgress.fraction, animate);
        binding.tabHome.tvRingDeadlineValue.setText(
                getString(R.string.home_ring_percent_format, Math.round(deadlineProgress.fraction * 100f))
        );

        float classProgress = HomeSummaryHelper.computeClassProgress(allSchedules, nowMillis);
        binding.tabHome.ringsView.setClassProgress(classProgress, animate);
        binding.tabHome.tvRingClassValue.setText(
                getString(R.string.home_ring_percent_format, Math.round(classProgress * 100f))
        );

        currentSpotlight = HomeSummaryHelper.findBestSpotlight(taskItems, allSchedules, nowMillis);
        bindSpotlightCandidate(currentSpotlight, nowMillis);
    }

    private void bindSpotlightCandidate(@NonNull HomeSummaryHelper.SpotlightCandidate candidate, long nowMillis) {
        if (candidate.kind == HomeSummaryHelper.SpotlightKind.DEADLINE_TASK && candidate.task != null) {
            StudyTask task = candidate.task;
            int badgeRes;
            int badgeColorRes;
            if (candidate.urgency == HomeSummaryHelper.DeadlineUrgency.OVERDUE) {
                badgeRes = R.string.home_spotlight_overdue_badge;
                badgeColorRes = R.color.accent_danger;
            } else if (candidate.urgency == HomeSummaryHelper.DeadlineUrgency.SOON_HOURS) {
                badgeRes = R.string.home_spotlight_soon_badge;
                badgeColorRes = R.color.accent_sapphire;
            } else {
                badgeRes = R.string.home_spotlight_upcoming_badge;
                badgeColorRes = R.color.accent_sapphire;
            }

            binding.tabHome.tvSpotlightBadge.setText(badgeRes);
            binding.tabHome.tvSpotlightBadge.setTextColor(ContextCompat.getColor(this, badgeColorRes));

            String title = hasText(task.getSubject())
                    ? getString(R.string.home_spotlight_title_format, task.getTitle(), task.getSubject().trim())
                    : task.getTitle();
            binding.tabHome.tvSpotlightTitle.setText(title);

            String duePrefix = HomeSummaryHelper.formatDueDateTime(this, task.getDueAtMillis(), nowMillis);
            String remaining = HomeSummaryHelper.formatRelativeDelta(this, task.getDueAtMillis(), nowMillis);
            binding.tabHome.tvSpotlightSub.setText(
                    getString(R.string.home_spotlight_due_remaining_format, duePrefix, remaining)
            );
            binding.tabHome.btnSpotlightAction.setText(R.string.home_spotlight_btn_complete);
            binding.tabHome.btnSpotlightAction.setBackgroundResource(R.drawable.bg_pill_primary);
            binding.tabHome.btnSpotlightAction.setTextColor(ContextCompat.getColor(this, R.color.white));
            return;
        }

        if (candidate.kind == HomeSummaryHelper.SpotlightKind.SCHEDULE_SLOT && candidate.slot != null) {
            ScheduleSlot slot = candidate.slot;
            String room = hasText(slot.getRoom())
                    ? slot.getRoom().trim()
                    : getString(R.string.schedule_room_fallback);
            String lecturer = hasText(slot.getLecturer())
                    ? slot.getLecturer().trim()
                    : getString(R.string.schedule_lecturer_fallback);
            String timeRange = getString(R.string.schedule_time_range_format, slot.getStartTime(), slot.getEndTime());

            binding.tabHome.tvSpotlightBadge.setText(
                    candidate.happeningNow ? R.string.home_spotlight_active_badge : R.string.home_spotlight_next_badge
            );
            binding.tabHome.tvSpotlightBadge.setTextColor(ContextCompat.getColor(
                    this,
                    candidate.happeningNow ? R.color.accent_sapphire : R.color.accent_mint_text
            ));
            binding.tabHome.tvSpotlightTitle.setText(
                    getString(R.string.home_spotlight_title_format, slot.getSubject(), room)
            );
            if (candidate.today) {
                binding.tabHome.tvSpotlightSub.setText(
                        getString(R.string.home_spotlight_sub_today, timeRange, lecturer)
                );
            } else {
                binding.tabHome.tvSpotlightSub.setText(
                        getString(R.string.home_spotlight_sub_day, getDayShortName(slot.getDayOfWeek()), timeRange, lecturer)
                );
            }
            binding.tabHome.btnSpotlightAction.setText(R.string.home_spotlight_btn_view_schedule);
            binding.tabHome.btnSpotlightAction.setBackgroundResource(R.drawable.bg_pill_secondary);
            binding.tabHome.btnSpotlightAction.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
            return;
        }

        if (candidate.kind == HomeSummaryHelper.SpotlightKind.PENDING_TASK && candidate.task != null) {
            StudyTask task = candidate.task;
            binding.tabHome.tvSpotlightBadge.setText(R.string.home_spotlight_task_badge);
            binding.tabHome.tvSpotlightBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_sapphire));
            String title = hasText(task.getSubject())
                    ? getString(R.string.home_spotlight_title_format, task.getTitle(), task.getSubject().trim())
                    : task.getTitle();
            binding.tabHome.tvSpotlightTitle.setText(title);
            binding.tabHome.tvSpotlightSub.setText(R.string.home_spotlight_task_meta_default);
            binding.tabHome.btnSpotlightAction.setText(R.string.home_spotlight_btn_complete);
            binding.tabHome.btnSpotlightAction.setBackgroundResource(R.drawable.bg_pill_primary);
            binding.tabHome.btnSpotlightAction.setTextColor(ContextCompat.getColor(this, R.color.white));
            return;
        }

        binding.tabHome.tvSpotlightBadge.setText(R.string.home_spotlight_all_done_badge);
        binding.tabHome.tvSpotlightBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_mint_text));
        binding.tabHome.tvSpotlightTitle.setText(R.string.home_spotlight_all_done_title);
        binding.tabHome.tvSpotlightSub.setText(R.string.home_spotlight_all_done_sub);
        binding.tabHome.btnSpotlightAction.setText(R.string.home_spotlight_btn_add_task);
        binding.tabHome.btnSpotlightAction.setBackgroundResource(R.drawable.bg_pill_secondary);
        binding.tabHome.btnSpotlightAction.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
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
        binding.tabHome.btnTimerToggle.setText(R.string.home_timer_btn_pause);
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
                binding.tabHome.btnTimerToggle.setText(R.string.home_timer_btn_start);
                binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_primary);
                updateTimerUi(true);
                Toast.makeText(MainActivity.this, R.string.home_timer_finished_toast, Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void pauseTimer() {
        if (focusTimer != null) {
            focusTimer.cancel();
            focusTimer = null;
        }
        timerRunning = false;
        binding.tabHome.btnTimerToggle.setText(R.string.home_timer_btn_resume);
        binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_primary);
    }

    private void resetTimer() {
        if (focusTimer != null) {
            focusTimer.cancel();
            focusTimer = null;
        }
        timerRunning = false;
        remainingTimerMs = FOCUS_DURATION_MS;
        binding.tabHome.btnTimerToggle.setText(R.string.home_timer_btn_start);
        binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_primary);
        updateTimerUi(true);
    }

    private void updateTimerUi(boolean animate) {
        long totalSeconds = remainingTimerMs / 1000L;
        int minutes = (int) (totalSeconds / 60L);
        int seconds = (int) (totalSeconds % 60L);
        binding.tabHome.tvTimerDisplay.setText(getString(R.string.home_timer_clock_format, minutes, seconds));
        float elapsedFraction = 1f - ((float) remainingTimerMs / (float) FOCUS_DURATION_MS);
        float focusRingProgress = Math.max(0.40f, Math.min(1.0f, 0.40f + 0.60f * elapsedFraction));
        binding.tabHome.ringsView.setFocusProgress(focusRingProgress, animate);
        binding.tabHome.tvRingFocusValue.setText(
                getString(R.string.home_ring_percent_format, Math.round(focusRingProgress * 100f))
        );
    }

    private void bindChecklist() {
        binding.tabHome.btnHomeOpenNotes.setOnClickListener(v -> openNotesActivity());
        binding.tabHome.btnAddTask.setOnClickListener(v -> {
            String quickTitle = binding.tabHome.edtNewTask.getText().toString().trim();
            if (quickTitle.isEmpty()) {
                openTaskEditor(null);
            } else {
                addNewTaskFromInput();
            }
        });
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
        StudyTask task = StudyTask.builder()
                .title(text)
                .subject("")
                .tag(getString(R.string.task_default_tag))
                .dueAtMillis(0L)
                .done(false)
                .createdAtMillis(System.currentTimeMillis())
                .build();
        dbHelper.insertTask(task);
        binding.tabHome.edtNewTask.setText("");
        Toast.makeText(this, R.string.task_toast_created, Toast.LENGTH_SHORT).show();
        refreshHomeDashboard(true);
    }

    private void openTaskEditor(@Nullable StudyTask existingTask) {
        FrostUiHelper.showTaskEditorDialog(
                this,
                existingTask,
                savedTask -> {
                    if (savedTask.getId() > 0) {
                        dbHelper.updateTask(savedTask);
                        Toast.makeText(this, R.string.task_toast_updated, Toast.LENGTH_SHORT).show();
                    } else {
                        long insertedId = dbHelper.insertTask(savedTask);
                        if (insertedId > 0L) {
                            savedTask.setId((int) insertedId);
                        }
                        Toast.makeText(this, R.string.task_toast_created, Toast.LENGTH_SHORT).show();
                    }
                    TaskReminderScheduler.clearFiredMarker(this, savedTask.getId());
                    scheduleTaskReminderWithPermission(savedTask);
                    refreshHomeDashboard(true);
                },
                targetTask -> FrostUiHelper.showConfirmDialog(
                        this,
                        R.string.task_delete_confirm_title,
                        getString(R.string.task_delete_confirm_message, targetTask.getTitle()),
                        R.string.notes_dialog_delete_confirm,
                        true,
                        () -> {
                            dbHelper.deleteTask(targetTask.getId());
                            TaskReminderScheduler.cancelReminder(this, targetTask.getId());
                            TaskReminderScheduler.clearFiredMarker(this, targetTask.getId());
                            Toast.makeText(this, R.string.task_toast_deleted, Toast.LENGTH_SHORT).show();
                            refreshHomeDashboard(true);
                        }
                )
        );
    }

    private void toggleTaskCompletion(@NonNull StudyTask task, boolean nextDone) {
        dbHelper.setTaskDone(task.getId(), nextDone);
        task.setDone(nextDone);
        TaskReminderScheduler.clearFiredMarker(this, task.getId());
        if (nextDone) {
            TaskReminderScheduler.cancelReminder(this, task.getId());
        } else {
            scheduleTaskReminderWithPermission(task);
        }
        Toast.makeText(
                this,
                getString(nextDone ? R.string.task_toast_completed : R.string.task_toast_reopened, task.getTitle()),
                Toast.LENGTH_SHORT
        ).show();
        refreshHomeDashboard(true);
    }

    private void scheduleTaskReminderWithPermission(@NonNull StudyTask task) {
        if (!TaskReminderHelper.isReminderConfigured(task)) {
            TaskReminderScheduler.cancelReminder(this, task.getId());
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingReminderTask = task;
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        TaskReminderScheduler.scheduleOrCancel(this, task);
    }

    private void renderChecklist(long nowMillis) {
        LinearLayout container = binding.tabHome.checklistContainer;
        container.removeAllViews();

        if (taskItems.isEmpty()) {
            binding.tabHome.tvChecklistEmpty.setVisibility(View.VISIBLE);
            binding.tabHome.checklistContainer.setVisibility(View.GONE);
            binding.tabHome.tvChecklistCount.setText(
                    getResources().getQuantityString(R.plurals.home_checklist_count_format, 0, 0, 0)
            );
            return;
        }

        binding.tabHome.tvChecklistEmpty.setVisibility(View.GONE);
        binding.tabHome.checklistContainer.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        int remainingCount = 0;

        for (StudyTask item : taskItems) {
            if (!item.isDone()) {
                remainingCount++;
            }
            View row = inflater.inflate(R.layout.item_checklist_task, container, false);
            View checkArea = row.findViewById(R.id.btnTaskCheckArea);
            View checkBox = row.findViewById(R.id.taskCheckBox);
            ImageView ivCheck = row.findViewById(R.id.ivTaskCheck);
            TextView tvTitle = row.findViewById(R.id.tvTaskTitle);
            TextView tvMeta = row.findViewById(R.id.tvTaskMeta);
            TextView tvTag = row.findViewById(R.id.tvTaskTag);

            tvTitle.setText(item.getTitle());
            tvMeta.setText(HomeSummaryHelper.buildTaskMeta(this, item, nowMillis));
            tvTag.setText(hasText(item.getTag()) ? item.getTag() : getString(R.string.task_default_tag));

            boolean overdue = !item.isDone() && item.hasDueAt() && item.getDueAtMillis() < nowMillis;
            applyTaskRowStyle(checkBox, ivCheck, tvTitle, tvMeta, tvTag, item.isDone(), overdue);

            checkArea.setOnClickListener(v -> toggleTaskCompletion(item, !item.isDone()));
            row.setOnClickListener(v -> openTaskEditor(item));
            container.addView(row);
        }

        binding.tabHome.tvChecklistCount.setText(
                getResources().getQuantityString(
                        R.plurals.home_checklist_count_format,
                        taskItems.size(),
                        remainingCount,
                        taskItems.size()
                )
        );
    }

    private void applyTaskRowStyle(
            View checkBox,
            ImageView ivCheck,
            TextView tvTitle,
            TextView tvMeta,
            TextView tvTag,
            boolean done,
            boolean overdue
    ) {
        if (done) {
            checkBox.setBackgroundResource(R.drawable.bg_check_checked);
            ivCheck.setVisibility(View.VISIBLE);
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.ink_muted));
            tvMeta.setTextColor(ContextCompat.getColor(this, R.color.ink_muted));
            tvTag.setBackgroundResource(R.drawable.bg_pill_success_soft);
            tvTag.setTextColor(ContextCompat.getColor(this, R.color.accent_mint_text));
        } else {
            checkBox.setBackgroundResource(R.drawable.bg_check_unchecked);
            ivCheck.setVisibility(View.GONE);
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
            tvMeta.setTextColor(ContextCompat.getColor(this, overdue ? R.color.accent_danger : R.color.ink_secondary));
            if (overdue) {
                tvTag.setBackgroundResource(R.drawable.bg_pill_danger_soft);
                tvTag.setTextColor(ContextCompat.getColor(this, R.color.accent_danger));
            } else {
                tvTag.setBackgroundResource(R.drawable.bg_pill_neutral_soft);
                tvTag.setTextColor(ContextCompat.getColor(this, R.color.ink_secondary));
            }
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

        binding.tabTools.tvToolNotesCount.setText(
                getResources().getQuantityString(R.plurals.tools_notes_count_format, all.size(), all.size())
        );
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

        binding.tabCalendar.btnScheduleAdd.setOnClickListener(v -> openScheduleEditor(null));
        binding.tabCalendar.btnScheduleEmptyAdd.setOnClickListener(v -> openScheduleEditor(null));
        binding.tabCalendar.btnScheduleImport.setOnClickListener(v -> launchImportPicker());
        binding.tabCalendar.btnScheduleExport.setOnClickListener(v -> openExportDialog());
    }

    private void selectScheduleDay(int dayIndex) {
        if (dayIndex < 0 || dayIndex > 5) {
            dayIndex = 0;
        }
        selectedDayIndex = dayIndex;
        int dayOfWeek = dayIndex + ScheduleSlot.DAY_MIN;

        TextView[] chips = new TextView[]{
                binding.tabCalendar.chipDayT2,
                binding.tabCalendar.chipDayT3,
                binding.tabCalendar.chipDayT4,
                binding.tabCalendar.chipDayT5,
                binding.tabCalendar.chipDayT6,
                binding.tabCalendar.chipDayT7
        };

        for (int i = 0; i < chips.length; i++) {
            boolean active = i == dayIndex;
            chips[i].setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chips[i].setTextColor(ContextCompat.getColor(this, active ? R.color.white : R.color.ink_primary));
        }

        binding.tabCalendar.tvCalendarDayTitle.setText(getDayFullTitleRes(dayOfWeek));
        List<ScheduleSlot> slots = dbHelper.getSchedulesByDay(dayOfWeek);
        binding.tabCalendar.tvCalendarCountBadge.setText(
                getString(R.string.schedule_count_badge_format, slots.size())
        );

        LinearLayout container = binding.tabCalendar.scheduleContainer;
        container.removeAllViews();

        if (slots.isEmpty()) {
            binding.tabCalendar.cardCalendarEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.tabCalendar.cardCalendarEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ScheduleSlot slot : slots) {
            View card = inflater.inflate(R.layout.item_schedule_slot, container, false);
            TextView tvTime = card.findViewById(R.id.tvSlotTime);
            TextView tvRoom = card.findViewById(R.id.tvSlotRoom);
            TextView tvSubject = card.findViewById(R.id.tvSlotSubject);
            TextView tvLecturer = card.findViewById(R.id.tvSlotLecturer);
            TextView tvNote = card.findViewById(R.id.tvSlotNote);

            tvTime.setText(getString(R.string.schedule_time_range_format, slot.getStartTime(), slot.getEndTime()));
            tvRoom.setText(hasText(slot.getRoom()) ? slot.getRoom() : getString(R.string.schedule_room_fallback));
            tvSubject.setText(slot.getSubject());
            tvLecturer.setText(hasText(slot.getLecturer()) ? slot.getLecturer() : getString(R.string.schedule_lecturer_fallback));

            if (hasText(slot.getNote())) {
                tvNote.setVisibility(View.VISIBLE);
                tvNote.setText(slot.getNote());
            } else {
                tvNote.setVisibility(View.GONE);
            }

            card.setOnClickListener(v -> openScheduleEditor(slot));
            card.setOnLongClickListener(v -> {
                confirmDeleteSchedule(slot);
                return true;
            });
            container.addView(card);
        }
    }

    private void openScheduleEditor(@Nullable ScheduleSlot existingSlot) {
        int currentDay = selectedDayIndex + ScheduleSlot.DAY_MIN;
        FrostUiHelper.showScheduleEditorDialog(
                this,
                existingSlot,
                currentDay,
                savedSlot -> {
                    if (existingSlot == null) {
                        dbHelper.insertSchedule(savedSlot);
                        Toast.makeText(
                                this,
                                getString(R.string.schedule_toast_created, getDayShortName(savedSlot.getDayOfWeek())),
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        dbHelper.updateSchedule(savedSlot);
                        Toast.makeText(this, R.string.schedule_toast_updated, Toast.LENGTH_SHORT).show();
                    }
                    selectedDayIndex = Math.max(0, Math.min(5, savedSlot.getDayOfWeek() - ScheduleSlot.DAY_MIN));
                    selectScheduleDay(selectedDayIndex);
                    refreshHomeDashboard(true);
                },
                this::confirmDeleteSchedule
        );
    }

    private void confirmDeleteSchedule(@NonNull ScheduleSlot slot) {
        String timeRange = getString(R.string.schedule_time_range_format, slot.getStartTime(), slot.getEndTime());
        FrostUiHelper.showConfirmDialog(
                this,
                R.string.schedule_delete_title,
                getString(R.string.schedule_delete_message, slot.getSubject(), timeRange),
                R.string.notes_dialog_delete_confirm,
                true,
                () -> {
                    dbHelper.deleteSchedule(slot.getId());
                    Toast.makeText(this, R.string.schedule_toast_deleted, Toast.LENGTH_SHORT).show();
                    selectScheduleDay(selectedDayIndex);
                    refreshHomeDashboard(true);
                }
        );
    }

    private void launchImportPicker() {
        importScheduleLauncher.launch(new String[]{"*/*"});
    }

    private void handleImportUri(@NonNull Uri uri) {
        String fileName = queryFileDisplayName(uri);
        Toast.makeText(this, R.string.schedule_import_parsing, Toast.LENGTH_SHORT).show();
        ioExecutor.execute(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) {
                    throw new IOException("Cannot open input stream");
                }
                ScheduleTransferHelper.ImportResult result = ScheduleTransferHelper.parseSchedules(is, fileName);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (result.validSlots.isEmpty()) {
                        Toast.makeText(this, R.string.schedule_import_empty_error, Toast.LENGTH_LONG).show();
                        return;
                    }
                    FrostUiHelper.showImportPreviewDialog(this, fileName, result, replaceExisting -> {
                        DatabaseHelper.ImportSummary summary = dbHelper.importSchedules(result.validSlots, replaceExisting);
                        if (replaceExisting) {
                            Toast.makeText(
                                    this,
                                    getString(R.string.schedule_import_replaced_toast, summary.insertedCount),
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else if (summary.skippedDuplicates > 0) {
                            Toast.makeText(
                                    this,
                                    getString(
                                            R.string.schedule_import_merged_skip_toast,
                                            summary.insertedCount,
                                            summary.skippedDuplicates
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                        } else {
                            Toast.makeText(
                                    this,
                                    getString(R.string.schedule_import_merged_toast, summary.insertedCount),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        if (!result.validSlots.isEmpty()) {
                            int targetDay = result.validSlots.get(0).getDayOfWeek();
                            selectedDayIndex = Math.max(0, Math.min(5, targetDay - ScheduleSlot.DAY_MIN));
                        }
                        selectScheduleDay(selectedDayIndex);
                        refreshHomeDashboard(true);
                    });
                });
            } catch (ScheduleTransferHelper.LegacyXlsException e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, R.string.schedule_import_legacy_xls_error, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, R.string.schedule_import_read_error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void openExportDialog() {
        int totalSlots = dbHelper.getTotalScheduleCount();
        FrostUiHelper.showExportOptionsDialog(this, totalSlots, new FrostUiHelper.OnExportActionListener() {
            @Override
            public void onSaveSchedules(@NonNull ScheduleTransferHelper.TransferFormat format) {
                pendingExportFormat = format;
                pendingExportTemplate = false;
                exportScheduleLauncher.launch(new ExportDocumentSpec(buildExportFileName(format, false), format));
            }

            @Override
            public void onShareSchedules(@NonNull ScheduleTransferHelper.TransferFormat format) {
                shareScheduleFile(format);
            }

            @Override
            public void onExportTemplate(@NonNull ScheduleTransferHelper.TransferFormat format) {
                pendingExportFormat = format;
                pendingExportTemplate = true;
                exportScheduleLauncher.launch(new ExportDocumentSpec(buildExportFileName(format, true), format));
            }
        });
    }

    private void handleExportUri(@NonNull Uri uri) {
        final ScheduleTransferHelper.TransferFormat format = pendingExportFormat;
        final boolean isTemplate = pendingExportTemplate;
        final List<ScheduleSlot> exportSlots = isTemplate
                ? ScheduleTransferHelper.buildSampleSlotsFromSeedRows(getResources().getStringArray(R.array.schedule_seed_rows), 3)
                : dbHelper.getAllSchedules();

        ioExecutor.execute(() -> {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os == null) {
                    throw new IOException("Cannot open output stream");
                }
                ScheduleTransferHelper.exportSchedules(exportSlots, format, os);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(
                                this,
                                isTemplate ? R.string.schedule_export_template_saved_toast : R.string.schedule_export_saved_toast,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, R.string.schedule_export_failed_toast, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void shareScheduleFile(@NonNull ScheduleTransferHelper.TransferFormat format) {
        final List<ScheduleSlot> slots = dbHelper.getAllSchedules();
        final String fileName = buildExportFileName(format, false);

        ioExecutor.execute(() -> {
            try {
                File exportDir = new File(getCacheDir(), "exports");
                if (!exportDir.exists() && !exportDir.mkdirs()) {
                    throw new IOException("Cannot create cache export directory");
                }
                File outFile = new File(exportDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    ScheduleTransferHelper.exportSchedules(slots, format, fos);
                }
                Uri contentUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        outFile
                );
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType(format.mimeType);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.schedule_export_share_text));
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.schedule_export_share_chooser)));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, R.string.schedule_export_no_share_app, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, R.string.schedule_export_failed_toast, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private String buildExportFileName(@NonNull ScheduleTransferHelper.TransferFormat format, boolean template) {
        if (template) {
            return "mau-thoi-khoa-bieu." + format.extension;
        }
        String stamp = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        return "thoi-khoa-bieu-" + stamp + "." + format.extension;
    }

    private String queryFileDisplayName(@NonNull Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String name = cursor.getString(idx);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return getString(R.string.schedule_import_file_unknown);
    }

    @StringRes
    private int getDayFullTitleRes(int dayOfWeek) {
        switch (dayOfWeek) {
            case 2:
                return R.string.schedule_day_full_2;
            case 3:
                return R.string.schedule_day_full_3;
            case 4:
                return R.string.schedule_day_full_4;
            case 5:
                return R.string.schedule_day_full_5;
            case 6:
                return R.string.schedule_day_full_6;
            case 7:
            default:
                return R.string.schedule_day_full_7;
        }
    }

    private String getDayShortName(int dayOfWeek) {
        switch (dayOfWeek) {
            case 2:
                return getString(R.string.schedule_day_name_2);
            case 3:
                return getString(R.string.schedule_day_name_3);
            case 4:
                return getString(R.string.schedule_day_name_4);
            case 5:
                return getString(R.string.schedule_day_name_5);
            case 6:
                return getString(R.string.schedule_day_name_6);
            case 7:
            default:
                return getString(R.string.schedule_day_name_7);
        }
    }

    private boolean hasText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void bindToolsTab() {
        binding.tabTools.chipCompress100.setOnClickListener(v -> selectCompressPreset(0));
        binding.tabTools.chipCompress500.setOnClickListener(v -> selectCompressPreset(1));
        binding.tabTools.chipCompress3x4.setOnClickListener(v -> selectCompressPreset(2));
        binding.tabTools.btnRunCompress.setOnClickListener(v -> {
            int presetRes = selectedCompressPreset == 0
                    ? R.string.tools_compress_preset_name_100
                    : (selectedCompressPreset == 1
                    ? R.string.tools_compress_preset_name_500
                    : R.string.tools_compress_preset_name_3x4);
            Toast.makeText(
                    this,
                    getString(R.string.tools_compress_ready_toast, getString(presetRes)),
                    Toast.LENGTH_SHORT
            ).show();
        });

        binding.tabTools.chipOcrSample1.setOnClickListener(v -> selectOcrSample(0));
        binding.tabTools.chipOcrSample2.setOnClickListener(v -> selectOcrSample(1));
        binding.tabTools.btnCopyOcr.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(
                        getString(R.string.tools_ocr_clip_label),
                        binding.tabTools.tvOcrOutput.getText()
                );
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.tools_ocr_copied_toast, Toast.LENGTH_SHORT).show();
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
            binding.tabTools.tvCompressSpec.setText(R.string.tools_compress_spec_100);
            binding.tabTools.tvCompressEstimate.setText(R.string.tools_compress_estimate_100);
        } else if (preset == 1) {
            binding.tabTools.tvCompressSpec.setText(R.string.tools_compress_spec_500);
            binding.tabTools.tvCompressEstimate.setText(R.string.tools_compress_estimate_500);
        } else {
            binding.tabTools.tvCompressSpec.setText(R.string.tools_compress_spec_3x4);
            binding.tabTools.tvCompressEstimate.setText(R.string.tools_compress_estimate_3x4);
        }
    }

    private void selectOcrSample(int sample) {
        selectedOcrSample = sample;
        boolean s1 = sample == 0;
        binding.tabTools.chipOcrSample1.setBackgroundResource(s1 ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        binding.tabTools.chipOcrSample1.setTextColor(ContextCompat.getColor(this, s1 ? R.color.white : R.color.ink_primary));
        binding.tabTools.chipOcrSample2.setBackgroundResource(!s1 ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        binding.tabTools.chipOcrSample2.setTextColor(ContextCompat.getColor(this, !s1 ? R.color.white : R.color.ink_primary));

        binding.tabTools.tvOcrOutput.setText(
                s1 ? R.string.tools_ocr_sample_slide : R.string.tools_ocr_sample_meeting
        );
    }

    private void bindProfileTab() {
        renderStudentProfile();
        int savedProgress = profileStore.loadSimulatedProgress();
        binding.tabProfile.seekSimulatedScore.setProgress(savedProgress);
        updateGpaSimulation(savedProgress);

        binding.tabProfile.seekSimulatedScore.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateGpaSimulation(progress);
                if (fromUser) {
                    profileStore.saveSimulatedProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                profileStore.saveSimulatedProgress(seekBar.getProgress());
            }
        });

        binding.tabProfile.btnEditProfile.setOnClickListener(v ->
                FrostUiHelper.showProfileEditorDialog(this, currentProfile, updatedProfile -> {
                    profileStore.saveProfile(updatedProfile);
                    currentProfile = updatedProfile;
                    renderStudentProfile();
                    updateGpaSimulation(binding.tabProfile.seekSimulatedScore.getProgress());
                    Toast.makeText(this, R.string.profile_toast_updated, Toast.LENGTH_SHORT).show();
                })
        );
        binding.tabProfile.btnProfileNotes.setOnClickListener(v -> openNotesActivity());
    }

    private void renderStudentProfile() {
        String initials = ProfileGpaHelper.buildInitials(
                currentProfile.getFullName(),
                getString(R.string.profile_default_initials)
        );
        binding.btnHeaderProfile.setText(initials);
        binding.tabProfile.tvProfileAvatar.setText(initials);
        binding.tabProfile.tvProfileName.setText(currentProfile.getFullName());

        String major = currentProfile.getMajorAndCohort();
        binding.tabProfile.tvProfileMajor.setText(
                major == null || major.trim().isEmpty()
                        ? getString(R.string.profile_major_fallback)
                        : major.trim()
        );
        binding.tabProfile.tvProfileCreditsValue.setText(
                getString(
                        R.string.profile_credits_value_format,
                        currentProfile.getEarnedCredits(),
                        currentProfile.getTotalCredits()
                )
        );
        binding.tabProfile.tvProfileAttendanceValue.setText(
                getString(R.string.profile_attendance_value_format, currentProfile.getAttendancePercent())
        );
        binding.tabProfile.tvGpaBaselineSummary.setText(
                getString(
                        R.string.profile_gpa_baseline_summary_format,
                        currentProfile.getBaselineGpa(),
                        currentProfile.getCurrentTermCredits()
                )
        );
    }

    private void updateGpaSimulation(int progressValue) {
        int clampedProgress = ProfileGpaHelper.clampSimulatedProgress(progressValue);
        float scoreTen = ProfileGpaHelper.progressToScoreTen(clampedProgress);
        float projectedGpa = ProfileGpaHelper.calculateProjectedGpa(
                currentProfile.getBaselineGpa(),
                currentProfile.getEarnedCredits(),
                currentProfile.getCurrentTermCredits(),
                scoreTen
        );

        binding.tabProfile.tvSimulatedScore.setText(
                getString(R.string.profile_gpa_sim_score_format, scoreTen)
        );
        binding.tabProfile.tvGpaProjected.setText(
                getString(R.string.profile_gpa_value_format, projectedGpa)
        );
        binding.tabProfile.pbGpaMeter.setProgress(Math.round(projectedGpa * 100f));

        ProfileGpaHelper.GpaRank rank = ProfileGpaHelper.classifyGpaRank(projectedGpa);
        switch (rank) {
            case EXCELLENT:
                binding.tabProfile.tvGpaRankBadge.setText(R.string.profile_gpa_rank_excellent);
                binding.tabProfile.tvGpaRankBadge.setBackgroundResource(R.drawable.bg_pill_success_soft);
                binding.tabProfile.tvGpaRankBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_mint_text));
                break;
            case GOOD:
                binding.tabProfile.tvGpaRankBadge.setText(R.string.profile_gpa_rank_good);
                binding.tabProfile.tvGpaRankBadge.setBackgroundResource(R.drawable.bg_pill_accent_soft);
                binding.tabProfile.tvGpaRankBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_sapphire));
                break;
            case FAIR:
                binding.tabProfile.tvGpaRankBadge.setText(R.string.profile_gpa_rank_fair);
                binding.tabProfile.tvGpaRankBadge.setBackgroundResource(R.drawable.bg_pill_neutral_soft);
                binding.tabProfile.tvGpaRankBadge.setTextColor(ContextCompat.getColor(this, R.color.ink_secondary));
                break;
            case AVERAGE:
            default:
                binding.tabProfile.tvGpaRankBadge.setText(R.string.profile_gpa_rank_average);
                binding.tabProfile.tvGpaRankBadge.setBackgroundResource(R.drawable.bg_pill_danger_soft);
                binding.tabProfile.tvGpaRankBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_danger));
                break;
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
                getString(R.string.tab_header_home),
                getString(R.string.tab_header_calendar),
                getString(R.string.tab_header_tools),
                getString(R.string.tab_header_profile)
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
