package com.statementiq.repository;

import com.statementiq.model.GoalCheckIn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalCheckInRepository extends MongoRepository<GoalCheckIn, String> {
    List<GoalCheckIn> findByGoalIdOrderByDateDesc(String goalId);
    List<GoalCheckIn> findByGoalIdAndDate(String goalId, String date);
    long countByGoalId(String goalId);
}
