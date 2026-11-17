package com.pocketkit.backend.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min =6, max = 72, message = "Mật khẩu phải có độ dài từ 6 đến 72 ký tự")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;
}
