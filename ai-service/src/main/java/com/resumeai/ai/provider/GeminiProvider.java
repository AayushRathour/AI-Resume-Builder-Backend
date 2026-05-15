package com.resumeai.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** AI provider integration used by generation workflows. */
@Component
public class GeminiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_RETRIES = 1;

    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    @Value("${gemini.api-key:#{null}}")
    private String configuredApiKey;

    @Value("${gemini.timeout-seconds:4}")
    private int timeoutSeconds;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.max-tokens:4096}")
    private int maxTokens;

    @Value("${gemini.temperature:0.3}")
    private double temperature;

    public GeminiProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void initRestTemplate() {
        int effectiveTimeout = Math.max(5, timeoutSeconds);

        // Create RestTemplate with timeout
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(effectiveTimeout));
        factory.setReadTimeout(Duration.ofSeconds(effectiveTimeout));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String generate(String prompt) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("GEMINI_API_KEY is not configured");
        }

        String url = BASE_URL + model + ":generateContent?key=" + apiKey;

        // Build Gemini API request body
        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            ),
            "generationConfig", Map.of(
                "temperature", temperature,
                "maxOutputTokens", maxTokens,
                "topP", 0.95
            )
        );

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                log.info("[Gemini] Attempt {}/{}  sending request...", attempt, MAX_RETRIES);
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                String responseBody = response.getBody();

                if (responseBody == null || responseBody.isBlank()) {
                    throw new AiProviderException("Gemini returned empty response");
                }

                String content = extractContent(responseBody);
                log.info("[Gemini] Success  response length: {} chars", content.length());
                return content;

            } catch (AiProviderException ex) {
                throw ex; // Don't retry on parse errors
            } catch (HttpStatusCodeException ex) {
                lastException = ex;
                if (isNonRetryableStatus(ex.getStatusCode())) {
                    throw new AiProviderException("Gemini request failed with status: " + ex.getStatusCode(), ex);
                }
                log.warn("[Gemini] Attempt {}/{} failed: {}", attempt, MAX_RETRIES, ex.getMessage());
            } catch (Exception ex) {
                lastException = ex;
                log.warn("[Gemini] Attempt {}/{} failed: {}", attempt, MAX_RETRIES, ex.getMessage());
                if (isNonRetryableError(ex)) {
                    throw new AiProviderException("Gemini request failed with non-retryable error: " + ex.getMessage(), ex);
                }

                if (attempt < MAX_RETRIES) {
                    try {
                        long delay = 300L;
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiProviderException("Interrupted during retry", ie);
                    }
                }
            }
        }

        throw new AiProviderException("Gemini AI failed after " + MAX_RETRIES + " attempts", lastException);
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isAvailable() {
        String key = resolveApiKey();
        return key != null && !key.isBlank() && !key.equals("your_gemini_api_key_here");
    }


    private String resolveApiKey() {
        // Priority: environment variable > application.properties
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        return configuredApiKey;
    }

    private boolean isNonRetryableError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains(" 400 ")
                || lower.contains(" 401 ")
                || lower.contains(" 403 ")
                || lower.contains(" 404 ")
                || lower.contains(" 429 ")
                || lower.contains("429")
                || lower.contains("invalid api key")
                || lower.contains("not found for api version")
                || lower.contains("quota exceeded")
                || lower.contains("resource_exhausted")
                || lower.contains("permission denied")
                || lower.contains("unauthorized")
                || lower.contains("forbidden");
    }

    private boolean isNonRetryableStatus(HttpStatusCode status) {
        if (status == null) {
            return false;
        }
        int code = status.value();
        return code == 400 || code == 401 || code == 403 || code == 404 || code == 429;
    }

    /**
     * Extracts the text content from Gemini API response.
     * Response format: { candidates: [{ content: { parts: [{ text: "..." }] } }] }
     */
    private String extractContent(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode candidates = root.path("candidates");

            if (candidates.isEmpty()) {
                // Check for error
                JsonNode error = root.path("error");
                if (!error.isMissingNode()) {
                    throw new AiProviderException("Gemini API error: " + error.path("message").asText("Unknown error"));
                }
                throw new AiProviderException("Gemini returned no candidates");
            }

            JsonNode content = candidates.get(0).path("content").path("parts").get(0).path("text");
            if (content.isMissingNode() || content.asText("").isBlank()) {
                throw new AiProviderException("Gemini returned empty content");
            }

            return content.asText().trim();
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[Gemini] Failed to parse response: {}", rawJson.substring(0, Math.min(500, rawJson.length())));
            throw new AiProviderException("Failed to parse Gemini response", ex);
        }
    }
}
