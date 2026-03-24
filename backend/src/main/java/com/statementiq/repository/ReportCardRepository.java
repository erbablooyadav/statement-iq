package com.statementiq.repository;

import com.statementiq.model.ReportCard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportCardRepository extends MongoRepository<ReportCard, String> {
    List<ReportCard> findByUserIdOrderByMonthDesc(String userId);
    Optional<ReportCard> findByUserIdAndMonth(String userId, String month);
}
