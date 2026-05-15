package com.resumeai.section.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "resume-service",
        fallback = ResumeServiceClientFallback.class
)

/** Feign client for synchronous calls to resume service APIs. */
public interface ResumeServiceClient {

    @GetMapping("/api/resumes/{resumeId}")
    ResumePayload getResumeById(
            @PathVariable("resumeId") Long resumeId,
            @RequestHeader("X-User-Id") Long requesterUserId);
}



