package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.SectionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
    name = "section-service",
    fallback = SectionClientFallback.class
)

/** Feign client for synchronous calls to section APIs. */
public interface SectionClient {

    @GetMapping("/api/sections/resume/{resumeId}")
    List<SectionDTO> getSectionsByResumeId(@PathVariable("resumeId") Long resumeId, @org.springframework.web.bind.annotation.RequestHeader("X-User-Id") Long userId);
}



