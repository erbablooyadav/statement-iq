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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String firebaseUid;

    @Indexed(unique = true)
    private String email;

    private String name;
    private String photoUrl;

    @Builder.Default
    private Plan plan = Plan.FREE;

    private String razorpaySubscriptionId;
    private String razorpayCustomerId;
    private SubscriptionStatus subscriptionStatus;
    private Instant planExpiresAt;

    @Builder.Default
    private NotificationPreferences notificationPreferences = new NotificationPreferences();

    // AI Provider preferences (BYOK)
    @Builder.Default
    private AiProvider preferredAiProvider = AiProvider.CLAUDE;

    /** Encrypted API keys per provider: { "CLAUDE": "sk-ant-...", "GEMINI": "AIza..." } */
    @Builder.Default
    private Map<String, String> aiApiKeys = new HashMap<>();

    /** Model overrides per provider: { "OPENAI": "gpt-4o", "GEMINI": "gemini-1.5-pro" } */
    @Builder.Default
    private Map<String, String> aiModelOverrides = new HashMap<>();

    /** Local model endpoint URL (e.g., http://localhost:11434 for Ollama) */
    private String localModelUrl;

    /** Local model name (e.g., llama3, mistral) */
    private String localModelName;

    private int totalStatementsUploaded;
    private int todayUploads;
    private String lastUploadDate;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum Plan {
        FREE, PRO
    }

    public enum SubscriptionStatus {
        ACTIVE, CANCELLED, PAUSED, EXPIRED, PENDING
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPreferences {
        @Builder.Default
        private boolean billDueReminders = true;
        @Builder.Default
        private boolean overspendAlerts = true;
        @Builder.Default
        private boolean goalNudges = true;
        @Builder.Default
        private boolean monthlyReportCard = true;
        @Builder.Default
        private boolean weeklyGoalSummary = true;
    }
}
