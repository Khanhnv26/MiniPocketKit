package com.pocketkit.backend.service.impl;

import com.pocketkit.backend.dto.AuthDtos;
import com.pocketkit.backend.entity.UserEntity;
import com.pocketkit.backend.repository.UserRepository;
import com.pocketkit.backend.service.AuthService;
import com.pocketkit.backend.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;


    @Override
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        return null;
    }

    @Override
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        return null;
    }

    private AuthDtos.AuthResponse buildAuthResponse(UserEntity user) {
        return AuthDtos.AuthResponse.builder()
                .accessToken(jwtUtils.generateToken(user))
                .expiresInMs(jwtUtils.getExpirationMs())
                .user(AuthDtos.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .build())
                .build();
    }
}
