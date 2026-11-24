package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketkit.R;
import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.network.exception.CloudException;
import com.example.pocketkit.network.gateway.CloudGateway;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.tools.ImageCompressGateway;
import com.example.pocketkit.tools.ImageCompressHelper;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

public class ToolsViewModel extends ViewModel {

    public static final class CompressUiState {
        public final boolean compressing;
        public final boolean saving;
        @Nullable
        public final ImageCompressGateway.CompressResult result;

        public CompressUiState(
                boolean compressing,
                boolean saving,
                @Nullable ImageCompressGateway.CompressResult result
        ) {
            this.compressing = compressing;
            this.saving = saving;
            this.result = result;
        }
    }

    public static final class OcrUiState {
        @Nullable
        public final String scannedText;
        @Nullable
        public final String sourceFileName;
        public final boolean scanning;

        public OcrUiState(@Nullable String scannedText, @Nullable String sourceFileName, boolean scanning) {
            this.scannedText = scannedText;
            this.sourceFileName = sourceFileName;
            this.scanning = scanning;
        }
    }

    private final PocketKitRepository repository;
    private final CloudGateway cloudGateway;
    private final ImageCompressGateway imageCompressGateway;
    private final Executor ioExecutor;
    private final Executor networkExecutor;

    private final MutableLiveData<Integer> compressPreset = new MutableLiveData<>(0);
    private final MutableLiveData<CompressUiState> compressState =
            new MutableLiveData<>(new CompressUiState(false, false, null));
    private final MutableLiveData<Integer> ocrSample = new MutableLiveData<>(0);
    private final MutableLiveData<OcrUiState> ocrState = new MutableLiveData<>(new OcrUiState(null, null, false));
    private final MutableLiveData<Integer> totalNotesCount = new MutableLiveData<>(0);
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<File>> shareCompressedEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<String>> launchSaveCompressedDocumentEvent = new MutableLiveData<>();

    private String lastScannedText;
    private String lastSourceFileName;
    private volatile boolean scanningOcr = false;

    private volatile boolean compressBusy = false;
    @Nullable
    private String lastCompressSourceUri;
    @Nullable
    private ImageCompressGateway.CompressResult lastCompressResult;

    public ToolsViewModel(
            @NonNull PocketKitRepository repository,
            @NonNull CloudGateway cloudGateway,
            @NonNull ImageCompressGateway imageCompressGateway,
            @NonNull Executor ioExecutor,
            @NonNull Executor networkExecutor
    ) {
        this.repository = repository;
        this.cloudGateway = cloudGateway;
        this.imageCompressGateway = imageCompressGateway;
        this.ioExecutor = ioExecutor;
        this.networkExecutor = networkExecutor;
    }

    @NonNull
    public LiveData<Integer> getCompressPreset() {
        return compressPreset;
    }

    @NonNull
    public LiveData<CompressUiState> getCompressState() {
        return compressState;
    }

    @NonNull
    public LiveData<Integer> getOcrSample() {
        return ocrSample;
    }

    @NonNull
    public LiveData<OcrUiState> getOcrState() {
        return ocrState;
    }

    @NonNull
    public LiveData<Integer> getTotalNotesCount() {
        return totalNotesCount;
    }

    @NonNull
    public LiveData<UiEvent<UiMessage>> getToastEvent() {
        return toastEvent;
    }

    @NonNull
    public LiveData<UiEvent<File>> getShareCompressedEvent() {
        return shareCompressedEvent;
    }

    @NonNull
    public LiveData<UiEvent<String>> getLaunchSaveCompressedDocumentEvent() {
        return launchSaveCompressedDocumentEvent;
    }

    public void selectCompressPreset(int preset) {
        CompressUiState current = compressState.getValue();
        if (compressBusy || (current != null && (current.compressing || current.saving))) {
            return;
        }
        int safePreset = Math.max(0, Math.min(2, preset));
        compressPreset.setValue(safePreset);
        if (lastCompressSourceUri != null
                && (lastCompressResult == null || lastCompressResult.presetIndex != safePreset)) {
            compressImageInternal(lastCompressSourceUri, safePreset, true, lastCompressSourceUri);
        }
    }

    public void compressImage(@NonNull String imageUriString) {
        if (compressBusy) {
            return;
        }
        Integer current = compressPreset.getValue();
        int presetIndex = current != null ? current : 0;
        String previousSourceUri = lastCompressSourceUri;
        lastCompressSourceUri = imageUriString;
        compressImageInternal(imageUriString, presetIndex, false, previousSourceUri);
    }

    private void compressImageInternal(
            @NonNull String imageUriString,
            int presetIndex,
            boolean autoRecompress,
            @Nullable String previousSourceUri
    ) {
        compressBusy = true;
        compressState.postValue(new CompressUiState(true, false, lastCompressResult));
        ImageCompressHelper.CompressPreset preset = ImageCompressHelper.CompressPreset.fromIndex(presetIndex);
        ioExecutor.execute(() -> {
            try {
                ImageCompressGateway.CompressResult result = imageCompressGateway.compress(imageUriString, preset);
                lastCompressResult = result;
                compressBusy = false;
                compressState.postValue(new CompressUiState(false, false, result));
                String sizeText = ImageCompressHelper.formatSize(result.compressedBytes);
                if (result.reachedTarget) {
                    toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(
                            R.string.tools_compress_done_toast,
                            sizeText
                    )));
                } else {
                    toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(
                            R.string.tools_compress_done_over_target_toast,
                            sizeText
                    )));
                }
            } catch (IOException ex) {
                lastCompressSourceUri = autoRecompress ? null : previousSourceUri;
                compressBusy = false;
                compressState.postValue(new CompressUiState(false, false, lastCompressResult));
                notifyCompressError(ex);
            }
        });
    }

    public void requestSaveCompressed(boolean useMediaStore) {
        if (compressBusy) {
            return;
        }
        ImageCompressGateway.CompressResult result = lastCompressResult;
        if (result == null || !result.outputFile.exists()) {
            toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.tools_compress_no_result_error)));
            return;
        }
        if (!useMediaStore) {
            launchSaveCompressedDocumentEvent.setValue(new UiEvent<>(result.fileName));
            return;
        }
        compressBusy = true;
        compressState.postValue(new CompressUiState(false, true, result));
        ioExecutor.execute(() -> {
            try {
                String savedLocation = imageCompressGateway.saveToGallery(result.outputFile, result.fileName);
                compressBusy = false;
                compressState.postValue(new CompressUiState(false, false, result));
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(
                        R.string.tools_compress_saved_gallery_toast,
                        savedLocation
                )));
            } catch (IOException ex) {
                compressBusy = false;
                compressState.postValue(new CompressUiState(false, false, result));
                notifyCompressError(ex);
            }
        });
    }

    public void saveCompressedToDocument(@NonNull String targetUriString) {
        if (compressBusy) {
            return;
        }
        ImageCompressGateway.CompressResult result = lastCompressResult;
        if (result == null || !result.outputFile.exists()) {
            toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.tools_compress_no_result_error)));
            return;
        }
        compressBusy = true;
        compressState.postValue(new CompressUiState(false, true, result));
        ioExecutor.execute(() -> {
            try {
                imageCompressGateway.saveToDocumentUri(result.outputFile, targetUriString);
                compressBusy = false;
                compressState.postValue(new CompressUiState(false, false, result));
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.tools_compress_saved_file_toast)));
            } catch (IOException ex) {
                compressBusy = false;
                compressState.postValue(new CompressUiState(false, false, result));
                notifyCompressError(ex);
            }
        });
    }

    public void requestShareCompressed() {
        if (compressBusy) {
            return;
        }
        ImageCompressGateway.CompressResult result = lastCompressResult;
        if (result == null || !result.outputFile.exists()) {
            toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.tools_compress_no_result_error)));
            return;
        }
        shareCompressedEvent.setValue(new UiEvent<>(result.outputFile));
    }

    private void notifyCompressError(@NonNull IOException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().trim() : "";
        if (!msg.isEmpty()) {
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_message_raw, msg)));
        } else {
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.tools_compress_error_fallback)));
        }
    }

    public void selectOcrSample(int sample) {
        int safeSample = sample == 1 ? 1 : 0;
        lastScannedText = null;
        lastSourceFileName = null;
        ocrState.setValue(new OcrUiState(null, null, scanningOcr));
        ocrSample.setValue(safeSample);
    }

    public void scanOcrImage(@NonNull String imageUriString) {
        if (scanningOcr) {
            return;
        }
        if (!cloudGateway.isLoggedIn()) {
            toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_error_login_required)));
            return;
        }
        scanningOcr = true;
        ocrState.postValue(new OcrUiState(lastScannedText, lastSourceFileName, true));
        networkExecutor.execute(() -> {
            try {
                ApiModels.OcrResponse res = cloudGateway.scanOcr(imageUriString);
                String text = res.getText() != null ? res.getText().trim() : "";
                if (text.isEmpty()) {
                    scanningOcr = false;
                    ocrState.postValue(new OcrUiState(lastScannedText, lastSourceFileName, false));
                    toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.tools_ocr_empty_result)));
                    return;
                }
                lastScannedText = text;
                lastSourceFileName = res.getFileName();
                scanningOcr = false;
                ocrState.postValue(new OcrUiState(lastScannedText, lastSourceFileName, false));
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.tools_ocr_scan_success)));
            } catch (CloudException ex) {
                scanningOcr = false;
                ocrState.postValue(new OcrUiState(lastScannedText, lastSourceFileName, false));
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_message_raw, ex.getMessage())));
            }
        });
    }

    public void notifyOcrCopied() {
        toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.tools_ocr_copied_toast)));
    }

    public void refreshNotesCount() {
        ioExecutor.execute(() -> totalNotesCount.postValue(repository.getTotalNotesCount()));
    }
}
