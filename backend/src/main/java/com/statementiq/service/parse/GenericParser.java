package com.statementiq.service.parse;

import com.statementiq.model.Statement;
import com.statementiq.model.Transaction;
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
 * Generic fallback parser for unsupported banks.
 * Uses broad regex patterns to extract transaction-like lines.
 * Lower confidence than bank-specific parsers.
 * In production, falls back to Claude API for enrichment.
 */
@Component
public class GenericParser implements StatementParser {

    private static final Logger log = LoggerFactory.getLogger(GenericParser.class);

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    // Generic: look for lines with a date followed by text and an amount
    private static final Pattern GENERIC_TX_PATTERN = Pattern.compile(
            "(\\d{2}[/-]\\d{2}[/-]\\d{2,4}|\\d{2}\\s+\\w{3}\\s+\\d{2,4}|\\d{4}-\\d{2}-\\d{2})" +
                    "\\s+(.+?)\\s+" +
                    "([\\d,]+\\.\\d{2})\\s*(Dr|Cr|DR|CR)?\\s*$",
            Pattern.MULTILINE
    );

    @Override
    public ParseResult parse(String pdfText, Statement.StatementType statementType) {
        List<RawTransaction> transactions = new ArrayList<>();
        double confidence = 0.55;  // Lower confidence for generic parser

        Matcher matcher = GENERIC_TX_PATTERN.matcher(pdfText);
        String statementMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amountStr = matcher.group(3).replace(",", "");
                String drCr = matcher.group(4);

                LocalDate date = parseDate(dateStr);
                BigDecimal amount = new BigDecimal(amountStr);

                boolean isCredit = drCr != null && drCr.toUpperCase().equals("CR");

                RawTransaction raw = RawTransaction.builder()
                        .date(date)
                        .rawDescription(description)
                        .merchantName(description)
                        .description(description)
                        .amount(amount)
                        .type(isCredit ? Transaction.TransactionType.CREDIT : Transaction.TransactionType.DEBIT)
                        .build();

                transactions.add(raw);
                statementMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (Exception e) {
                confidence -= 0.01;
            }
        }

        if (transactions.isEmpty()) confidence = 0.0;

        return ParseResult.builder()
                .transactions(transactions)
                .confidence(Math.max(0, confidence))
                .statementMonth(statementMonth)
                .bankName("GENERIC")
                .parserUsed("REGEX")
                .build();
    }

    @Override
    public String getBankName() { return "GENERIC"; }

    @Override
    public boolean supports(Statement.StatementType statementType) {
        return true; // Supports all types as fallback
    }

    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(dateStr, fmt); }
            catch (DateTimeParseException ignored) {}
        }
        throw new RuntimeException("Cannot parse date: " + dateStr);
    }
}
