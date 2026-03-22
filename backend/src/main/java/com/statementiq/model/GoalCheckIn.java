package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "goal_checkins")
@CompoundIndex(name = "goal_date_idx", def = "{'goalId': 1, 'date': -1}")
public class GoalCheckIn {

    @Id
    private String id;

    private String goalId;
    private String userId;
    private String date;               // "2025-03-22" — simple string for date matching
    private BigDecimal amount;
    private BigDecimal cumulativeTotal;
    private String note;

    @CreatedDate
    private Instant createdAt;
}
