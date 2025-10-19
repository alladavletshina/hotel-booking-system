package com.hotelbooking.booking.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String password;  // Добавляем поле password
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean active;
}