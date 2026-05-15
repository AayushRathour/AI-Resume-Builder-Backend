package com.resumeai.ai.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GeminiProviderReflectTest {

    private GeminiProvider geminiProvider;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        geminiProvider = new GeminiProvider(objectMapper);
        ReflectionTestUtils.setField(geminiProvider, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(geminiProvider, "model", "gemini-flash");
        ReflectionTestUtils.setField(geminiProvider, "maxTokens", 100);
        ReflectionTestUtils.setField(geminiProvider, "temperature", 0.7);
    }

    @Test
    void testIsNonRetryableError() {
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(geminiProvider, "isNonRetryableError", new Exception("403 Forbidden"));
        assertTrue(result);
        
        result = (boolean) ReflectionTestUtils.invokeMethod(geminiProvider, "isNonRetryableError", new Exception("Timeout"));
        assertFalse(result);
    }

    @Test
    void testExtractContent_success() {
        String json = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Hello\"}]}}]}";
        String result = (String) ReflectionTestUtils.invokeMethod(geminiProvider, "extractContent", json);
        assertEquals("Hello", result);
    }

    @Test
    void testExtractContent_error() {
        String json = "{\"error\": {\"message\": \"API Key Invalid\"}}";
        assertThrows(AiProviderException.class, () -> ReflectionTestUtils.invokeMethod(geminiProvider, "extractContent", json));
    }
}
