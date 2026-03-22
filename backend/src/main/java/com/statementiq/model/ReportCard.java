package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "report_cards")
public class ReportCard {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String reportMonth;          // "2025-03"

    private int spendControlScore;       // 0-100
    private int savingsRateScore;
    private int rewardOptimizationScore;
    private int hiddenChargesScore;
    private int goalProgressScore;
    private int overallScore;

    private String spendControlGrade;    // A+, A, B+, B, C, D, F
    private String savingsRateGrade;
    private String rewardOptimizationGrade;
    private String hiddenChargesGrade;
    private String goalProgressGrade;
    private String overallGrade;

    private int scoreChangeFromLastMonth;

    @Builder.Default
    private List<String> highlights = new ArrayList<>();

    @Builder.Default
    private List<String> opportunities = new ArrayList<>();

    private Instant generatedAt;
}
