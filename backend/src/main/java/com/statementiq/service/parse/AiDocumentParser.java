package com.statementiq.service.parse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.statementiq.dto.AiResponse;
import com.statementiq.model.Statement;
import com.statementiq.model.User;
import com.statementiq.service.ai.AiProviderRouter;
import com.statementiq.service.domain.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Uses the user's selected AI provider to extract transactions from raw statement text.
 * Supports chunked processing: splits large PDFs into manageable chunks, processes each
 * independently via AI, then merges and deduplicates results.
 */
@Service
public class AiDocumentParser {

    private static final Logger log = LoggerFactory.getLogger(AiDocumentParser.class);
    private static final int MAX_CHARS_PER_CHUNK = 50000;
    private static final int MAX_OUTPUT_TOKENS = 16384;
    private static final String SYSTEM_PROMPT =
            "You are an expert financial data extraction system. You must output ONLY RAW JSON. No markdown, no conversational text.";

    private final AiProviderRouter aiProviderRouter;
    private final UserService userService;
    private final Gson gson;
    private final Executor pdfProcessingExecutor;

    public AiDocumentParser(AiProviderRouter aiProviderRouter,
                            UserService userService,
                            @Qualifier("pdfProcessingExecutor") Executor pdfProcessingExecutor) {
        this.aiProviderRouter = aiProviderRouter;
        this.userService = userService;
        this.pdfProcessingExecutor = pdfProcessingExecutor;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                    @Override
                    public void write(JsonWriter out, LocalDate value) throws IOException {
                        out.value(value != null ? value.toString() : null);
                    }

                    @Override
                    public LocalDate read(JsonReader in) throws IOException {
                        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                            in.nextNull();
                            return null;
                        }
                        return LocalDate.parse(in.nextString());
                    }
                })
                .create();
    }

    /**
     * Parse statement using chunked page-level processing.
     *
     * @param pages List of page texts extracted from the PDF
     */
    public ParseResult parse(List<String> pages, Statement.StatementType type, String bankName, String userId,
                             java.util.function.BiConsumer<Integer, Integer> progressCallback) {
        User user = userService.getUserById(userId);
        List<List<String>> chunks = groupPagesIntoChunks(pages);

        log.info("Processing {} pages in {} chunks for user={}", pages.size(), chunks.size(), userId);

        if (chunks.size() == 1) {
            String chunkText = String.join("\n", chunks.get(0));
            ParseResult result = parseSingleChunk(chunkText, type, bankName, user, 1, 1);
            if (progressCallback != null) progressCallback.accept(1, 1);
            return result;
        }

        // PARALLEL chunk processing — all chunks fire simultaneously
        List<CompletableFuture<ParseResult>> futures = new ArrayList<>();
        int totalChunks = chunks.size();

        for (int i = 0; i < totalChunks; i++) {
            final String chunkText = String.join("\n", chunks.get(i));
            final int chunkIndex = i + 1;

            CompletableFuture<ParseResult> future = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return parseSingleChunk(chunkText, type, bankName, user, chunkIndex, totalChunks);
                        } catch (Exception e) {
                            log.warn("Chunk {}/{} failed: {}. Returning empty result.", chunkIndex, totalChunks, e.getMessage());
                            return null; // handled in merge step
                        }
                    }, pdfProcessingExecutor);

            futures.add(future);
        }

        // Wait for ALL chunks to complete simultaneously
        List<ParseResult> chunkResults = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                ParseResult result = futures.get(i).get(60, java.util.concurrent.TimeUnit.SECONDS);
                if (result != null && result.getTransactions() != null && !result.getTransactions().isEmpty()) {
                    chunkResults.add(result);
                    log.info("Chunk {}/{}: extracted {} transactions", i + 1, totalChunks, result.getTransactions().size());
                }
                if (progressCallback != null) progressCallback.accept(i + 1, totalChunks);
            } catch (Exception e) {
                log.warn("Chunk {}/{} timed out or failed: {}", i + 1, totalChunks, e.getMessage());
            }
        }

        if (chunkResults.isEmpty()) {
            throw new RuntimeException("All AI parsing chunks failed. No transactions extracted.");
        }

        return mergeResults(chunkResults, bankName);
    }

    /**
     * Legacy single-text parse method (backward compatible).
     */
    public ParseResult parse(String rawText, Statement.StatementType type, String bankName, String userId) {
        // Split by form-feed (PDFBox page separator) or treat as single chunk
        List<String> pages = Arrays.stream(rawText.split("\\f"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        if (pages.isEmpty()) {
            pages = List.of(rawText);
        }
        return parse(pages, type, bankName, userId, null);
    }

    // ─── Chunking ─────────────────────────────────────────────

    /**
     * Group pages into chunks, keeping each chunk under MAX_CHARS_PER_CHUNK.
     * Pages are never split — a single page always stays together even if it exceeds the limit.
     */
    private List<List<String>> groupPagesIntoChunks(List<String> pages) {
        List<List<String>> chunks = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();
        int currentSize = 0;

        for (String page : pages) {
            if (currentSize + page.length() > MAX_CHARS_PER_CHUNK && !currentChunk.isEmpty()) {
                chunks.add(currentChunk);
                currentChunk = new ArrayList<>();
                currentSize = 0;
            }
            currentChunk.add(page);
            currentSize += page.length();
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }

        return chunks;
    }

    // ─── Single Chunk AI Call ─────────────────────────────────

    private ParseResult parseSingleChunk(String chunkText, Statement.StatementType type,
                                         String bankName, User user,
                                         int chunkIndex, int totalChunks) {
        String prompt = buildPrompt(chunkText, type, bankName, chunkIndex, totalChunks);

        AiResponse response = aiProviderRouter.route(SYSTEM_PROMPT, prompt, user, MAX_OUTPUT_TOKENS);

        if (!response.success() || response.content() == null) {
            throw new RuntimeException("AI provider failed: " + response.error());
        }

        String rawJson = cleanJsonResponse(response.content());
        log.debug("Chunk {}/{} raw JSON length: {} chars", chunkIndex, totalChunks, rawJson.length());

        ParseResult result = gson.fromJson(rawJson, ParseResult.class);
        result.setParserUsed("AI_EXTRACTION");
        result.setBankName(bankName);
        return result;
    }

    private String cleanJsonResponse(String raw) {
        if (raw.startsWith("```json")) {
            raw = raw.substring(7);
        } else if (raw.startsWith("```")) {
            raw = raw.substring(3);
        }
        if (raw.endsWith("```")) {
            raw = raw.substring(0, raw.length() - 3);
        }
        return raw.trim();
    }

    // ─── Merge & Deduplicate ─────────────────────────────────

    private ParseResult mergeResults(List<ParseResult> chunkResults, String bankName) {
        List<RawTransaction> allTransactions = new ArrayList<>();
        String statementMonth = null;
        String billDueDate = null;
        java.math.BigDecimal totalAmountDue = null;
        java.math.BigDecimal minimumAmountDue = null;
        double totalConfidence = 0;

        for (ParseResult result : chunkResults) {
            if (result.getTransactions() != null) {
                allTransactions.addAll(result.getTransactions());
            }
            if (statementMonth == null && result.getStatementMonth() != null) {
                statementMonth = result.getStatementMonth();
            }
            if (billDueDate == null && result.getBillDueDate() != null) {
                billDueDate = result.getBillDueDate();
            }
            if (totalAmountDue == null && result.getTotalAmountDue() != null) {
                totalAmountDue = result.getTotalAmountDue();
            }
            if (minimumAmountDue == null && result.getMinimumAmountDue() != null) {
                minimumAmountDue = result.getMinimumAmountDue();
            }
            totalConfidence += result.getConfidence();
        }

        // Deduplicate transactions (same date + amount + raw description)
        List<RawTransaction> deduplicated = deduplicateTransactions(allTransactions);

        log.info("Merged {} chunks: {} raw transactions → {} after deduplication",
                chunkResults.size(), allTransactions.size(), deduplicated.size());

        ParseResult merged = new ParseResult();
        merged.setTransactions(deduplicated);
        merged.setConfidence(totalConfidence / chunkResults.size());
        merged.setStatementMonth(statementMonth);
        merged.setBillDueDate(billDueDate);
        merged.setTotalAmountDue(totalAmountDue);
        merged.setMinimumAmountDue(minimumAmountDue);
        merged.setBankName(bankName);
        merged.setParserUsed("AI_EXTRACTION_CHUNKED");
        return merged;
    }

    private List<RawTransaction> deduplicateTransactions(List<RawTransaction> transactions) {
        Set<String> seen = new LinkedHashSet<>();
        List<RawTransaction> unique = new ArrayList<>();

        for (RawTransaction tx : transactions) {
            String key = (tx.getDate() != null ? tx.getDate().toString() : "null") + "|" +
                    (tx.getAmount() != null ? tx.getAmount().toPlainString() : "null") + "|" +
                    (tx.getRawDescription() != null ? tx.getRawDescription().trim().toLowerCase() : "null");
            if (seen.add(key)) {
                unique.add(tx);
            }
        }
        return unique;
    }

    // ─── Prompt Builder ──────────────────────────────────────

    private String buildPrompt(String rawText, Statement.StatementType type, String bankName,
                               int chunkIndex, int totalChunks) {
        String chunkInfo = totalChunks > 1
                ? "This is part " + chunkIndex + " of " + totalChunks + " of the statement. " +
                "Extract ONLY the transactions visible in this part. Do NOT invent or guess missing transactions.\n\n"
                : "";

        return chunkInfo +
                "Extract all financial transactions from the following bank statement text. " +
                "This is a " + type.name() + " statement from " + bankName + ".\n\n" +
                "RULES:\n" +
                "1. Extract EVERY SINGLE transaction visible in this text. Do not miss any.\n" +
                "2. Clean up 'rawDescription' into a concise 'merchantName' (e.g. 'UPI/Paytm/Swiggy' -> 'Swiggy') and a readable 'description'.\n" +
                "3. Classify the transaction into ONE of these categories: Housing, Transportation, Food & Dining, Utilities, Healthcare, Insurance, Personal, Education, Debt, Saving & Investing, Income, Miscellaneous.\n" +
                "4. Detect if it's a fee (isFee), EMI (isEmi), subscription (isRecurring), ATM withdrawal (isAtmWithdrawal), or salary (isSalaryCredit).\n" +
                "5. Output valid JSON exactly matching this structure (no extra keys, no missing required fields):\n" +
                "{\n" +
                "  \"confidence\": 0.95,\n" +
                "  \"statementMonth\": \"2023-10\",\n" +
                "  \"billDueDate\": \"2023-11-05\", // YYYY-MM-DD, null if not applicable or not visible in this part\n" +
                "  \"totalAmountDue\": 10500.50, // null if not applicable or not visible in this part\n" +
                "  \"minimumAmountDue\": 500.00, // null if not applicable or not visible in this part\n" +
                "  \"transactions\": [\n" +
                "    {\n" +
                "       \"date\": \"YYYY-MM-DD\",\n" +
                "       \"rawDescription\": \"original raw line\",\n" +
                "       \"merchantName\": \"Clean Name\",\n" +
                "       \"description\": \"Human readable description\",\n" +
                "       \"amount\": 450.00,\n" +
                "       \"type\": \"DEBIT\", // or CREDIT\n" +
                "       \"channel\": \"UPI\", // UPI, POS, NEFT, ATM, etc\n" +
                "       \"closingBalance\": 10500.00, // running balance if present\n" +
                "       \"category\": \"Food & Dining\",\n" +
                "       \"subCategory\": \"Delivery\",\n" +
                "       \"isFee\": false,\n" +
                "       \"isEmi\": false,\n" +
                "       \"isRecurring\": false,\n" +
                "       \"isAtmWithdrawal\": false,\n" +
                "       \"isSalaryCredit\": false\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "IMPORTANT: Ensure your output is purely the JSON object, absolutely zero additional conversational text. If there are no transactions in this part, return an empty array for transactions.\n\n" +
                "STATEMENT TEXT START:\n" +
                "=====================\n" +
                rawText + "\n" +
                "=====================\n" +
                "STATEMENT TEXT END\n";
    }
}

