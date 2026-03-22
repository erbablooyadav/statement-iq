package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
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
@Document(collection = "goals")
public class Goal {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String name;
    private String emoji;
    private String description;
    private BigDecimal targetAmount;
    private String targetDate;          // "2027-03-01" format
    private BigDecimal monthlyTarget;

    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Builder.Default
    private GoalStatus status = GoalStatus.ACTIVE;

    @Builder.Default
    private Integer streak = 0;

    @Builder.Default
    private Integer longestStreak = 0;

    @Builder.Default
    private Integer priority = 1;

    private Instant aiPlanGeneratedAt;
    private Instant completedAt;

    @Builder.Default
    private List<Milestone> milestones = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum GoalStatus {
        ACTIVE, COMPLETED, PAUSED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Milestone {
        private String month;           // "2025-03" format
        private BigDecimal target;
        private BigDecimal actual;
        private MilestoneStatus status;

        public enum MilestoneStatus {
            ACHIEVED, MISSED, IN_PROGRESS, UPCOMING
        }
    }
}
