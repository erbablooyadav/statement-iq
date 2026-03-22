package com.statementiq.service.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Identifies bank from PDF text content by matching header keywords.
 */
@Service
public class BankIdentifierService {

    private static final Logger log = LoggerFactory.getLogger(BankIdentifierService.class);

    // Bank identification keywords — ordered by priority
    private static final Map<String, String[]> BANK_KEYWORDS = new LinkedHashMap<>() {{
        put("HDFC", new String[]{"HDFC BANK", "HDFC Bank", "hdfcbank", "HDFC CREDIT CARD"});
        put("ICICI", new String[]{"ICICI BANK", "ICICI Bank", "icicibank", "ICICI CREDIT CARD"});
        put("SBI", new String[]{"STATE BANK OF INDIA", "SBI Card", "SBICARD", "SBI CREDIT CARD"});
        put("AXIS", new String[]{"AXIS BANK", "Axis Bank", "AXIS CREDIT CARD"});
        put("KOTAK", new String[]{"KOTAK MAHINDRA", "Kotak Mahindra", "KOTAK BANK"});
        put("INDUSIND", new String[]{"INDUSIND BANK", "IndusInd Bank", "INDUSIND"});
        put("RBL", new String[]{"RBL BANK", "RBL Bank", "RATNAKAR BANK"});
        put("AMEX", new String[]{"AMERICAN EXPRESS", "AMEX", "American Express"});
        put("YES", new String[]{"YES BANK", "Yes Bank"});
        put("IDFC", new String[]{"IDFC FIRST", "IDFC First", "IDFC BANK"});
        put("FEDERAL", new String[]{"FEDERAL BANK", "Federal Bank"});
        put("BOB", new String[]{"BANK OF BARODA", "Bank of Baroda"});
        put("PNB", new String[]{"PUNJAB NATIONAL BANK", "PNB"});
        put("CANARA", new String[]{"CANARA BANK", "Canara Bank"});
    }};

    /**
     * Identify bank from extracted PDF text.
     * Scans first 2000 characters (header area) for bank keywords.
     *
     * @param pdfText Full extracted text from PDF
     * @return Bank name or null if not detected
     */
    public String identifyBank(String pdfText) {
        if (pdfText == null || pdfText.isBlank()) {
            return null;
        }

        // Only scan header area for bank identification
        String header = pdfText.substring(0, Math.min(pdfText.length(), 2000)).toUpperCase();

        for (Map.Entry<String, String[]> entry : BANK_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (header.contains(keyword.toUpperCase())) {
                    log.info("Bank identified: {} (keyword: {})", entry.getKey(), keyword);
                    return entry.getKey();
                }
            }
        }

        log.warn("Bank not identified from PDF header. Will use AI fallback.");
        return null;
    }
}
