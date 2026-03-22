package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import com.statementiq.model.Alert;
import com.statementiq.model.Statement;
import com.statementiq.model.User;
import com.statementiq.repository.AlertRepository;
import com.statementiq.repository.StatementRepository;
import com.statementiq.service.domain.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final StatementRepository statementRepository;
    private final AlertRepository alertRepository;
    private final UserService userService;

    public DashboardController(StatementRepository statementRepository,
                               AlertRepository alertRepository,
                               UserService userService) {
        this.statementRepository = statementRepository;
        this.alertRepository = alertRepository;
        this.userService = userService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(Authentication auth) {
        String firebaseUid = (String) auth.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        List<Statement> statements = statementRepository.findByUserIdOrderByUploadedAtDesc(user.getId());
        List<Alert> unreadAlerts = alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());

        boolean hasStatements = !statements.isEmpty();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;

        if (hasStatements) {
            totalIncome = statements.stream()
                    .map(stmt -> stmt.getTotalCredit() != null ? stmt.getTotalCredit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalSpent = statements.stream()
                    .map(stmt -> stmt.getTotalDebit() != null ? stmt.getTotalDebit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("hasStatements", hasStatements);
        summary.put("totalIncome", totalIncome);
        summary.put("totalSpent", totalSpent);
        summary.put("totalSaved", totalIncome.subtract(totalSpent));
        summary.put("unreadAlerts", unreadAlerts.size());

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
