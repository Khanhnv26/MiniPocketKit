package com.pocketkit.backend.service;

import com.pocketkit.backend.dto.BackupDtos;
import com.pocketkit.backend.repository.UserBackupRepository;

public interface BackupService {

    BackupDtos.BackupSnapshotResponse saveBackup(Long userId, BackupDtos.BackupPayload payload);
    BackupDtos.BackupSnapshotResponse getLastestBackup(Long userId);

}
