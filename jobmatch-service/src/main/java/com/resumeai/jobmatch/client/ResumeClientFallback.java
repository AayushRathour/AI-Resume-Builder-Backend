package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.ResumeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResumeClientFallback implements ResumeClient {

    @Override
    public ResumeDTO getResumeById(Long resumeId) {
        log.warn("ResumeClient fallback triggered for resumeId={}", resumeId);
        return null;
    }
}
