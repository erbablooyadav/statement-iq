package com.statementiq.service.parse;

import com.statementiq.model.Statement;

/**
 * Interface for bank-specific statement parsers.
 * Each bank has unique PDF layouts requiring custom regex/parsing logic.
 */
public interface StatementParser {

    /**
     * Parse raw PDF text into structured transactions.
     * @param pdfText Extracted text from PDF
     * @param statementType CC or BANK
     * @return ParseResult with transactions, confidence, and metadata
     */
    ParseResult parse(String pdfText, Statement.StatementType statementType);

    /**
     * Get the bank name this parser handles.
     */
    String getBankName();

    /**
     * Check if this parser supports the given statement type.
     */
    boolean supports(Statement.StatementType statementType);
}
