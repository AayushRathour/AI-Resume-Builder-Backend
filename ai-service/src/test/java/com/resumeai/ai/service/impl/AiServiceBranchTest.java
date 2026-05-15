package com.resumeai.ai.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.repository.AiRequestRepository;
import com.resumeai.ai.provider.AiProviderFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AiServiceBranchTest {

    @Mock private AiRequestRepository repository;
    @Mock private ObjectMapper objectMapper;
    @Mock private AiProviderFactory providerFactory;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AiServiceImpl aiService;

    @Test
    void testNormalize() {
        String input = "Hello, World! @2023 #Software";
        String result = (String) ReflectionTestUtils.invokeMethod(aiService, "normalize", input);
        assertEquals("hello world 2023 #software", result);
    }

    @Test
    void testExtractKeywords() {
        String text = "Java developer with Spring Boot and Microservices experience. Java Java Java.";
        List<String> keywords = (List<String>) ReflectionTestUtils.invokeMethod(aiService, "extractKeywords", text);
        
        assertTrue(keywords.contains("java"));
        assertTrue(keywords.contains("spring"));
        assertTrue(keywords.contains("boot"));
        assertTrue(keywords.contains("microservices"));
    }

    @Test
    void testCleanJsonString() {
        String json = "```json\n{\"score\": 85}\n```";
        String result = (String) ReflectionTestUtils.invokeMethod(aiService, "cleanJsonString", json);
        assertEquals("{\"score\": 85}", result);
        
        json = "```\n{\"score\": 90}\n```";
        result = (String) ReflectionTestUtils.invokeMethod(aiService, "cleanJsonString", json);
        assertEquals("{\"score\": 90}", result);
    }

    @Test
    void testComputeStandaloneAts() {
        String resume = "John Doe\nEmail: john@example.com\nExperience: 5 years at Google\nEducation: MS CS\nSkills: Java, Python";
        Object result = ReflectionTestUtils.invokeMethod(aiService, "computeStandaloneAts", resume);
        
        assertNotNull(result);
        int score = (int) ReflectionTestUtils.getField(result, "score");
        assertTrue(score >= 40, "Score should be reasonable. Actual: " + score);
    }

    @Test
    void testEstimateTokens() {
        int tokens = (int) ReflectionTestUtils.invokeMethod(aiService, "estimateTokens", "abcd");
        assertEquals(1, tokens);
        
        tokens = (int) ReflectionTestUtils.invokeMethod(aiService, "estimateTokens", "");
        assertEquals(0, tokens);
    }

    @Test
    void testNormalizeList() {
        List<String> input = List.of(" Java ", "spring ", "JAVA", "");
        List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(aiService, "normalizeList", input);
        
        assertEquals(2, result.size());
        assertTrue(result.contains("java"));
        assertTrue(result.contains("spring"));
    }
}
