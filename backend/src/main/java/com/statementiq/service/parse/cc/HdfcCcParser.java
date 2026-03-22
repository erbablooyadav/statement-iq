package com.statementiq.service.parse.cc;

import com.statementiq.model.Statement;
import com.statementiq.model.Transaction;
import com.statementiq.service.parse.ParseResult;
import com.statementiq.service.parse.RawTransaction;
import com.statementiq.service.parse.StatementParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HDFC Credit Card statement parser.
 * Handles HDFC CC statements which typically have tabular layouts
 * with Date | Description | Amount | Dr/Cr columns.
 */
@Component
public class HdfcCcParser implements StatementParser {

    private static final Logger log = LoggerFactory.getLogger(HdfcCcParser.class);

    // HDFC CC date format: DD/MM/YYYY or DD/MM/YY
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMAT_SHORT = DateTimeFormatter.ofPattern("dd/MM/yy");

    // Transaction line pattern: Date Description Amount [Cr]
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{2,4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s*(Cr)?\\s*$",
            Pattern.MULTILINE
    );

    // Bill summary patterns
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile(
            "(?:Payment\\s+Due\\s+Date|Due\\s+Date)[:\\s]*(\\d{2}/\\d{2}/\\d{2,4})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TOTAL_DUE_PATTERN = Pattern.compile(
            "(?:Total\\s+Amount\\s+Due|Amount\\s+Payable)[:\\s]*(?:Rs\\.?|INR)?\\s*([\\d,]+\\.\\d{2})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MIN_DUE_PATTERN = Pattern.compile(
            "(?:Minimum\\s+Amount\\s+Due|Min\\.?\\s+Due)[:\\s]*(?:Rs\\.?|INR)?\\s*([\\d,]+\\.\\d{2})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STATEMENT_PERIOD_PATTERN = Pattern.compile(
            "(?:Statement\\s+Period|Statement\\s+Date)[:\\s]*(\\d{2}/\\d{2}/\\d{2,4})",
            Pattern.CASE_INSENSITIVE
    );

    // Fee detection keywords
    private static final String[] FEE_KEYWORDS = {
            "LATE PAYMENT", "ANNUAL FEE", "MEMBERSHIP FEE", "FINANCE CHARGE",
            "INTEREST", "GST", "IGST", "OVERLIMIT", "OVER LIMIT", "CASH ADVANCE FEE",
            "FOREX", "FOREIGN TRANSACTION", "SERVICE TAX", "REWARD REVERSAL"
    };

    private static final String[] EMI_KEYWORDS = {"EMI", "INSTALMENT", "INSTALLMENT"};
    private static final String[] ATM_KEYWORDS = {"ATM", "CASH ADVANCE", "CASH WITHDRAWAL"};

    @Override
    public ParseResult parse(String pdfText, Statement.StatementType statementType) {
        List<RawTransaction> transactions = new ArrayList<>();
        double confidence = 0.85;

        // Extract bill metadata
        String dueDate = extractPattern(pdfText, DUE_DATE_PATTERN);
        BigDecimal totalDue = extractAmount(pdfText, TOTAL_DUE_PATTERN);
        BigDecimal minDue = extractAmount(pdfText, MIN_DUE_PATTERN);

        // Extract statement month
        String statementMonth = extractStatementMonth(pdfText);

        // Parse transaction lines
        Matcher matcher = TRANSACTION_PATTERN.matcher(pdfText);
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amountStr = matcher.group(3).replace(",", "");
                boolean isCredit = matcher.group(4) != null;

                LocalDate date = parseDate(dateStr);
                BigDecimal amount = new BigDecimal(amountStr);

                // Detect category flags
                String upperDesc = description.toUpperCase();
                boolean isFee = containsKeyword(upperDesc, FEE_KEYWORDS);
                boolean isEmi = containsKeyword(upperDesc, EMI_KEYWORDS);
                boolean isAtm = containsKeyword(upperDesc, ATM_KEYWORDS);

                // Auto-categorize fees
                String category = isFee ? "Fees & Charges"
                        : isEmi ? "EMI & Loans"
                        : null;  // Will be enriched by AI later

                RawTransaction raw = RawTransaction.builder()
                        .date(date)
                        .rawDescription(description)
                        .merchantName(cleanMerchantName(description))
                        .description(description)
                        .amount(amount)
                        .type(isCredit ? Transaction.TransactionType.CREDIT : Transaction.TransactionType.DEBIT)
                        .channel("POS")
                        .isFee(isFee)
                        .isEmi(isEmi)
                        .isAtmWithdrawal(isAtm)
                        .category(category)
                        .build();

                transactions.add(raw);
            } catch (Exception e) {
                log.warn("Failed to parse HDFC CC transaction line: {}", matcher.group(0), e);
                confidence -= 0.02;  // Reduce confidence for each parsing error
            }
        }

        if (transactions.isEmpty()) {
            confidence = 0.0;
        }

        return ParseResult.builder()
                .transactions(transactions)
                .confidence(Math.max(0, confidence))
                .statementMonth(statementMonth)
                .billDueDate(dueDate)
                .totalAmountDue(totalDue)
                .minimumAmountDue(minDue)
                .bankName("HDFC")
                .parserUsed("REGEX")
                .build();
    }

    @Override
    public String getBankName() {
        return "HDFC";
    }

    @Override
    public boolean supports(Statement.StatementType statementType) {
        return statementType == Statement.StatementType.CC;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(dateStr, DATE_FORMAT_SHORT);
            } catch (DateTimeParseException e2) {
                throw new RuntimeException("Cannot parse date: " + dateStr);
            }
        }
    }

    private String cleanMerchantName(String description) {
        // Remove POS prefix, location codes, and transaction IDs
        String cleaned = description
                .replaceAll("(?i)^(POS|ECOM|MB|IB|UPI)[/\\s]*", "")
                .replaceAll("[*].*$", "")
                .replaceAll("/[A-Z]{2,6}$", "")  // Remove trailing city codes
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? description : cleaned;
    }

    private boolean containsKeyword(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String extractPattern(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private BigDecimal extractAmount(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String extractStatementMonth(String pdfText) {
        Matcher m = STATEMENT_PERIOD_PATTERN.matcher(pdfText);
        if (m.find()) {
            try {
                LocalDate date = parseDate(m.group(1));
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (Exception e) {
                // ignore
            }
        }
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}
