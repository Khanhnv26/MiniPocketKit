package com.pocketkit.backend.service;

import com.pocketkit.backend.config.JwtService;
import com.pocketkit.backend.dto.AuthDtos;
import com.pocketkit.backend.entity.UserEntity;
import com.pocketkit.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }
        UserEntity user = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .createdAtEpoch(System.currentTimeMillis())
                .build();
        UserEntity saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai email hoặc mật khẩu"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai email hoặc mật khẩu");
        }
        return buildAuthResponse(user);
    }

    private AuthDtos.AuthResponse buildAuthResponse(UserEntity user) {
        return AuthDtos.AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .expiresInMs(jwtService.getExpirationMs())
                .user(AuthDtos.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .build())
                .build();
    }
}
