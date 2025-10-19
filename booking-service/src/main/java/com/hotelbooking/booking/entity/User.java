package com.hotelbooking.booking.entity;

import lombok.Data;
import javax.persistence.*;

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

    @Column(unique = true)
    private String email;

    private String firstName;
    private String lastName;

    @Column(nullable = false)
    private String role = "USER"; // USER, ADMIN

    @Column(nullable = false)
    private Boolean active = true;
}