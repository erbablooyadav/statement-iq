package com.statementiq.service.parse;

import com.statementiq.model.Statement;
import com.statementiq.model.Transaction;
import com.statementiq.repository.StatementRepository;
import com.statementiq.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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

    public StatementProcessingService(PdfTextExtractor pdfTextExtractor,
                                      BankIdentifierService bankIdentifierService,
                                      AiDocumentParser aiDocumentParser,
                                      TransactionRepository transactionRepository,
                                      StatementRepository statementRepository,
                                      InsightGeneratorService insightGeneratorService) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.bankIdentifierService = bankIdentifierService;
        this.aiDocumentParser = aiDocumentParser;
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.insightGeneratorService = insightGeneratorService;
    }

    /**
     * Process statement asynchronously.
     * Steps:
     * 1. Extract text from PDF bytes page-by-page (in-memory)
     * 2. Identify bank from text headers
     * 3. Parse transactions using chunked AI extraction
     * 4. Persist transactions to MongoDB
     * 5. Update statement status to COMPLETED
     *
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

            // Step 2: Identify bank (use full text for header detection)
            updateProgress(statementId, 20);
            String fullText = String.join("\n", pages);
            String detectedBank = bankIdentifierService.identifyBank(fullText);
            if (detectedBank != null && !detectedBank.equalsIgnoreCase(bankName)) {
                log.info("Bank detection mismatch: user={}, detected={}. Using detected.", bankName, detectedBank);
                bankName = detectedBank;
            }

            // Step 3: Parse transactions using chunked AI processing
            updateProgress(statementId, 30);
            log.info("Sending {} pages to AI parser for statement {}", pages.size(), statementId);
            ParseResult parseResult = aiDocumentParser.parse(pages, statementType, bankName, userId, 
                    (completedChunks, totalChunks) -> {
                        int currentProgress = 30 + (completedChunks * 30 / totalChunks);
                        updateProgress(statementId, currentProgress);
                    });

            updateProgress(statementId, 60);

            if (parseResult.getTransactions().isEmpty()) {
                updateError(statementId, "No transactions found in the statement. Please check the file format.");
                return;
            }

            // Step 4: Enrich and persist transactions
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

            transactionRepository.saveAll(transactions);
            updateProgress(statementId, 80);

            // Step 5: Update statement with totals
            BigDecimal totalDebit = transactions.stream()
                    .filter(t -> t.getTransactionType() == Transaction.TransactionType.DEBIT)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCredit = transactions.stream()
                    .filter(t -> t.getTransactionType() == Transaction.TransactionType.CREDIT)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            statementRepository.findById(statementId).ifPresent(stmt -> {
                stmt.setParseStatus(Statement.ParseStatus.COMPLETED);
                stmt.setParseProgress(95);
                stmt.setTransactionCount(transactions.size());
                stmt.setTotalDebit(totalDebit);
                stmt.setTotalCredit(totalCredit);
                stmt.setParseConfidence(parseResult.getConfidence());
                stmt.setStatementMonth(parseResult.getStatementMonth());
                if (parseResult.getBillDueDate() != null) {
                    stmt.setBillDueDate(parseResult.getBillDueDate());
                }
                if (parseResult.getTotalAmountDue() != null) {
                    stmt.setTotalAmountDue(parseResult.getTotalAmountDue());
                }
                if (parseResult.getMinimumAmountDue() != null) {
                    stmt.setMinimumAmountDue(parseResult.getMinimumAmountDue());
                }
                statementRepository.save(stmt);
            });

            // Step 6: Generate AI Insights
            updateProgress(statementId, 95);
            log.info("Generating final insights for statement {}", statementId);
            insightGeneratorService.generateAndSaveInsight(statementId, userId, transactions);
            updateProgress(statementId, 100);

            log.info("Statement processing complete: id={}, transactions={}, confidence={}",
                    statementId, transactions.size(), parseResult.getConfidence());

        } catch (Exception e) {
            log.error("Error processing statement: id={}", statementId, e);
            updateError(statementId, "Processing failed: " + e.getMessage());
        }
        // MultipartFile goes out of scope here — bytes eligible for GC
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
