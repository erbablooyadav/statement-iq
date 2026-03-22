package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
@CompoundIndexes({
        @CompoundIndex(name = "user_date_idx", def = "{'userId': 1, 'transactionDate': -1}"),
        @CompoundIndex(name = "user_category_idx", def = "{'userId': 1, 'category': 1}"),
        @CompoundIndex(name = "statement_idx", def = "{'statementId': 1}")
})
public class Transaction {

    @Id
    private String id;

    @Indexed
    private String statementId;

    @Indexed
    private String userId;

    private LocalDate transactionDate;
    private String rawDescription;         // Original from PDF, kept for debugging
    private String merchantName;           // Cleaned: "POS/SWIGGY*ORDER123" → "Swiggy"
    private String description;            // Human readable: "Food delivery - Swiggy"

    private BigDecimal amount;
    private TransactionType transactionType;

    private String transactionChannel;     // UPI / NEFT / IMPS / ATM / POS / CHEQUE / ECS
    private String upiId;                  // From UPI description if available
    private String referenceNumber;        // For dispute purposes
    private BigDecimal closingBalance;     // Running balance (bank statements)

    private String category;               // From 12-category taxonomy
    private String subCategory;            // Drill-down

    @Builder.Default
    private boolean isFee = false;         // Late fee, annual fee, forex, GST

    @Builder.Default
    private boolean isEmi = false;

    @Builder.Default
    private boolean isRecurring = false;

    @Builder.Default
    private boolean isAtmWithdrawal = false;

    @Builder.Default
    private boolean isSalaryCredit = false;

    private Integer rewardPoints;

    @Builder.Default
    private boolean manuallyOverridden = false;

    private String overriddenCategory;
    private String overriddenSubCategory;

    @CreatedDate
    private Instant createdAt;

    public enum TransactionType {
        DEBIT, CREDIT
    }
}
