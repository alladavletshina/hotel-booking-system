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
    public void run(String... args) throws Exception {
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
    }
}