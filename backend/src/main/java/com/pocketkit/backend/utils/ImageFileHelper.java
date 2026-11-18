package com.pocketkit.backend.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public final class ImageFileHelper {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private ImageFileHelper() {
    }

    public static ValidatedImage validateAndRead(MultipartFile file) {
        String normalizedContentType = getNormalizedContentType(file);

        try {
            return ValidatedImage.builder()
                    .bytes(file.getBytes())
                    .contentType(normalizedContentType)
                    .originalFilename(file.getOriginalFilename())
                    .build();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đọc dữ liệu ảnh tải lên");
        }
    }

    private static String getNormalizedContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp ảnh hợp lệ");
        }

        String contentType = file.getContentType();
        String normalizedContentType = contentType != null ? contentType.toLowerCase(Locale.ROOT) : null;

        if (normalizedContentType == null || !ALLOWED_TYPES.contains(normalizedContentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP");
        }
        return normalizedContentType;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ValidatedImage {
        private final byte[] bytes;
        private final String contentType;
        private final String originalFilename;
    }
}
