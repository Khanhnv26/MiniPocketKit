package com.example.pocketkit.tools;

import androidx.annotation.NonNull;

import java.util.Locale;

public final class ImageCompressHelper {

    public enum CompressPreset {
        UNDER_100_KB(0, 100 * 1024, 1280, 960, false, 90, 20, "pocketkit_100kb"),
        UNDER_500_KB(1, 500 * 1024, 2048, 1536, false, 92, 30, "pocketkit_500kb"),
        ID_PHOTO_3X4(2, 200 * 1024, 354, 472, true, 92, 50, "pocketkit_3x4");

        public final int index;
        public final int maxBytes;
        public final int targetWidth;
        public final int targetHeight;
        public final boolean exactCrop3x4;
        public final int maxQuality;
        public final int minQuality;
        public final String filePrefix;

        CompressPreset(
                int index,
                int maxBytes,
                int targetWidth,
                int targetHeight,
                boolean exactCrop3x4,
                int maxQuality,
                int minQuality,
                @NonNull String filePrefix
        ) {
            this.index = index;
            this.maxBytes = maxBytes;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            this.exactCrop3x4 = exactCrop3x4;
            this.maxQuality = maxQuality;
            this.minQuality = minQuality;
            this.filePrefix = filePrefix;
        }

        @NonNull
        public static CompressPreset fromIndex(int index) {
            if (index <= 0) {
                return UNDER_100_KB;
            }
            if (index == 1) {
                return UNDER_500_KB;
            }
            return ID_PHOTO_3X4;
        }
    }

    public interface QualityEncoder {
        @NonNull
        byte[] encode(int quality);
    }

    public static final class EncodedResult {
        @NonNull
        public final byte[] bytes;
        public final int quality;
        public final boolean reachedTarget;

        public EncodedResult(@NonNull byte[] bytes, int quality, boolean reachedTarget) {
            this.bytes = bytes;
            this.quality = quality;
            this.reachedTarget = reachedTarget;
        }
    }

    public static final class Size {
        public final int width;
        public final int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static final class CropRect {
        public final int left;
        public final int top;
        public final int width;
        public final int height;

        public CropRect(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }

    private ImageCompressHelper() {
    }

    public static int computeInSampleSize(int srcW, int srcH, int targetEdge, boolean cropToFill) {
        if (srcW <= 0 || srcH <= 0 || targetEdge <= 0) {
            return 1;
        }
        int sample = 1;
        if (cropToFill) {
            while ((srcW / (sample * 2)) >= targetEdge && (srcH / (sample * 2)) >= targetEdge) {
                sample *= 2;
            }
            while (Math.max(srcW, srcH) / sample > 4096) {
                sample *= 2;
            }
        } else {
            while ((srcW / (sample * 2)) >= targetEdge || (srcH / (sample * 2)) >= targetEdge) {
                sample *= 2;
            }
        }
        return sample;
    }

    @NonNull
    public static Size computeFitDimensions(int srcW, int srcH, int maxWidth, int maxHeight) {
        int maxEdge = Math.max(maxWidth, maxHeight);
        if (srcW <= 0 || srcH <= 0 || maxEdge <= 0) {
            return new Size(Math.max(1, srcW), Math.max(1, srcH));
        }
        int srcMax = Math.max(srcW, srcH);
        if (srcMax <= maxEdge) {
            return new Size(srcW, srcH);
        }
        double scale = (double) maxEdge / srcMax;
        return new Size(
                Math.max(1, (int) Math.round(srcW * scale)),
                Math.max(1, (int) Math.round(srcH * scale))
        );
    }

    @NonNull
    public static CropRect computeCenterCropRect(int srcW, int srcH, int aspectW, int aspectH) {
        if (srcW <= 0 || srcH <= 0 || aspectW <= 0 || aspectH <= 0) {
            return new CropRect(0, 0, Math.max(1, srcW), Math.max(1, srcH));
        }
        if ((long) srcW * aspectH > (long) srcH * aspectW) {
            int cropH = srcH;
            int cropW = Math.min(srcW, Math.max(1, (int) Math.round((double) srcH * aspectW / aspectH)));
            int left = (srcW - cropW) / 2;
            return new CropRect(left, 0, cropW, cropH);
        }
        int cropW = srcW;
        int cropH = Math.min(srcH, Math.max(1, (int) Math.round((double) srcW * aspectH / aspectW)));
        int top = (srcH - cropH) / 2;
        return new CropRect(0, top, cropW, cropH);
    }

    @NonNull
    public static EncodedResult pickBestQuality(
            int minQuality,
            int maxQuality,
            int maxBytes,
            @NonNull QualityEncoder encoder
    ) {
        int lo = Math.max(1, Math.min(minQuality, maxQuality));
        int hi = Math.min(100, Math.max(minQuality, maxQuality));

        byte[] highBytes = encoder.encode(hi);
        if (highBytes.length <= maxBytes) {
            return new EncodedResult(highBytes, hi, true);
        }
        if (lo == hi) {
            return new EncodedResult(highBytes, hi, false);
        }

        byte[] lowBytes = encoder.encode(lo);
        if (lowBytes.length > maxBytes) {
            return new EncodedResult(lowBytes, lo, false);
        }

        byte[] bestBytes = lowBytes;
        int bestQuality = lo;
        int left = lo + 1;
        int right = hi - 1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            byte[] candidate = encoder.encode(mid);
            if (candidate.length <= maxBytes) {
                bestBytes = candidate;
                bestQuality = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return new EncodedResult(bestBytes, bestQuality, true);
    }

    @NonNull
    public static String formatSize(long bytes) {
        long safeBytes = Math.max(0L, bytes);
        if (safeBytes < 1024L) {
            return safeBytes + " B";
        }
        if (safeBytes < 1024L * 1024L) {
            long kb = Math.max(1L, Math.round(safeBytes / 1024.0));
            return kb + " KB";
        }
        return String.format(Locale.US, "%.2f MB", safeBytes / (1024.0 * 1024.0));
    }

    public static int computeSavingsPercent(long originalBytes, long compressedBytes) {
        if (originalBytes <= 0L || compressedBytes >= originalBytes) {
            return 0;
        }
        int percent = (int) Math.round((originalBytes - compressedBytes) * 100.0 / originalBytes);
        return Math.min(99, Math.max(0, percent));
    }

    @NonNull
    public static String buildOutputFileName(@NonNull CompressPreset preset, long timestampMillis) {
        return preset.filePrefix + "_" + timestampMillis + ".jpg";
    }
}
