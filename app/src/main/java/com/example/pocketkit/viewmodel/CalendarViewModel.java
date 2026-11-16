package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketkit.R;
import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.transfer.ScheduleTransferGateway;
import com.example.pocketkit.transfer.ScheduleTransferHelper;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class CalendarViewModel extends ViewModel {

    public static final class CalendarUiState {
        public final int selectedDayIndex;
        public final int dayOfWeek;
        @NonNull
        public final List<ScheduleSlot> daySlots;

        public CalendarUiState(int selectedDayIndex, int dayOfWeek, @NonNull List<ScheduleSlot> daySlots) {
            this.selectedDayIndex = selectedDayIndex;
            this.dayOfWeek = dayOfWeek;
            this.daySlots = daySlots;
        }
    }

    public static final class ImportPreviewEventData {
        @NonNull
        public final String fileName;
        @NonNull
        public final ScheduleTransferHelper.ImportResult result;

        public ImportPreviewEventData(
                @NonNull String fileName,
                @NonNull ScheduleTransferHelper.ImportResult result
        ) {
            this.fileName = fileName;
            this.result = result;
        }
    }

    public static final class ExportDocumentRequest {
        @NonNull
        public final String fileName;
        @NonNull
        public final ScheduleTransferHelper.TransferFormat format;

        public ExportDocumentRequest(
                @NonNull String fileName,
                @NonNull ScheduleTransferHelper.TransferFormat format
        ) {
            this.fileName = fileName;
            this.format = format;
        }
    }

    public static final class ShareFileEventData {
        @NonNull
        public final File file;
        @NonNull
        public final String mimeType;

        public ShareFileEventData(@NonNull File file, @NonNull String mimeType) {
            this.file = file;
            this.mimeType = mimeType;
        }
    }

    private final PocketKitRepository repository;
    private final ScheduleTransferGateway transferGateway;
    private final Executor ioExecutor;

    private final MutableLiveData<CalendarUiState> calendarState = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<ImportPreviewEventData>> importPreviewEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<ExportDocumentRequest>> launchExportDocumentEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<ShareFileEventData>> shareFileEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Integer>> openExportDialogEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Boolean>> scheduleMutatedEvent = new MutableLiveData<>();

    private int selectedDayIndex = getInitialDayIndex();
    private ScheduleTransferHelper.TransferFormat pendingExportFormat = ScheduleTransferHelper.TransferFormat.XLSX;
    private boolean pendingExportTemplate = false;

    public CalendarViewModel(
            @NonNull PocketKitRepository repository,
            @NonNull ScheduleTransferGateway transferGateway,
            @NonNull Executor ioExecutor
    ) {
        this.repository = repository;
        this.transferGateway = transferGateway;
        this.ioExecutor = ioExecutor;
    }

    @NonNull
    public LiveData<CalendarUiState> getCalendarState() {
        return calendarState;
    }

    @NonNull
    public LiveData<UiEvent<ImportPreviewEventData>> getImportPreviewEvent() {
        return importPreviewEvent;
    }

    @NonNull
    public LiveData<UiEvent<ExportDocumentRequest>> getLaunchExportDocumentEvent() {
        return launchExportDocumentEvent;
    }

    @NonNull
    public LiveData<UiEvent<ShareFileEventData>> getShareFileEvent() {
        return shareFileEvent;
    }

    @NonNull
    public LiveData<UiEvent<Integer>> getOpenExportDialogEvent() {
        return openExportDialogEvent;
    }

    @NonNull
    public LiveData<UiEvent<UiMessage>> getToastEvent() {
        return toastEvent;
    }

    @NonNull
    public LiveData<UiEvent<Boolean>> getScheduleMutatedEvent() {
        return scheduleMutatedEvent;
    }

    public int getSelectedDayOfWeek() {
        return selectedDayIndex + ScheduleSlot.DAY_MIN;
    }

    public void selectDay(int dayIndex) {
        selectedDayIndex = Math.max(0, Math.min(5, dayIndex));
        refreshCurrentDay();
    }

    public void refreshCurrentDay() {
        final int targetIndex = selectedDayIndex;
        final int dayOfWeek = targetIndex + ScheduleSlot.DAY_MIN;
        ioExecutor.execute(() -> {
            List<ScheduleSlot> slots = repository.getSchedulesByDay(dayOfWeek);
            calendarState.postValue(new CalendarUiState(targetIndex, dayOfWeek, slots));
        });
    }

    public void saveSchedule(@NonNull ScheduleSlot savedSlot, boolean isNew) {
        ioExecutor.execute(() -> {
            if (isNew) {
                repository.insertSchedule(savedSlot);
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(
                        R.string.schedule_toast_created,
                        UiMessage.strRef(getDayShortNameRes(savedSlot.getDayOfWeek()))
                )));
            } else {
                repository.updateSchedule(savedSlot);
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.schedule_toast_updated)));
            }
            selectedDayIndex = Math.max(0, Math.min(5, savedSlot.getDayOfWeek() - ScheduleSlot.DAY_MIN));
            List<ScheduleSlot> slots = repository.getSchedulesByDay(getSelectedDayOfWeek());
            calendarState.postValue(new CalendarUiState(selectedDayIndex, getSelectedDayOfWeek(), slots));
            scheduleMutatedEvent.postValue(new UiEvent<>(true));
        });
    }

    public void deleteSchedule(int slotId) {
        ioExecutor.execute(() -> {
            repository.deleteSchedule(slotId);
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.schedule_toast_deleted)));
            List<ScheduleSlot> slots = repository.getSchedulesByDay(getSelectedDayOfWeek());
            calendarState.postValue(new CalendarUiState(selectedDayIndex, getSelectedDayOfWeek(), slots));
            scheduleMutatedEvent.postValue(new UiEvent<>(true));
        });
    }

    public void parseImportFile(@NonNull String uriString, @NonNull String fileName) {
        toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.schedule_import_parsing)));
        ioExecutor.execute(() -> {
            try {
                ScheduleTransferHelper.ImportResult result = transferGateway.parseImport(uriString, fileName);
                if (result.validSlots.isEmpty()) {
                    toastEvent.postValue(new UiEvent<>(UiMessage.longToast(R.string.schedule_import_empty_error)));
                    return;
                }
                importPreviewEvent.postValue(new UiEvent<>(new ImportPreviewEventData(fileName, result)));
            } catch (ScheduleTransferHelper.LegacyXlsException e) {
                toastEvent.postValue(new UiEvent<>(UiMessage.longToast(R.string.schedule_import_legacy_xls_error)));
            } catch (Exception e) {
                toastEvent.postValue(new UiEvent<>(UiMessage.longToast(R.string.schedule_import_read_error)));
            }
        });
    }

    public void confirmImport(
            @NonNull ScheduleTransferHelper.ImportResult result,
            boolean replaceExisting
    ) {
        ioExecutor.execute(() -> {
            DatabaseHelper.ImportSummary summary = repository.importSchedules(result.validSlots, replaceExisting);
            if (replaceExisting) {
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(
                        R.string.schedule_import_replaced_toast,
                        summary.insertedCount
                )));
            } else if (summary.skippedDuplicates > 0) {
                toastEvent.postValue(new UiEvent<>(UiMessage.longToast(
                        R.string.schedule_import_merged_skip_toast,
                        summary.insertedCount,
                        summary.skippedDuplicates
                )));
            } else {
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(
                        R.string.schedule_import_merged_toast,
                        summary.insertedCount
                )));
            }
            if (!result.validSlots.isEmpty()) {
                int targetDay = result.validSlots.get(0).getDayOfWeek();
                selectedDayIndex = Math.max(0, Math.min(5, targetDay - ScheduleSlot.DAY_MIN));
            }
            List<ScheduleSlot> slots = repository.getSchedulesByDay(getSelectedDayOfWeek());
            calendarState.postValue(new CalendarUiState(selectedDayIndex, getSelectedDayOfWeek(), slots));
            scheduleMutatedEvent.postValue(new UiEvent<>(true));
        });
    }

    public void requestOpenExportDialog() {
        ioExecutor.execute(() -> openExportDialogEvent.postValue(new UiEvent<>(repository.getTotalScheduleCount())));
    }

    public void prepareExportDocument(
            @NonNull ScheduleTransferHelper.TransferFormat format,
            boolean isTemplate
    ) {
        pendingExportFormat = format;
        pendingExportTemplate = isTemplate;
        launchExportDocumentEvent.postValue(new UiEvent<>(new ExportDocumentRequest(
                buildExportFileName(format, isTemplate),
                format
        )));
    }

    public void handleExportUri(@NonNull String targetUriString) {
        final ScheduleTransferHelper.TransferFormat format = pendingExportFormat;
        final boolean isTemplate = pendingExportTemplate;
        ioExecutor.execute(() -> {
            try {
                List<ScheduleSlot> slots = isTemplate
                        ? transferGateway.buildTemplateSlots()
                        : repository.getAllSchedules();
                transferGateway.writeExport(targetUriString, slots, format);
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(
                        isTemplate
                                ? R.string.schedule_export_template_saved_toast
                                : R.string.schedule_export_saved_toast
                )));
            } catch (Exception e) {
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.schedule_export_failed_toast)));
            }
        });
    }

    public void shareSchedules(@NonNull ScheduleTransferHelper.TransferFormat format) {
        final String fileName = buildExportFileName(format, false);
        ioExecutor.execute(() -> {
            try {
                List<ScheduleSlot> slots = repository.getAllSchedules();
                File outFile = transferGateway.writeShareFile(slots, format, fileName);
                shareFileEvent.postValue(new UiEvent<>(new ShareFileEventData(outFile, format.mimeType)));
            } catch (Exception e) {
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.schedule_export_failed_toast)));
            }
        });
    }

    public static int getDayShortNameRes(int dayOfWeek) {
        switch (dayOfWeek) {
            case 2:
                return R.string.schedule_day_name_2;
            case 3:
                return R.string.schedule_day_name_3;
            case 4:
                return R.string.schedule_day_name_4;
            case 5:
                return R.string.schedule_day_name_5;
            case 6:
                return R.string.schedule_day_name_6;
            case 7:
            default:
                return R.string.schedule_day_name_7;
        }
    }

    public static int getDayFullTitleRes(int dayOfWeek) {
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

    private static int getInitialDayIndex() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (day >= Calendar.MONDAY && day <= Calendar.SATURDAY) {
            return day - Calendar.MONDAY;
        }
        return 0;
    }

    @NonNull
    private static String buildExportFileName(
            @NonNull ScheduleTransferHelper.TransferFormat format,
            boolean template
    ) {
        if (template) {
            return "mau-thoi-khoa-bieu." + format.extension;
        }
        String stamp = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        return "thoi-khoa-bieu-" + stamp + "." + format.extension;
    }
}
