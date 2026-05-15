package com.resumeai.section.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Fallback behavior for resume service call failures. */

@Slf4j
@Component
public class ResumeServiceClientFallback implements ResumeServiceClient {

    @Override
    public ResumePayload getResumeById(Long resumeId, Long requesterUserId) {
        log.warn("ResumeServiceClient fallback triggered for resumeId={}", resumeId);
        throw new IllegalStateException(
                "resume-service is currently unavailable; unable to fetch resumeId=" + resumeId);
    }
}




