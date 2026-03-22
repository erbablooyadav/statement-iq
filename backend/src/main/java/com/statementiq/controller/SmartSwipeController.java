package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import com.statementiq.model.SmartSwipe;
import com.statementiq.model.User;
import com.statementiq.service.domain.SmartSwipeService;
import com.statementiq.service.domain.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/smart-swipe")
public class SmartSwipeController {

    private final SmartSwipeService smartSwipeService;
    private final UserService userService;

    public SmartSwipeController(SmartSwipeService smartSwipeService, UserService userService) {
        this.smartSwipeService = smartSwipeService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SmartSwipe>> getSmartSwipe(Authentication auth) {
        String firebaseUid = (String) auth.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        SmartSwipe recommendations = smartSwipeService.getRecommendations(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(recommendations));
    }
}
