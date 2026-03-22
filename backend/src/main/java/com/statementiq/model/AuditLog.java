package com.statementiq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Audit log for security and accountability.
 * PRIVACY: Only stores user IDs and resource IDs — never transaction data or PII.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String action;             // UPLOAD, DELETE, EXPORT, LOGIN, CATEGORY_OVERRIDE
    private String resourceType;       // STATEMENT, TRANSACTION, GOAL, USER
    private String resourceId;
    private String ip;
    private String userAgent;

    @Indexed
    private Instant timestamp;
}
