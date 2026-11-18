package com.pocketkit.backend.service.impl;

import com.pocketkit.backend.service.OcrEngine;
import com.pocketkit.backend.utils.AiClient;
import com.pocketkit.backend.utils.ImageFileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OcrEngineImpl implements OcrEngine {

    private final AiClient aiClient;

    private static final String OCR_PROMPT =
            "Hãy trích xuất chính xác toàn bộ văn bản xuất hiện trong ảnh này (hỗ trợ tiếng Việt có dấu và tiếng Anh). "
                    + "Giữ nguyên bố cục dòng/đoạn quan trọng. Chỉ trả về đúng văn bản trích xuất, không thêm lời giải thích.";

    @Override
    public String extractText(byte[] imageBytes, String contentType, String originalFileName) {

        String safeName = originalFileName != null ? originalFileName : "image";

        if(!aiClient.isConfigured()) {
            return String.format(
                    "Chưa gắn key trên server (đã nhận %s, %d bytes, %s).",
                    safeName,
                    imageBytes.length,
                    contentType
            );
        }

        return aiClient.generateText(
                OCR_PROMPT,
                new ImageFileHelper.ValidatedImage(imageBytes, contentType, safeName)
        );

    }
}
