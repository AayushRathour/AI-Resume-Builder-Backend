package com.resumeai.export.client;

import com.resumeai.export.dto.ResumeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "resume-service",
        fallback = ResumeClientFallback.class
)

/** Feign client for synchronous calls to resume APIs. */
public interface ResumeClient {

    @GetMapping("/api/resumes/{resumeId}")
    ResumeDTO getResumeById(
            @PathVariable("resumeId") Long resumeId,
            @RequestHeader("X-User-Id") Long userId);
}



