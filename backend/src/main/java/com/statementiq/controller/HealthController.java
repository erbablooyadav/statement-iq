package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthData = Map.of(
                "status", "UP",
                "service", "StatementIQ Backend",
                "version", "1.0.0",
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.ok(healthData));
    }
}
