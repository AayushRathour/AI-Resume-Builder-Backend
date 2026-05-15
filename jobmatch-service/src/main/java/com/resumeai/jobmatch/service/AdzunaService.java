package com.resumeai.jobmatch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.jobmatch.entity.Job;
import com.resumeai.jobmatch.entity.JobSource;
import com.resumeai.jobmatch.repository.JobRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/** Provides supporting adzuna operations for workflow execution. */

@Service
@Slf4j
public class AdzunaService {

    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final String appId;
    private final String appKey;
    private final RestTemplate restTemplate;
    private static final String ADZUNA_API_BASE = "https://api.adzuna.com/v1/api/jobs/in/search/1";

    public AdzunaService(ObjectMapper objectMapper,
                        JobRepository jobRepository,
                        @Value("${adzuna.app.id:}") String appId,
                        @Value("${adzuna.app.key:}") String appKey,
                        @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.boot.web.client.RestTemplateBuilder restTemplateBuilder) {
        this.objectMapper = objectMapper;
        this.jobRepository = jobRepository;
        this.appId = appId;
        this.appKey = appKey;
        this.restTemplate = restTemplateBuilder != null ? restTemplateBuilder.build() : new RestTemplate();
    }

    public List<Map<String, Object>> fetchJobs(String query) {
        return fetchJobs(query, null);
    }

    public List<Map<String, Object>> fetchJobs(String query, String location) {
        String safeQuery = query == null || query.isBlank() ? "software developer" : query.trim();
        String safeLocation = location == null ? "" : location.trim();
        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank()) {
            log.warn("[ADZUNA] API credentials missing. appId/appKey not configured.");
            return new ArrayList<>();
        }
        return fetchJobsInternal(safeQuery, safeLocation, true);
    }

    private List<Map<String, Object>> fetchJobsInternal(String safeQuery, String safeLocation, boolean allowLocationRetry) {
        log.info("[ADZUNA] QUERY: {}", safeQuery);
        String url = ADZUNA_API_BASE
                + "?app_id=" + appId
                + "&app_key=" + appKey
                + "&what=" + encodeUrl(safeQuery)
                + "&results_per_page=20"
                + (safeLocation.isBlank() ? "" : "&where=" + encodeUrl(safeLocation));
        String responseBody;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            responseBody = response.getBody();
        } catch (Exception ex) {
            log.warn("[ADZUNA] Request failed for query='{}', location='{}'. Returning empty result. Cause: {}",
                    safeQuery, safeLocation, ex.getMessage());
            return new ArrayList<>();
        }
        log.debug("[ADZUNA] Response received, length={}", responseBody != null ? responseBody.length() : 0);

        List<Map<String, Object>> jobs = new ArrayList<>();

        if (responseBody == null || responseBody.isBlank()) {
            return jobs;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode results = root.get("results");

            if (results != null && results.isArray()) {
                List<Job> persistedJobs = new ArrayList<>();
                jobRepository.deleteBySource(JobSource.ADZUNA);

                for (JsonNode job : results) {
                    Map<String, Object> dto = new HashMap<>();
                    String title = job.path("title").asText("Untitled Job");
                    String company = job.path("company").path("display_name").asText("Unknown");
                    String jobLocation = job.path("location").path("display_name").asText("Unknown");
                    String urlValue = job.path("redirect_url").asText("");
                    String description = job.path("description").asText("");

                    dto.put("title", title);
                    dto.put("company", company);
                    dto.put("location", jobLocation);
                    dto.put("url", urlValue);
                    dto.put("description", description);
                    dto.put("source", "ADZUNA");
                    jobs.add(dto);

                    persistedJobs.add(Job.builder()
                            .title(title)
                            .company(company)
                            .location(jobLocation)
                            .description(description)
                            .requiredSkills("")
                            .source(JobSource.ADZUNA)
                            .build());
                }

                if (!persistedJobs.isEmpty()) {
                    jobRepository.saveAll(persistedJobs);
                }
            }
        } catch (Exception e) {
            log.error("[ADZUNA] Failed to parse response", e);
        }

        if (jobs.isEmpty() && allowLocationRetry && !safeLocation.isBlank()) {
            log.info("[ADZUNA] 0 jobs for location='{}'. Retrying query without location filter.", safeLocation);
            return fetchJobsInternal(safeQuery, "", false);
        }

        log.info("[ADZUNA] Jobs fetched: {}", jobs.size());
        return jobs;
    }

    /**
     * Search jobs from Adzuna API
     * - Fetches jobs based on skills and job title
     * - Returns up to 20 results
     * - Handles API failures gracefully
     */
    @org.springframework.cache.annotation.Cacheable(
            value = "adzunaJobs",
            key = "#jobTitle + '-' + #skills.toString() + '-' + #location",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<Map<String, Object>> searchJobs(String jobTitle, List<String> skills, String location) {
        log.debug("[ADZUNA] Starting job search - jobTitle: {}, skills: {}, location: {}", jobTitle, skills, location);

        try {
            String primaryQuery = jobTitle == null || jobTitle.isBlank()
                    ? "software developer"
                    : jobTitle.trim();

            List<Map<String, Object>> jobs = fetchJobs(primaryQuery);
            if (!jobs.isEmpty()) {
                return jobs;
            }

            String fallbackQuery = buildSearchQuery(jobTitle, skills);
            if (!fallbackQuery.equalsIgnoreCase(primaryQuery)) {
                log.debug("[ADZUNA] Primary search returned no jobs, trying fallback query: {}", fallbackQuery);
                jobs = fetchJobs(fallbackQuery);
            }

            return jobs;

        } catch (Exception e) {
            log.error("[ADZUNA] Error fetching jobs from Adzuna API: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Build a search query from job title and top skills
     */
    private String buildSearchQuery(String jobTitle, List<String> skills) {
        StringBuilder query = new StringBuilder();

        // Add job title
        if (jobTitle != null && !jobTitle.isBlank()) {
            query.append(jobTitle.trim());
        }

        // Add only a few skills as a fallback if the title-only search fails.
        if (skills != null && !skills.isEmpty()) {
            List<String> topSkills = skills.stream()
                    .limit(3)
                    .toList();
            if (!topSkills.isEmpty()) {
                query.append(" ").append(String.join(" ", topSkills));
            }
        }

        // Fallback if query is empty
        if (query.toString().isBlank()) {
            query.append("software developer");
        }

        return query.toString();
    }

    /**
     * URL encode string
     */
    private String encodeUrl(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[ADZUNA] Error encoding URL: {}", e.getMessage());
            return str.replaceAll(" ", "+");
        }
    }
}



