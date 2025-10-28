package com.auth.config;

import com.auth.entity.User;
import com.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Создаем начального администратора, если его нет
        if (authService.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@hotelbooking.com");
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setRole("ADMIN");

            authService.saveUser(admin);
            System.out.println("Default admin user created: admin / admin123");
        }

        // Создаем INTERNAL пользователя для межсервисного взаимодействия
        if (authService.findByUsername("internal-service").isEmpty()) {
            User internalUser = new User();
            internalUser.setUsername("internal-service");
            internalUser.setPassword(passwordEncoder.encode("internal-secret-123"));
            internalUser.setEmail("internal@hotelbooking.com");
            internalUser.setFirstName("Internal");
            internalUser.setLastName("Service");
            internalUser.setRole("INTERNAL");

            authService.saveUser(internalUser);
            System.out.println("Internal service user created: internal-service / internal-secret-123");
        }
    }
}