package com.statementiq.service.domain;

import com.statementiq.exception.ResourceNotFoundException;
import com.statementiq.model.Insight;
import com.statementiq.model.Statement;
import com.statementiq.repository.InsightRepository;
import com.statementiq.repository.StatementRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InsightService {

    private final InsightRepository insightRepository;
    private final StatementRepository statementRepository;

    public InsightService(InsightRepository insightRepository, StatementRepository statementRepository) {
        this.insightRepository = insightRepository;
        this.statementRepository = statementRepository;
    }

    public Insight getInsightByStatement(String statementId, String userId) {
        verifyStatementOwnership(statementId, userId);
        return insightRepository.findByStatementId(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Insight not found for this statement"));
    }

    public List<Insight> getAllInsightsForUser(String userId) {
        return insightRepository.findByUserId(userId);
    }

    private void verifyStatementOwnership(String statementId, String userId) {
        Statement stmt = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement not found"));
        if (!stmt.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Statement not found");
        }
    }
}
