package com.pocketkit.backend.service.impl;

import com.pocketkit.backend.dto.AuthDtos;
import com.pocketkit.backend.entity.UserEntity;
import com.pocketkit.backend.repository.UserRepository;
import com.pocketkit.backend.service.AuthService;
import com.pocketkit.backend.utils.GoogleTokenVerifier;
import com.pocketkit.backend.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Override
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        UserEntity user = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider("LOCAL")
                .fullName(request.getFullName().trim())
                .createdAtEpoch(System.currentTimeMillis())
                .build();

        return buildAuthResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản này đăng nhập bằng Google. Vui lòng chọn Tiếp tục với Google"
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse loginWithGoogle(AuthDtos.GoogleLoginRequest request) {
        GoogleTokenVerifier.VerifiedGoogleUser verified = googleTokenVerifier.verifyIdToken(request.getIdToken());

        UserEntity bySub = userRepository.findByGoogleSub(verified.getSub()).orElse(null);
        if (bySub != null) {
            return buildAuthResponse(bySub);
        }

        UserEntity existingByEmail = userRepository.findByEmailIgnoreCase(verified.getEmail()).orElse(null);
        if (existingByEmail != null) {
            String existingSub = existingByEmail.getGoogleSub();
            if (existingSub != null && !existingSub.isBlank() && !existingSub.equals(verified.getSub())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Email đã liên kết với tài khoản Google khác"
                );
            }
            existingByEmail.setGoogleSub(verified.getSub());
            boolean hasPassword = existingByEmail.getPasswordHash() != null
                    && !existingByEmail.getPasswordHash().isBlank();
            existingByEmail.setAuthProvider(hasPassword ? "LOCAL_GOOGLE" : "GOOGLE");
            if (existingByEmail.getFullName() == null || existingByEmail.getFullName().isBlank()) {
                existingByEmail.setFullName(verified.getFullName());
            }
            return buildAuthResponse(userRepository.save(existingByEmail));
        }

        UserEntity newUser = UserEntity.builder()
                .email(verified.getEmail())
                .passwordHash(null)
                .googleSub(verified.getSub())
                .authProvider("GOOGLE")
                .fullName(verified.getFullName())
                .createdAtEpoch(System.currentTimeMillis())
                .build();
        return buildAuthResponse(userRepository.save(newUser));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDtos.UserSummary getCurrentUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));

        return toUserSummary(user);
    }

    private AuthDtos.AuthResponse buildAuthResponse(UserEntity user) {
        return AuthDtos.AuthResponse.builder()
                .accessToken(jwtUtils.generateToken(user))
                .expiresInMs(jwtUtils.getExpirationMs())
                .user(toUserSummary(user))
                .build();
    }

    private AuthDtos.UserSummary toUserSummary(UserEntity user) {
        return AuthDtos.UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }
}
