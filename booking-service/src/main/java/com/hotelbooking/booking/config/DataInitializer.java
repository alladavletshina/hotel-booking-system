package com.hotelbooking.booking.config;

import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.repository.UserRepository;
import com.hotelbooking.booking.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            // Создаем администратора если его нет
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@example.com");
                admin.setFirstName("Admin");
                admin.setLastName("User");
                admin.setRole("ADMIN");
                admin.setActive(true);

                userRepository.save(admin);
                log.info("Admin user created: admin/admin123");
            }

            // Создаем тестового пользователя если его нет
            if (userRepository.findByUsername("testuser").isEmpty()) {
                User user = new User();
                user.setUsername("testuser");
                user.setPassword(passwordEncoder.encode("password"));
                user.setEmail("test@example.com");
                user.setFirstName("Test");
                user.setLastName("User");
                user.setRole("USER");
                user.setActive(true);

                userRepository.save(user);
                log.info("Test user created: testuser/password");
            }
        };
    }
}