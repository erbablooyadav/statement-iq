package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import com.statementiq.model.Transaction;
import com.statementiq.model.User;
import com.statementiq.service.domain.TransactionService;
import com.statementiq.service.domain.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statements/{statementId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Transaction>>> getTransactions(
            @PathVariable String statementId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication) {

        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Transaction> transactions = transactionService.getTransactionsByStatement(statementId, user.getId(), pageRequest);

        return ResponseEntity.ok(ApiResponse.ok(transactions));
    }

    @GetMapping("/chart-data")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getChartData(
            @PathVariable String statementId,
            Authentication authentication) {

        String firebaseUid = (String) authentication.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        java.util.List<Transaction> transactions = transactionService.getAllTransactionsByStatement(statementId, user.getId());

        // Aggregate DEBIT transactions by category
        java.util.Map<String, java.math.BigDecimal> categoryTotals = new java.util.HashMap<>();
        for (Transaction t : transactions) {
            if (t.getTransactionType() == Transaction.TransactionType.DEBIT && t.getAmount() != null) {
                String cat = (t.getCategory() == null || t.getCategory().isEmpty()) ? "Miscellaneous" : t.getCategory();
                categoryTotals.merge(cat, t.getAmount(), java.math.BigDecimal::add);
            }
        }

        // Convert to list for Recharts
        java.util.List<java.util.Map<String, Object>> chartData = new java.util.ArrayList<>();
        categoryTotals.forEach((cat, total) -> {
            chartData.add(java.util.Map.of("name", cat, "value", total));
        });

        // Sort descending by value
        chartData.sort((a, b) -> ((java.math.BigDecimal) b.get("value")).compareTo((java.math.BigDecimal) a.get("value")));

        return ResponseEntity.ok(ApiResponse.ok(chartData));
    }
}
