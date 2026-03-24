package com.statementiq.service.parse;

import com.statementiq.model.Statement;
import com.statementiq.model.Transaction;
import com.statementiq.repository.StatementRepository;
import com.statementiq.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Async PDF processing service.
 * PRIVACY: All processing happens in-memory. PDF bytes are never written to disk.
 * After processing, the MultipartFile reference goes out of scope and is GC'd.
 */
@Service
public class StatementProcessingService {

    private static final Logger log = LoggerFactory.getLogger(StatementProcessingService.class);

    private final PdfTextExtractor pdfTextExtractor;
    private final BankIdentifierService bankIdentifierService;
    private final AiDocumentParser aiDocumentParser;
    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final InsightGeneratorService insightGeneratorService;
    private final Executor pdfProcessingExecutor;

    public StatementProcessingService(PdfTextExtractor pdfTextExtractor,
                                      BankIdentifierService bankIdentifierService,
                                      AiDocumentParser aiDocumentParser,
                                      TransactionRepository transactionRepository,
                                      StatementRepository statementRepository,
                                      InsightGeneratorService insightGeneratorService, @Qualifier("pdfProcessingExecutor") Executor pdfProcessingExecutor) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.bankIdentifierService = bankIdentifierService;
        this.aiDocumentParser = aiDocumentParser;
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.insightGeneratorService = insightGeneratorService;
        this.pdfProcessingExecutor = pdfProcessingExecutor;
    }

    /**
     * Process statement asynchronously.
     * Steps:
     * 1. Extract text from PDF bytes page-by-page (in-memory)
     * 2. Identify bank from text headers
     * 3. Parse transactions using chunked AI extraction
     * 4. Persist transactions to MongoDB
     * 5. Update statement status to COMPLETED
     * <p>
     * PRIVACY: PDF bytes stay in memory throughout. Never persisted.
     */
    @Async("pdfProcessingExecutor")
    public void processStatementAsync(String statementId, byte[] pdfBytes,
                                      String userId, String bankName,
                                      Statement.StatementType statementType,
                                      String password) {
        try {
            log.info("Processing statement: id={}, bank={}, type={}", statementId, bankName, statementType);

            // Step 1: Extract text from PDF page-by-page
            updateProgress(statementId, 10);
            List<String> pages = (password != null && !password.isEmpty()) ?
                    pdfTextExtractor.extractTextByPages(pdfBytes, password) :
                    pdfTextExtractor.extractTextByPages(pdfBytes);

            if (pages == null || pages.isEmpty() || pages.stream().allMatch(String::isBlank)) {
                updateError(statementId, "Could not extract text from PDF. The file may be image-based or corrupted.");
                return;
            }

            // Step 2: Identify bank
            updateProgress(statementId, 20);
            String fullText = String.join("\n", pages);
            String detectedBank = bankIdentifierService.identifyBank(fullText);
            if (detectedBank != null && !detectedBank.equalsIgnoreCase(bankName)) {
                log.info("Bank detection mismatch: user={}, detected={}. Using detected.", bankName, detectedBank);
                bankName = detectedBank;
            }

            // Step 3: Parse transactions using chunked AI processing (now parallel internally)
            updateProgress(statementId, 30);
            log.info("Sending {} pages to AI parser for statement {}", pages.size(), statementId);

            final String finalBankName = bankName;
            ParseResult parseResult = aiDocumentParser.parse(pages, statementType, finalBankName, userId,
                    (completedChunks, totalChunks) -> {
                        int currentProgress = 30 + (completedChunks * 30 / totalChunks);
                        updateProgress(statementId, currentProgress);
                    });

            updateProgress(statementId, 65);

            if (parseResult.getTransactions().isEmpty()) {
                updateError(statementId, "No transactions found in the statement. Please check the file format.");
                return;
            }

            // Step 4: Build transaction objects
            List<Transaction> transactions = parseResult.getTransactions().stream()
                    .map(raw -> Transaction.builder()
                            .statementId(statementId)
                            .userId(userId)
                            .transactionDate(raw.getDate())
                            .rawDescription(raw.getRawDescription())
                            .merchantName(raw.getMerchantName())
                            .description(raw.getDescription())
                            .amount(raw.getAmount())
                            .transactionType(raw.getType())
                            .transactionChannel(raw.getChannel())
                            .closingBalance(raw.getClosingBalance())
                            .category(raw.getCategory() != null ? raw.getCategory() : "Miscellaneous")
                            .subCategory(raw.getSubCategory())
                            .isFee(raw.isFee())
                            .isEmi(raw.isEmi())
                            .isRecurring(raw.isRecurring())
                            .isAtmWithdrawal(raw.isAtmWithdrawal())
                            .isSalaryCredit(raw.isSalaryCredit())
                            .build())
                    .toList();

            // Step 5: Save transactions
            transactionRepository.saveAll(transactions);
            updateProgress(statementId, 80);

            // Step 6: Calculate totals
            BigDecimal totalDebit = transactions.stream()
                    .filter(t -> t.getTransactionType() == Transaction.TransactionType.DEBIT)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCredit = transactions.stream()
                    .filter(t -> t.getTransactionType() == Transaction.TransactionType.CREDIT)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Step 7: Mark statement COMPLETED immediately — user can see transactions NOW
            // Insights will be generated in background
            statementRepository.findById(statementId).ifPresent(stmt -> {
                stmt.setParseStatus(Statement.ParseStatus.COMPLETED);
                stmt.setParseProgress(90);
                stmt.setTransactionCount(transactions.size());
                stmt.setTotalDebit(totalDebit);
                stmt.setTotalCredit(totalCredit);
                stmt.setParseConfidence(parseResult.getConfidence());
                stmt.setStatementMonth(parseResult.getStatementMonth());
                if (parseResult.getBillDueDate() != null) stmt.setBillDueDate(parseResult.getBillDueDate());
                if (parseResult.getTotalAmountDue() != null) stmt.setTotalAmountDue(parseResult.getTotalAmountDue());
                if (parseResult.getMinimumAmountDue() != null)
                    stmt.setMinimumAmountDue(parseResult.getMinimumAmountDue());
                statementRepository.save(stmt);
            });

            log.info("Statement COMPLETED (transactions visible): id={}, transactions={}", statementId, transactions.size());

            // Step 8: Generate insights in BACKGROUND — don't make user wait for this
            final List<Transaction> finalTransactions = transactions;
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("Generating insights in background for statement {}", statementId);
                    insightGeneratorService.generateAndSaveInsight(statementId, userId, finalTransactions);

                    // Mark fully done (100%) after insights
                    statementRepository.findById(statementId).ifPresent(stmt -> {
                        stmt.setParseProgress(100);
                        stmt.setProcessedAt(java.time.Instant.now());
                        statementRepository.save(stmt);
                    });

                    log.info("Background insight generation complete for statement {}", statementId);
                } catch (Exception e) {
                    // Insights failing should NOT affect the user — transactions are already visible
                    log.error("Background insight generation failed for statement {}: {}", statementId, e.getMessage());
                }
            }, pdfProcessingExecutor);

        } catch (Exception e) {
            log.error("Error processing statement: id={}", statementId, e);
            updateError(statementId, "Processing failed: " + e.getMessage());
        }
    }

    private void updateProgress(String statementId, int progress) {
        statementRepository.findById(statementId).ifPresent(stmt -> {
            stmt.setParseProgress(progress);
            statementRepository.save(stmt);
        });
    }

    private void updateError(String statementId, String error) {
        statementRepository.findById(statementId).ifPresent(stmt -> {
            stmt.setParseStatus(Statement.ParseStatus.FAILED);
            stmt.setParseError(error);
            statementRepository.save(stmt);
        });
    }
}
