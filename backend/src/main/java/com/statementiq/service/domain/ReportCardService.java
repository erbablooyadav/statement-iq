package com.statementiq.service.domain;

import com.statementiq.model.Insight;
import com.statementiq.model.ReportCard;
import com.statementiq.model.Transaction;
import com.statementiq.repository.InsightRepository;
import com.statementiq.repository.ReportCardRepository;
import com.statementiq.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportCardService {

    private static final Logger log = LoggerFactory.getLogger(ReportCardService.class);

    private final TransactionRepository transactionRepository;
    private final InsightRepository insightRepository;
    private final ReportCardRepository reportCardRepository;

    public ReportCardService(TransactionRepository transactionRepository,
                             InsightRepository insightRepository,
                             ReportCardRepository reportCardRepository) {
        this.transactionRepository = transactionRepository;
        this.insightRepository = insightRepository;
        this.reportCardRepository = reportCardRepository;
    }

    /**
     * Generate (or refresh) the ReportCard for a user for the current month.
     */
    public ReportCard generateForCurrentMonth(String userId) {
        String month = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return generateForMonth(userId, month);
    }

    public ReportCard generateForMonth(String userId, String month) {
        // Parse month boundaries
        YearMonth ym = YearMonth.parse(month);
        java.time.LocalDate start = ym.atDay(1);
        java.time.LocalDate end = ym.atEndOfMonth();

        // Fetch transactions for this month
        List<Transaction> monthlyTxns = transactionRepository
                .findByUserIdAndTransactionDateBetween(userId, start, end);

        BigDecimal totalIncome = monthlyTxns.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.CREDIT)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = monthlyTxns.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.DEBIT)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSaved = totalIncome.subtract(totalSpent);

        // Compute scores (0-100)
        int savingsScore = computeSavingsScore(totalIncome, totalSaved);
        int spendControlScore = computeSpendControlScore(monthlyTxns);
        int hiddenChargeScore = computeHiddenChargeScore(userId, month);
        int overallScore = (savingsScore + spendControlScore + hiddenChargeScore) / 3;

        // Top category
        String topCategory = monthlyTxns.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.DEBIT
                        && t.getCategory() != null)
                .collect(Collectors.groupingBy(Transaction::getCategory,
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("General");

        String badge = computeBadge(overallScore, savingsScore, totalSaved);

        String narrative = String.format(
                "This month you spent %s across %d transactions. Your top category was %s. " +
                "You %s ₹%s overall. %s",
                formatCurrency(totalSpent),
                monthlyTxns.size(),
                topCategory,
                totalSaved.compareTo(BigDecimal.ZERO) >= 0 ? "saved" : "overspent by",
                totalSaved.abs().setScale(0, RoundingMode.HALF_UP),
                badge.contains("Saver") ? "Great job keeping expenses in check! 🎉" : "Consider reviewing your top spending category."
        );

        ReportCard card = ReportCard.builder()
                .userId(userId)
                .month(month)
                .overallScore(overallScore)
                .savingsScore(savingsScore)
                .spendControlScore(spendControlScore)
                .hiddenChargeScore(hiddenChargeScore)
                .goalAdherenceScore(0) // placeholder until goals are linked
                .totalIncome(totalIncome)
                .totalSpent(totalSpent)
                .totalSaved(totalSaved)
                .transactionCount(monthlyTxns.size())
                .topCategory(topCategory)
                .badge(badge)
                .aiNarrative(narrative)
                .generatedAt(Instant.now())
                .build();

        // Upsert
        reportCardRepository.findByUserIdAndMonth(userId, month)
                .ifPresent(existing -> card.setId(existing.getId()));

        return reportCardRepository.save(card);
    }

    private int computeSavingsScore(BigDecimal income, BigDecimal saved) {
        if (income.compareTo(BigDecimal.ZERO) == 0) return 50;
        double ratio = saved.divide(income, 4, RoundingMode.HALF_UP).doubleValue();
        // 0% saved = 0, 20%+ saved = 100
        return (int) Math.max(0, Math.min(100, ratio * 500));
    }

    private int computeSpendControlScore(List<Transaction> txns) {
        long feeCount = txns.stream().filter(Transaction::isFee).count();
        return (int) Math.max(0, 100 - feeCount * 20);
    }

    private int computeHiddenChargeScore(String userId, String month) {
        // Check if any insight for this user this month flags hidden charges
        List<Insight> insights = insightRepository.findByUserId(userId);
        boolean hasHiddenCharges = insights.stream()
                .filter(i -> i.getGeneratedAt() != null)
                .anyMatch(i -> {
                    String iMonth = YearMonth.from(i.getGeneratedAt().atZone(java.time.ZoneId.systemDefault())).format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    return month.equals(iMonth) && !i.getHiddenCharges().isEmpty();
                });
        return hasHiddenCharges ? 40 : 100;
    }

    private String computeBadge(int overallScore, int savingsScore, BigDecimal saved) {
        if (overallScore >= 80) return "Financial Champion 🏆";
        if (savingsScore >= 70 && saved.compareTo(BigDecimal.ZERO) > 0) return "Smart Saver 💰";
        if (overallScore >= 50) return "On Track 📈";
        return "Needs Review 🔍";
    }

    private String formatCurrency(BigDecimal amount) {
        return "₹" + amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
