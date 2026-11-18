package com.pocketkit.backend.service.impl;

import com.pocketkit.backend.dto.OcrResponseDto;
import com.pocketkit.backend.service.OcrEngine;
import com.pocketkit.backend.service.OcrService;
import com.pocketkit.backend.utils.ImageFileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

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
