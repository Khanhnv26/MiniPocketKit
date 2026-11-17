package com.pocketkit.backend.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class JsonCodec {
    private final ObjectMapper objectMapper;

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch(JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tuần tự hóa dữ liệu JSON");
        }
    }



}
