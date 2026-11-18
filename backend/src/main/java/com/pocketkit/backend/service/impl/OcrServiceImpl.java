package com.pocketkit.backend.service.impl;

import com.pocketkit.backend.dto.OcrResponseDto;
import com.pocketkit.backend.service.OcrEngine;
import com.pocketkit.backend.service.OcrService;
import com.pocketkit.backend.utils.ImageFileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final OcrEngine ocrEngine;

    @Override
    public OcrResponseDto extractText(MultipartFile file) {
        ImageFileHelper.ValidatedImage image = ImageFileHelper.validateAndRead(file);
        String text = ocrEngine.extractText(image.getBytes(), image.getContentType(), image.getOriginalFilename());
        return OcrResponseDto.builder()
                .text(text)
                .language("vie+eng")
                .fileName(image.getOriginalFilename())
                .sizeBytes(image.getBytes().length)
                .build();
    }
}
