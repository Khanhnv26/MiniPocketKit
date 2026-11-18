package com.pocketkit.backend.service;

import com.pocketkit.backend.dto.AuthDtos;

public interface AuthService {

    AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request);
    AuthDtos.AuthResponse login(AuthDtos.LoginRequest request);
}
