package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketkit.R;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;

import java.util.concurrent.Executor;

public class ToolsViewModel extends ViewModel {

    private final PocketKitRepository repository;
    private final Executor ioExecutor;

    private final MutableLiveData<Integer> compressPreset = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> ocrSample = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalNotesCount = new MutableLiveData<>(0);
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();

    public ToolsViewModel(@NonNull PocketKitRepository repository, @NonNull Executor ioExecutor) {
        this.repository = repository;
        this.ioExecutor = ioExecutor;
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
        ocrSample.setValue(safeSample);
    }

    public void notifyOcrCopied() {
        toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.tools_ocr_copied_toast)));
    }

    public void refreshNotesCount() {
        ioExecutor.execute(() -> totalNotesCount.postValue(repository.getTotalNotesCount()));
    }
}
