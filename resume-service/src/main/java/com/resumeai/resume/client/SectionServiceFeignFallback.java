package com.resumeai.resume.client;

import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for section-service to keep resume operations resilient.
 */
@Slf4j
@Component
public class SectionServiceFeignFallback implements SectionServiceFeignClient {

    @Override
    public List<SectionPayload> getSectionsByResumeId(Long resumeId, Long userId) {
        log.warn("SectionServiceFeignClient fallback triggered for resumeId={}", resumeId);
        return Collections.emptyList();
    }

    @Override
    public void deleteAllSections(Long resumeId, Long userId) {
        log.warn("SectionServiceFeignClient fallback deleteAllSections for resumeId={}", resumeId);
    }

    @Override
    public void createSection(SectionCreateRequest request, Long userId) {
        log.warn("SectionServiceFeignClient fallback createSection for resumeId={}", request.getResumeId());
    }
}
