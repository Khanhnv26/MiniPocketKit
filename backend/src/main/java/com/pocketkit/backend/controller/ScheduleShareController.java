package com.pocketkit.backend.controller;

import com.pocketkit.backend.dto.ApiResponse;
import com.pocketkit.backend.dto.ScheduleShareDtos;
import com.pocketkit.backend.service.ScheduleShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
