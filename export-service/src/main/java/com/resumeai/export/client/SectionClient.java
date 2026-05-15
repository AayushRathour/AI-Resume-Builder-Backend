package com.resumeai.export.client;

import com.resumeai.export.dto.SectionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(
        name = "section-service",
        fallback = SectionClientFallback.class
)

/** Feign client for synchronous calls to section APIs. */
public interface SectionClient {

    @GetMapping("/api/sections/resume/{resumeId}")
    List<SectionDTO> getSectionsByResumeId(
            @PathVariable("resumeId") Long resumeId,
            @RequestHeader("X-User-Id") Long userId);
}



