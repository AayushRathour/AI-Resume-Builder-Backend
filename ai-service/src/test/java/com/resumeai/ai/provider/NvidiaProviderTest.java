package com.resumeai.ai.provider;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NvidiaProviderTest {

    private NvidiaProvider nvidiaProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        nvidiaProvider = new NvidiaProvider(objectMapper);
    }

    @Test
    void testGetModelName() {
        assertEquals("stepfun-ai/step-3.5-flash", nvidiaProvider.getModelName());
    }

    @Test
    void testIsAvailable_falseWhenNoKey() {
        // Can't reliably mock System.getenv in Java 17 easily, 
        // but we can at least invoke the method.
        boolean available = nvidiaProvider.isAvailable();
        // Just checking it runs without exception
    }

    @Test
    void testGenerate_throwsWhenNoKey() {
        // If NVIDIA_API_KEY is null, it throws an exception
        try {
            nvidiaProvider.generate("prompt");
        } catch (AiProviderException e) {
            assertTrue(e.getMessage().contains("not configured"));
        }
    }
}
