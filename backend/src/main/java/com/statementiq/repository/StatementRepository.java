package com.statementiq.repository;

import com.statementiq.model.Statement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatementRepository extends MongoRepository<Statement, String> {
    List<Statement> findByUserIdOrderByUploadedAtDesc(String userId);

    Page<Statement> findByUserId(String userId, Pageable pageable);

    Page<Statement> findByUserIdAndStatementType(String userId, Statement.StatementType type, Pageable pageable);

    Page<Statement> findByUserIdAndStatementMonth(String userId, String month, Pageable pageable);

    Page<Statement> findByUserIdAndStatementTypeAndStatementMonth(
            String userId, Statement.StatementType type, String month, Pageable pageable);

    List<Statement> findByUserIdAndParseStatus(String userId, Statement.ParseStatus status);

    Optional<Statement> findByIdAndUserId(String id, String userId);

    long countByUserIdAndUploadedAtBetween(String userId, java.time.Instant start, java.time.Instant end);
}
