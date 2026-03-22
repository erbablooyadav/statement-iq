package com.statementiq.service.parse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of parsing a bank statement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {

    @Builder.Default
    private List<RawTransaction> transactions = new ArrayList<>();

    private double confidence;          // 0.0 - 1.0
    private String statementMonth;      // "2025-03"
    private String billDueDate;         // CC only
    private BigDecimal totalAmountDue;  // CC only
    private BigDecimal minimumAmountDue; // CC only
    private String bankName;
    private String parserUsed;          // "REGEX" or "AI_FALLBACK"
}
