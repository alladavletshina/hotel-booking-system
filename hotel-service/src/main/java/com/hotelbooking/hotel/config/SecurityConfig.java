package com.hotelbooking.hotel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
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
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/configuration/**",
                        "/h2-console/**",
                        "/actuator/health"
                ).permitAll()

                .antMatchers("/hotel/test/**").permitAll()

                // INTERNAL endpoints
                .antMatchers("/rooms/*/confirm-availability", "/rooms/*/release")
                .hasRole("INTERNAL")  // Закомментируйте эту строку

                .antMatchers("/rooms/recommend/date").hasRole("INTERNAL")

                // USER endpoints
                .antMatchers(HttpMethod.GET, "/hotels", "/hotels/{id}", "/rooms", "/rooms/{id}","/rooms/recommend", "/rooms/{id}","/rooms/hotel/{hotelId}")
                .hasAnyRole("USER", "ADMIN") // Должен работать для USER и ADMIN

                // ADMIN only endpoints
                .antMatchers(HttpMethod.POST, "/hotels", "/rooms").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/hotels/{id}", "/rooms/{id}").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/hotels/{id}", "/rooms/{id}").hasRole("ADMIN")

                .anyRequest().authenticated()
                .and()
                .headers().frameOptions().disable()
                .and()
                .oauth2ResourceServer()
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()));

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
    public JwtDecoder jwtDecoder() {
        String secretString = "mySuperSecretKeyForJWTTokenGenerationInAuthService123!";
        byte[] keyBytes = secretString.getBytes();
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}