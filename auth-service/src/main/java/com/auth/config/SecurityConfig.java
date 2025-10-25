package com.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                // Все endpoints аутентификации публичные
                .antMatchers("/auth/**").permitAll()

                // Swagger endpoints
                .antMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api-docs/**",
                        "/auth/validate/",
                        "/webjars/**",
                        "/swagger-resources/**"
                ).permitAll()

                // Test endpoints
                .antMatchers("/test/**").permitAll()

                // H2 Console
                .antMatchers("/h2-console/**").permitAll()

                // Actuator endpoints
                .antMatchers("/actuator/**").permitAll()

                .anyRequest().authenticated()
                .and()
                .headers().frameOptions().disable(); // Для H2 Console

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}