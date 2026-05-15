package com.resumeai.resume.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Local client wrapper for section-service calls related to resume sections.
 */
@Component
@RequiredArgsConstructor
public class SectionServiceClient {

    private static final Logger log = LoggerFactory.getLogger(SectionServiceClient.class);

    private final SectionServiceFeignClient feignClient;

    /**
     * Copies all sections from a source resume into a target resume.
     */
    public void copySections(Long sourceResumeId, Long targetResumeId, Long userId) {
        List<SectionPayload> sourceSections = feignClient.getSectionsByResumeId(sourceResumeId, userId);
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

            feignClient.createSection(request, userId);
        }
        log.info("Copied {} sections from resume {} to {}", sourceSections.size(), sourceResumeId, targetResumeId);
    }

    /**
     * Deletes all sections associated with a resume.
     */
    public void deleteAllSections(Long resumeId, Long userId) {
        feignClient.deleteAllSections(resumeId, userId);
        log.info("Deleted all sections for resume {}", resumeId);
    }
}
