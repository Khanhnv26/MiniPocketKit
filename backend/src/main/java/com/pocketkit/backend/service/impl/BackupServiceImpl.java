package com.pocketkit.backend.service.impl;

import com.pocketkit.backend.dto.BackupDtos;
import com.pocketkit.backend.entity.UserBackupEntity;
import com.pocketkit.backend.repository.UserBackupRepository;
import com.pocketkit.backend.service.BackupService;
import com.pocketkit.backend.utils.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final UserBackupRepository userBackupRepository;
    private final JsonCodec jsonCodec;

    @Override
    @Transactional
    public BackupDtos.BackupSnapshotResponse saveBackup(Long userId, BackupDtos.BackupPayload payload) {
        long now = System.currentTimeMillis();
        String json = jsonCodec.writeValue(payload);

        UserBackupEntity entity = userBackupRepository.findByUserId(userId)
                .orElseGet(() -> UserBackupEntity.builder()
                        .userId(userId)
                        .build());

        entity.setPayloadJson(json);
        entity.setBackedUpAtEpoch(now);

        userBackupRepository.save(entity);

        return BackupDtos.BackupSnapshotResponse.builder()
                .backedUpAtEpoch(now)
                .payload(payload)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BackupDtos.BackupSnapshotResponse getLatestBackup(Long userId) {
        UserBackupEntity entity = userBackupRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản sao lưu cho người dùng"));

        BackupDtos.BackupPayload payload = jsonCodec.readValue(entity.getPayloadJson(), BackupDtos.BackupPayload.class);

        return BackupDtos.BackupSnapshotResponse.builder()
                .backedUpAtEpoch(entity.getBackedUpAtEpoch())
                .payload(payload)
                .build();
    }
}
