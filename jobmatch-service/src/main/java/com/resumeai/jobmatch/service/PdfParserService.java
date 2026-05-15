package com.resumeai.jobmatch.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;

/** Provides supporting pdf parser operations for workflow execution. */

@Service
@Slf4j
public class PdfParserService {

    public String extractTextFromPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        log.info("STEP 2: Extracting text from PDF: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("Resume text extraction failed");
            }
            
            log.info("Raw PDF text length: {} chars", text.length());
            log.info("PDF text preview (first 300 chars): {}", 
                text.substring(0, Math.min(300, text.length())));
            
            // DO NOT lowercase or strip aggressively - preserve original text for Gemini
            String cleaned = cleanText(text);
            log.info("Cleaned text length: {} chars", cleaned.length());
            
            if (cleaned == null || cleaned.trim().isEmpty()) {
                throw new RuntimeException("Resume text extraction failed");
            }

            return cleaned;
            
        } catch (Exception e) {
            log.error("Error parsing PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse PDF document: " + e.getMessage(), e);
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        // Preserve original case and meaningful characters
        // Only collapse excessive whitespace
        String cleaned = text.replaceAll("\\r\\n", "\n");
        cleaned = cleaned.replaceAll("[ \\t]+", " ");
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        return cleaned.trim();
    }
}



