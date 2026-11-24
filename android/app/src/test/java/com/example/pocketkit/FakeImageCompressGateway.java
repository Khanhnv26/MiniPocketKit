package com.example.pocketkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.tools.ImageCompressGateway;
import com.example.pocketkit.tools.ImageCompressHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class FakeImageCompressGateway implements ImageCompressGateway {

    @NonNull
    File outputFile = new File("fake_compressed.jpg");
    long originalBytes = 3_400_000L;
    long compressedBytes = 94_000L;
    boolean reachedTarget = true;
    @Nullable
    IOException compressErrorToThrow;
    @Nullable
    IOException saveErrorToThrow;
    final List<ImageCompressHelper.CompressPreset> compressedPresets = new ArrayList<>();
    @Nullable
    String lastImageUri;
    @Nullable
    String savedGalleryFileName;
    @Nullable
    String savedDocumentUri;

    @NonNull
    @Override
    public CompressResult compress(
            @NonNull String imageUriString,
            @NonNull ImageCompressHelper.CompressPreset preset
    ) throws IOException {
        if (compressErrorToThrow != null) {
            throw compressErrorToThrow;
        }
        lastImageUri = imageUriString;
        compressedPresets.add(preset);
        return new CompressResult(
                outputFile,
                ImageCompressHelper.buildOutputFileName(preset, 1_700_000_000_000L),
                originalBytes,
                compressedBytes,
                preset.targetWidth,
                preset.targetHeight,
                preset.maxQuality,
                reachedTarget,
                preset.index
        );
    }

    @NonNull
    @Override
    public String saveToGallery(@NonNull File sourceFile, @NonNull String fileName) throws IOException {
        if (saveErrorToThrow != null) {
            throw saveErrorToThrow;
        }
        savedGalleryFileName = fileName;
        return "Pictures/PocketKit/" + fileName;
    }

    @Override
    public void saveToDocumentUri(@NonNull File sourceFile, @NonNull String targetUriString) throws IOException {
        if (saveErrorToThrow != null) {
            throw saveErrorToThrow;
        }
        savedDocumentUri = targetUriString;
    }
}
