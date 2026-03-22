package com.statementiq.repository;

import com.statementiq.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    Page<Transaction> findByStatementId(String statementId, Pageable pageable);

    Page<Transaction> findByUserId(String userId, Pageable pageable);

    Page<Transaction> findByUserIdAndCategory(String userId, String category, Pageable pageable);

    Page<Transaction> findByUserIdAndTransactionType(String userId, Transaction.TransactionType type, Pageable pageable);

    List<Transaction> findByUserIdAndTransactionDateBetween(String userId, LocalDate from, LocalDate to);

    List<Transaction> findByUserIdAndIsFeeTrue(String userId);

    List<Transaction> findByUserIdAndIsRecurringTrue(String userId);

    List<Transaction> findByUserIdAndIsEmiTrue(String userId);

    List<Transaction> findByUserIdAndIsAtmWithdrawalTrue(String userId);

    List<Transaction> findByUserIdAndIsSalaryCreditTrue(String userId);

    @Query("{'userId': ?0, 'merchantName': {$regex: ?1, $options: 'i'}}")
    Page<Transaction> searchByMerchantName(String userId, String query, Pageable pageable);

    void deleteByStatementId(String statementId);

    long countByStatementId(String statementId);
}
