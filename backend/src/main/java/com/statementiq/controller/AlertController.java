package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import com.statementiq.model.Alert;
import com.statementiq.model.User;
import com.statementiq.repository.AlertRepository;
import com.statementiq.service.domain.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertRepository alertRepository;
    private final UserService userService;

    public AlertController(AlertRepository alertRepository, UserService userService) {
        this.alertRepository = alertRepository;
        this.userService = userService;
    }

    /**
     * GET /alerts — List all alerts for the logged-in user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Alert>>> getAlerts(Authentication auth) {
        String firebaseUid = (String) auth.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        List<Alert> alerts = alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(alerts));
    }

    /**
     * PATCH /alerts/{id}/read — Mark a specific alert as read.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Alert>> markAlertAsRead(
            @PathVariable String id, Authentication auth) {
        String firebaseUid = (String) auth.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        Alert alert = alertRepository.findById(id)
                .filter(a -> a.getUserId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        alert.setRead(true);
        alertRepository.save(alert);
        return ResponseEntity.ok(ApiResponse.ok(alert));
    }
}
