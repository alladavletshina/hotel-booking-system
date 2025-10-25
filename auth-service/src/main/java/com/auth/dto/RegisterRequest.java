package com.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на регистрацию пользователя")
public class RegisterRequest {
    @Schema(description = "Имя пользователя", example = "ivan", required = true)
    private String username;

    @Schema(description = "Пароль", example = "password123", required = true)
    private String password;

    @Schema(description = "Email", example = "ivan@example.com")
    private String email;

    @Schema(description = "Имя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия", example = "Петров")
    private String lastName;

    @Schema(
            description = "Роль пользователя",
            example = "USER",
            allowableValues = {"USER", "ADMIN"},
            defaultValue = "USER"
    )
    private String role;
}