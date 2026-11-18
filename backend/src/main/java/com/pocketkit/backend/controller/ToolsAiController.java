package com.pocketkit.backend.controller;

import com.pocketkit.backend.dto.AiDtos;
import com.pocketkit.backend.dto.ApiResponse;
import com.pocketkit.backend.service.AiToolsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolsAiController {

    private final AiToolsService aiToolsService;

    @PostMapping("/notes/summarize")
    public ResponseEntity<ApiResponse<AiDtos.NoteSummarizeResponse>> summarizeNote(
            @Valid @RequestBody AiDtos.NoteSummarizeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(aiToolsService.summarizeNote(request)));
    }

    @PostMapping(value = "/notes/summarize-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AiDtos.NoteSummarizeResponse>> summarizeNoteImage(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(aiToolsService.summarizeNoteImage(file)));
    }

    @PostMapping(value = "/schedules/parse-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AiDtos.ScheduleScanResponse>> parseScheduleImage(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(aiToolsService.parseScheduleImage(file)));
    }
}
