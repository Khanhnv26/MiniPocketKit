package com.pocketkit.backend.service;

import com.pocketkit.backend.dto.OcrResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    OcrResponseDto extractText(MultipartFile file);
}
