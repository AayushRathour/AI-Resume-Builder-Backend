package com.resumeai.resume.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client for section-service to manage resume sections.
 */
@FeignClient(
        name = "section-service",
        fallback = SectionServiceFeignFallback.class
)

/** Feign client used for communication with section-service. */
public interface SectionServiceFeignClient {

    @GetMapping("/api/sections/resume/{resumeId}")
    List<SectionPayload> getSectionsByResumeId(
            @PathVariable("resumeId") Long resumeId,
            @RequestHeader("X-User-Id") Long userId);

    @DeleteMapping("/api/sections/resume/{resumeId}")
    void deleteAllSections(
            @PathVariable("resumeId") Long resumeId,
            @RequestHeader("X-User-Id") Long userId);

    @PostMapping("/api/sections")
    void createSection(
            @RequestBody SectionCreateRequest request,
            @RequestHeader("X-User-Id") Long userId);
}
