package com.hotelbooking.booking.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String password; // Только для создания/обновления
    private String email;
    private String firstName;
    private String lastName;
    private String role; // USER, ADMIN
    private Boolean active;
}