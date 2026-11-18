package com.pocketkit.backend.service;

import com.pocketkit.backend.dto.BackupDtos;

public interface BackupService {

    BackupDtos.BackupSnapshotResponse saveBackup(Long userId, BackupDtos.BackupPayload payload);

    BackupDtos.BackupSnapshotResponse getLatestBackup(Long userId);
}
