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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketkit.databinding.ActivityMainBinding;
import com.example.pocketkit.home.HomeSummaryHelper;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.profile.ProfileGpaHelper;
import com.example.pocketkit.reminder.TaskReminderHelper;
import com.example.pocketkit.tools.ImageCompressGateway;
import com.example.pocketkit.tools.ImageCompressHelper;
import com.example.pocketkit.transfer.ScheduleTransferHelper;
import com.example.pocketkit.ui.FrostUiHelper;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;
import com.example.pocketkit.viewmodel.CalendarViewModel;
import com.example.pocketkit.viewmodel.HomeViewModel;
import com.example.pocketkit.viewmodel.PocketKitViewModelFactory;
import com.example.pocketkit.viewmodel.ProfileViewModel;
import com.example.pocketkit.viewmodel.ToolsViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "state_selected_tab";

    private ActivityMainBinding binding;
    private HomeViewModel homeViewModel;
    private CalendarViewModel calendarViewModel;
    private ToolsViewModel toolsViewModel;
    private ProfileViewModel profileViewModel;

    private int selectedTab = 0;
    private StudyTask pendingReminderTask;
    private boolean taskReceiverRegistered = false;
    private boolean calendarCloudBusy = false;
    @Nullable
    private String lastCompressedPreviewKey;
    @Nullable
    private FrostUiHelper.OnlineShareDialogHandle onlineShareDialog;

    private final BroadcastReceiver taskUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            homeViewModel.loadDashboard(false);
        }
    };

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                StudyTask pending = pendingReminderTask;
                pendingReminderTask = null;
                homeViewModel.onNotificationPermissionResult(granted, pending);
            }
    );

    private final ActivityResultLauncher<String[]> importScheduleLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    calendarViewModel.parseImportFile(uri.toString(), queryFileDisplayName(uri));
                }
            }
    );

    private final ActivityResultLauncher<PickVisualMediaRequest> pickOcrImageLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    toolsViewModel.scanOcrImage(uri.toString());
                }
            }
    );

    private final ActivityResultLauncher<PickVisualMediaRequest> pickScheduleImageLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    calendarViewModel.scanScheduleImage(uri.toString());
                }
            }
    );

    private final ActivityResultLauncher<PickVisualMediaRequest> pickCompressImageLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    toolsViewModel.compressImage(uri.toString());
                }
            }
    );

    private final ActivityResultLauncher<String> saveCompressedDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("image/jpeg"),
            uri -> {
                if (uri != null) {
                    toolsViewModel.saveCompressedToDocument(uri.toString());
                }
            }
    );

    private final ActivityResultLauncher<CalendarViewModel.ExportDocumentRequest> exportScheduleLauncher =
            registerForActivityResult(
                    new ActivityResultContract<CalendarViewModel.ExportDocumentRequest, Uri>() {
                        @NonNull
                        @Override
                        public Intent createIntent(
                                @NonNull Context context,
                                CalendarViewModel.ExportDocumentRequest input
                        ) {
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
                            calendarViewModel.handleExportUri(uri.toString());
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            selectedTab = Math.max(0, Math.min(3, savedInstanceState.getInt(STATE_SELECTED_TAB, 0)));
        }

        PocketKitViewModelFactory factory = ((PocketKitApplication) getApplication()).getViewModelFactory();
        ViewModelProvider provider = new ViewModelProvider(this, factory);
        homeViewModel = provider.get(HomeViewModel.class);
        calendarViewModel = provider.get(CalendarViewModel.class);
        toolsViewModel = provider.get(ToolsViewModel.class);
        profileViewModel = provider.get(ProfileViewModel.class);

        FrostUiHelper.setupEdgeToEdgeAndBackdrop(
                this,
                binding.rootContainer,
                binding.mainScrollView,
                binding.ambientBackdropView,
                binding.dockBar
        );

        bindHeaderDate();
        bindHeroCard();
        bindFocusTimer();
        bindChecklist();
        bindCalendarTab();
        bindToolsTab();
        bindProfileTab();
        bindDockNavigation();
        observeViewModels();
        switchTab(selectedTab);

        if (savedInstanceState == null) {
            homeViewModel.reschedulePendingRemindersOnStart();
        }
        homeViewModel.loadDashboard(false);
        calendarViewModel.refreshCurrentDay();
        toolsViewModel.refreshNotesCount();
        profileViewModel.loadProfile();
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
        homeViewModel.loadDashboard(false);
        calendarViewModel.refreshCurrentDay();
        toolsViewModel.refreshNotesCount();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (onlineShareDialog != null) {
            onlineShareDialog.dialog.dismiss();
            onlineShareDialog = null;
        }
        super.onDestroy();
    }

    private void observeViewModels() {
        observeToast(homeViewModel.getToastEvent());
        observeToast(calendarViewModel.getToastEvent());
        observeToast(toolsViewModel.getToastEvent());
        observeToast(profileViewModel.getToastEvent());

        homeViewModel.getDashboardState().observe(this, this::renderHomeDashboard);
        homeViewModel.getTimerState().observe(this, this::renderTimerState);
        homeViewModel.getNotificationPermissionEvent().observe(this, event -> {
            StudyTask task = event.getContentIfNotHandled();
            if (task != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pendingReminderTask = task;
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        calendarViewModel.getCalendarState().observe(this, this::renderCalendarState);
        calendarViewModel.getImportPreviewEvent().observe(this, event -> {
            CalendarViewModel.ImportPreviewEventData data = event.getContentIfNotHandled();
            if (data != null) {
                FrostUiHelper.showImportPreviewDialog(
                        this,
                        data.fileName,
                        data.result,
                        replaceExisting -> calendarViewModel.confirmImport(data.result, replaceExisting)
                );
            }
        });
        calendarViewModel.getOpenExportDialogEvent().observe(this, event -> {
            Integer totalSlots = event.getContentIfNotHandled();
            if (totalSlots != null) {
                showExportDialog(totalSlots);
            }
        });
        calendarViewModel.getLaunchExportDocumentEvent().observe(this, event -> {
            CalendarViewModel.ExportDocumentRequest request = event.getContentIfNotHandled();
            if (request != null) {
                exportScheduleLauncher.launch(request);
            }
        });
        calendarViewModel.getShareFileEvent().observe(this, event -> {
            CalendarViewModel.ShareFileEventData data = event.getContentIfNotHandled();
            if (data != null) {
                launchShareChooser(data);
            }
        });
        calendarViewModel.getScheduleMutatedEvent().observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                homeViewModel.loadDashboard(true);
            }
        });
        calendarViewModel.getCloudBusy().observe(this, this::renderCalendarCloudBusy);
        calendarViewModel.getMyShares().observe(this, this::renderOnlineShareList);
        calendarViewModel.getShareCreatedEvent().observe(this, event -> {
            ApiModels.CreateShareResponse created = event.getContentIfNotHandled();
            if (created != null && onlineShareDialog != null && onlineShareDialog.dialog.isShowing()) {
                onlineShareDialog.layoutCreatedShare.setVisibility(View.VISIBLE);
                onlineShareDialog.tvCreatedCode.setText(created.getCode());
                onlineShareDialog.edtTitle.setText("");
                onlineShareDialog.btnCopyCreatedCode.setOnClickListener(v -> copyShareCode(created.getCode()));
            }
        });

        toolsViewModel.getCompressPreset().observe(this, this::renderCompressPreset);
        toolsViewModel.getCompressState().observe(this, this::renderCompressState);
        toolsViewModel.getLaunchSaveCompressedDocumentEvent().observe(this, event -> {
            String fileName = event.getContentIfNotHandled();
            if (fileName != null) {
                saveCompressedDocumentLauncher.launch(fileName);
            }
        });
        toolsViewModel.getShareCompressedEvent().observe(this, event -> {
            File file = event.getContentIfNotHandled();
            if (file != null) {
                launchShareCompressedImage(file);
            }
        });
        toolsViewModel.getOcrSample().observe(this, this::renderOcrSample);
        toolsViewModel.getOcrState().observe(this, this::renderOcrState);
        toolsViewModel.getTotalNotesCount().observe(this, count ->
                binding.tabTools.tvToolNotesCount.setText(
                        getResources().getQuantityString(R.plurals.tools_notes_count_format, count, count)
                )
        );

        profileViewModel.getProfileState().observe(this, this::renderProfileState);
        profileViewModel.getCloudState().observe(this, this::renderCloudState);
        profileViewModel.getDataRestoredEvent().observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                homeViewModel.loadDashboard(true);
                calendarViewModel.refreshCurrentDay();
                toolsViewModel.refreshNotesCount();
            }
        });
    }

    private void observeToast(@NonNull LiveData<UiEvent<UiMessage>> liveData) {
        liveData.observe(this, event -> FrostUiHelper.showToast(this, event));
    }

    private void bindHeaderDate() {
        Locale vi = Locale.forLanguageTag("vi-VN");
        String formatted = new SimpleDateFormat(getString(R.string.home_header_date_pattern), vi).format(new Date());
        binding.tvHeaderDate.setText(formatted.toUpperCase(vi));
        binding.btnHeaderProfile.setOnClickListener(v -> switchTab(3));
    }

    private void bindHeroCard() {
        binding.tabHome.btnSpotlightAction.setOnClickListener(v -> {
            HomeViewModel.HomeDashboardUiState state = homeViewModel.getDashboardState().getValue();
            if (state == null) {
                return;
            }
            HomeSummaryHelper.SpotlightCandidate spotlight = state.spotlightCandidate;
            if ((spotlight.kind == HomeSummaryHelper.SpotlightKind.DEADLINE_TASK
                    || spotlight.kind == HomeSummaryHelper.SpotlightKind.PENDING_TASK)
                    && spotlight.task != null) {
                homeViewModel.toggleTaskCompletion(spotlight.task, true, hasNotificationPermission());
            } else if (spotlight.kind == HomeSummaryHelper.SpotlightKind.SCHEDULE_SLOT
                    && spotlight.slot != null) {
                int dayIdx = spotlight.slot.getDayOfWeek() - ScheduleSlot.DAY_MIN;
                if (dayIdx >= 0 && dayIdx <= 5) {
                    calendarViewModel.selectDay(dayIdx);
                }
                switchTab(1);
            } else {
                openTaskEditor(null);
            }
        });
    }

    private void renderHomeDashboard(@NonNull HomeViewModel.HomeDashboardUiState state) {
        renderChecklist(state.tasks, state.snapshotNowMillis);

        binding.tabHome.ringsView.setDeadlineProgress(state.deadlineProgress.fraction, state.animateProgress);
        binding.tabHome.tvRingDeadlineValue.setText(
                getString(R.string.home_ring_percent_format, Math.round(state.deadlineProgress.fraction * 100f))
        );

        binding.tabHome.ringsView.setClassProgress(state.classProgress, state.animateProgress);
        binding.tabHome.tvRingClassValue.setText(
                getString(R.string.home_ring_percent_format, Math.round(state.classProgress * 100f))
        );

        bindSpotlightCandidate(state.spotlightCandidate, state.snapshotNowMillis);
        renderPinnedHomeNote(state.pinnedNote);
    }

    private void renderPinnedHomeNote(@Nullable Note pinnedNote) {
        if (pinnedNote != null) {
            binding.tabHome.layoutPinnedNoteBar.setVisibility(View.VISIBLE);
            String subject = hasText(pinnedNote.getSubject())
                    ? pinnedNote.getSubject().trim()
                    : getString(R.string.note_default_category);
            binding.tabHome.tvPinnedNoteSummary.setText(
                    getString(R.string.notes_summary_pinned_bar, subject, pinnedNote.getTitle())
            );
        } else {
            binding.tabHome.layoutPinnedNoteBar.setVisibility(View.GONE);
        }
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
                String shortDay = getString(CalendarViewModel.getDayShortNameRes(slot.getDayOfWeek()));
                binding.tabHome.tvSpotlightSub.setText(
                        getString(R.string.home_spotlight_sub_day, shortDay, timeRange, lecturer)
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
        binding.tabHome.btnTimerToggle.setOnClickListener(v -> homeViewModel.toggleFocusTimer());
        binding.tabHome.btnTimerReset.setOnClickListener(v -> homeViewModel.resetFocusTimer());
    }

    private void renderTimerState(@NonNull HomeViewModel.TimerUiState timer) {
        long totalSeconds = timer.remainingMs / 1000L;
        int minutes = (int) (totalSeconds / 60L);
        int seconds = (int) (totalSeconds % 60L);
        binding.tabHome.tvTimerDisplay.setText(getString(R.string.home_timer_clock_format, minutes, seconds));

        if (timer.running) {
            binding.tabHome.btnTimerToggle.setText(R.string.home_timer_btn_pause);
            binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_dark);
        } else if (timer.started && timer.remainingMs > 0L) {
            binding.tabHome.btnTimerToggle.setText(R.string.home_timer_btn_resume);
            binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_primary);
        } else {
            binding.tabHome.btnTimerToggle.setText(R.string.home_timer_btn_start);
            binding.tabHome.btnTimerToggle.setBackgroundResource(R.drawable.bg_pill_primary);
        }

        binding.tabHome.ringsView.setFocusProgress(timer.focusProgress, timer.animateRing);
        binding.tabHome.tvRingFocusValue.setText(
                getString(R.string.home_ring_percent_format, Math.round(timer.focusProgress * 100f))
        );
    }

    private void bindChecklist() {
        binding.tabHome.btnHomeOpenNotes.setOnClickListener(v -> openNotesActivity());
        binding.tabHome.btnAddTask.setOnClickListener(v -> {
            String quickTitle = binding.tabHome.edtNewTask.getText().toString().trim();
            if (quickTitle.isEmpty()) {
                openTaskEditor(null);
            } else {
                submitQuickTask();
            }
        });
        binding.tabHome.edtNewTask.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitQuickTask();
                return true;
            }
            return false;
        });
        binding.tabHome.layoutPinnedNoteBar.setOnClickListener(v -> {
            HomeViewModel.HomeDashboardUiState state = homeViewModel.getDashboardState().getValue();
            if (state != null && state.pinnedNote != null) {
                Intent intent = new Intent(this, NoteEditorActivity.class);
                intent.putExtra(Note.EXTRA_NOTE_ITEM, state.pinnedNote);
                startActivity(intent);
            }
        });
    }

    private void submitQuickTask() {
        String text = binding.tabHome.edtNewTask.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        homeViewModel.addQuickTask(text, getString(R.string.task_default_tag));
        binding.tabHome.edtNewTask.setText("");
    }

    private void openTaskEditor(@Nullable StudyTask existingTask) {
        FrostUiHelper.showTaskEditorDialog(
                this,
                existingTask,
                savedTask -> homeViewModel.saveTask(savedTask, hasNotificationPermission()),
                targetTask -> FrostUiHelper.showConfirmDialog(
                        this,
                        R.string.task_delete_confirm_title,
                        getString(R.string.task_delete_confirm_message, targetTask.getTitle()),
                        R.string.notes_dialog_delete_confirm,
                        true,
                        () -> homeViewModel.deleteTask(targetTask)
                )
        );
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void renderChecklist(@NonNull List<StudyTask> taskItems, long nowMillis) {
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

            checkArea.setOnClickListener(v ->
                    homeViewModel.toggleTaskCompletion(item, !item.isDone(), hasNotificationPermission())
            );
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

    private void bindCalendarTab() {
        TextView[] chips = getCalendarDayChips();
        for (int i = 0; i < chips.length; i++) {
            final int index = i;
            chips[i].setOnClickListener(v -> calendarViewModel.selectDay(index));
        }

        binding.tabCalendar.btnScheduleAdd.setOnClickListener(v -> openScheduleEditor(null));
        binding.tabCalendar.btnScheduleEmptyAdd.setOnClickListener(v -> openScheduleEditor(null));
        binding.tabCalendar.btnScheduleImport.setOnClickListener(v -> openScheduleImportSourceDialog());
        binding.tabCalendar.btnScheduleExport.setOnClickListener(v ->
                calendarViewModel.requestOpenExportDialog()
        );
    }

    private void openScheduleImportSourceDialog() {
        FrostUiHelper.showImportSourceDialog(this, new FrostUiHelper.OnImportSourceListener() {
            @Override
            public void onImportFromFile() {
                importScheduleLauncher.launch(new String[]{"*/*"});
            }

            @Override
            public void onImportFromCode() {
                FrostUiHelper.showShareCodeInputDialog(
                        MainActivity.this,
                        code -> calendarViewModel.importByShareCode(code)
                );
            }

            @Override
            public void onImportFromAiImage() {
                pickScheduleImageLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }
        });
    }

    @NonNull
    private TextView[] getCalendarDayChips() {
        return new TextView[]{
                binding.tabCalendar.chipDayT2,
                binding.tabCalendar.chipDayT3,
                binding.tabCalendar.chipDayT4,
                binding.tabCalendar.chipDayT5,
                binding.tabCalendar.chipDayT6,
                binding.tabCalendar.chipDayT7
        };
    }

    private void renderCalendarState(@NonNull CalendarViewModel.CalendarUiState state) {
        FrostUiHelper.applyChipSelection(this, getCalendarDayChips(), state.selectedDayIndex);
        binding.tabCalendar.tvCalendarDayTitle.setText(CalendarViewModel.getDayFullTitleRes(state.dayOfWeek));
        binding.tabCalendar.tvCalendarCountBadge.setText(
                getString(R.string.schedule_count_badge_format, state.daySlots.size())
        );

        LinearLayout container = binding.tabCalendar.scheduleContainer;
        container.removeAllViews();

        if (state.daySlots.isEmpty()) {
            binding.tabCalendar.cardCalendarEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.tabCalendar.cardCalendarEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ScheduleSlot slot : state.daySlots) {
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
        int currentDay = calendarViewModel.getSelectedDayOfWeek();
        FrostUiHelper.showScheduleEditorDialog(
                this,
                existingSlot,
                currentDay,
                savedSlot -> calendarViewModel.saveSchedule(savedSlot, existingSlot == null),
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
                () -> calendarViewModel.deleteSchedule(slot.getId())
        );
    }

    private void showExportDialog(int totalSlots) {
        FrostUiHelper.showExportOptionsDialog(this, totalSlots, new FrostUiHelper.OnExportActionListener() {
            @Override
            public void onSaveSchedules(@NonNull ScheduleTransferHelper.TransferFormat format) {
                calendarViewModel.prepareExportDocument(format, false);
            }

            @Override
            public void onShareSchedules(@NonNull ScheduleTransferHelper.TransferFormat format) {
                calendarViewModel.shareSchedules(format);
            }

            @Override
            public void onManageOnlineShares() {
                openOnlineShareDialog();
            }

            @Override
            public void onExportTemplate(@NonNull ScheduleTransferHelper.TransferFormat format) {
                calendarViewModel.prepareExportDocument(format, true);
            }
        });
    }

    private void openOnlineShareDialog() {
        if (onlineShareDialog != null) {
            onlineShareDialog.dialog.dismiss();
        }
        onlineShareDialog = FrostUiHelper.createOnlineShareDialog(
                this,
                title -> calendarViewModel.createOnlineShare(title)
        );
        onlineShareDialog.dialog.setOnDismissListener(d -> onlineShareDialog = null);
        renderCalendarCloudBusy(calendarCloudBusy);
        List<ApiModels.ShareSummaryResponse> currentShares = calendarViewModel.getMyShares().getValue();
        renderOnlineShareList(currentShares);
        if (calendarViewModel.isCloudLoggedIn()) {
            calendarViewModel.loadMyShares();
        }
    }

    private void renderCalendarCloudBusy(@Nullable Boolean busy) {
        calendarCloudBusy = Boolean.TRUE.equals(busy);
        boolean enabled = !calendarCloudBusy;
        float alpha = enabled ? 1.0f : 0.55f;

        binding.tabCalendar.btnScheduleImport.setEnabled(enabled);
        binding.tabCalendar.btnScheduleImport.setAlpha(alpha);
        binding.tabCalendar.btnScheduleImport.setText(
                calendarCloudBusy ? R.string.schedule_cloud_busy_label : R.string.schedule_action_import
        );
        binding.tabCalendar.btnScheduleExport.setEnabled(enabled);
        binding.tabCalendar.btnScheduleExport.setAlpha(alpha);

        if (onlineShareDialog != null && onlineShareDialog.dialog.isShowing()) {
            onlineShareDialog.btnCreate.setEnabled(enabled);
            onlineShareDialog.btnCreate.setAlpha(alpha);
            onlineShareDialog.btnCreate.setText(
                    calendarCloudBusy ? R.string.schedule_cloud_busy_label : R.string.schedule_share_btn_create
            );
            int childCount = onlineShareDialog.layoutListContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View row = onlineShareDialog.layoutListContainer.getChildAt(i);
                View btnDelete = row.findViewWithTag("delete_share");
                if (btnDelete != null) {
                    btnDelete.setEnabled(enabled);
                    btnDelete.setAlpha(alpha);
                }
            }
        }
    }

    private void renderOnlineShareList(@Nullable List<ApiModels.ShareSummaryResponse> shares) {
        if (onlineShareDialog == null || !onlineShareDialog.dialog.isShowing()) {
            return;
        }
        onlineShareDialog.layoutListContainer.removeAllViews();

        if (!calendarViewModel.isCloudLoggedIn()) {
            onlineShareDialog.tvListEmpty.setVisibility(View.VISIBLE);
            onlineShareDialog.tvListEmpty.setText(R.string.schedule_share_login_hint);
            return;
        }
        if (shares == null || shares.isEmpty()) {
            onlineShareDialog.tvListEmpty.setVisibility(View.VISIBLE);
            onlineShareDialog.tvListEmpty.setText(R.string.schedule_share_list_empty);
            return;
        }
        onlineShareDialog.tvListEmpty.setVisibility(View.GONE);
        for (ApiModels.ShareSummaryResponse item : shares) {
            onlineShareDialog.layoutListContainer.addView(createShareItemRow(item));
        }
    }

    @NonNull
    private LinearLayout createShareItemRow(@NonNull ApiModels.ShareSummaryResponse item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_glass_inner);
        int pad = getResources().getDimensionPixelSize(R.dimen.card_padding_compact);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.chip_gap);
        row.setLayoutParams(rowParams);

        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        infoCol.setLayoutParams(infoParams);

        TextView tvCodeTitle = new TextView(this);
        String shareTitle = hasText(item.getTitle())
                ? item.getTitle().trim()
                : getString(R.string.schedule_share_default_title);
        tvCodeTitle.setText(getString(R.string.home_spotlight_title_format, item.getCode(), shareTitle));
        tvCodeTitle.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
        tvCodeTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_input));
        tvCodeTitle.setTypeface(null, Typeface.BOLD);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(item.isExpired()
                ? getString(R.string.schedule_share_status_expired)
                : getString(R.string.schedule_share_status_active_format, formatShareExpiry(item.getExpiresAtEpoch())));
        tvStatus.setTextColor(ContextCompat.getColor(
                this,
                item.isExpired() ? R.color.accent_danger : R.color.ink_secondary
        ));
        tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_caption));

        infoCol.addView(tvCodeTitle);
        infoCol.addView(tvStatus);
        row.addView(infoCol);

        int gap = getResources().getDimensionPixelSize(R.dimen.chip_gap);
        TextView btnCopy = createShareRowButton(
                getString(R.string.schedule_share_btn_copy),
                R.color.accent_sapphire,
                R.drawable.bg_pill_accent_soft,
                gap,
                () -> copyShareCode(item.getCode())
        );
        row.addView(btnCopy);

        TextView btnDelete = createShareRowButton(
                getString(R.string.schedule_share_btn_delete),
                R.color.accent_danger,
                R.drawable.bg_pill_danger_soft,
                gap,
                () -> FrostUiHelper.showConfirmDialog(
                        this,
                        R.string.schedule_share_delete_title,
                        getString(R.string.schedule_share_delete_message, item.getCode()),
                        R.string.notes_dialog_delete_confirm,
                        true,
                        () -> calendarViewModel.deleteOnlineShare(item.getCode())
                )
        );
        btnDelete.setTag("delete_share");
        btnDelete.setEnabled(!calendarCloudBusy);
        btnDelete.setAlpha(calendarCloudBusy ? 0.55f : 1.0f);
        row.addView(btnDelete);

        return row;
    }

    @NonNull
    private TextView createShareRowButton(
            @NonNull String label,
            int textColorRes,
            int backgroundRes,
            int marginStart,
            @NonNull Runnable onClick
    ) {
        int chipPadH = getResources().getDimensionPixelSize(R.dimen.chip_padding_h);
        int minTouch = getResources().getDimensionPixelSize(R.dimen.touch_target_min);
        TextView btn = new TextView(this);
        btn.setText(label);
        btn.setTextColor(ContextCompat.getColor(this, textColorRes));
        btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_caption));
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundResource(backgroundRes);
        btn.setGravity(Gravity.CENTER);
        btn.setMinHeight(minTouch);
        btn.setPadding(chipPadH, 0, chipPadH, 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginStart(marginStart);
        btn.setLayoutParams(params);
        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    private void copyShareCode(@NonNull String code) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("schedule_share_code", code));
            Toast.makeText(this, getString(R.string.schedule_share_copied_toast, code), Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private String formatShareExpiry(long expiresAtEpoch) {
        if (expiresAtEpoch <= 0L) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
                .format(new Date(expiresAtEpoch));
    }

    private void launchShareChooser(@NonNull CalendarViewModel.ShareFileEventData data) {
        Uri contentUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                data.file
        );
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(data.mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.schedule_export_share_text));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.schedule_export_share_chooser)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.schedule_export_no_share_app, Toast.LENGTH_SHORT).show();
        }
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

    private void bindToolsTab() {
        binding.tabTools.chipCompress100.setOnClickListener(v -> toolsViewModel.selectCompressPreset(0));
        binding.tabTools.chipCompress500.setOnClickListener(v -> toolsViewModel.selectCompressPreset(1));
        binding.tabTools.chipCompress3x4.setOnClickListener(v -> toolsViewModel.selectCompressPreset(2));
        binding.tabTools.btnRunCompress.setOnClickListener(v ->
                pickCompressImageLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build())
        );
        binding.tabTools.btnSaveCompressed.setOnClickListener(v ->
                toolsViewModel.requestSaveCompressed(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        );
        binding.tabTools.btnShareCompressed.setOnClickListener(v -> toolsViewModel.requestShareCompressed());

        binding.tabTools.chipOcrSample1.setOnClickListener(v -> toolsViewModel.selectOcrSample(0));
        binding.tabTools.chipOcrSample2.setOnClickListener(v -> toolsViewModel.selectOcrSample(1));
        binding.tabTools.btnPickOcrImage.setOnClickListener(v ->
                pickOcrImageLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build())
        );
        binding.tabTools.btnCopyOcr.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(
                        getString(R.string.tools_ocr_clip_label),
                        binding.tabTools.tvOcrOutput.getText()
                );
                clipboard.setPrimaryClip(clip);
                toolsViewModel.notifyOcrCopied();
            }
        });
        binding.tabTools.btnOcrCreateNote.setOnClickListener(v -> {
            String ocrText = binding.tabTools.tvOcrOutput.getText().toString().trim();
            Intent intent = new Intent(this, NoteEditorActivity.class);
            if (!ocrText.isEmpty()) {
                intent.putExtra(Note.EXTRA_INITIAL_CONTENT, ocrText);
            }
            startActivity(intent);
        });

        binding.tabTools.btnToolOpenNotes.setOnClickListener(v -> openNotesActivity());
        binding.tabTools.btnToolNewNote.setOnClickListener(v ->
                startActivity(new Intent(this, NoteEditorActivity.class))
        );
    }

    private void renderCompressPreset(int preset) {
        TextView[] chips = new TextView[]{
                binding.tabTools.chipCompress100,
                binding.tabTools.chipCompress500,
                binding.tabTools.chipCompress3x4
        };
        FrostUiHelper.applyChipSelection(this, chips, preset);

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

    private void renderCompressState(@NonNull ToolsViewModel.CompressUiState state) {
        boolean busy = state.compressing || state.saving;
        boolean enabled = !busy;
        float alpha = enabled ? 1.0f : 0.55f;

        binding.tabTools.chipCompress100.setEnabled(enabled);
        binding.tabTools.chipCompress100.setAlpha(alpha);
        binding.tabTools.chipCompress500.setEnabled(enabled);
        binding.tabTools.chipCompress500.setAlpha(alpha);
        binding.tabTools.chipCompress3x4.setEnabled(enabled);
        binding.tabTools.chipCompress3x4.setAlpha(alpha);

        binding.tabTools.btnRunCompress.setEnabled(enabled);
        binding.tabTools.btnRunCompress.setAlpha(alpha);
        if (state.compressing) {
            binding.tabTools.btnRunCompress.setText(R.string.tools_compress_running);
        } else if (state.result != null) {
            binding.tabTools.btnRunCompress.setText(R.string.tools_compress_btn_pick_another);
        } else {
            binding.tabTools.btnRunCompress.setText(R.string.tools_compress_btn_run);
        }

        binding.tabTools.btnSaveCompressed.setEnabled(enabled);
        binding.tabTools.btnSaveCompressed.setAlpha(alpha);
        binding.tabTools.btnSaveCompressed.setText(
                state.saving ? R.string.tools_compress_saving : R.string.tools_compress_btn_save
        );
        binding.tabTools.btnShareCompressed.setEnabled(enabled);
        binding.tabTools.btnShareCompressed.setAlpha(alpha);

        ImageCompressGateway.CompressResult result = state.result;
        if (result == null) {
            lastCompressedPreviewKey = null;
            binding.tabTools.ivCompressPreview.setImageDrawable(null);
            binding.tabTools.layoutCompressResult.setVisibility(View.GONE);
            return;
        }

        binding.tabTools.layoutCompressResult.setVisibility(View.VISIBLE);
        String originalText = ImageCompressHelper.formatSize(result.originalBytes);
        String compressedText = ImageCompressHelper.formatSize(result.compressedBytes);
        int savedPercent = ImageCompressHelper.computeSavingsPercent(result.originalBytes, result.compressedBytes);
        if (savedPercent > 0) {
            binding.tabTools.tvCompressResultSize.setText(
                    getString(R.string.tools_compress_result_size_format, originalText, compressedText, savedPercent)
            );
        } else {
            binding.tabTools.tvCompressResultSize.setText(
                    getString(R.string.tools_compress_result_size_same_format, originalText, compressedText)
            );
        }
        binding.tabTools.tvCompressResultMeta.setText(
                getString(R.string.tools_compress_result_meta_format, result.width, result.height, result.quality)
        );
        binding.tabTools.tvCompressResultNote.setVisibility(result.reachedTarget ? View.GONE : View.VISIBLE);

        String previewKey = result.outputFile.getAbsolutePath()
                + ":" + result.outputFile.lastModified()
                + ":" + result.compressedBytes;
        if (!previewKey.equals(lastCompressedPreviewKey)) {
            Bitmap thumb = decodeCompressedThumbnail(result.outputFile);
            if (thumb != null) {
                binding.tabTools.ivCompressPreview.setImageBitmap(thumb);
                lastCompressedPreviewKey = previewKey;
            } else {
                lastCompressedPreviewKey = null;
                binding.tabTools.ivCompressPreview.setImageDrawable(null);
            }
        }
    }

    @Nullable
    private static Bitmap decodeCompressedThumbnail(@NonNull File file) {
        if (!file.exists()) {
            return null;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = ImageCompressHelper.computeInSampleSize(bounds.outWidth, bounds.outHeight, 320, false);
        return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
    }

    private void launchShareCompressedImage(@NonNull File file) {
        Uri contentUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                file
        );
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpeg");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.tools_compress_share_chooser)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.schedule_export_no_share_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void renderOcrSample(int sample) {
        TextView[] chips = new TextView[]{
                binding.tabTools.chipOcrSample1,
                binding.tabTools.chipOcrSample2
        };
        FrostUiHelper.applyChipSelection(this, chips, sample);
        ToolsViewModel.OcrUiState currentOcr = toolsViewModel.getOcrState().getValue();
        if (currentOcr != null && hasText(currentOcr.scannedText)) {
            binding.tabTools.tvOcrOutput.setText(currentOcr.scannedText);
        } else {
            binding.tabTools.tvOcrOutput.setText(
                    sample == 0 ? R.string.tools_ocr_sample_slide : R.string.tools_ocr_sample_meeting
            );
        }
    }

    private void renderOcrState(@NonNull ToolsViewModel.OcrUiState state) {
        if (hasText(state.scannedText)) {
            binding.tabTools.tvOcrOutput.setText(state.scannedText);
        }
        boolean enabled = !state.scanning;
        binding.tabTools.btnPickOcrImage.setEnabled(enabled);
        binding.tabTools.btnPickOcrImage.setAlpha(enabled ? 1.0f : 0.55f);
        binding.tabTools.btnPickOcrImage.setText(
                state.scanning ? R.string.tools_ocr_scanning : R.string.tools_ocr_btn_pick
        );
        binding.tabTools.btnOcrCreateNote.setEnabled(enabled);
        binding.tabTools.btnOcrCreateNote.setAlpha(enabled ? 1.0f : 0.55f);
    }

    private void bindProfileTab() {
        binding.tabProfile.seekSimulatedScore.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    profileViewModel.updateSimulatedProgress(progress, true);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                profileViewModel.persistCurrentSimulatedProgress(seekBar.getProgress());
            }
        });

        binding.tabProfile.btnEditProfile.setOnClickListener(v ->
                FrostUiHelper.showProfileEditorDialog(
                        this,
                        profileViewModel.getCurrentProfileSnapshot(),
                        updatedProfile -> profileViewModel.saveProfile(updatedProfile)
                )
        );
        binding.tabProfile.btnProfileNotes.setOnClickListener(v -> openNotesActivity());
        binding.tabProfile.btnCloudAuth.setOnClickListener(v -> openAuthDialog());
        binding.tabProfile.btnCloudBackup.setOnClickListener(v -> profileViewModel.backupNow());
        binding.tabProfile.btnCloudRestore.setOnClickListener(v ->
                FrostUiHelper.showConfirmDialog(
                        this,
                        R.string.cloud_restore_confirm_title,
                        getString(R.string.cloud_restore_confirm_message),
                        R.string.cloud_btn_restore,
                        false,
                        () -> profileViewModel.restoreLatest()
                )
        );
        binding.tabProfile.btnCloudLogout.setOnClickListener(v ->
                FrostUiHelper.showConfirmDialog(
                        this,
                        R.string.cloud_logout_confirm_title,
                        getString(R.string.cloud_logout_confirm_message),
                        R.string.cloud_btn_logout,
                        true,
                        () -> profileViewModel.logout()
                )
        );
    }

    private void renderProfileState(@NonNull ProfileViewModel.ProfileUiState state) {
        StudentProfile profile = state.profile;
        String initials = ProfileGpaHelper.buildInitials(
                profile.getFullName(),
                getString(R.string.profile_default_initials)
        );
        binding.btnHeaderProfile.setText(initials);
        binding.tabProfile.tvProfileAvatar.setText(initials);
        binding.tabProfile.tvProfileName.setText(profile.getFullName());

        String major = profile.getMajorAndCohort();
        binding.tabProfile.tvProfileMajor.setText(
                hasText(major) ? major.trim() : getString(R.string.profile_major_fallback)
        );
        binding.tabProfile.tvProfileCreditsValue.setText(
                getString(R.string.profile_credits_value_format, profile.getEarnedCredits(), profile.getTotalCredits())
        );
        binding.tabProfile.tvProfileAttendanceValue.setText(
                getString(R.string.profile_attendance_value_format, profile.getAttendancePercent())
        );
        binding.tabProfile.tvGpaBaselineSummary.setText(
                getString(
                        R.string.profile_gpa_baseline_summary_format,
                        profile.getBaselineGpa(),
                        profile.getCurrentTermCredits()
                )
        );

        if (binding.tabProfile.seekSimulatedScore.getProgress() != state.simulatedProgress) {
            binding.tabProfile.seekSimulatedScore.setProgress(state.simulatedProgress);
        }

        binding.tabProfile.tvSimulatedScore.setText(
                getString(R.string.profile_gpa_sim_score_format, state.scoreTen)
        );
        binding.tabProfile.tvGpaProjected.setText(
                getString(R.string.profile_gpa_value_format, state.projectedGpa)
        );
        binding.tabProfile.pbGpaMeter.setProgress(Math.round(state.projectedGpa * 100f));

        switch (state.rank) {
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

    private void openAuthDialog() {
        FrostUiHelper.showAuthDialog(this, (isRegister, fullName, email, password) -> {
            if (isRegister) {
                profileViewModel.register(fullName, email, password);
            } else {
                profileViewModel.login(email, password);
            }
        });
    }

    private void renderCloudState(@NonNull ProfileViewModel.CloudUiState state) {
        if (state.loggedIn) {
            binding.tabProfile.layoutCloudGuest.setVisibility(View.GONE);
            binding.tabProfile.layoutCloudSignedIn.setVisibility(View.VISIBLE);
            binding.tabProfile.tvCloudEmail.setText(state.email);
            if (state.lastBackupEpoch > 0L) {
                Locale vi = Locale.forLanguageTag("vi-VN");
                String formatted = new SimpleDateFormat("dd/MM/yyyy HH:mm", vi)
                        .format(new Date(state.lastBackupEpoch));
                binding.tabProfile.tvCloudLastBackup.setText(
                        getString(R.string.cloud_last_backup_format, formatted)
                );
            } else {
                binding.tabProfile.tvCloudLastBackup.setText(R.string.cloud_last_backup_none);
            }
        } else {
            binding.tabProfile.layoutCloudGuest.setVisibility(View.VISIBLE);
            binding.tabProfile.layoutCloudSignedIn.setVisibility(View.GONE);
        }

        boolean enabled = !state.busy;
        float alpha = enabled ? 1.0f : 0.55f;
        TextView[] actionButtons = new TextView[]{
                binding.tabProfile.btnCloudAuth,
                binding.tabProfile.btnCloudBackup,
                binding.tabProfile.btnCloudRestore,
                binding.tabProfile.btnCloudLogout
        };
        for (TextView btn : actionButtons) {
            btn.setEnabled(enabled);
            btn.setAlpha(alpha);
        }
        binding.tabProfile.btnCloudBackup.setText(
                state.busy ? R.string.cloud_busy_label : R.string.cloud_btn_backup
        );
    }

    private void bindDockNavigation() {
        binding.dockTabHome.setOnClickListener(v -> switchTab(0));
        binding.dockTabCalendar.setOnClickListener(v -> switchTab(1));
        binding.dockTabTools.setOnClickListener(v -> switchTab(2));
        binding.dockTabProfile.setOnClickListener(v -> switchTab(3));
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

    private boolean hasText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }
}
