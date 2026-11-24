package com.example.pocketkit;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.pocketkit.tools.ImageCompressHelper;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;
import com.example.pocketkit.viewmodel.ToolsViewModel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ToolsViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private ToolsViewModel createViewModel(FakeImageCompressGateway gateway) {
        return new ToolsViewModel(
                new FakePocketKitRepository(),
                new FakeCloudGateway(),
                gateway,
                Runnable::run,
                Runnable::run
        );
    }

    @Test
    public void compressImage_updatesStateAndEmitsSuccessToast() throws IOException {
        FakeImageCompressGateway gateway = new FakeImageCompressGateway();
        gateway.outputFile = tempFolder.newFile("out_100.jpg");
        ToolsViewModel viewModel = createViewModel(gateway);

        viewModel.compressImage("content://media/photo.jpg");

        ToolsViewModel.CompressUiState state = viewModel.getCompressState().getValue();
        assertNotNull(state);
        assertFalse(state.compressing);
        assertFalse(state.saving);
        assertNotNull(state.result);
        assertEquals(0, state.result.presetIndex);
        assertEquals(1, gateway.compressedPresets.size());

        UiEvent<UiMessage> toast = viewModel.getToastEvent().getValue();
        assertNotNull(toast);
        UiMessage message = toast.getContentIfNotHandled();
        assertNotNull(message);
        assertEquals(R.string.tools_compress_done_toast, message.resId);
    }

    @Test
    public void compressImage_whenOverTarget_emitsOverTargetToast() throws IOException {
        FakeImageCompressGateway gateway = new FakeImageCompressGateway();
        gateway.outputFile = tempFolder.newFile("out_over.jpg");
        gateway.reachedTarget = false;
        gateway.compressedBytes = 140_000L;
        ToolsViewModel viewModel = createViewModel(gateway);

        viewModel.compressImage("content://media/dense.jpg");

        UiMessage message = viewModel.getToastEvent().getValue().getContentIfNotHandled();
        assertNotNull(message);
        assertEquals(R.string.tools_compress_done_over_target_toast, message.resId);
    }

    @Test
    public void selectCompressPreset_afterImagePicked_autoRecompressesWithNewPreset() throws IOException {
        FakeImageCompressGateway gateway = new FakeImageCompressGateway();
        gateway.outputFile = tempFolder.newFile("out_recompress.jpg");
        ToolsViewModel viewModel = createViewModel(gateway);

        viewModel.compressImage("content://media/portrait.jpg");
        viewModel.selectCompressPreset(2);

        assertEquals(2, gateway.compressedPresets.size());
        assertEquals(ImageCompressHelper.CompressPreset.UNDER_100_KB, gateway.compressedPresets.get(0));
        assertEquals(ImageCompressHelper.CompressPreset.ID_PHOTO_3X4, gateway.compressedPresets.get(1));
        assertEquals(2, viewModel.getCompressState().getValue().result.presetIndex);
    }

    @Test
    public void selectCompressPreset_whenRecompressFails_clearsSourceUri() throws IOException {
        FakeImageCompressGateway gateway = new FakeImageCompressGateway();
        gateway.outputFile = tempFolder.newFile("out_ok.jpg");
        ToolsViewModel viewModel = createViewModel(gateway);

        viewModel.compressImage("content://media/portrait.jpg");
        gateway.compressErrorToThrow = new IOException("Tệp không còn tồn tại");

        viewModel.selectCompressPreset(1);
        UiMessage errorToast = viewModel.getToastEvent().getValue().getContentIfNotHandled();
        assertNotNull(errorToast);
        assertEquals(R.string.cloud_message_raw, errorToast.resId);

        gateway.compressErrorToThrow = null;
        viewModel.selectCompressPreset(2);
        assertEquals(1, gateway.compressedPresets.size());
    }

    @Test
    public void requestSaveCompressed_supportsMediaStoreAndDocumentPicker() throws IOException {
        FakeImageCompressGateway gateway = new FakeImageCompressGateway();
        gateway.outputFile = tempFolder.newFile("out_save.jpg");
        ToolsViewModel viewModel = createViewModel(gateway);

        viewModel.compressImage("content://media/id.jpg");
        viewModel.requestSaveCompressed(true);

        assertNotNull(gateway.savedGalleryFileName);
        UiMessage galleryToast = viewModel.getToastEvent().getValue().getContentIfNotHandled();
        assertNotNull(galleryToast);
        assertEquals(R.string.tools_compress_saved_gallery_toast, galleryToast.resId);

        viewModel.requestSaveCompressed(false);
        UiEvent<String> docEvent = viewModel.getLaunchSaveCompressedDocumentEvent().getValue();
        assertNotNull(docEvent);
        assertEquals(gateway.savedGalleryFileName, docEvent.getContentIfNotHandled());

        viewModel.saveCompressedToDocument("content://documents/target.jpg");
        assertEquals("content://documents/target.jpg", gateway.savedDocumentUri);
        UiMessage fileToast = viewModel.getToastEvent().getValue().getContentIfNotHandled();
        assertNotNull(fileToast);
        assertEquals(R.string.tools_compress_saved_file_toast, fileToast.resId);
    }

    @Test
    public void requestShareCompressed_emitsFileWhenPresentAndErrorsWhenMissing() throws IOException {
        FakeImageCompressGateway gateway = new FakeImageCompressGateway();
        ToolsViewModel viewModel = createViewModel(gateway);

        viewModel.requestShareCompressed();
        UiMessage missingToast = viewModel.getToastEvent().getValue().getContentIfNotHandled();
        assertNotNull(missingToast);
        assertEquals(R.string.tools_compress_no_result_error, missingToast.resId);
        assertNull(viewModel.getShareCompressedEvent().getValue());

        File tempFile = tempFolder.newFile("out_share.jpg");
        gateway.outputFile = tempFile;
        viewModel.compressImage("content://media/share.jpg");
        viewModel.requestShareCompressed();

        UiEvent<File> shareEvent = viewModel.getShareCompressedEvent().getValue();
        assertNotNull(shareEvent);
        assertEquals(tempFile, shareEvent.getContentIfNotHandled());
    }

    @Test
    public void compressImage_whenErrorOccurs_allowsRetryAfterRecovery() throws IOException {
        FakeImageCompressGateway gateway = new FakeImageCompressGateway();
        gateway.outputFile = tempFolder.newFile("out_retry.jpg");
        gateway.compressErrorToThrow = new IOException("Lỗi đọc ảnh");
        ToolsViewModel viewModel = createViewModel(gateway);

        viewModel.compressImage("content://media/broken.jpg");
        assertNull(viewModel.getCompressState().getValue().result);
        assertFalse(viewModel.getCompressState().getValue().compressing);
        assertTrue(gateway.compressedPresets.isEmpty());

        gateway.compressErrorToThrow = null;
        viewModel.compressImage("content://media/valid.jpg");
        assertNotNull(viewModel.getCompressState().getValue().result);
        assertEquals(1, gateway.compressedPresets.size());
    }
}
