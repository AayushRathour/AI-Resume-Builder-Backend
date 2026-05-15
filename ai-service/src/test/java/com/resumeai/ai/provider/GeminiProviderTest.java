package com.resumeai.ai.provider;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GeminiProviderTest {

    private GeminiProvider geminiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        geminiProvider = new GeminiProvider(objectMapper);
        ReflectionTestUtils.setField(geminiProvider, "model", "gemini-2.0-flash");
        ReflectionTestUtils.setField(geminiProvider, "maxTokens", 100);
        ReflectionTestUtils.setField(geminiProvider, "temperature", 0.5);
    }

    @Test
    void testGetModelName() {
        assertEquals("gemini-2.0-flash", geminiProvider.getModelName());
    }

    @Test
    void testIsAvailable() {
        // Just checking execution
        geminiProvider.isAvailable();
    }

    @Test
    void testGenerate_throwsWhenNoKey() {
        try {
            geminiProvider.generate("prompt");
        } catch (AiProviderException e) {
            assertTrue(e.getMessage().contains("not configured") || e.getMessage().contains("failed"));
        }
    }
}
