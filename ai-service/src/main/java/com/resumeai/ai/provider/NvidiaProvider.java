package com.resumeai.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

/** AI provider integration used by generation workflows. */
@Component
public class NvidiaProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(NvidiaProvider.class);
    private static final String MODEL = "stepfun-ai/step-3.5-flash";
    private static final String URL = "https://integrate.api.nvidia.com/v1/chat/completions";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public NvidiaProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(4));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String generate(String prompt) {
        String apiKey = System.getenv("NVIDIA_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("NVIDIA_API_KEY is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", MODEL);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 1.0);
        body.put("top_p", 0.9);
        body.put("max_tokens", 16384);
        body.put("stream", false);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("[NVIDIA] Sending request...");
            ResponseEntity<String> response = restTemplate.postForEntity(URL, request, String.class);
            String responseBody = response.getBody();

            if (responseBody == null || responseBody.isBlank()) {
                throw new AiProviderException("NVIDIA AI returned empty response");
            }

            return extractContent(responseBody);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiProviderException("NVIDIA AI failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getModelName() {
        return MODEL;
    }

    @Override
    public boolean isAvailable() {
        String key = System.getenv("NVIDIA_API_KEY");
        return key != null && !key.isBlank() && !key.equals("your_new_secure_key");
    }

    private String extractContent(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isMissingNode() && !content.asText("").isBlank()) {
                return content.asText().trim();
            }
        } catch (Exception ex) {
            log.warn("[NVIDIA] Failed to parse response JSON", ex);
        }
        // Return raw if parsing fails
        return rawJson;
    }
}
