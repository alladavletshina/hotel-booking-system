package com.hotelbooking.booking.config;

import com.hotelbooking.booking.service.InternalAuthService;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignConfig {

    private final InternalAuthService internalAuthService;

    public FeignConfig(InternalAuthService internalAuthService) {
        this.internalAuthService = internalAuthService;
        log.info("🎯 === FEIGN CONFIG CONSTRUCTOR CALLED ===");
        log.info("🎯 InternalAuthService injected: {}", internalAuthService != null ? "SUCCESS" : "FAILED");
    }

    @Bean
    public RequestInterceptor internalAuthRequestInterceptor() {
        log.info("🎯 === CREATING FEIGN INTERCEPTOR BEAN ===");

        return requestTemplate -> {
            log.debug("🎯 🔥 FEIGN INTERCEPTOR TRIGGERED!");
            log.debug("🎯 URL: {}", requestTemplate.url());
            log.debug("🎯 Method: {}", requestTemplate.method());

            if (requestTemplate.url().contains("/confirm-availability") ||
                    requestTemplate.url().contains("/release")) {

                log.info("🎯 ✅ INTERNAL ENDPOINT DETECTED - Adding auth headers for: {}", requestTemplate.url());

                // Проверяем, что сервис инициализирован
                if (internalAuthService == null) {
                    log.error("🎯 ❌ InternalAuthService is NULL! Cannot add auth headers");
                    return;
                }

                // Используем InternalAuthService вместо хардкода
                String token = internalAuthService.getInternalToken();

                if (token != null && !token.trim().isEmpty()) {
                    requestTemplate.header("Authorization", "Bearer " + token);
                    requestTemplate.header("X-Internal-Call", "true");
                    requestTemplate.header("X-Service-Name", "booking-service");

                    log.info("🎯 ✅ Added Authorization header with fresh token");
                    log.debug("🎯 Token prefix: {}...", token.substring(0, Math.min(50, token.length())));
                } else {
                    log.error("🎯 ❌ Cannot add auth headers - token is null or empty");

                    // Fallback: временно используем простой токен для тестирования
                    String fallbackToken = createSimpleToken();
                    requestTemplate.header("Authorization", "Bearer " + fallbackToken);
                    requestTemplate.header("X-Internal-Call", "true");
                    requestTemplate.header("X-Service-Name", "booking-service");
                    log.warn("🎯 🔄 Using fallback token for testing");
                }
            }
        };
    }

    /**
     * Создает простой токен для тестирования (временное решение)
     */
    private String createSimpleToken() {
        try {
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            long expiryTimeSeconds = currentTimeSeconds + 3600;

            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payload = String.format(
                    "{\"sub\":\"internal-service\",\"role\":\"INTERNAL\",\"iat\":%d,\"exp\":%d}",
                    currentTimeSeconds, expiryTimeSeconds
            );

            String encodedHeader = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(header.getBytes());
            String encodedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes());

            return encodedHeader + "." + encodedPayload + ".simple_token_for_testing";
        } catch (Exception e) {
            log.error("Error creating simple token: {}", e.getMessage());
            return "simple-test-token-" + System.currentTimeMillis();
        }
    }
}