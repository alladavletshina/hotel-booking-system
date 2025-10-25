package com.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на аутентификацию")
public class AuthRequest {

    @Schema(description = "Имя пользователя", example = "ivan")
    private String username;

    @Schema(description = "Пароль", example = "password123")
    private String password;
}