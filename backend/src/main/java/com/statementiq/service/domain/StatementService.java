package com.statementiq.service.domain;

import com.statementiq.exception.ResourceNotFoundException;
import com.statementiq.model.Statement;
import com.statementiq.model.User;
import com.statementiq.repository.StatementRepository;
import com.statementiq.repository.TransactionRepository;
import com.statementiq.service.parse.StatementProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Service
public class StatementService {

    private static final Logger log = LoggerFactory.getLogger(StatementService.class);
    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final StatementProcessingService processingService;

    public StatementService(StatementRepository statementRepository,
                            TransactionRepository transactionRepository,
                            StatementProcessingService processingService) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.processingService = processingService;
    }

    /**
     * Create statement doc with PROCESSING status, trigger async processing.
     * PRIVACY: MultipartFile is passed to processing service in-memory.
     * PDF bytes are NEVER written to disk or any persistent storage.
     */
    public Statement uploadAndProcess(MultipartFile file, User user,
                                      Statement.StatementType type, String bankName,
                                      String cardLast4, String accountLast4, String password) {
        // Create statement document immediately
        Statement statement = Statement.builder()
                .userId(user.getId())
                .statementType(type)
                .bankName(bankName)
                .cardLast4(cardLast4)
                .accountLast4(accountLast4)
                .parseStatus(Statement.ParseStatus.PROCESSING)
                .parseProgress(0)
                .fileName(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .transactionCount(0)
                .build();

        statement = statementRepository.save(statement);
        log.info("Statement created: id={}, type={}, bank={}", statement.getId(), type, bankName);

        byte[] pdfBytes;
        try {
            pdfBytes = file.getBytes();
        } catch (Exception e) {
            log.error("Failed to read file", e);
            throw new RuntimeException("Failed to read uploaded file", e);
        }

        // Trigger async processing — PDF bytes stay in memory
        processingService.processStatementAsync(statement.getId(), pdfBytes, user.getId(), bankName, type, password);

        return statement;
    }

    /**
     * Get statements for a user with optional filters.
     */
    public Page<Statement> getStatements(String userId, String type, String month, PageRequest pageRequest) {
        if (type != null && month != null) {
            return statementRepository.findByUserIdAndStatementTypeAndStatementMonth(
                    userId, Statement.StatementType.valueOf(type.toUpperCase()), month, pageRequest);
        } else if (type != null) {
            return statementRepository.findByUserIdAndStatementType(
                    userId, Statement.StatementType.valueOf(type.toUpperCase()), pageRequest);
        } else if (month != null) {
            return statementRepository.findByUserIdAndStatementMonth(userId, month, pageRequest);
        } else {
            return statementRepository.findByUserId(userId, pageRequest);
        }
    }

    /**
     * Get statement by ID, ensuring it belongs to the requesting user.
     */
    public Statement getStatementByIdAndUser(String id, String userId) {
        return statementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement not found"));
    }

    /**
     * Delete statement and all associated transactions.
     */
    public void deleteStatement(String id, String userId) {
        Statement statement = getStatementByIdAndUser(id, userId);
        transactionRepository.deleteByStatementId(id);
        statementRepository.delete(statement);
        log.info("Deleted statement: id={} with all transactions", id);
    }

    /**
     * Update parse status and progress.
     */
    public void updateParseStatus(String statementId, Statement.ParseStatus status, int progress) {
        statementRepository.findById(statementId).ifPresent(stmt -> {
            stmt.setParseStatus(status);
            stmt.setParseProgress(progress);
            if (status == Statement.ParseStatus.COMPLETED) {
                stmt.setProcessedAt(Instant.now());
            }
            statementRepository.save(stmt);
        });
    }

    /**
     * Update parse error.
     */
    public void updateParseError(String statementId, String error) {
        statementRepository.findById(statementId).ifPresent(stmt -> {
            stmt.setParseStatus(Statement.ParseStatus.FAILED);
            stmt.setParseError(error);
            statementRepository.save(stmt);
        });
    }
}
