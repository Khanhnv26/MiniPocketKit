package com.example.pocketkit.network.utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.network.exception.CloudException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public final class ImageUploadHelper {

    private static final int MAX_RAW_READ_BYTES = 15 * 1024 * 1024;
    private static final int PASSTHROUGH_MAX_BYTES = 4 * 1024 * 1024;
    private static final int MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DIMENSION_PX = 2048;

    private ImageUploadHelper() {
    }

    @NonNull
    public static MultipartBody.Part prepareImagePart(
            @NonNull ContentResolver contentResolver,
            @NonNull String imageUriString
    ) throws CloudException {
        Uri uri = Uri.parse(imageUriString);
        byte[] rawBytes = readBoundedBytes(contentResolver, uri);
        if (rawBytes.length == 0) {
            throw new CloudException(0, "Tệp ảnh trống hoặc không đọc được");
        }

        String normalizedMime = normalizeAllowedMime(contentResolver.getType(uri));
        byte[] uploadBytes;
        String uploadMime;
        String fileName;

        if (normalizedMime != null && rawBytes.length <= PASSTHROUGH_MAX_BYTES) {
            uploadBytes = rawBytes;
            uploadMime = normalizedMime;
            fileName = defaultFileNameForMime(normalizedMime);
        } else {
            uploadBytes = transcodeToJpeg(rawBytes);
            uploadMime = "image/jpeg";
            fileName = "image.jpg";
        }

        if (uploadBytes.length == 0 || uploadBytes.length > MAX_UPLOAD_BYTES) {
            throw new CloudException(0, "Ảnh vượt quá giới hạn 5MB sau khi nén");
        }

        RequestBody body = RequestBody.create(uploadBytes, MediaType.parse(uploadMime));
        return MultipartBody.Part.createFormData("file", fileName, body);
    }

    public static int computeInSampleSize(int width, int height, int maxDimension) {
        if (width <= 0 || height <= 0 || maxDimension <= 0) {
            return 1;
        }
        int sample = 1;
        while ((width / sample) > maxDimension || (height / sample) > maxDimension) {
            sample *= 2;
        }
        return sample;
    }

    @NonNull
    private static byte[] readBoundedBytes(
            @NonNull ContentResolver contentResolver,
            @NonNull Uri uri
    ) throws CloudException {
        try (InputStream input = contentResolver.openInputStream(uri)) {
            if (input == null) {
                throw new CloudException(0, "Không thể mở tệp ảnh đã chọn");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_RAW_READ_BYTES) {
                    throw new CloudException(0, "Ảnh gốc quá lớn (vượt quá 15MB)");
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (CloudException ex) {
            throw ex;
        } catch (IOException | SecurityException ex) {
            throw new CloudException(0, "Không thể đọc dữ liệu ảnh từ thiết bị");
        }
    }

    @NonNull
    private static byte[] transcodeToJpeg(@NonNull byte[] rawBytes) throws CloudException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new CloudException(0, "Định dạng ảnh không hợp lệ");
        }

        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        decodeOpts.inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX);
        Bitmap bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length, decodeOpts);
        if (bitmap == null) {
            throw new CloudException(0, "Không thể giải mã ảnh đã chọn");
        }

        try {
            byte[] highQuality = compressJpeg(bitmap, 85);
            if (highQuality.length <= MAX_UPLOAD_BYTES) {
                return highQuality;
            }
            return compressJpeg(bitmap, 70);
        } finally {
            bitmap.recycle();
        }
    }

    @NonNull
    private static byte[] compressJpeg(@NonNull Bitmap bitmap, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        return out.toByteArray();
    }

    @Nullable
    private static String normalizeAllowedMime(@Nullable String rawMime) {
        if (rawMime == null) {
            return null;
        }
        String lower = rawMime.trim().toLowerCase(Locale.ROOT);
        if ("image/jpeg".equals(lower) || "image/jpg".equals(lower)) {
            return "image/jpeg";
        }
        if ("image/png".equals(lower)) {
            return "image/png";
        }
        if ("image/webp".equals(lower)) {
            return "image/webp";
        }
        return null;
    }

    @NonNull
    private static String defaultFileNameForMime(@NonNull String mimeType) {
        if ("image/png".equals(mimeType)) {
            return "image.png";
        }
        if ("image/webp".equals(mimeType)) {
            return "image.webp";
        }
        return "image.jpg";
    }
}
