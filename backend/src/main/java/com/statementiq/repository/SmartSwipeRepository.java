package com.statementiq.repository;

import com.statementiq.model.SmartSwipe;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmartSwipeRepository extends MongoRepository<SmartSwipe, String> {
    Optional<SmartSwipe> findFirstByUserIdOrderByGeneratedAtDesc(String userId);
}
