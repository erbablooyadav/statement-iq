package com.statementiq.repository;

import com.statementiq.model.ReportCard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportCardRepository extends MongoRepository<ReportCard, String> {
    Optional<ReportCard> findByUserIdAndReportMonth(String userId, String reportMonth);
    List<ReportCard> findByUserIdOrderByReportMonthDesc(String userId);
}
