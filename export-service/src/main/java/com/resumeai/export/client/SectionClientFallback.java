package com.resumeai.export.client;

import com.resumeai.export.dto.SectionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** Fallback behavior for section service call failures. */

@Slf4j
@Component
public class SectionClientFallback implements SectionClient {

    @Override
    public List<SectionDTO> getSectionsByResumeId(Long resumeId, Long userId) {
        log.warn("SectionClient fallback triggered for resumeId={}", resumeId);
        return Collections.emptyList();
    }
}



