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
 * ICICI Credit Card statement parser.
 * ICICI CC statements vary by card type (Coral, Sapphiro, Amazon Pay).
 * Uses line-by-line parsing with multiple date format support.
 */
@Component
public class IciciCcParser implements StatementParser {

    private static final Logger log = LoggerFactory.getLogger(IciciCcParser.class);

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yy")
    };

    // ICICI typically: Date | Transaction Details | Amount (Dr/Cr on same or next line)
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
            "(\\d{2}[/-]\\d{2}[/-]\\d{2,4}|\\d{2}\\s+\\w{3}\\s+\\d{2,4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s*(Dr|Cr|DR|CR)?\\s*$",
            Pattern.MULTILINE
    );

    private static final Pattern DUE_DATE_PATTERN = Pattern.compile(
            "(?:Payment\\s+Due\\s+Date|Due\\s+Date|Bill\\s+Due)[:\\s]*(\\d{2}[/-]\\d{2}[/-]\\d{2,4})",
            Pattern.CASE_INSENSITIVE
    );

    private static final String[] FEE_KEYWORDS = {
            "LATE PAYMENT", "ANNUAL FEE", "FINANCE CHARGE", "INTEREST",
            "GST", "IGST", "OVER LIMIT", "CASH ADVANCE", "FOREX", "SERVICE CHARGE"
    };

    @Override
    public ParseResult parse(String pdfText, Statement.StatementType statementType) {
        List<RawTransaction> transactions = new ArrayList<>();
        double confidence = 0.85;

        String dueDate = extractPattern(pdfText, DUE_DATE_PATTERN);
        String statementMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Matcher matcher = TRANSACTION_PATTERN.matcher(pdfText);
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amountStr = matcher.group(3).replace(",", "");
                String drCr = matcher.group(4);

                LocalDate date = parseDate(dateStr);
                BigDecimal amount = new BigDecimal(amountStr);
                boolean isCredit = drCr != null && drCr.toUpperCase().equals("CR");

                String upperDesc = description.toUpperCase();
                boolean isFee = containsKeyword(upperDesc, FEE_KEYWORDS);

                RawTransaction raw = RawTransaction.builder()
                        .date(date)
                        .rawDescription(description)
                        .merchantName(cleanMerchantName(description))
                        .description(description)
                        .amount(amount)
                        .type(isCredit ? Transaction.TransactionType.CREDIT : Transaction.TransactionType.DEBIT)
                        .channel("POS")
                        .isFee(isFee)
                        .category(isFee ? "Fees & Charges" : null)
                        .build();

                transactions.add(raw);

                if (date != null) {
                    statementMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                }
            } catch (Exception e) {
                log.warn("Failed to parse ICICI CC line: {}", matcher.group(0));
                confidence -= 0.02;
            }
        }

        if (transactions.isEmpty()) confidence = 0.0;

        return ParseResult.builder()
                .transactions(transactions)
                .confidence(Math.max(0, confidence))
                .statementMonth(statementMonth)
                .billDueDate(dueDate)
                .bankName("ICICI")
                .parserUsed("REGEX")
                .build();
    }

    @Override
    public String getBankName() { return "ICICI"; }

    @Override
    public boolean supports(Statement.StatementType statementType) {
        return statementType == Statement.StatementType.CC;
    }

    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(dateStr, fmt); }
            catch (DateTimeParseException ignored) {}
        }
        throw new RuntimeException("Cannot parse date: " + dateStr);
    }

    private String cleanMerchantName(String desc) {
        return desc.replaceAll("(?i)^(POS|ECOM|UPI|MB)[/\\s]*", "")
                .replaceAll("[*].*$", "").replaceAll("/[A-Z]{2,6}$", "").trim();
    }

    private boolean containsKeyword(String text, String[] keywords) {
        for (String k : keywords) { if (text.contains(k)) return true; }
        return false;
    }

    private String extractPattern(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
