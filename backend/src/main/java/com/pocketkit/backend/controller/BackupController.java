package com.pocketkit.backend.controller;

import com.pocketkit.backend.dto.ApiResponse;
import com.pocketkit.backend.dto.BackupDtos;
import com.pocketkit.backend.service.BackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @PostMapping
    public ResponseEntity<ApiResponse<BackupDtos.BackupSnapshotResponse>> saveBackup(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BackupDtos.BackupPayload payload) {
        return ResponseEntity.ok(ApiResponse.ok("Sao lưu thành công", backupService.saveBackup(userId, payload)));
    }

    @GetMapping({ "", "/latest" })
    public ResponseEntity<ApiResponse<BackupDtos.BackupSnapshotResponse>> getLatestBackup(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(backupService.getLatestBackup(userId)));
    }
}
