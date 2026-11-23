package com.example.pocketkit.network.gateway;

import androidx.annotation.NonNull;

import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.network.exception.CloudException;

public interface CloudGateway {

    boolean isLoggedIn();

    @NonNull
    String getEmail();

    @NonNull
    String getFullName();

    long getLastBackupEpoch();

    @NonNull
    ApiModels.AuthResponse register(
            @NonNull String fullName,
            @NonNull String email,
            @NonNull String password
    ) throws CloudException;

    @NonNull
    ApiModels.AuthResponse login(
            @NonNull String email,
            @NonNull String password
    ) throws CloudException;

    void logout();

    @NonNull
    ApiModels.BackupSnapshotResponse saveBackup(@NonNull ApiModels.BackupPayload payload) throws CloudException;

    @NonNull
    ApiModels.BackupSnapshotResponse getLatestBackup() throws CloudException;

    @NonNull
    ApiModels.OcrResponse scanOcr(@NonNull String imageUriString) throws CloudException;

    @NonNull
    ApiModels.NoteSummarizeResponse summarizeNote(
            @NonNull String title,
            @NonNull String content
    ) throws CloudException;

    @NonNull
    ApiModels.NoteSummarizeResponse summarizeNoteImage(@NonNull String imageUriString) throws CloudException;
}
