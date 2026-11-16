package com.pocketkit.backend.service;

import com.pocketkit.backend.dto.OcrResponseDto;
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
public class OcrService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final OcrEngine ocrEngine;

    public OcrResponseDto extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp ảnh hợp lệ");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.US))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đọc dữ liệu ảnh tải lên");
        }
        String text = ocrEngine.extractText(bytes, contentType, file.getOriginalFilename());
        return new OcrResponseDto(text, "vie+eng", file.getOriginalFilename(), bytes.length);
    }
}
