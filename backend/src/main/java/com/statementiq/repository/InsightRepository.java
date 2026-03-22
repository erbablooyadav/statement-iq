package com.statementiq.repository;

import com.statementiq.model.Insight;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsightRepository extends MongoRepository<Insight, String> {
    Optional<Insight> findByStatementId(String statementId);
    List<Insight> findByUserId(String userId);
    Optional<Insight> findByStatementIdAndUserId(String statementId, String userId);
}
