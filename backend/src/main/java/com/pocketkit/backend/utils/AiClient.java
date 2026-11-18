package com.pocketkit.backend.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    @Autowired
    public AiClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:gemini-2.5-flash}") String model,
            @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-2.5-flash";
    }

    public AiClient(RestClient restClient, ObjectMapper objectMapper, String apiKey, String model) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-2.5-flash";
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    public String generateText(String prompt, ImageFileHelper.ValidatedImage image) {
        return callGenerateContent(prompt, image, false);
    }

    public String generateJson(String prompt, ImageFileHelper.ValidatedImage image) {
        String raw = callGenerateContent(prompt, image, true);
        return stripMarkdownCodeFence(raw);
    }

    private String callGenerateContent(String prompt, ImageFileHelper.ValidatedImage image, boolean expectJson) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình AI trên server");
        }

        Map<String, Object> body = buildRequestBody(prompt, image, expectJson);

        try {
            String responseJson = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return extractCandidateText(responseJson);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể kết nối tới AI");
        }
    }

    private Map<String, Object> buildRequestBody(String prompt, ImageFileHelper.ValidatedImage image, boolean expectJson) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        if (image != null) {
            parts.add(Map.of(
                    "inlineData",
                    Map.of(
                            "mimeType", image.getContentType(),
                            "data", Base64.getEncoder().encodeToString(image.getBytes())
                    )
            ));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("parts", parts)));

        if (expectJson) {
            body.put("generationConfig", Map.of("responseMimeType", "application/json"));
        }

        return body;
    }

    private String extractCandidateText(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI trả về phản hồi rỗng");
        }

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI không trả về kết quả");
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI không trả về nội dung văn bản");
            }

            String text = parts.get(0).path("text").asText("");
            if (text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI không đọc được nội dung");
            }
            return text.trim();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Phản hồi từ AI không hợp lệ");
        }
    }

    private String stripMarkdownCodeFence(String raw) {
        String trimmed = raw.trim();

        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
