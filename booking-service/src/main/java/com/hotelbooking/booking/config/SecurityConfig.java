package com.hotelbooking.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                // Public endpoints
                .antMatchers(
                        "/",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api-docs/**",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/configuration/**",
                        "/booking/test/**",
                        "/diagnostic/**",
                        "/actuator/health"
                ).permitAll()

                // H2 Console (для разработки)
                .antMatchers("/h2-console/**").permitAll()

                // ADMIN endpoints - управление пользователями
                .antMatchers("/admin/users/**").hasRole("ADMIN")

                // BOOKING endpoints - требуют аутентификации
                .antMatchers("/bookings/**").hasAnyRole("USER", "ADMIN")

                .anyRequest().authenticated()
                .and()
                .headers().frameOptions().disable()
                .and()
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();

            // Извлекаем роль из claim "role"
            String role = jwt.getClaim("role");
            System.out.println("=== JWT DEBUG ===");
            System.out.println("Username: " + jwt.getSubject());
            System.out.println("Role from token: " + role);

            if (role != null && !role.trim().isEmpty()) {
                // Добавляем с префиксом ROLE_ для Spring Security
                String authority = "ROLE_" + role;
                authorities.add(new SimpleGrantedAuthority(authority));
                System.out.println("Added authority: " + authority);
            }

            System.out.println("Final authorities: " + authorities);
            System.out.println("==================");

            return authorities;
        });

        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}