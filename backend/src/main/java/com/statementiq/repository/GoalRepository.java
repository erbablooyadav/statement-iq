package com.statementiq.repository;

import com.statementiq.model.Goal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends MongoRepository<Goal, String> {
    List<Goal> findByUserId(String userId);
    List<Goal> findByUserIdOrderByPriorityAsc(String userId);
    List<Goal> findByUserIdAndStatus(String userId, Goal.GoalStatus status);
    Optional<Goal> findByIdAndUserId(String id, String userId);
    long countByUserIdAndStatus(String userId, Goal.GoalStatus status);
}
