package com.example.pocketkit.tools;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

public interface ImageCompressGateway {

    final class CompressResult {
        @NonNull
        public final File outputFile;
        @NonNull
        public final String fileName;
        public final long originalBytes;
        public final long compressedBytes;
        public final int width;
        public final int height;
        public final int quality;
        public final boolean reachedTarget;
        public final int presetIndex;

        public CompressResult(
                @NonNull File outputFile,
                @NonNull String fileName,
                long originalBytes,
                long compressedBytes,
                int width,
                int height,
                int quality,
                boolean reachedTarget,
                int presetIndex
        ) {
            this.outputFile = outputFile;
            this.fileName = fileName;
            this.originalBytes = originalBytes;
            this.compressedBytes = compressedBytes;
            this.width = width;
            this.height = height;
            this.quality = quality;
            this.reachedTarget = reachedTarget;
            this.presetIndex = presetIndex;
        }
    }

    @NonNull
    CompressResult compress(@NonNull String imageUriString, @NonNull ImageCompressHelper.CompressPreset preset)
            throws IOException;

    @NonNull
    String saveToGallery(@NonNull File sourceFile, @NonNull String fileName) throws IOException;

    void saveToDocumentUri(@NonNull File sourceFile, @NonNull String targetUriString) throws IOException;
}
