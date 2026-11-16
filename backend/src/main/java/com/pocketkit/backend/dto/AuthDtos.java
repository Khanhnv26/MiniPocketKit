package com.pocketkit.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        private String email;

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6 đến 72 ký tự")
        private String password;

        @NotBlank(message = "Họ và tên không được để trống")
        private String fullName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        private String email;

        @NotBlank(message = "Mật khẩu không được để trống")
        private String password;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AuthResponse {
        private final String accessToken;
        private final long expiresInMs;
        private final UserSummary user;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class UserSummary {
        private final Long id;
        private final String email;
        private final String fullName;
    }
}
