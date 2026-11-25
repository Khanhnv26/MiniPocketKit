package com.pocketkit.backend.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class GoogleTokenVerifier {

    private static final int MAX_FULL_NAME_LENGTH = 120;
    private static final String ISSUER_HTTPS = "https://accounts.google.com";
    private static final String ISSUER_PLAIN = "accounts.google.com";

    @Getter
    @AllArgsConstructor
    public static final class VerifiedGoogleUser {
        private final String sub;
        private final String email;
        private final String fullName;
    }

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Set<String> allowedClientIds;

    @Autowired
    public GoogleTokenVerifier(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.google.client-id:}") String clientIdsCsv,
            @Value("${app.google.token-info-base-url:https://oauth2.googleapis.com}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.allowedClientIds = parseClientIds(clientIdsCsv);
    }

    public GoogleTokenVerifier(RestClient restClient, ObjectMapper objectMapper, String clientIdsCsv) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.allowedClientIds = parseClientIds(clientIdsCsv);
    }

    public VerifiedGoogleUser verifyIdToken(String idToken) {
        if (allowedClientIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình GOOGLE_CLIENT_ID trên server"
            );
        }
        if (idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google ID token không được để trống");
        }

        String rawJson;
        try {
            rawJson = restClient.get()
                    .uri("/tokeninfo?id_token={idToken}", idToken.trim())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn"
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể xác thực tài khoản với Google"
            );
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể xác thực tài khoản với Google"
            );
        }

        if (rawJson == null || rawJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Phản hồi xác thực Google trống");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Phản hồi xác thực Google không hợp lệ");
        }

        String aud = root.path("aud").asText("").trim();
        String iss = root.path("iss").asText("").trim();
        String sub = root.path("sub").asText("").trim();
        String email = root.path("email").asText("").trim().toLowerCase(Locale.ROOT);
        boolean emailVerified = isEmailVerified(root.path("email_verified"));

        if (!allowedClientIds.contains(aud)
                || (!ISSUER_HTTPS.equals(iss) && !ISSUER_PLAIN.equals(iss))
                || !emailVerified
                || sub.isEmpty()
                || email.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản Google không hợp lệ hoặc chưa xác minh email"
            );
        }

        JsonNode expNode = root.get("exp");
        if (expNode != null && !expNode.isNull()) {
            String expText = expNode.asText("").trim();
            if (!expText.isEmpty()) {
                try {
                    long expEpochSeconds = Long.parseLong(expText);
                    if (expEpochSeconds <= Instant.now().getEpochSecond()) {
                        throw new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Phiên đăng nhập Google đã hết hạn"
                        );
                    }
                } catch (NumberFormatException ex) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Phiên đăng nhập Google không hợp lệ"
                    );
                }
            }
        }

        String rawName = root.path("name").asText("").trim();
        String resolvedName = rawName.isEmpty() ? fallbackNameFromEmail(email) : rawName;
        if (resolvedName.length() > MAX_FULL_NAME_LENGTH) {
            resolvedName = resolvedName.substring(0, MAX_FULL_NAME_LENGTH).trim();
        }
        return new VerifiedGoogleUser(sub, email, resolvedName);
    }

    private static boolean isEmailVerified(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return false;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return "true".equalsIgnoreCase(node.asText("").trim());
    }

    private static String fallbackNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }
        return "PocketKit User";
    }

    private static Set<String> parseClientIds(String csv) {
        Set<String> result = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return result;
        }
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
