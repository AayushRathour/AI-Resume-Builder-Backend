package com.resumeai.jobmatch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class PdfParserServiceTest {

    private PdfParserService pdfParserService = new PdfParserService();

    @Test
    void testCleanText() {
        String input = "Line 1\r\nLine 2    with spaces\n\n\nLine 3";
        String result = (String) ReflectionTestUtils.invokeMethod(pdfParserService, "cleanText", input);
        
        assertEquals("Line 1\nLine 2 with spaces\n\nLine 3", result);
    }

    @Test
    void testExtractText_nullFile() {
        assertThrows(IllegalArgumentException.class, () -> pdfParserService.extractTextFromPdf(null));
    }

    @Test
    void testExtractText_emptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> pdfParserService.extractTextFromPdf(file));
    }

    @Test
    void testExtractText_corruptedPdf() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "not a pdf".getBytes());
        assertThrows(RuntimeException.class, () -> pdfParserService.extractTextFromPdf(file));
    }
}
