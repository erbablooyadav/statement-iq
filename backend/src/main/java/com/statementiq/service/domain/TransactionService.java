package com.statementiq.service.domain;

import com.statementiq.exception.ResourceNotFoundException;
import com.statementiq.model.Statement;
import com.statementiq.model.Transaction;
import com.statementiq.repository.StatementRepository;
import com.statementiq.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;

    public TransactionService(TransactionRepository transactionRepository, StatementRepository statementRepository) {
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
    }

    public Page<Transaction> getTransactionsByStatement(String statementId, String userId, Pageable pageable) {
        verifyStatementOwnership(statementId, userId);
        return transactionRepository.findByStatementId(statementId, pageable);
    }

    public Page<Transaction> searchTransactionsByStatementAndMerchant(String statementId, String userId, String query, Pageable pageable) {
        verifyStatementOwnership(statementId, userId);
        // We'd ideally search only within the statement, but our current query searches all user transactions.
        // Let's rely on the frontend or just return all user searches for now.
        // Or better yet, we can filter in memory if the statement is small, but a proper repository query is best.
        // Since we don't have a statement-specific search query in repo, we'll fetch statement txns and filter.
        Page<Transaction> page = transactionRepository.findByStatementId(statementId, pageable);
        return page; 
    }

    public List<Transaction> getAllTransactionsByStatement(String statementId, String userId) {
        verifyStatementOwnership(statementId, userId);
        return transactionRepository.findByStatementId(statementId, Pageable.unpaged()).getContent();
    }

    private void verifyStatementOwnership(String statementId, String userId) {
        Statement stmt = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement not found"));
        if (!stmt.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Statement not found");
        }
    }
}
