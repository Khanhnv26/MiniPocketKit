package com.pocketkit.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketkit.backend.dto.AuthDtos;
import com.pocketkit.backend.entity.UserEntity;
import com.pocketkit.backend.repository.UserRepository;
import com.pocketkit.backend.service.impl.AuthServiceImpl;
import com.pocketkit.backend.utils.GoogleTokenVerifier;
import com.pocketkit.backend.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GoogleOAuthServiceTest {

    private static final String CLIENT_ID = "test-web-client.apps.googleusercontent.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private MockRestServiceServer mockServer;
    private GoogleTokenVerifier tokenVerifier;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://oauth2.googleapis.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        tokenVerifier = new GoogleTokenVerifier(builder.build(), objectMapper, CLIENT_ID);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtUtils, tokenVerifier);
    }

    @Test
    void loginWithGoogle_firstTime_createsUserAndBlocksPasswordLogin() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "aud", CLIENT_ID,
                "iss", "https://accounts.google.com",
                "sub", "google-sub-101",
                "email", "StudentGoogle@fpt.edu.vn",
                "email_verified", "true",
                "name", "Sinh Viên Google",
                "exp", String.valueOf(Instant.now().getEpochSecond() + 3600)
        ));
        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-id-token-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        AuthDtos.AuthResponse response = authService.loginWithGoogle(
                AuthDtos.GoogleLoginRequest.builder().idToken("google-id-token-1").build()
        );

        mockServer.verify();
        assertNotNull(response.getAccessToken());
        assertEquals("studentgoogle@fpt.edu.vn", response.getUser().getEmail());
        assertEquals("Sinh Viên Google", response.getUser().getFullName());

        UserEntity saved = userRepository.findByGoogleSub("google-sub-101").orElseThrow();
        assertNull(saved.getPasswordHash());
        assertEquals("GOOGLE", saved.getAuthProvider());

        ResponseStatusException loginEx = assertThrows(ResponseStatusException.class, () ->
                authService.login(AuthDtos.LoginRequest.builder()
                        .email("studentgoogle@fpt.edu.vn")
                        .password("secret123")
                        .build())
        );
        assertEquals(HttpStatus.UNAUTHORIZED, loginEx.getStatusCode());
        assertTrue(loginEx.getReason() != null && loginEx.getReason().contains("Google"));
    }

    @Test
    void loginWithGoogle_matchingLocalEmail_linksExistingUserAndKeepsLocalPassword() throws Exception {
        AuthDtos.AuthResponse localRegistered = authService.register(AuthDtos.RegisterRequest.builder()
                .email("linked@fpt.edu.vn")
                .password("secret123")
                .fullName("Nguyễn Văn Linked")
                .build());

        String body = objectMapper.writeValueAsString(Map.of(
                "aud", CLIENT_ID,
                "iss", "accounts.google.com",
                "sub", "google-sub-202",
                "email", "linked@fpt.edu.vn",
                "email_verified", true,
                "name", "Google Display Name",
                "exp", String.valueOf(Instant.now().getEpochSecond() + 3600)
        ));
        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-id-token-2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        AuthDtos.AuthResponse googleResponse = authService.loginWithGoogle(
                AuthDtos.GoogleLoginRequest.builder().idToken("google-id-token-2").build()
        );

        mockServer.verify();
        assertEquals(localRegistered.getUser().getId(), googleResponse.getUser().getId());

        UserEntity linkedUser = userRepository.findById(localRegistered.getUser().getId()).orElseThrow();
        assertEquals("LOCAL_GOOGLE", linkedUser.getAuthProvider());
        assertEquals("google-sub-202", linkedUser.getGoogleSub());

        AuthDtos.AuthResponse localLogin = authService.login(AuthDtos.LoginRequest.builder()
                .email("linked@fpt.edu.vn")
                .password("secret123")
                .build());
        assertEquals(localRegistered.getUser().getId(), localLogin.getUser().getId());
    }

    @Test
    void verifyIdToken_wrongAudienceOrUnverifiedEmail_throwsUnauthorized() throws Exception {
        String wrongAudBody = objectMapper.writeValueAsString(Map.of(
                "aud", "other-client-id",
                "iss", "https://accounts.google.com",
                "sub", "sub-1",
                "email", "bad@fpt.edu.vn",
                "email_verified", "true"
        ));
        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=bad-aud"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(wrongAudBody, MediaType.APPLICATION_JSON));

        String unverifiedBody = objectMapper.writeValueAsString(Map.of(
                "aud", CLIENT_ID,
                "iss", "https://accounts.google.com",
                "sub", "sub-2",
                "email", "unverified@fpt.edu.vn",
                "email_verified", "false"
        ));
        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=unverified"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(unverifiedBody, MediaType.APPLICATION_JSON));

        ResponseStatusException audEx = assertThrows(ResponseStatusException.class, () ->
                tokenVerifier.verifyIdToken("bad-aud")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, audEx.getStatusCode());

        ResponseStatusException verifiedEx = assertThrows(ResponseStatusException.class, () ->
                tokenVerifier.verifyIdToken("unverified")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, verifiedEx.getStatusCode());
        mockServer.verify();
    }
}
