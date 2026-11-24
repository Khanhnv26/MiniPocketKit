package com.example.pocketkit.tools;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class AndroidImageCompressGateway implements ImageCompressGateway {

    private static final long MAX_INPUT_BYTES = 25L * 1024 * 1024;
    private static final int MIN_DOWNSCALE_EDGE = 320;
    private static final int MAX_DOWNSCALE_STEPS = 5;
    private static final double DOWNSCALE_FACTOR = 0.85;
    private static final String COMPRESSED_CACHE_DIR = "compressed";
    private static final String GALLERY_ALBUM = "PocketKit";

    private final Context appContext;

    public AndroidImageCompressGateway(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    public CompressResult compress(
            @NonNull String imageUriString,
            @NonNull ImageCompressHelper.CompressPreset preset
    ) throws IOException {
        ContentResolver resolver = appContext.getContentResolver();
        Uri uri = Uri.parse(imageUriString);
        long originalBytes = resolveInputSize(resolver, uri);
        if (originalBytes <= 0L) {
            throw new IOException("Tệp ảnh trống hoặc không đọc được");
        }
        if (originalBytes > MAX_INPUT_BYTES) {
            throw new IOException("Ảnh gốc vượt quá giới hạn 25MB");
        }

        int orientation = readExifOrientation(resolver, uri);
        BitmapFactory.Options bounds = decodeBounds(resolver, uri);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Định dạng ảnh không hợp lệ (hãy chọn JPEG, PNG hoặc WebP)");
        }

        boolean swapAxes = swapsDimensions(orientation);
        int orientedW = swapAxes ? bounds.outHeight : bounds.outWidth;
        int orientedH = swapAxes ? bounds.outWidth : bounds.outHeight;
        int targetEdge = Math.max(preset.targetWidth, preset.targetHeight);
        int sampleSize = ImageCompressHelper.computeInSampleSize(
                orientedW,
                orientedH,
                targetEdge,
                preset.exactCrop3x4
        );

        Bitmap decoded = decodeSubsampled(resolver, uri, sampleSize);
        Bitmap oriented = applyExifOrientation(decoded, orientation);
        Bitmap working;
        try {
            if (preset.exactCrop3x4) {
                ImageCompressHelper.CropRect cropRect = ImageCompressHelper.computeCenterCropRect(
                        oriented.getWidth(),
                        oriented.getHeight(),
                        3,
                        4
                );
                Rect srcRect = new Rect(
                        cropRect.left,
                        cropRect.top,
                        cropRect.left + cropRect.width,
                        cropRect.top + cropRect.height
                );
                working = renderWhiteBackedBitmap(oriented, srcRect, preset.targetWidth, preset.targetHeight);
            } else {
                ImageCompressHelper.Size fit = ImageCompressHelper.computeFitDimensions(
                        oriented.getWidth(),
                        oriented.getHeight(),
                        preset.targetWidth,
                        preset.targetHeight
                );
                working = renderWhiteBackedBitmap(oriented, null, fit.width, fit.height);
            }
        } finally {
            oriented.recycle();
        }

        ImageCompressHelper.EncodedResult encoded;
        int finalWidth;
        int finalHeight;
        try {
            encoded = encodeWithPreset(working, preset);
            if (!encoded.reachedTarget && !preset.exactCrop3x4) {
                for (int step = 0; step < MAX_DOWNSCALE_STEPS; step++) {
                    int currentMax = Math.max(working.getWidth(), working.getHeight());
                    if (currentMax <= MIN_DOWNSCALE_EDGE) {
                        break;
                    }
                    int nextMax = Math.max(MIN_DOWNSCALE_EDGE, (int) Math.round(currentMax * DOWNSCALE_FACTOR));
                    if (nextMax >= currentMax) {
                        break;
                    }
                    ImageCompressHelper.Size nextSize = ImageCompressHelper.computeFitDimensions(
                            working.getWidth(),
                            working.getHeight(),
                            nextMax,
                            nextMax
                    );
                    Bitmap downscaled = renderWhiteBackedBitmap(working, null, nextSize.width, nextSize.height);
                    working.recycle();
                    working = downscaled;
                    encoded = encodeWithPreset(working, preset);
                    if (encoded.reachedTarget) {
                        break;
                    }
                }
            }
            finalWidth = working.getWidth();
            finalHeight = working.getHeight();
        } finally {
            working.recycle();
        }

        File outDir = prepareCleanCacheDir();
        String fileName = ImageCompressHelper.buildOutputFileName(preset, System.currentTimeMillis());
        File outFile = new File(outDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(encoded.bytes);
        }

        return new CompressResult(
                outFile,
                fileName,
                originalBytes,
                encoded.bytes.length,
                finalWidth,
                finalHeight,
                encoded.quality,
                encoded.reachedTarget,
                preset.index
        );
    }

    @NonNull
    @Override
    public String saveToGallery(@NonNull File sourceFile, @NonNull String fileName) throws IOException {
        if (!sourceFile.exists()) {
            throw new IOException("Không tìm thấy tệp ảnh đã nén");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new IOException("Hãy chọn thư mục lưu tệp");
        }
        ContentResolver resolver = appContext.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + GALLERY_ALBUM);
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri inserted = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (inserted == null) {
            throw new IOException("Không thể tạo tệp trong Thư viện ảnh");
        }

        boolean completed = false;
        try {
            try (InputStream is = new FileInputStream(sourceFile);
                 OutputStream os = resolver.openOutputStream(inserted)) {
                if (os == null) {
                    throw new IOException("Không thể mở luồng ghi vào Thư viện ảnh");
                }
                copyStream(is, os);
            }
            ContentValues published = new ContentValues();
            published.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(inserted, published, null, null);
            completed = true;
            return Environment.DIRECTORY_PICTURES + "/" + GALLERY_ALBUM + "/" + fileName;
        } finally {
            if (!completed) {
                resolver.delete(inserted, null, null);
            }
        }
    }

    @Override
    public void saveToDocumentUri(@NonNull File sourceFile, @NonNull String targetUriString) throws IOException {
        if (!sourceFile.exists()) {
            throw new IOException("Không tìm thấy tệp ảnh đã nén");
        }
        Uri targetUri = Uri.parse(targetUriString);
        try (InputStream is = new FileInputStream(sourceFile);
             OutputStream os = appContext.getContentResolver().openOutputStream(targetUri)) {
            if (os == null) {
                throw new IOException("Không thể ghi vào tệp đích");
            }
            copyStream(is, os);
        }
    }

    private long resolveInputSize(@NonNull ContentResolver resolver, @NonNull Uri uri) throws IOException {
        try (Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0 && !cursor.isNull(idx)) {
                    long reported = cursor.getLong(idx);
                    if (reported > 0L) {
                        return reported;
                    }
                }
            }
        } catch (SecurityException ex) {
            throw new IOException("Không có quyền đọc tệp ảnh đã chọn", ex);
        }

        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                throw new IOException("Không thể mở tệp ảnh đã chọn");
            }
            byte[] buffer = new byte[8192];
            long total = 0L;
            int read;
            while ((read = is.read(buffer)) != -1) {
                total += read;
                if (total > MAX_INPUT_BYTES) {
                    return total;
                }
            }
            return total;
        } catch (SecurityException ex) {
            throw new IOException("Không có quyền đọc tệp ảnh đã chọn", ex);
        }
    }

    private int readExifOrientation(@NonNull ContentResolver resolver, @NonNull Uri uri) {
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                return ExifInterface.ORIENTATION_NORMAL;
            }
            ExifInterface exif = new ExifInterface(is);
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ignored) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    @NonNull
    private BitmapFactory.Options decodeBounds(@NonNull ContentResolver resolver, @NonNull Uri uri)
            throws IOException {
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                throw new IOException("Không thể mở tệp ảnh đã chọn");
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, bounds);
            return bounds;
        } catch (SecurityException ex) {
            throw new IOException("Không có quyền đọc tệp ảnh đã chọn", ex);
        }
    }

    @NonNull
    private Bitmap decodeSubsampled(
            @NonNull ContentResolver resolver,
            @NonNull Uri uri,
            int sampleSize
    ) throws IOException {
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                throw new IOException("Không thể mở tệp ảnh đã chọn");
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, sampleSize);
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            if (bitmap == null) {
                throw new IOException("Không thể giải mã ảnh đã chọn");
            }
            return bitmap;
        } catch (SecurityException ex) {
            throw new IOException("Không có quyền đọc tệp ảnh đã chọn", ex);
        }
    }

    private static boolean swapsDimensions(int orientation) {
        return orientation == ExifInterface.ORIENTATION_ROTATE_90
                || orientation == ExifInterface.ORIENTATION_ROTATE_270
                || orientation == ExifInterface.ORIENTATION_TRANSPOSE
                || orientation == ExifInterface.ORIENTATION_TRANSVERSE;
    }

    @NonNull
    private static Bitmap applyExifOrientation(@NonNull Bitmap source, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(270f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270f);
                break;
            default:
                return source;
        }
        try {
            Bitmap transformed = Bitmap.createBitmap(
                    source,
                    0,
                    0,
                    source.getWidth(),
                    source.getHeight(),
                    matrix,
                    true
            );
            if (transformed != source) {
                source.recycle();
            }
            return transformed;
        } catch (OutOfMemoryError ex) {
            return source;
        }
    }

    @NonNull
    private static Bitmap renderWhiteBackedBitmap(
            @NonNull Bitmap source,
            @Nullable Rect srcRect,
            int dstWidth,
            int dstHeight
    ) {
        Rect sourceBounds = srcRect != null ? srcRect : new Rect(0, 0, source.getWidth(), source.getHeight());
        Rect dstBounds = new Rect(0, 0, Math.max(1, dstWidth), Math.max(1, dstHeight));
        Bitmap output = Bitmap.createBitmap(dstBounds.width(), dstBounds.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(source, sourceBounds, dstBounds, paint);
        return output;
    }

    @NonNull
    private static ImageCompressHelper.EncodedResult encodeWithPreset(
            @NonNull Bitmap bitmap,
            @NonNull ImageCompressHelper.CompressPreset preset
    ) {
        return ImageCompressHelper.pickBestQuality(
                preset.minQuality,
                preset.maxQuality,
                preset.maxBytes,
                quality -> {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                    return out.toByteArray();
                }
        );
    }

    @NonNull
    private File prepareCleanCacheDir() throws IOException {
        File dir = new File(appContext.getCacheDir(), COMPRESSED_CACHE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Không thể tạo thư mục lưu tạm ảnh nén");
        }
        File[] existing = dir.listFiles();
        if (existing != null) {
            for (File file : existing) {
                if (file != null && file.isFile()) {
                    file.delete();
                }
            }
        }
        return dir;
    }

    private static void copyStream(@NonNull InputStream is, @NonNull OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
        os.flush();
    }
}
