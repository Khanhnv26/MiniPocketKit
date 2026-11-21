package com.example.pocketkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.network.exception.CloudException;
import com.example.pocketkit.network.gateway.CloudGateway;

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

    private void throwIfConfigured() throws CloudException {
        if (errorToThrow != null) {
            throw errorToThrow;
        }
    }
}
