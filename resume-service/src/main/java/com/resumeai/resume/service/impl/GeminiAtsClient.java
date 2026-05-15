package com.resumeai.resume.service.impl;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.resumeai.resume.exception.InvalidInputException;

/**
 * Gemini client for ATS score generation based on resume and job text.
 */
@Component
public class GeminiAtsClient {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private final RestClient restClient;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String apiBaseUrl;

    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String model;

    public GeminiAtsClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Calls Gemini to generate a numeric ATS score between 0 and 100.
     */
    public Double generateAtsScore(String resumeText, String jobDescription) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new InvalidInputException("Gemini API key is not configured");
        }

        String prompt = buildPrompt(resumeText, jobDescription);

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(apiBaseUrl + "/models/" + model + ":generateContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new InvalidInputException("Empty response from Gemini API");
        }

        Double parsedScore = parseScoreFromGeminiResponse(response);
        if (parsedScore == null) {
            throw new InvalidInputException("Could not parse ATS score from Gemini response");
        }

        return clampScore(parsedScore);
    }

    private String buildPrompt(String resumeText, String jobDescription) {
        String jobPart = (jobDescription == null || jobDescription.isBlank())
                ? "No specific job description provided."
                : jobDescription;

        return "You are an ATS evaluator. "
                + "Given the resume and job description, output ONLY one numeric score between 0 and 100. "
                + "Do not output any text except the number.\n\n"
                + "Job Description:\n" + jobPart + "\n\n"
                + "Resume:\n" + resumeText;
    }

    @SuppressWarnings("unchecked")
    private Double parseScoreFromGeminiResponse(Map<String, Object> response) {
        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }

        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidateMap)) {
            return null;
        }

        Object contentObj = candidateMap.get("content");
        if (!(contentObj instanceof Map<?, ?> contentMap)) {
            return null;
        }

        Object partsObj = contentMap.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return null;
        }

        Object firstPart = parts.get(0);
        if (!(firstPart instanceof Map<?, ?> partMap)) {
            return null;
        }

        Object textObj = partMap.get("text");
        if (!(textObj instanceof String text) || text.isBlank()) {
            return null;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        return Double.parseDouble(matcher.group());
    }

    private Double clampScore(Double score) {
        if (score < 0) {
            return 0.0;
        }
        if (score > 100) {
            return 100.0;
        }
        return score;
    }
}
