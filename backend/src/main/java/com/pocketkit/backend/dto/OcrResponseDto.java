package com.pocketkit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OcrResponseDto {
    private final String text;
    private final String language;
    private final String fileName;
    private final long sizeBytes;
}
