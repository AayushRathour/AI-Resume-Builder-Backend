package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.dto.ResumeStructuredData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Provides supporting resume structuring operations for workflow execution. */

@Service
public class ResumeStructuringService {

    private static final Pattern NON_TEXT = Pattern.compile("[^a-zA-Z0-9\\s\\.,;:\\-\\+/#]");
    private static final List<String> KNOWN_SKILLS = List.of(
            "java", "spring", "spring boot", "react", "typescript", "javascript", "mysql", "postgresql",
            "redis", "docker", "kubernetes", "aws", "azure", "git", "rest api", "microservices", "html",
            "css", "tailwind", "node", "express", "python", "django", "mongodb", "kafka", "rabbitmq");

    public ResumeStructuredData fromRawText(String rawText) {
        String cleaned = normalizeText(rawText);
        if (cleaned.isBlank()) {
            return ResumeStructuredData.builder().normalizedText("").build();
        }

        List<String> lines = Arrays.stream(rawText.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        String name = detectName(lines);
        List<String> skills = detectSkills(cleaned);
        List<String> experience = detectByKeywords(lines, List.of("experience", "intern", "developer", "engineer", "project"));
        List<String> education = detectByKeywords(lines, List.of("education", "bachelor", "master", "university", "college", "b.tech", "b.e"));
        String summary = buildSummary(cleaned);

        return ResumeStructuredData.builder()
                .name(name)
                .skills(skills)
                .experience(experience)
                .education(education)
                .summary(summary)
                .normalizedText(cleaned)
                .build();
    }

    public String normalizeText(String rawText) {
        if (rawText == null) {
            return "";
        }
        String cleaned = NON_TEXT.matcher(rawText).replaceAll(" ");
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned.toLowerCase(Locale.ROOT).trim();
    }

    private String detectName(List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        for (String line : lines) {
            String compact = line.trim();
            if (compact.length() < 3 || compact.length() > 50) {
                continue;
            }
            String lower = compact.toLowerCase(Locale.ROOT);
            if (lower.contains("@") || lower.contains("http") || lower.matches(".*\\d.*")) {
                continue;
            }
            return compact;
        }
        return lines.get(0);
    }

    private List<String> detectSkills(String normalizedText) {
        Set<String> found = new LinkedHashSet<>();
        for (String skill : KNOWN_SKILLS) {
            if (normalizedText.contains(skill)) {
                found.add(skill);
            }
        }
        return new ArrayList<>(found);
    }

    private List<String> detectByKeywords(List<String> lines, List<String> keywords) {
        return lines.stream()
                .filter(line -> {
                    String lower = line.toLowerCase(Locale.ROOT);
                    return keywords.stream().anyMatch(lower::contains);
                })
                .limit(8)
                .toList();
    }

    private String buildSummary(String normalizedText) {
        if (normalizedText.isBlank()) {
            return "";
        }
        return normalizedText.length() <= 350 ? normalizedText : normalizedText.substring(0, 350);
    }
}



