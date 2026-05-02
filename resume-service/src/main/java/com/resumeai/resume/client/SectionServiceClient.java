package com.resumeai.resume.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SectionServiceClient {

    private static final Logger log = LoggerFactory.getLogger(SectionServiceClient.class);

    private static final String SECTION_SERVICE_BASE = "http://section-service/api/sections";

    private final RestTemplate restTemplate;

    public void copySections(Long sourceResumeId, Long targetResumeId, Long userId) {
        List<SectionPayload> sourceSections = getSectionsByResume(sourceResumeId, userId);
        for (SectionPayload sourceSection : sourceSections) {
            SectionCreateRequest request = SectionCreateRequest.builder()
                    .resumeId(targetResumeId)
                    .sectionType(sourceSection.getSectionType())
                    .title(sourceSection.getTitle())
                    .content(sourceSection.getContent())
                    .displayOrder(sourceSection.getDisplayOrder())
                    .isVisible(sourceSection.getIsVisible())
                    .aiGenerated(sourceSection.getAiGenerated())
                    .build();

            HttpHeaders headers = buildHeaders(userId);
            HttpEntity<SectionCreateRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(SECTION_SERVICE_BASE, entity, Void.class);
        }
        log.info("Copied {} sections from resume {} to {}", sourceSections.size(), sourceResumeId, targetResumeId);
    }

    public void deleteAllSections(Long resumeId, Long userId) {
        HttpHeaders headers = buildHeaders(userId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(
                SECTION_SERVICE_BASE + "/resume/{resumeId}",
                HttpMethod.DELETE,
                entity,
                Void.class,
                resumeId);
        log.info("Deleted all sections for resume {}", resumeId);
    }

    private List<SectionPayload> getSectionsByResume(Long resumeId, Long userId) {
        HttpHeaders headers = buildHeaders(userId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<List<SectionPayload>> response = restTemplate.exchange(
                SECTION_SERVICE_BASE + "/resume/{resumeId}",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<SectionPayload>>() {
                },
                resumeId);

        return response.getBody() == null ? List.of() : response.getBody();
    }

    private HttpHeaders buildHeaders(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-User-Id", String.valueOf(userId));
        return headers;
    }
}
