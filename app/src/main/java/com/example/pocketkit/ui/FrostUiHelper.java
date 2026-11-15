package com.example.pocketkit.ui;

import android.animation.ValueAnimator;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
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
import com.example.pocketkit.home.HomeSummaryHelper;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.model.StudentProfile;
import com.example.pocketkit.model.StudyTask;
import com.example.pocketkit.profile.ProfileGpaHelper;
import com.example.pocketkit.transfer.ScheduleTransferHelper;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class FrostUiHelper {

    public interface OnCategoryCreatedListener {
        void onCategoryCreated(@NonNull String categoryName);
    }

    public interface OnScheduleSavedListener {
        void onScheduleSaved(@NonNull ScheduleSlot slot);
    }

    public interface OnScheduleDeletedListener {
        void onScheduleDeleted(@NonNull ScheduleSlot slot);
    }

    public interface OnTaskSavedListener {
        void onTaskSaved(@NonNull StudyTask task);
    }

    public interface OnTaskDeletedListener {
        void onTaskDeleted(@NonNull StudyTask task);
    }

    public interface OnImportActionListener {
        void onImportConfirmed(boolean replaceExisting);
    }

    public interface OnExportActionListener {
        void onSaveSchedules(@NonNull ScheduleTransferHelper.TransferFormat format);

        void onShareSchedules(@NonNull ScheduleTransferHelper.TransferFormat format);

        void onExportTemplate(@NonNull ScheduleTransferHelper.TransferFormat format);
    }

    public interface OnProfileSavedListener {
        void onProfileSaved(@NonNull StudentProfile profile);
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

        final int bottomMarginH = activity.getResources().getDimensionPixelSize(R.dimen.dock_horizontal_padding);
        final int bottomMarginB = activity.getResources().getDimensionPixelSize(R.dimen.dock_bottom_padding);
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
            if (bottomBarView != null && bottomBarView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomBarView.getLayoutParams();
                lp.leftMargin = bottomMarginH + bars.left;
                lp.rightMargin = bottomMarginH + bars.right;
                lp.bottomMargin = bottomMarginB + bars.bottom;
                bottomBarView.setLayoutParams(lp);
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
        View content = inflateDialogView(context, R.layout.dialog_frost_input);
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
        View content = inflateDialogView(context, R.layout.dialog_frost_confirm);
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

    public static void showScheduleEditorDialog(
            @NonNull Context context,
            @Nullable ScheduleSlot existingSlot,
            int defaultDayOfWeek,
            @NonNull OnScheduleSavedListener saveListener,
            @Nullable OnScheduleDeletedListener deleteListener
    ) {
        AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = inflateDialogView(context, R.layout.dialog_frost_schedule);
        dialog.setContentView(content);

        TextView tvTitle = content.findViewById(R.id.tvScheduleDialogTitle);
        EditText edtSubject = content.findViewById(R.id.edtScheduleSubject);
        EditText edtRoom = content.findViewById(R.id.edtScheduleRoom);
        EditText edtLecturer = content.findViewById(R.id.edtScheduleLecturer);
        EditText edtNote = content.findViewById(R.id.edtScheduleNote);
        View btnStartTime = content.findViewById(R.id.btnDialogStartTime);
        View btnEndTime = content.findViewById(R.id.btnDialogEndTime);
        TextView tvStartTime = content.findViewById(R.id.tvDialogStartTimeValue);
        TextView tvEndTime = content.findViewById(R.id.tvDialogEndTimeValue);
        TextView btnCancel = content.findViewById(R.id.btnScheduleDialogCancel);
        TextView btnSave = content.findViewById(R.id.btnScheduleDialogSave);
        View btnDelete = content.findViewById(R.id.btnScheduleDialogDelete);

        TextView[] dayChips = new TextView[]{
                content.findViewById(R.id.chipDialogT2),
                content.findViewById(R.id.chipDialogT3),
                content.findViewById(R.id.chipDialogT4),
                content.findViewById(R.id.chipDialogT5),
                content.findViewById(R.id.chipDialogT6),
                content.findViewById(R.id.chipDialogT7)
        };

        boolean editing = existingSlot != null;
        tvTitle.setText(editing ? R.string.schedule_dialog_edit_title : R.string.schedule_dialog_add_title);

        final int[] selectedDay = new int[]{
                editing ? existingSlot.getDayOfWeek() : clampDayOfWeek(defaultDayOfWeek)
        };
        final String[] startTime = new String[]{
                editing && existingSlot.getStartTime() != null
                        ? existingSlot.getStartTime()
                        : context.getString(R.string.schedule_default_start_time)
        };
        final String[] endTime = new String[]{
                editing && existingSlot.getEndTime() != null
                        ? existingSlot.getEndTime()
                        : context.getString(R.string.schedule_default_end_time)
        };

        if (editing) {
            edtSubject.setText(existingSlot.getSubject());
            edtRoom.setText(existingSlot.getRoom());
            edtLecturer.setText(existingSlot.getLecturer());
            edtNote.setText(existingSlot.getNote());
        }

        Runnable refreshChips = () -> {
            for (int i = 0; i < dayChips.length; i++) {
                boolean active = (i + ScheduleSlot.DAY_MIN) == selectedDay[0];
                dayChips[i].setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
                dayChips[i].setTextColor(ContextCompat.getColor(context, active ? R.color.white : R.color.ink_primary));
            }
        };
        refreshChips.run();

        for (int i = 0; i < dayChips.length; i++) {
            final int dayValue = i + ScheduleSlot.DAY_MIN;
            dayChips[i].setOnClickListener(v -> {
                selectedDay[0] = dayValue;
                refreshChips.run();
            });
        }

        tvStartTime.setText(startTime[0]);
        tvEndTime.setText(endTime[0]);

        btnStartTime.setOnClickListener(v -> showTimePicker(context, startTime[0], picked -> {
            startTime[0] = picked;
            tvStartTime.setText(picked);
        }));

        btnEndTime.setOnClickListener(v -> showTimePicker(context, endTime[0], picked -> {
            endTime[0] = picked;
            tvEndTime.setText(picked);
        }));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String subject = edtSubject.getText().toString().trim();
            if (subject.isEmpty()) {
                edtSubject.setError(context.getString(R.string.schedule_error_empty_subject));
                edtSubject.requestFocus();
                return;
            }
            int startMinutes = ScheduleSlot.toMinutes(startTime[0]);
            int endMinutes = ScheduleSlot.toMinutes(endTime[0]);
            if (startMinutes < 0 || endMinutes <= startMinutes) {
                Toast.makeText(context, R.string.schedule_error_invalid_time, Toast.LENGTH_SHORT).show();
                return;
            }
            ScheduleSlot result = ScheduleSlot.builder()
                    .id(editing ? existingSlot.getId() : 0)
                    .dayOfWeek(selectedDay[0])
                    .startTime(startTime[0])
                    .endTime(endTime[0])
                    .subject(subject)
                    .room(edtRoom.getText().toString().trim())
                    .lecturer(edtLecturer.getText().toString().trim())
                    .note(edtNote.getText().toString().trim())
                    .build();
            dialog.dismiss();
            saveListener.onScheduleSaved(result);
        });

        if (editing && deleteListener != null) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                deleteListener.onScheduleDeleted(existingSlot);
            });
        }

        configureFrostDialogWindow(dialog, !editing);
        dialog.show();
    }

    public static void showTaskEditorDialog(
            @NonNull Context context,
            @Nullable StudyTask existingTask,
            @NonNull OnTaskSavedListener saveListener,
            @Nullable OnTaskDeletedListener deleteListener
    ) {
        AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = inflateDialogView(context, R.layout.dialog_frost_task);
        dialog.setContentView(content);

        TextView tvTitle = content.findViewById(R.id.tvTaskDialogTitle);
        EditText edtTitle = content.findViewById(R.id.edtTaskDialogTitle);
        EditText edtSubject = content.findViewById(R.id.edtTaskDialogSubject);
        View btnPickDue = content.findViewById(R.id.btnTaskPickDue);
        TextView tvDueDisplay = content.findViewById(R.id.tvTaskDueDisplay);
        TextView btnClearDue = content.findViewById(R.id.btnTaskClearDue);
        TextView btnPresetToday = content.findViewById(R.id.btnTaskPresetToday);
        TextView btnPresetTomorrow = content.findViewById(R.id.btnTaskPresetTomorrow);
        TextView btnCancel = content.findViewById(R.id.btnTaskDialogCancel);
        TextView btnSave = content.findViewById(R.id.btnTaskDialogSave);
        View btnDelete = content.findViewById(R.id.btnTaskDialogDelete);

        TextView[] tagChips = new TextView[]{
                content.findViewById(R.id.chipTaskTagDeadline),
                content.findViewById(R.id.chipTaskTagAssignment),
                content.findViewById(R.id.chipTaskTagExam),
                content.findViewById(R.id.chipTaskTagGroup),
                content.findViewById(R.id.chipTaskTagPersonal)
        };
        String[] tagValues = new String[]{
                context.getString(R.string.task_tag_deadline),
                context.getString(R.string.task_tag_assignment),
                context.getString(R.string.task_tag_exam),
                context.getString(R.string.task_tag_group),
                context.getString(R.string.task_default_tag)
        };

        boolean editing = existingTask != null;
        tvTitle.setText(editing ? R.string.task_dialog_edit_title : R.string.task_dialog_add_title);

        final int[] selectedTagIdx = new int[]{0};
        final long[] selectedDueAt = new long[]{editing ? existingTask.getDueAtMillis() : 0L};

        if (editing) {
            edtTitle.setText(existingTask.getTitle());
            edtSubject.setText(existingTask.getSubject());
            String currentTag = existingTask.getTag() != null ? existingTask.getTag().trim() : "";
            for (int i = 0; i < tagValues.length; i++) {
                if (tagValues[i].equalsIgnoreCase(currentTag)) {
                    selectedTagIdx[0] = i;
                    break;
                }
            }
        }

        Runnable refreshTags = () -> {
            for (int i = 0; i < tagChips.length; i++) {
                boolean active = i == selectedTagIdx[0];
                tagChips[i].setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
                tagChips[i].setTextColor(ContextCompat.getColor(context, active ? R.color.white : R.color.ink_primary));
            }
        };
        refreshTags.run();

        for (int i = 0; i < tagChips.length; i++) {
            final int index = i;
            tagChips[i].setOnClickListener(v -> {
                selectedTagIdx[0] = index;
                refreshTags.run();
            });
        }

        Runnable refreshDueDisplay = () -> {
            if (selectedDueAt[0] > 0L) {
                tvDueDisplay.setText(HomeSummaryHelper.formatDueDateTime(context, selectedDueAt[0], System.currentTimeMillis()));
                tvDueDisplay.setTextColor(ContextCompat.getColor(context, R.color.ink_primary));
                btnClearDue.setVisibility(View.VISIBLE);
            } else {
                tvDueDisplay.setText(R.string.task_no_due_label);
                tvDueDisplay.setTextColor(ContextCompat.getColor(context, R.color.ink_secondary));
                btnClearDue.setVisibility(View.GONE);
            }
        };
        refreshDueDisplay.run();

        btnPickDue.setOnClickListener(v -> {
            Calendar initial = Calendar.getInstance();
            if (selectedDueAt[0] > 0L) {
                initial.setTimeInMillis(selectedDueAt[0]);
            } else {
                initial.set(Calendar.HOUR_OF_DAY, 17);
                initial.set(Calendar.MINUTE, 0);
            }
            new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> new TimePickerDialog(
                            context,
                            (timeView, hourOfDay, minute) -> {
                                Calendar chosen = Calendar.getInstance();
                                chosen.set(Calendar.YEAR, year);
                                chosen.set(Calendar.MONTH, month);
                                chosen.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                chosen.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                chosen.set(Calendar.MINUTE, minute);
                                chosen.set(Calendar.SECOND, 0);
                                chosen.set(Calendar.MILLISECOND, 0);
                                selectedDueAt[0] = chosen.getTimeInMillis();
                                refreshDueDisplay.run();
                            },
                            initial.get(Calendar.HOUR_OF_DAY),
                            initial.get(Calendar.MINUTE),
                            true
                    ).show(),
                    initial.get(Calendar.YEAR),
                    initial.get(Calendar.MONTH),
                    initial.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        btnPresetToday.setOnClickListener(v -> {
            selectedDueAt[0] = HomeSummaryHelper.buildUpcomingDueAtMillis(0, 17, 0, System.currentTimeMillis(), false);
            refreshDueDisplay.run();
        });

        btnPresetTomorrow.setOnClickListener(v -> {
            selectedDueAt[0] = HomeSummaryHelper.buildUpcomingDueAtMillis(1, 9, 0, System.currentTimeMillis(), false);
            refreshDueDisplay.run();
        });

        btnClearDue.setOnClickListener(v -> {
            selectedDueAt[0] = 0L;
            refreshDueDisplay.run();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            if (title.isEmpty()) {
                edtTitle.setError(context.getString(R.string.task_dialog_empty_title_error));
                edtTitle.requestFocus();
                return;
            }
            StudyTask saved = StudyTask.builder()
                    .id(editing ? existingTask.getId() : 0)
                    .title(title)
                    .subject(edtSubject.getText().toString().trim())
                    .tag(tagValues[selectedTagIdx[0]])
                    .dueAtMillis(selectedDueAt[0])
                    .done(editing && existingTask.isDone())
                    .createdAtMillis(editing ? existingTask.getCreatedAtMillis() : System.currentTimeMillis())
                    .build();
            dialog.dismiss();
            saveListener.onTaskSaved(saved);
        });

        if (editing && deleteListener != null) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                deleteListener.onTaskDeleted(existingTask);
            });
        }

        configureFrostDialogWindow(dialog, !editing);
        dialog.show();
    }

    public static void showImportPreviewDialog(
            @NonNull Context context,
            @NonNull String fileName,
            @NonNull ScheduleTransferHelper.ImportResult result,
            @NonNull OnImportActionListener listener
    ) {
        AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = inflateDialogView(context, R.layout.dialog_frost_schedule_import);
        dialog.setContentView(content);

        TextView tvSummary = content.findViewById(R.id.tvImportSummary);
        TextView tvIssues = content.findViewById(R.id.tvImportIssues);
        TextView btnMerge = content.findViewById(R.id.btnImportMerge);
        TextView btnReplace = content.findViewById(R.id.btnImportReplace);
        TextView btnCancel = content.findViewById(R.id.btnImportCancel);

        tvSummary.setText(context.getString(
                R.string.schedule_import_summary_format,
                fileName,
                getFormatDisplayLabel(context, result.detectedFormat),
                result.validSlots.size(),
                result.totalRows
        ));

        if (!result.issues.isEmpty()) {
            tvIssues.setVisibility(View.VISIBLE);
            tvIssues.setText(context.getString(
                    R.string.schedule_import_skipped_format,
                    result.issues.size(),
                    formatIssueSamples(context, result.issues)
            ));
        }

        btnMerge.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onImportConfirmed(false);
        });

        btnReplace.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onImportConfirmed(true);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        configureFrostDialogWindow(dialog, false);
        dialog.show();
    }

    public static void showExportOptionsDialog(
            @NonNull Context context,
            int totalSlots,
            @NonNull OnExportActionListener listener
    ) {
        AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = inflateDialogView(context, R.layout.dialog_frost_schedule_export);
        dialog.setContentView(content);

        TextView tvSummary = content.findViewById(R.id.tvExportSummary);
        TextView chipXlsx = content.findViewById(R.id.chipExportXlsx);
        TextView chipCsv = content.findViewById(R.id.chipExportCsv);
        TextView chipJson = content.findViewById(R.id.chipExportJson);
        TextView btnSave = content.findViewById(R.id.btnExportSave);
        TextView btnShare = content.findViewById(R.id.btnExportShare);
        TextView btnTemplate = content.findViewById(R.id.btnExportTemplate);
        TextView btnClose = content.findViewById(R.id.btnExportClose);

        tvSummary.setText(context.getString(R.string.schedule_export_summary_format, totalSlots));

        final ScheduleTransferHelper.TransferFormat[] selectedFormat = new ScheduleTransferHelper.TransferFormat[]{
                ScheduleTransferHelper.TransferFormat.XLSX
        };
        TextView[] formatChips = new TextView[]{chipXlsx, chipCsv, chipJson};
        ScheduleTransferHelper.TransferFormat[] formats = new ScheduleTransferHelper.TransferFormat[]{
                ScheduleTransferHelper.TransferFormat.XLSX,
                ScheduleTransferHelper.TransferFormat.CSV,
                ScheduleTransferHelper.TransferFormat.JSON
        };

        Runnable refreshFormatChips = () -> {
            for (int i = 0; i < formatChips.length; i++) {
                boolean active = formats[i] == selectedFormat[0];
                formatChips[i].setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
                formatChips[i].setTextColor(ContextCompat.getColor(context, active ? R.color.white : R.color.ink_primary));
            }
        };
        refreshFormatChips.run();

        for (int i = 0; i < formatChips.length; i++) {
            final ScheduleTransferHelper.TransferFormat fmt = formats[i];
            formatChips[i].setOnClickListener(v -> {
                selectedFormat[0] = fmt;
                refreshFormatChips.run();
            });
        }

        btnSave.setOnClickListener(v -> {
            if (totalSlots <= 0) {
                Toast.makeText(context, R.string.schedule_export_empty_error, Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            listener.onSaveSchedules(selectedFormat[0]);
        });

        btnShare.setOnClickListener(v -> {
            if (totalSlots <= 0) {
                Toast.makeText(context, R.string.schedule_export_empty_error, Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            listener.onShareSchedules(selectedFormat[0]);
        });

        btnTemplate.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onExportTemplate(selectedFormat[0]);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        configureFrostDialogWindow(dialog, false);
        dialog.show();
    }

    public static void showProfileEditorDialog(
            @NonNull Context context,
            @NonNull StudentProfile currentProfile,
            @NonNull OnProfileSavedListener listener
    ) {
        AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = inflateDialogView(context, R.layout.dialog_frost_profile);
        dialog.setContentView(content);

        EditText edtName = content.findViewById(R.id.edtProfileName);
        EditText edtMajor = content.findViewById(R.id.edtProfileMajor);
        EditText edtEarned = content.findViewById(R.id.edtProfileEarnedCredits);
        EditText edtTotal = content.findViewById(R.id.edtProfileTotalCredits);
        EditText edtAttendance = content.findViewById(R.id.edtProfileAttendance);
        EditText edtBaselineGpa = content.findViewById(R.id.edtProfileBaselineGpa);
        EditText edtTermCredits = content.findViewById(R.id.edtProfileTermCredits);
        TextView btnCancel = content.findViewById(R.id.btnProfileDialogCancel);
        TextView btnSave = content.findViewById(R.id.btnProfileDialogSave);

        edtName.setText(currentProfile.getFullName());
        edtMajor.setText(currentProfile.getMajorAndCohort());
        edtEarned.setText(String.valueOf(currentProfile.getEarnedCredits()));
        edtTotal.setText(String.valueOf(currentProfile.getTotalCredits()));
        edtAttendance.setText(String.valueOf(currentProfile.getAttendancePercent()));
        edtBaselineGpa.setText(String.format(Locale.US, "%.2f", currentProfile.getBaselineGpa()));
        edtTermCredits.setText(String.valueOf(currentProfile.getCurrentTermCredits()));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String fullName = edtName.getText().toString().trim();
            String major = edtMajor.getText().toString().trim();
            int earned = parseIntOrInvalid(edtEarned.getText().toString());
            int total = parseIntOrInvalid(edtTotal.getText().toString());
            int attendance = parseIntOrInvalid(edtAttendance.getText().toString());
            float baselineGpa = parseFloatOrNaN(edtBaselineGpa.getText().toString());
            int termCredits = parseIntOrInvalid(edtTermCredits.getText().toString());

            ProfileGpaHelper.ValidationError error = ProfileGpaHelper.validateProfile(
                    fullName,
                    earned,
                    total,
                    attendance,
                    baselineGpa,
                    termCredits
            );
            if (error != ProfileGpaHelper.ValidationError.NONE) {
                showProfileValidationError(
                        context,
                        error,
                        edtName,
                        edtEarned,
                        edtTotal,
                        edtAttendance,
                        edtBaselineGpa,
                        edtTermCredits
                );
                return;
            }

            StudentProfile updated = StudentProfile.builder()
                    .fullName(fullName)
                    .majorAndCohort(major)
                    .earnedCredits(earned)
                    .totalCredits(total)
                    .attendancePercent(attendance)
                    .baselineGpa(baselineGpa)
                    .currentTermCredits(termCredits)
                    .build();
            dialog.dismiss();
            listener.onProfileSaved(updated);
        });

        configureFrostDialogWindow(dialog, false);
        dialog.show();
    }

    private static void showProfileValidationError(
            @NonNull Context context,
            @NonNull ProfileGpaHelper.ValidationError error,
            @NonNull EditText edtName,
            @NonNull EditText edtEarned,
            @NonNull EditText edtTotal,
            @NonNull EditText edtAttendance,
            @NonNull EditText edtBaselineGpa,
            @NonNull EditText edtTermCredits
    ) {
        switch (error) {
            case EMPTY_NAME:
                setFieldError(edtName, context.getString(R.string.profile_dialog_error_name));
                break;
            case INVALID_EARNED_CREDITS:
                setFieldError(edtEarned, context.getString(R.string.profile_dialog_error_earned));
                break;
            case INVALID_TOTAL_CREDITS:
                setFieldError(edtTotal, context.getString(R.string.profile_dialog_error_total));
                break;
            case EARNED_EXCEEDS_TOTAL:
                setFieldError(edtEarned, context.getString(R.string.profile_dialog_error_earned_exceeds));
                break;
            case INVALID_ATTENDANCE:
                setFieldError(edtAttendance, context.getString(R.string.profile_dialog_error_attendance));
                break;
            case INVALID_BASELINE_GPA:
                setFieldError(edtBaselineGpa, context.getString(R.string.profile_dialog_error_gpa));
                break;
            case INVALID_TERM_CREDITS:
            default:
                setFieldError(edtTermCredits, context.getString(R.string.profile_dialog_error_term_credits));
                break;
        }
    }

    private static void setFieldError(@NonNull EditText field, @NonNull String message) {
        field.setError(message);
        field.requestFocus();
    }

    private static int parseIntOrInvalid(@Nullable String raw) {
        if (raw == null) {
            return -1;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static float parseFloatOrNaN(@Nullable String raw) {
        if (raw == null) {
            return Float.NaN;
        }
        String normalized = raw.trim().replace(',', '.');
        if (normalized.isEmpty()) {
            return Float.NaN;
        }
        try {
            return Float.parseFloat(normalized);
        } catch (NumberFormatException ignored) {
            return Float.NaN;
        }
    }

    @NonNull
    public static String getFormatDisplayLabel(
            @NonNull Context context,
            @NonNull ScheduleTransferHelper.TransferFormat format
    ) {
        switch (format) {
            case XLSX:
                return context.getString(R.string.schedule_export_format_xlsx);
            case CSV:
                return context.getString(R.string.schedule_export_format_csv);
            case JSON:
            default:
                return context.getString(R.string.schedule_export_format_json);
        }
    }

    private interface OnTimePickedListener {
        void onTimePicked(@NonNull String formattedTime);
    }

    private static void showTimePicker(
            @NonNull Context context,
            @NonNull String currentTime,
            @NonNull OnTimePickedListener listener
    ) {
        int totalMinutes = ScheduleSlot.toMinutes(currentTime);
        int initHour = totalMinutes >= 0 ? totalMinutes / 60 : 7;
        int initMinute = totalMinutes >= 0 ? totalMinutes % 60 : 30;
        new TimePickerDialog(
                context,
                (view, hourOfDay, minute) -> listener.onTimePicked(
                        String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                ),
                initHour,
                initMinute,
                true
        ).show();
    }

    private static String formatIssueSamples(
            @NonNull Context context,
            @NonNull List<ScheduleTransferHelper.RowIssue> issues
    ) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(2, issues.size());
        for (int i = 0; i < limit; i++) {
            ScheduleTransferHelper.RowIssue issue = issues.get(i);
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(context.getString(
                    R.string.schedule_import_issue_row_format,
                    issue.rowNumber,
                    getIssueReason(context, issue.issueType)
            ));
        }
        return sb.toString();
    }

    private static String getIssueReason(
            @NonNull Context context,
            @NonNull ScheduleTransferHelper.IssueType type
    ) {
        switch (type) {
            case INVALID_DAY:
                return context.getString(R.string.schedule_import_issue_day);
            case INVALID_TIME:
                return context.getString(R.string.schedule_import_issue_time);
            case INVALID_RANGE:
                return context.getString(R.string.schedule_import_issue_range);
            case EMPTY_SUBJECT:
            default:
                return context.getString(R.string.schedule_import_issue_subject);
        }
    }

    private static int clampDayOfWeek(int dayOfWeek) {
        if (dayOfWeek < ScheduleSlot.DAY_MIN || dayOfWeek > ScheduleSlot.DAY_MAX) {
            return ScheduleSlot.DAY_MIN;
        }
        return dayOfWeek;
    }

    private static View inflateDialogView(@NonNull Context context, @LayoutRes int layoutRes) {
        return LayoutInflater.from(context).inflate(layoutRes, new FrameLayout(context), false);
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
