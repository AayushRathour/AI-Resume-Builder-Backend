package com.resumeai.jobmatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TheirStackService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;

    public TheirStackService(ObjectMapper objectMapper,
                             @Value("${theirstack.api.url}") String apiUrl,
                             @Value("${theirstack.api.key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    @Cacheable(value = "theirStackJobs", key = "#jobTitle + '-' + #location")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchJobs(String jobTitle, List<String> skills, String location) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("TheirStack API key missing. Returning empty list.");
            return new ArrayList<>();
        }

        String query = jobTitle == null || jobTitle.isBlank() ? "software developer" : jobTitle.trim();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("limit", 10);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("Searching TheirStack for: {}", query);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            if ((response.getBody() == null || response.getBody().isBlank()) && skills != null && !skills.isEmpty()) {
                String fallbackQuery = query + " " + String.join(" ", skills.stream().limit(3).toList());
                requestBody.put("query", fallbackQuery);
                log.info("TheirStack primary query empty, retrying with: {}", fallbackQuery);
                request = new HttpEntity<>(requestBody, headers);
                response = restTemplate.postForEntity(apiUrl, request, String.class);
            }

            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            if (responseMap != null && responseMap.containsKey("data")) {
                return (List<Map<String, Object>>) responseMap.get("data");
            }
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error fetching jobs from TheirStack API", e);
            return new ArrayList<>();
        }
    }
}
