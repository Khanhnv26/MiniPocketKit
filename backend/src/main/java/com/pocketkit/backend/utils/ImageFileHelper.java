package com.pocketkit.backend.utils;

import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;

@NoArgsConstructor
public class ImageFileHelper {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/jpg");

    public static ValidatedImage validateAndRead(MultipartFile file) throws Exception {
        String normalizedContentType = getNormalizedContentType(file);

        try {
            return ValidatedImage.builder()
                    .bytes(file.getBytes())
                    .contentType(normalizedContentType)
                    .originalFilename(file.getOriginalFilename())
                    .build();

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đọc dữ liệu ảnh tải lên");
        }

    }

    private static @NonNull String getNormalizedContentType(MultipartFile file) {
        if(file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp ảnh hợp lệ");
        }

        String contentType = file.getContentType();
        String normalizedContentType = contentType != null ? contentType.toLowerCase() : null;

        if(normalizedContentType == null || !ALLOWED_TYPES.contains(normalizedContentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ ảnh JPG, PNG, GIF hoặc WebP");
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
