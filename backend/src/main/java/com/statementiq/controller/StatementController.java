package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import com.statementiq.exception.InvalidFileException;
import com.statementiq.exception.RateLimitExceededException;
import com.statementiq.model.Statement;
import com.statementiq.model.User;
import com.statementiq.service.domain.AuditService;
import com.statementiq.service.domain.StatementService;
import com.statementiq.service.domain.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/statements")
public class StatementController {

    private final StatementService statementService;
    private final UserService userService;
    private final AuditService auditService;

    public StatementController(StatementService statementService, UserService userService, AuditService auditService) {
        this.statementService = statementService;
        this.userService = userService;
        this.auditService = auditService;
    }

    /**
     * POST /api/v1/statements/upload
     * Upload CC or Bank PDF statement.
     * PRIVACY: PDF processed in-memory only — never written to disk.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Statement>> uploadStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("statementType") String statementType,
            @RequestParam("bankName") String bankName,
            @RequestParam(value = "cardLast4", required = false) String cardLast4,
            @RequestParam(value = "accountLast4", required = false) String accountLast4,
            @RequestParam(value = "password", required = false) String password,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        // Rate limit check
        if (!userService.canUpload(user)) {
            int limit = user.getPlan() == User.Plan.PRO ? 30 : 5;
            throw new RateLimitExceededException(
                    String.format("Daily upload limit reached (%d). %s",
                            limit,
                            user.getPlan() == User.Plan.FREE ? "Upgrade to Pro for 30 uploads/day." : "Try again tomorrow."));
        }

        // File validation
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new InvalidFileException("File size exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new InvalidFileException("Only PDF files are accepted");
        }

        Statement.StatementType type = Statement.StatementType.valueOf(statementType.toUpperCase());

        Statement statement = statementService.uploadAndProcess(file, user, type, bankName, cardLast4, accountLast4, password);

        userService.incrementUploadCount(user);

        auditService.log(user.getId(), "UPLOAD", "STATEMENT", statement.getId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResponse.ok(statement));
    }

    /**
     * GET /api/v1/statements
     * List all statements for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Statement>>> listStatements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String month,
            Authentication authentication) {

        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        Page<Statement> statements = statementService.getStatements(user.getId(), type, month, pageRequest);

        return ResponseEntity.ok(ApiResponse.ok(statements));
    }

    /**
     * GET /api/v1/statements/{id}
     * Get statement metadata + parse status.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Statement>> getStatement(
            @PathVariable String id,
            Authentication authentication) {

        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        Statement statement = statementService.getStatementByIdAndUser(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(statement));
    }

    /**
     * GET /api/v1/statements/{id}/status
     * Poll endpoint for async processing status.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatementStatus(
            @PathVariable String id,
            Authentication authentication) {

        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        Statement statement = statementService.getStatementByIdAndUser(id, user.getId());

        Map<String, Object> status = Map.of(
                "status", statement.getParseStatus().name(),
                "progress", statement.getParseProgress(),
                "transactionCount", statement.getTransactionCount(),
                "error", statement.getParseError() != null ? statement.getParseError() : ""
        );

        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * DELETE /api/v1/statements/{id}
     * Delete statement and all associated transactions.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStatement(
            @PathVariable String id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        statementService.deleteStatement(id, user.getId());

        auditService.log(user.getId(), "DELETE", "STATEMENT", id,
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResponse.ok("Statement deleted successfully"));
    }
}
