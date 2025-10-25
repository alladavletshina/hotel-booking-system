package com.hotelbooking.booking.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public class JwtUtils {

    public static Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return Optional.ofNullable(jwt.getClaimAsString("sub"));
        }
        return Optional.empty();
    }

    public static Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return extractUserIdFromJwt(jwt);
        }
        return Optional.empty();
    }

    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }

    private static Optional<Long> extractUserIdFromJwt(Jwt jwt) {
        try {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim instanceof Long) {
                return Optional.of((Long) userIdClaim);
            } else if (userIdClaim instanceof Integer) {
                return Optional.of(((Integer) userIdClaim).longValue());
            } else if (userIdClaim instanceof String) {
                return Optional.of(Long.parseLong((String) userIdClaim));
            }
        } catch (Exception e) {
            // ignore
        }
        return Optional.empty();
    }
}