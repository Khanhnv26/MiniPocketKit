package com.pocketkit.backend.service.impl;

import com.pocketkit.backend.service.OcrEngine;
import org.springframework.stereotype.Service;

@Service
public class OcrEngineImpl implements OcrEngine {

    @Override
    public String extractText(byte[] imageBytes, String contentType, String originalFileName) {
        String safeName = originalFileName != null ? originalFileName : "image";
        return String.format(
                "Chưa gắn engine OCR thật trên server (đã nhận %s, %d bytes, %s).",
                safeName,
                imageBytes.length,
                contentType
        );
    }
}
