package com.hotelbooking.hotel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/diagnostic")
public class DiagnosticController {

    @GetMapping("/headers")
    public ResponseEntity<?> showHeaders(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Собираем все заголовки
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, String> headers = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        response.put("headers", headers);
        response.put("method", request.getMethod());
        response.put("uri", request.getRequestURI());
        response.put("timestamp", new Date());
        response.put("service", "hotel-service");
        response.put("status", "JWT authentication successful!");

        return ResponseEntity.ok(response);
    }
}