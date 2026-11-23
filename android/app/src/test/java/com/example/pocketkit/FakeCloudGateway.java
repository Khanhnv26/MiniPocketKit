package com.example.pocketkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.network.exception.CloudException;
import com.example.pocketkit.network.gateway.CloudGateway;

import java.util.Arrays;
import java.util.Collections;

final class FakeCloudGateway implements CloudGateway {

    boolean loggedIn = false;
    String email = "";
    String fullName = "";
    long lastBackupEpoch = 0L;
    @Nullable
    ApiModels.BackupSnapshotResponse latestBackup;
    @Nullable
    ApiModels.BackupPayload lastSavedPayload;
    @Nullable
    ApiModels.OcrResponse ocrResponse;
    @Nullable
    ApiModels.NoteSummarizeResponse summarizeResponse;
    @Nullable
    String lastOcrUri;
    @Nullable
    String lastSummarizedContent;
    @Nullable
    String lastSummarizedImageUri;
    @Nullable
    CloudException errorToThrow;

    @Override
    public boolean isLoggedIn() {
        return loggedIn;
    }

    @NonNull
    @Override
    public String getEmail() {
        return email;
    }

    @NonNull
    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getLastBackupEpoch() {
        return lastBackupEpoch;
    }

    @NonNull
    @Override
    public ApiModels.AuthResponse register(
            @NonNull String fullName,
            @NonNull String email,
            @NonNull String password
    ) throws CloudException {
        throwIfConfigured();
        this.loggedIn = true;
        this.fullName = fullName;
        this.email = email;
        return new ApiModels.AuthResponse(
                "fake-jwt",
                604800000L,
                new ApiModels.UserSummary(1L, email, fullName)
        );
    }

    @NonNull
    @Override
    public ApiModels.AuthResponse login(
            @NonNull String email,
            @NonNull String password
    ) throws CloudException {
        throwIfConfigured();
        this.loggedIn = true;
        this.email = email;
        if (this.fullName.isEmpty()) {
            this.fullName = "Sinh viên Test";
        }
        return new ApiModels.AuthResponse(
                "fake-jwt",
                604800000L,
                new ApiModels.UserSummary(1L, email, fullName)
        );
    }

    @Override
    public void logout() {
        this.loggedIn = false;
        this.email = "";
        this.fullName = "";
        this.lastBackupEpoch = 0L;
    }

    @NonNull
    @Override
    public ApiModels.BackupSnapshotResponse saveBackup(@NonNull ApiModels.BackupPayload payload) throws CloudException {
        throwIfConfigured();
        this.lastSavedPayload = payload;
        this.lastBackupEpoch = 1795000000000L;
        ApiModels.BackupSnapshotResponse response = new ApiModels.BackupSnapshotResponse(lastBackupEpoch, payload);
        this.latestBackup = response;
        return response;
    }

    @NonNull
    @Override
    public ApiModels.BackupSnapshotResponse getLatestBackup() throws CloudException {
        throwIfConfigured();
        if (latestBackup == null) {
            throw new CloudException(404, "Chưa có bản sao lưu");
        }
        this.lastBackupEpoch = latestBackup.getBackedUpAtEpoch();
        return latestBackup;
    }

    @NonNull
    @Override
    public ApiModels.OcrResponse scanOcr(@NonNull String imageUriString) throws CloudException {
        throwIfConfigured();
        this.lastOcrUri = imageUriString;
        if (ocrResponse != null) {
            return ocrResponse;
        }
        return new ApiModels.OcrResponse("Nội dung OCR mẫu", "vie+eng", "slide.png", 1024L);
    }

    @NonNull
    @Override
    public ApiModels.NoteSummarizeResponse summarizeNote(
            @NonNull String title,
            @NonNull String content
    ) throws CloudException {
        throwIfConfigured();
        this.lastSummarizedContent = content;
        if (summarizeResponse != null) {
            return summarizeResponse;
        }
        return new ApiModels.NoteSummarizeResponse(
                title.isEmpty() ? "Tiêu đề AI" : title,
                "Tóm tắt ngắn gọn.",
                "- Ý đã trình bày lại",
                Arrays.asList("Ý chính 1", "Ý chính 2"),
                true
        );
    }

    @NonNull
    @Override
    public ApiModels.NoteSummarizeResponse summarizeNoteImage(@NonNull String imageUriString) throws CloudException {
        throwIfConfigured();
        this.lastSummarizedImageUri = imageUriString;
        if (summarizeResponse != null) {
            return summarizeResponse;
        }
        return new ApiModels.NoteSummarizeResponse(
                "Ghi chú từ ảnh",
                "Tóm tắt nội dung slide.",
                "1. Ý trong ảnh",
                Collections.singletonList("Điểm quan trọng"),
                true
        );
    }

    private void throwIfConfigured() throws CloudException {
        if (errorToThrow != null) {
            throw errorToThrow;
        }
    }
}
