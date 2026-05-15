package com.resumeai.jobmatch.service;

import static org.junit.jupiter.api.Assertions.*;

import com.resumeai.jobmatch.dto.ResumeStructuredData;
import org.junit.jupiter.api.Test;
import java.util.List;

class ResumeStructuringServiceTest {

    private ResumeStructuringService service = new ResumeStructuringService();

    @Test
    void testFromRawText() {
        String raw = "John Doe\njohn@example.com\nExperience: Senior Java Dev at Google\nSkills: Java, Spring Boot, React\nEducation: B.S. CS";
        ResumeStructuredData result = service.fromRawText(raw);
        
        assertEquals("John Doe", result.getName());
        assertTrue(result.getSkills().contains("java"));
        assertTrue(result.getSkills().contains("spring boot"));
        assertFalse(result.getExperience().isEmpty());
    }

    @Test
    void testNormalizeText() {
        String raw = "Hello $# World!";
        assertEquals("hello # world", service.normalizeText(raw));
        assertEquals("", service.normalizeText(null));
    }
    
    @Test
    void testEmptyText() {
        ResumeStructuredData result = service.fromRawText("");
        assertEquals("", result.getNormalizedText());
    }
}
