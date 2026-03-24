package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Document(collection = "report_cards")
@CompoundIndex(def = "{'userId': 1, 'month': 1}", unique = true)
public class ReportCard {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** Month in "yyyy-MM" format */
    private String month;

    /** Overall financial health score (0-100) */
    private int overallScore;

    private int savingsScore;       // Did they save? (0-100)
    private int spendControlScore;  // Spending within limits? (0-100)
    private int hiddenChargeScore;  // Zero hidden fees = 100 (0-100)
    private int goalAdherenceScore; // Goal progress this month? (0-100)

    private BigDecimal totalIncome;
    private BigDecimal totalSpent;
    private BigDecimal totalSaved;
    private int transactionCount;

    private String topCategory;
    private String badge;          // "Saver 💰", "Overspender 🔥", "Streak Master 🏆", etc.
    private String aiNarrative;    // 2-3 sentence summary

    private Instant generatedAt;
}
