package com.pocketkit.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pocketkit.backend.common.JsonCodec;
import com.pocketkit.backend.dto.BackupDtos;
import com.pocketkit.backend.dto.ScheduleShareDtos;
import com.pocketkit.backend.entity.ScheduleShareEntity;
import com.pocketkit.backend.repository.ScheduleShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ScheduleShareService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final long SHARE_TTL_MS = 30L * 24L * 60L * 60L * 1000L;

    private final ScheduleShareRepository shareRepository;
    private final JsonCodec jsonCodec;
    private final SecureRandom secureRandom = new SecureRandom();

    public ScheduleShareDtos.CreateShareResponse createShare(
            Long ownerUserId,
            ScheduleShareDtos.CreateShareRequest request
    ) {
        long now = System.currentTimeMillis();
        long expiresAt = now + SHARE_TTL_MS;
        String code = generateUniqueCode();
        String json = jsonCodec.writeValue(request.getSlots());

        ScheduleShareEntity entity = ScheduleShareEntity.builder()
                .code(code)
                .ownerUserId(ownerUserId)
                .title(request.getTitle() != null ? request.getTitle().trim() : "")
                .slotsJson(json)
                .createdAtEpoch(now)
                .expiresAtEpoch(expiresAt)
                .build();
        shareRepository.save(entity);

        return ScheduleShareDtos.CreateShareResponse.builder()
                .code(code)
                .expiresAtEpoch(expiresAt)
                .build();
    }

    public ScheduleShareDtos.ShareDetailResponse getByCode(String rawCode) {
        String code = rawCode.trim().toUpperCase(Locale.US);
        ScheduleShareEntity entity = shareRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã thời khóa biểu không tồn tại"));
        if (entity.getExpiresAtEpoch() < System.currentTimeMillis()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã thời khóa biểu đã hết hạn");
        }
        List<BackupDtos.ScheduleSlotDto> slots = jsonCodec.readValue(
                entity.getSlotsJson(),
                new TypeReference<>() {
                }
        );
        return ScheduleShareDtos.ShareDetailResponse.builder()
                .code(entity.getCode())
                .title(entity.getTitle())
                .expiresAtEpoch(entity.getExpiresAtEpoch())
                .slots(slots)
                .build();
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
            }
            String candidate = sb.toString();
            if (!shareRepository.existsById(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo mã chia sẻ");
    }
}
