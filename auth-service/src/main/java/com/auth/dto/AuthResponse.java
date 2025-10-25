package com.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Ответ с токеном аутентификации")
public class AuthResponse {
    @Schema(description = "JWT токен", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Имя пользователя", example = "ivan")
    private String username;

    @Schema(description = "Роль пользователя", example = "USER")
    private String role;

    @Schema(description = "Сообщение", example = "Login successful")
    private String message;

    public AuthResponse(String token, String username, String role, String message) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.message = message;
    }
}