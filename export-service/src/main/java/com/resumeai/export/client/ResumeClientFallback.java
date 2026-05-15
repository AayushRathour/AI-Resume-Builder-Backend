package com.resumeai.export.client;

import com.resumeai.export.dto.ResumeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Fallback behavior for resume service call failures. */

@Slf4j
@Component
public class ResumeClientFallback implements ResumeClient {

    @Override
    public ResumeDTO getResumeById(Long resumeId, Long userId) {
        log.warn("ResumeClient fallback triggered for resumeId={}", resumeId);
        throw new IllegalStateException(
                "resume-service is currently unavailable; unable to fetch resumeId=" + resumeId);
    }
}



