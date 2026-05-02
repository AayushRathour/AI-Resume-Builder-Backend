package com.resumeai.ai.client;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.resumeai.ai.exception.AiServiceException;
import com.resumeai.ai.exception.QuotaExceededException;

/**
 * Client for Google Gemini Generative AI REST API.
 * POST
 * https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(@Qualifier("geminiRestClient") RestClient restClient,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model}") String model) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Sends a prompt to Gemini and returns the generated text.
     */
    @SuppressWarnings("unchecked")
    public String generate(String prompt) {
        log.info("Calling Gemini API with model: {}", model);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)))));

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            return extractText(response);

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 429) {
                log.warn("Gemini quota exceeded: {}", ex.getResponseBodyAsString());
                throw new QuotaExceededException(
                        "Gemini API quota exceeded for the configured key. Please check free-tier limits or try again later.");
            }
            log.error("Gemini API call failed: {}", ex.getMessage());
            throw new AiServiceException("Gemini API call failed: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.error("Gemini API call failed: {}", ex.getMessage());
            throw new AiServiceException("Gemini API call failed: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");

            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            return (String) parts.get(0).get("text");

        } catch (Exception ex) {
            log.error("Failed to parse Gemini response: {}", response);
            throw new AiServiceException("Failed to parse Gemini API response");
        }
    }

    public String getModel() {
        return model;
    }
}
