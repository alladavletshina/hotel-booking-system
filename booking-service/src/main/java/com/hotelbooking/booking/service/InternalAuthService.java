package com.hotelbooking.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class InternalAuthService {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    private final String internalUsername;
    private final String internalPassword;

    private String internalToken;
    private long tokenExpiryTime;
    private boolean initialized = false;

    public InternalAuthService(
            RestTemplate restTemplate,
            @Value("${auth.service.url:http://localhost:8080/api/auth}") String authServiceUrl,
            @Value("${internal.service.username:internal-service}") String internalUsername,
            @Value("${internal.service.password:internal-secret-123}") String internalPassword) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
        this.internalUsername = internalUsername;
        this.internalPassword = internalPassword;

        log.info("InternalAuthService initialized with URL: {}", authServiceUrl);
    }

    @PostConstruct
    public void init() {
        log.info("=== INIT INTERNAL AUTH SERVICE ===");
        log.info("Using auth URL: {}", authServiceUrl);
        log.info("Internal username: {}", internalUsername);

        // Принудительно получаем токен при старте с задержкой
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                log.info("Attempting to get internal token...");
                refreshInternalToken();

                if (internalToken != null && isTokenValid()) {
                    initialized = true;
                    log.info("✅ Internal token obtained successfully");
                    if (internalToken.length() > 50) {
                        log.debug("Token prefix: {}...", internalToken.substring(0, 50));
                    } else {
                        log.debug("Token: {}", internalToken);
                    }
                } else {
                    log.error("❌ Failed to get valid internal token - service will not be able to make internal calls");
                    // ❌ УБРАН FALLBACK - оставляем токен null
                }
            } catch (Exception e) {
                log.error("❌ Critical error during internal auth init: {}", e.getMessage());
                // ❌ УБРАН FALLBACK - не создаем токен при ошибке
            }
        }, 10, TimeUnit.SECONDS);

        // Периодическое обновление токена каждые 30 минут
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (initialized && isTokenExpiringSoon()) {
                    log.info("🔄 Token expiring soon, refreshing...");
                    refreshInternalToken();
                }
            } catch (Exception e) {
                log.error("Error during scheduled token refresh: {}", e.getMessage());
            }
        }, 30, 30, TimeUnit.MINUTES);
    }

    /**
     * Получает или обновляет токен для internal пользователя
     */
    public void refreshInternalToken() {
        try {
            String loginUrl = authServiceUrl + "/login";
            log.debug("Making login request to: {}", loginUrl);

            // Создаем запрос для логина
            String loginRequest = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\"}",
                    internalUsername, internalPassword
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(loginRequest, headers);

            log.info("Attempting to login as internal service user: {}", internalUsername);

            ResponseEntity<String> response = restTemplate.exchange(
                    loginUrl, HttpMethod.POST, request, String.class
            );

            log.debug("Login response status: {}", response.getStatusCode());
            log.debug("Login response body: {}", response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Парсим токен из ответа
                String responseBody = response.getBody();
                if (responseBody.contains("\"token\"")) {
                    internalToken = extractTokenFromResponse(responseBody);
                    // Устанавливаем время expiry (предполагаем 24 часа)
                    tokenExpiryTime = System.currentTimeMillis() + 86400000;
                    initialized = true;
                    log.info("✅ Successfully obtained internal service token via API Gateway");
                    log.debug("Full token: {}", internalToken);
                } else {
                    // ❌ УБРАН FALLBACK - выбрасываем исключение
                    log.error("Token not found in auth response: {}", responseBody);
                    throw new RuntimeException("Token not found in authentication response");
                }
            } else {
                // ❌ УБРАН FALLBACK - выбрасываем исключение
                log.error("Failed to get internal token. Status: {}, Response: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Authentication service returned status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            // ❌ УБРАН FALLBACK - пробрасываем исключение дальше
            log.error("Critical error refreshing internal token: {}", e.getMessage());
            initialized = false;
            throw new RuntimeException("Failed to refresh internal service token", e);
        }
    }

    /**
     * Извлекает токен из JSON ответа
     */
    private String extractTokenFromResponse(String jsonResponse) {
        try {
            log.debug("Parsing token from response: {}", jsonResponse);

            // Пробуем несколько способов извлечения токена
            String[] tokenKeys = {"\"token\":\"", "\"access_token\":\"", "token\":\""};

            for (String tokenKey : tokenKeys) {
                int startIndex = jsonResponse.indexOf(tokenKey);
                if (startIndex != -1) {
                    startIndex += tokenKey.length();
                    int endIndex = jsonResponse.indexOf("\"", startIndex);
                    if (endIndex != -1) {
                        String token = jsonResponse.substring(startIndex, endIndex);
                        log.debug("Successfully extracted token using key '{}'", tokenKey);
                        return token;
                    }
                }
            }

            log.error("Could not extract token using standard keys, response: {}", jsonResponse);
            throw new RuntimeException("Unable to parse token from authentication response");

        } catch (Exception e) {
            log.error("Error extracting token from response: {}", e.getMessage());
            throw new RuntimeException("Token extraction failed", e);
        }
    }

    /**
     * Возвращает текущий internal токен
     */
    public String getInternalToken() {
        if (!initialized) {
            log.warn("Internal auth service not initialized - attempting to refresh token");
            refreshInternalToken();
        }

        // Если токен отсутствует или скоро истечет - обновляем
        if (internalToken == null || isTokenExpiringSoon()) {
            log.info("Token missing or expiring soon, refreshing...");
            refreshInternalToken();
        }

        if (internalToken == null) {
            throw new RuntimeException("Internal service token is not available");
        }

        return internalToken;
    }

    /**
     * Проверяет, валиден ли текущий токен
     */
    public boolean isTokenValid() {
        return initialized &&
                internalToken != null &&
                !internalToken.trim().isEmpty() &&
                !isTokenExpired();
    }

    /**
     * Проверяет, инициализирован ли сервис
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Проверяет, истек ли срок действия токена
     */
    private boolean isTokenExpired() {
        return System.currentTimeMillis() > tokenExpiryTime;
    }

    /**
     * Проверяет, скоро ли истечет срок действия токена (менее 5 минут)
     */
    private boolean isTokenExpiringSoon() {
        return tokenExpiryTime - System.currentTimeMillis() < 300000; // 5 минут
    }

    /**
     * Создает заголовки с internal аутентификацией
     */
    public HttpHeaders createInternalAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String token = getInternalToken();
            headers.setBearerAuth(token);
            log.debug("Added Bearer token to headers");
        } catch (Exception e) {
            log.error("Cannot create auth headers - internal token not available: {}", e.getMessage());
            throw new RuntimeException("Internal service authentication unavailable", e);
        }

        // Добавляем заголовок для идентификации internal вызовов
        headers.set("X-Internal-Call", "true");
        headers.set("X-Service-Name", "booking-service");

        return headers;
    }

    /**
     * Принудительно обновляет токен
     */
    public void forceRefreshToken() {
        log.info("🔄 Forcing internal token refresh...");
        refreshInternalToken();
    }

    /**
     * Возвращает информацию о текущем токене (для отладки)
     */
    public String getTokenInfo() {
        if (!initialized || internalToken == null) {
            return "Service not initialized - token unavailable";
        }

        long timeUntilExpiry = tokenExpiryTime - System.currentTimeMillis();
        long minutesUntilExpiry = timeUntilExpiry / 60000;

        return String.format("Token: %s... (expires in %d minutes)",
                internalToken.substring(0, Math.min(30, internalToken.length())),
                minutesUntilExpiry);
    }
}