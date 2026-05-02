package com.resumeai.section.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResumeServiceClient {

    private static final String RESUME_SERVICE_BASE = "http://resume-service/api/resumes";

    private final RestTemplate restTemplate;

    public ResumePayload getResumeById(Long resumeId, Long requesterUserId) {
        HttpHeaders headers = new HttpHeaders();
        if (requesterUserId != null) {
            headers.add("X-User-Id", String.valueOf(requesterUserId));
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<ResumePayload> response = restTemplate.exchange(
                RESUME_SERVICE_BASE + "/{resumeId}",
                HttpMethod.GET,
                entity,
                ResumePayload.class,
                resumeId);

        return response.getBody();
    }
}
