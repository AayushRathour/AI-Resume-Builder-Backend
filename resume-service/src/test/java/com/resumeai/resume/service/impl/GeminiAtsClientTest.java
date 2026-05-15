package com.resumeai.resume.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import com.resumeai.resume.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeminiAtsClientTest {

    @Mock private RestClient.Builder restClientBuilder;
    @Mock private RestClient restClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private GeminiAtsClient geminiAtsClient;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        geminiAtsClient = new GeminiAtsClient(restClientBuilder);
        ReflectionTestUtils.setField(geminiAtsClient, "apiKey", "test-key");

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        
        // Mock both overloads of body
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(requestBodySpec).when(requestBodySpec).body(any(ParameterizedTypeReference.class));
        
        doReturn(responseSpec).when(requestBodySpec).retrieve();
    }

    @Test
    void generateAtsScore_success() {
        Map<String, Object> mockResponse = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", "85"))))));

        doReturn(mockResponse).when(responseSpec).body(any(Class.class));

        Double score = geminiAtsClient.generateAtsScore("resume", "job");
        assertEquals(85.0, score);
    }

    @Test
    void generateAtsScore_noApiKey_throws() {
        ReflectionTestUtils.setField(geminiAtsClient, "apiKey", "");
        assertThrows(InvalidInputException.class, () -> geminiAtsClient.generateAtsScore("r", "j"));
    }

    @Test
    void generateAtsScore_emptyResponse_throws() {
        doReturn(null).when(responseSpec).body(any(Class.class));
        assertThrows(InvalidInputException.class, () -> geminiAtsClient.generateAtsScore("r", "j"));
    }

    @Test
    void parseScore_withClamp() {
        Map<String, Object> mockResponse = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", "150"))))));

        doReturn(mockResponse).when(responseSpec).body(any(Class.class));
        assertEquals(100.0, geminiAtsClient.generateAtsScore("r", "j"));
    }

    @Test
    void parseScore_invalidFormat_throws() {
        Map<String, Object> mockResponse = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", "not a number"))))));

        doReturn(mockResponse).when(responseSpec).body(any(Class.class));
        assertThrows(InvalidInputException.class, () -> geminiAtsClient.generateAtsScore("r", "j"));
    }

    @Test
    void parseScore_noCandidates_throws() {
        Map<String, Object> mockResponse = Map.of("candidates", List.of());
        doReturn(mockResponse).when(responseSpec).body(any(Class.class));
        assertThrows(InvalidInputException.class, () -> geminiAtsClient.generateAtsScore("r", "j"));
    }

    @Test
    void parseScore_missingParts_throws() {
        Map<String, Object> mockResponse = Map.of(
                "candidates", List.of(Map.of("content", Map.of())));
        doReturn(mockResponse).when(responseSpec).body(any(Class.class));
        assertThrows(InvalidInputException.class, () -> geminiAtsClient.generateAtsScore("r", "j"));
    }
}
