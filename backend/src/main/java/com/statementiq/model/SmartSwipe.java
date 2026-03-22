package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "smart_swipes")
public class SmartSwipe {

    @Id
    private String id;

    @Indexed
    private String userId;

    private List<CardRecommendation> recommendations;

    private Instant generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardRecommendation {
        private String category;
        private String cardName;
        private String benefitDescription;
        private String potentialSaving;
    }
}
