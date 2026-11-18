package com.pocketkit.backend.controller;

import com.pocketkit.backend.dto.ApiResponse;
import com.pocketkit.backend.dto.ScheduleShareDtos;
import com.pocketkit.backend.service.ScheduleShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules/share")
@RequiredArgsConstructor
public class ScheduleShareController {

    private final ScheduleShareService scheduleShareService;

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleShareDtos.CreateShareResponse>> createShare(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ScheduleShareDtos.CreateShareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(scheduleShareService.createShare(userId, request)));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<ScheduleShareDtos.ShareDetailResponse>> getShare(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleShareService.getByCode(code)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleShareDtos.ShareSummaryResponse>>> listMyShares(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleShareService.listMyShares(userId)));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> deleteShare(@AuthenticationPrincipal Long userId, @PathVariable String code) {
        scheduleShareService.deleteShare(userId, code);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá mã chia sẻ", null));
    }
}
