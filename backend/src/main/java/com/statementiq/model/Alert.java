package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alerts")
public class Alert {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String statementId;

    private AlertType type;
    private String title;
    private String description;
    
    private BigDecimal amount;
    private AlertSeverity severity;

    @Builder.Default
    private boolean isRead = false;

    @CreatedDate
    private Instant createdAt;

    public enum AlertType {
        HIDDEN_CHARGE,
        OVERSPEND,
        BILL_DUE,
        REWARD_LEAKAGE,
        SECURITY,
        SAVING_OPPORTUNITY
    }

    public enum AlertSeverity {
        HIGH, MEDIUM, LOW
    }
}
