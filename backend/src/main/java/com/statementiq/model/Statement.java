package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "statements")
@CompoundIndex(name = "user_month_idx", def = "{'userId': 1, 'statementMonth': -1}")
public class Statement {

    @Id
    private String id;

    @Indexed
    private String userId;

    private StatementType statementType;  // CC or BANK
    private String bankName;
    private String cardLast4;             // CC only
    private String accountLast4;          // Bank only
    private String statementMonth;        // "2025-03" format

    @Builder.Default
    private ParseStatus parseStatus = ParseStatus.PROCESSING;

    private int parseProgress;            // 0-100
    private double parseConfidence;       // 0.0-1.0
    private String parseError;

    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private int transactionCount;

    private String fileName;
    private long fileSizeBytes;

    // Bill due info (CC statements)
    private String billDueDate;
    private BigDecimal totalAmountDue;
    private BigDecimal minimumAmountDue;

    @CreatedDate
    private Instant uploadedAt;

    private Instant processedAt;

    public enum StatementType {
        CC, BANK
    }

    public enum ParseStatus {
        PROCESSING, COMPLETED, FAILED
    }
}
