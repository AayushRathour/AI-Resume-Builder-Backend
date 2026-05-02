package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.SectionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class SectionClientFallback implements SectionClient {

    @Override
    public List<SectionDTO> getSectionsByResumeId(Long resumeId) {
        log.warn("SectionClient fallback triggered for resumeId={}", resumeId);
        return Collections.emptyList();
    }
}
