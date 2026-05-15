package com.resumeai.ai.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiProviderFactoryTest {

    @Mock private GeminiProvider geminiProvider;
    @Mock private NvidiaProvider nvidiaProvider;

    private AiProviderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AiProviderFactory(geminiProvider, nvidiaProvider);
        ReflectionTestUtils.setField(factory, "preferredProvider", "auto");
    }

    @Test
    void testGetProvider() {
        when(geminiProvider.isAvailable()).thenReturn(true);
        AiProvider provider = factory.getProvider();
        assertEquals(geminiProvider, provider);
    }

    @Test
    void testGetProvider_fallback() {
        when(geminiProvider.isAvailable()).thenReturn(false);
        when(nvidiaProvider.isAvailable()).thenReturn(true);
        AiProvider provider = factory.getProvider();
        assertEquals(nvidiaProvider, provider);
    }

    @Test
    void testGenerateWithFallback_success() {
        when(geminiProvider.isAvailable()).thenReturn(true);
        when(geminiProvider.generate(anyString())).thenReturn("Success");
        when(geminiProvider.getModelName()).thenReturn("gemini");

        AiProviderFactory.AiProviderResult result = factory.generateWithFallback("prompt");
        assertEquals("Success", result.text());
        assertEquals("gemini", result.model());
    }

    @Test
    void testGenerateWithFallback_primaryFails_usesFallback() {
        when(geminiProvider.isAvailable()).thenReturn(true);
        when(nvidiaProvider.isAvailable()).thenReturn(true);
        when(geminiProvider.generate(anyString())).thenThrow(new AiProviderException("Failed"));
        when(nvidiaProvider.generate(anyString())).thenReturn("Fallback Success");
        when(nvidiaProvider.getModelName()).thenReturn("nvidia");

        AiProviderFactory.AiProviderResult result = factory.generateWithFallback("prompt");
        assertEquals("Fallback Success", result.text());
        assertEquals("nvidia", result.model());
    }
}
