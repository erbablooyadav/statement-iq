package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "insights")
public class Insight {

    @Id
    private String id;

    @Indexed
    private String statementId;

    @Indexed
    private String userId;

    private Instant generatedAt;

    private BigDecimal totalSpend;
    private BigDecimal totalCredit;
    private String topCategory;
    private double spendVsLastMonth;       // percentage change

    @Builder.Default
    private List<HiddenCharge> hiddenCharges = new ArrayList<>();

    @Builder.Default
    private List<Subscription> subscriptions = new ArrayList<>();

    @Builder.Default
    private List<EmiInfo> emiList = new ArrayList<>();

    @Builder.Default
    private List<String> savingSuggestions = new ArrayList<>();

    private BigDecimal rewardLeakage;
    private String narrativeSummary;       // AI-generated prose summary

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HiddenCharge {
        private String type;               // LATE_FEE, ANNUAL_FEE, FOREX, etc.
        private String description;
        private BigDecimal amount;
        private String advice;             // "Set up auto-pay to avoid this"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subscription {
        private String merchantName;
        private BigDecimal amount;
        private String frequency;          // MONTHLY, ANNUAL
        private boolean isNew;             // New this month?
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmiInfo {
        private String description;
        private BigDecimal amount;
        private String endDate;
    }
}
