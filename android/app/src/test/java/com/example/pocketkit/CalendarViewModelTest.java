package com.example.pocketkit;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.transfer.ScheduleTransferGateway;
import com.example.pocketkit.transfer.ScheduleTransferHelper;
import com.example.pocketkit.viewmodel.CalendarViewModel;
import com.example.pocketkit.viewmodel.ToolsViewModel;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CalendarViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    private static final class StubTransferGateway implements ScheduleTransferGateway {
        @NonNull
        @Override
        public ScheduleTransferHelper.ImportResult parseImport(@NonNull String uriString, @NonNull String fileName) {
            return new ScheduleTransferHelper.ImportResult(
                    ScheduleTransferHelper.TransferFormat.CSV,
                    0,
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        @Override
        public void writeExport(
                @NonNull String uriString,
                @NonNull List<ScheduleSlot> slots,
                @NonNull ScheduleTransferHelper.TransferFormat format
        ) {
        }

        @NonNull
        @Override
        public File writeShareFile(
                @NonNull List<ScheduleSlot> slots,
                @NonNull ScheduleTransferHelper.TransferFormat format,
                @NonNull String fileName
        ) {
            return new File(fileName);
        }

        @NonNull
        @Override
        public List<ScheduleSlot> buildTemplateSlots() {
            return Collections.emptyList();
        }
    }

    private CalendarViewModel createCalendarViewModel(
            @NonNull FakePocketKitRepository repository,
            @NonNull FakeCloudGateway cloudGateway
    ) {
        return new CalendarViewModel(
                repository,
                new StubTransferGateway(),
                cloudGateway,
                Runnable::run,
                Runnable::run
        );
    }

    @Test
    public void saveAndDeleteSchedule_updatesCalendarStateAndNotifiesDashboard() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        CalendarViewModel viewModel = createCalendarViewModel(repository, new FakeCloudGateway());

        ScheduleSlot slot = ScheduleSlot.builder()
                .dayOfWeek(3)
                .startTime("07:30")
                .endTime("09:45")
                .subject("PRM392")
                .room("BE201")
                .build();

        viewModel.saveSchedule(slot, true);
        CalendarViewModel.CalendarUiState savedState = viewModel.getCalendarState().getValue();
        assertNotNull(savedState);
        assertEquals(3, savedState.dayOfWeek);
        assertEquals(1, savedState.daySlots.size());
        assertNotNull(viewModel.getScheduleMutatedEvent().getValue().getContentIfNotHandled());
        assertNull(viewModel.getScheduleMutatedEvent().getValue().getContentIfNotHandled());

        viewModel.deleteSchedule(savedState.daySlots.get(0).getId());
        CalendarViewModel.CalendarUiState afterDelete = viewModel.getCalendarState().getValue();
        assertNotNull(afterDelete);
        assertEquals(0, afterDelete.daySlots.size());
    }

    @Test
    public void importByShareCode_withoutLogin_emitsImportPreview() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        CalendarViewModel viewModel = createCalendarViewModel(repository, cloudGateway);

        viewModel.importByShareCode(" abc123 ");

        assertEquals("ABC123", cloudGateway.lastShareCodeQueried);
        CalendarViewModel.ImportPreviewEventData preview = viewModel.getImportPreviewEvent().getValue().getContentIfNotHandled();
        assertNotNull(preview);
        assertEquals("TKB Chia Sẻ", preview.fileName);
        assertEquals(1, preview.result.validSlots.size());
        assertEquals("PRM392", preview.result.validSlots.get(0).getSubject());
    }

    @Test
    public void createOnlineShare_requiresLoginAndSanitizesSchedules() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        repository.insertSchedule(ScheduleSlot.builder()
                .dayOfWeek(2)
                .startTime("7:30")
                .endTime("9:45")
                .subject("PRM392")
                .build());
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        CalendarViewModel viewModel = createCalendarViewModel(repository, cloudGateway);

        viewModel.createOnlineShare("TKB PRM");
        assertNull(cloudGateway.lastSharedSlots);
        assertNotNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());

        cloudGateway.loggedIn = true;
        viewModel.createOnlineShare("TKB PRM");

        assertNotNull(cloudGateway.lastSharedSlots);
        assertEquals(1, cloudGateway.lastSharedSlots.size());
        assertEquals("07:30", cloudGateway.lastSharedSlots.get(0).getStartTime());
        ApiModels.CreateShareResponse created = viewModel.getShareCreatedEvent().getValue().getContentIfNotHandled();
        assertNotNull(created);
        assertEquals("ABC123", created.getCode());
        assertEquals(1, viewModel.getMyShares().getValue().size());
    }

    @Test
    public void scanScheduleImage_requiresLoginAndEmitsImportPreview() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        CalendarViewModel viewModel = createCalendarViewModel(repository, cloudGateway);

        viewModel.scanScheduleImage("content://media/tkb.png");
        assertNull(cloudGateway.lastScheduleImageUri);
        assertNotNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());

        cloudGateway.loggedIn = true;
        viewModel.scanScheduleImage("content://media/tkb.png");

        assertEquals("content://media/tkb.png", cloudGateway.lastScheduleImageUri);
        CalendarViewModel.ImportPreviewEventData preview = viewModel.getImportPreviewEvent().getValue().getContentIfNotHandled();
        assertNotNull(preview);
        assertEquals(1, preview.result.validSlots.size());
        assertEquals("SWP391", preview.result.validSlots.get(0).getSubject());
    }

    @Test
    public void deleteOnlineShare_removesCodeFromMyShares() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        repository.insertSchedule(ScheduleSlot.builder()
                .dayOfWeek(2)
                .startTime("07:30")
                .endTime("09:45")
                .subject("PRM392")
                .build());
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        cloudGateway.loggedIn = true;
        CalendarViewModel viewModel = createCalendarViewModel(repository, cloudGateway);

        viewModel.createOnlineShare("TKB Kỳ 1");
        assertEquals(1, viewModel.getMyShares().getValue().size());

        viewModel.deleteOnlineShare("abc123");
        assertEquals("ABC123", cloudGateway.lastDeletedShareCode);
        assertTrue(viewModel.getMyShares().getValue().isEmpty());
    }

    @Test
    public void toolsViewModel_tracksSelectionsAndNotesCount() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        repository.insertCategory("PRM392");
        FakeImageCompressGateway compressGateway = new FakeImageCompressGateway();
        ToolsViewModel viewModel = new ToolsViewModel(
                repository,
                new FakeCloudGateway(),
                compressGateway,
                Runnable::run,
                Runnable::run
        );

        viewModel.selectCompressPreset(2);
        viewModel.selectOcrSample(1);
        viewModel.compressImage("content://media/sample.jpg");
        viewModel.refreshNotesCount();

        assertEquals(Integer.valueOf(2), viewModel.getCompressPreset().getValue());
        assertEquals(Integer.valueOf(1), viewModel.getOcrSample().getValue());
        assertEquals(Integer.valueOf(0), viewModel.getTotalNotesCount().getValue());
        assertNotNull(viewModel.getCompressState().getValue().result);
        assertNotNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void toolsViewModel_scanOcrImage_requiresLoginAndUpdatesStateWhenLoggedIn() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        ToolsViewModel viewModel = new ToolsViewModel(
                repository,
                cloudGateway,
                new FakeImageCompressGateway(),
                Runnable::run,
                Runnable::run
        );

        viewModel.scanOcrImage("content://media/slide.png");
        assertNull(cloudGateway.lastOcrUri);
        assertNotNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());

        cloudGateway.loggedIn = true;
        viewModel.scanOcrImage("content://media/slide.png");

        ToolsViewModel.OcrUiState state = viewModel.getOcrState().getValue();
        assertNotNull(state);
        assertEquals("Nội dung OCR mẫu", state.scannedText);
        assertEquals("content://media/slide.png", cloudGateway.lastOcrUri);
    }
}
