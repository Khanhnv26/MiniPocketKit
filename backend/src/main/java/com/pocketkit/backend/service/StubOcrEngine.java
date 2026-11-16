package com.pocketkit.backend.service;

import org.springframework.stereotype.Component;

@Component
public class StubOcrEngine implements OcrEngine {

    @Override
    public String extractText(byte[] imageBytes, String contentType, String originalFilename) {
        String safeName = originalFilename != null ? originalFilename : "image";
        return String.format(
                "Chưa gắn engine OCR thật trên server (đã nhận %s, %d bytes, %s).",
                safeName,
                imageBytes.length,
                contentType
        );
    }
}
