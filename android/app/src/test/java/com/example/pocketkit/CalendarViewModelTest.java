package com.example.pocketkit;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.pocketkit.model.ScheduleSlot;
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

    @Test
    public void saveAndDeleteSchedule_updatesCalendarStateAndNotifiesDashboard() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        CalendarViewModel viewModel = new CalendarViewModel(repository, new StubTransferGateway(), Runnable::run);

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
    public void toolsViewModel_tracksSelectionsAndNotesCount() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        repository.insertCategory("PRM392");
        ToolsViewModel viewModel = new ToolsViewModel(repository, new FakeCloudGateway(), Runnable::run, Runnable::run);

        viewModel.selectCompressPreset(2);
        viewModel.selectOcrSample(1);
        viewModel.triggerCompressReadyMessage();
        viewModel.refreshNotesCount();

        assertEquals(Integer.valueOf(2), viewModel.getCompressPreset().getValue());
        assertEquals(Integer.valueOf(1), viewModel.getOcrSample().getValue());
        assertEquals(Integer.valueOf(0), viewModel.getTotalNotesCount().getValue());
        assertNotNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void toolsViewModel_scanOcrImage_requiresLoginAndUpdatesStateWhenLoggedIn() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        ToolsViewModel viewModel = new ToolsViewModel(repository, cloudGateway, Runnable::run, Runnable::run);

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
