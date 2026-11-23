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
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;

import java.util.concurrent.Executor;

public class ToolsViewModel extends ViewModel {

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
    private final Executor ioExecutor;
    private final Executor networkExecutor;

    private final MutableLiveData<Integer> compressPreset = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> ocrSample = new MutableLiveData<>(0);
    private final MutableLiveData<OcrUiState> ocrState = new MutableLiveData<>(new OcrUiState(null, null, false));
    private final MutableLiveData<Integer> totalNotesCount = new MutableLiveData<>(0);
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();

    private String lastScannedText;
    private String lastSourceFileName;
    private volatile boolean scanningOcr = false;

    public ToolsViewModel(
            @NonNull PocketKitRepository repository,
            @NonNull CloudGateway cloudGateway,
            @NonNull Executor ioExecutor,
            @NonNull Executor networkExecutor
    ) {
        this.repository = repository;
        this.cloudGateway = cloudGateway;
        this.ioExecutor = ioExecutor;
        this.networkExecutor = networkExecutor;
    }

    @NonNull
    public LiveData<Integer> getCompressPreset() {
        return compressPreset;
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

    public void selectCompressPreset(int preset) {
        int safePreset = Math.max(0, Math.min(2, preset));
        compressPreset.setValue(safePreset);
    }

    public void triggerCompressReadyMessage() {
        Integer current = compressPreset.getValue();
        int preset = current != null ? current : 0;
        int presetNameRes;
        if (preset == 0) {
            presetNameRes = R.string.tools_compress_preset_name_100;
        } else if (preset == 1) {
            presetNameRes = R.string.tools_compress_preset_name_500;
        } else {
            presetNameRes = R.string.tools_compress_preset_name_3x4;
        }
        toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(
                R.string.tools_compress_ready_toast,
                UiMessage.strRef(presetNameRes)
        )));
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
