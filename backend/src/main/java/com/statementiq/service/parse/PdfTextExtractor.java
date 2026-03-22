package com.statementiq.service.parse;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts text from PDF files using Apache PDFBox.
 * PRIVACY: Uses MultipartFile.getBytes() — processes entirely in memory.
 * PDF bytes are never written to disk or any persistent storage.
 */
@Service
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    /**
     * Extract all text from a PDF file in memory.
     * @param pdfBytes byte array containing PDF bytes
     * @return Extracted text or null if extraction fails
     */
    public String extractText(byte[] pdfBytes) {
        try {
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                log.info("Extracted {} characters from PDF ({} pages)",
                        text.length(), document.getNumberOfPages());
                return text;
            }
        } catch (Exception e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract text from a password-protected PDF.
     * Pro feature only.
     */
    public String extractText(byte[] pdfBytes, String password) {
        try {
            try (PDDocument document = Loader.loadPDF(pdfBytes, password)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                log.info("Extracted {} characters from password-protected PDF ({} pages)",
                        text.length(), document.getNumberOfPages());
                return text;
            }
        } catch (Exception e) {
            log.error("Failed to extract text from password-protected PDF: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract text page-by-page from a PDF.
     * Returns a list where each element is the text from one page.
     * Used by the chunked AI processing pipeline.
     */
    public List<String> extractTextByPages(byte[] pdfBytes) {
        try {
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                return extractPagesFromDocument(document);
            }
        } catch (Exception e) {
            log.error("Failed to extract pages from PDF: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract text page-by-page from a password-protected PDF.
     */
    public List<String> extractTextByPages(byte[] pdfBytes, String password) {
        try {
            try (PDDocument document = Loader.loadPDF(pdfBytes, password)) {
                return extractPagesFromDocument(document);
            }
        } catch (Exception e) {
            log.error("Failed to extract pages from password-protected PDF: {}", e.getMessage());
            return null;
        }
    }

    private List<String> extractPagesFromDocument(PDDocument document) throws Exception {
        int totalPages = document.getNumberOfPages();
        List<String> pages = new ArrayList<>(totalPages);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);

        for (int i = 1; i <= totalPages; i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            String pageText = stripper.getText(document);
            pages.add(pageText);
        }

        int totalChars = pages.stream().mapToInt(String::length).sum();
        log.info("Extracted {} pages ({} total characters) from PDF", totalPages, totalChars);
        return pages;
    }
}
