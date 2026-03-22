package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import com.statementiq.dto.UserSyncRequest;
import com.statementiq.model.User;
import com.statementiq.service.domain.AuditService;
import com.statementiq.service.domain.UserService;
import com.statementiq.service.validation.DisposableEmailValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuditService auditService;
    private final DisposableEmailValidator emailValidator;

    public AuthController(UserService userService, AuditService auditService,
                          DisposableEmailValidator emailValidator) {
        this.userService = userService;
        this.auditService = auditService;
        this.emailValidator = emailValidator;
    }

    /**
     * POST /api/v1/auth/sync
     * Sync Firebase user to MongoDB on first login.
     * Blocks disposable/temp email addresses.
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<User>> syncUser(
            @RequestBody UserSyncRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        // Validate email — block temp/disposable emails
        String invalidReason = emailValidator.getInvalidReason(request.getEmail());
        if (invalidReason != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_EMAIL", invalidReason));
        }

        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.syncUser(firebaseUid, request);

        auditService.log(user.getId(), "LOGIN", "USER", user.getId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    /**
     * GET /api/v1/auth/me
     * Returns current user profile + plan details.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(Authentication authentication) {
        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
