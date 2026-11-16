package com.pocketkit.backend.service;

import com.pocketkit.backend.common.JsonCodec;
import com.pocketkit.backend.dto.BackupDtos;
import com.pocketkit.backend.entity.UserBackupEntity;
import com.pocketkit.backend.repository.UserBackupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final UserBackupRepository backupRepository;
    private final JsonCodec jsonCodec;

    public BackupDtos.BackupSnapshotResponse saveBackup(Long userId, BackupDtos.BackupPayload payload) {
        long now = System.currentTimeMillis();
        String json = jsonCodec.writeValue(payload);

        UserBackupEntity entity = backupRepository.findByUserId(userId)
                .orElseGet(() -> UserBackupEntity.builder().userId(userId).build());
        entity.setPayloadJson(json);
        entity.setBackedUpAtEpoch(now);
        backupRepository.save(entity);

        return BackupDtos.BackupSnapshotResponse.builder()
                .backedUpAtEpoch(now)
                .payload(payload)
                .build();
    }

    public BackupDtos.BackupSnapshotResponse getLatestBackup(Long userId) {
        UserBackupEntity entity = backupRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có bản sao lưu nào"));
        BackupDtos.BackupPayload payload = jsonCodec.readValue(
                entity.getPayloadJson(),
                BackupDtos.BackupPayload.class
        );
        return BackupDtos.BackupSnapshotResponse.builder()
                .backedUpAtEpoch(entity.getBackedUpAtEpoch())
                .payload(payload)
                .build();
    }
}
