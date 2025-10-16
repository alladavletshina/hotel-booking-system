package com.hotelbooking.booking.entity;

import javax.persistence.*;  // Используем javax.persistence для Spring Boot 2.7.x
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;
    private String role = "USER";
}