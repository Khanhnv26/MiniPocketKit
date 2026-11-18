package com.pocketkit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class OcrResponseDto {
    private final String text;
    private final String language;
    private final String fileName;
    private final long sizeBytes;
}
