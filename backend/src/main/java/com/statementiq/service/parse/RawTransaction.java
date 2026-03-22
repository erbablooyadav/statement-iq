package com.statementiq.service.parse;

import com.statementiq.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Raw parsed transaction before enrichment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawTransaction {
    private LocalDate date;
    private String rawDescription;
    private String merchantName;
    private String description;
    private BigDecimal amount;
    private Transaction.TransactionType type;
    private String channel;           // POS, UPI, NEFT, ATM, etc.
    private BigDecimal closingBalance;
    private String category;
    private String subCategory;
    private boolean isFee;
    private boolean isEmi;
    private boolean isRecurring;
    private boolean isAtmWithdrawal;
    private boolean isSalaryCredit;
}
