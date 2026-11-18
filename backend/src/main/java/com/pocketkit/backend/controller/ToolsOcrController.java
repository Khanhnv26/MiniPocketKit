package com.pocketkit.backend.controller;

import com.pocketkit.backend.dto.ApiResponse;
import com.pocketkit.backend.dto.OcrResponseDto;
import com.pocketkit.backend.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolsOcrController {

    private final OcrService ocrService;

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OcrResponseDto>> scanOcr(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(ocrService.extractText(file)));
    }
}
