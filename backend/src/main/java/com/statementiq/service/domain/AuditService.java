package com.statementiq.service.domain;

import com.statementiq.model.AuditLog;
import com.statementiq.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Audit logging service.
 * PRIVACY: Only logs action type + resource IDs.
 * NEVER logs transaction data, amounts, or merchant names.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String userId, String action, String resourceType, String resourceId, String ip, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ip(ip)
                .userAgent(userAgent)
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(auditLog);
    }
}
