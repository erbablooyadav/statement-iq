package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import com.statementiq.model.Insight;
import com.statementiq.model.User;
import com.statementiq.service.domain.InsightService;
import com.statementiq.service.domain.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statements/{statementId}/insights")
public class InsightController {

    private final InsightService insightService;
    private final UserService userService;

    public InsightController(InsightService insightService, UserService userService) {
        this.insightService = insightService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Insight>> getInsight(
            @PathVariable String statementId,
            Authentication authentication) {

        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        Insight insight = insightService.getInsightByStatement(statementId, user.getId());

        return ResponseEntity.ok(ApiResponse.ok(insight));
    }
}
