package com.statementiq.repository;

import com.statementiq.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends MongoRepository<Alert, String> {
    List<Alert> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Alert> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);
}
