package com.statementiq.service.parse;

import com.statementiq.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for selecting the appropriate bank-specific parser.
 * Uses Strategy pattern — each parser registers itself via Spring DI.
 */
@Service
public class StatementParserFactory {

    private static final Logger log = LoggerFactory.getLogger(StatementParserFactory.class);
    private final Map<String, StatementParser> parsers;
    private final StatementParser fallbackParser;

    public StatementParserFactory(List<StatementParser> parserList) {
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(
                        p -> p.getBankName().toUpperCase(),
                        Function.identity(),
                        (a, b) -> a  // keep first if duplicate
                ));

        // GenericParser serves as fallback for unsupported banks
        this.fallbackParser = parsers.getOrDefault("GENERIC",
                parserList.stream()
                        .filter(p -> p.getBankName().equalsIgnoreCase("GENERIC"))
                        .findFirst()
                        .orElse(null));

        log.info("Registered {} bank parsers: {}", parsers.size(), parsers.keySet());
    }

    /**
     * Get parser for specific bank and statement type.
     */
    public StatementParser getParser(String bankName, Statement.StatementType statementType) {
        if (bankName == null) {
            log.info("No bank specified, using fallback parser");
            return getFallbackParser();
        }

        StatementParser parser = parsers.get(bankName.toUpperCase());
        if (parser != null && parser.supports(statementType)) {
            return parser;
        }

        log.info("No specific parser for bank={}, type={}. Using fallback.", bankName, statementType);
        return getFallbackParser();
    }

    private StatementParser getFallbackParser() {
        if (fallbackParser != null) {
            return fallbackParser;
        }
        throw new RuntimeException("No fallback parser available");
    }
}
