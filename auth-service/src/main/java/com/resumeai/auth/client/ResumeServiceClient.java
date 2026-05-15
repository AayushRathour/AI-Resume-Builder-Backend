package com.resumeai.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/** Feign client for synchronous calls to resume service APIs. */
@FeignClient(name = "resume-service", fallback = ResumeServiceClientFallback.class)
public interface ResumeServiceClient {

    @GetMapping("/api/resumes/internal/count")
    Long countResumes(@RequestHeader("X-Internal-Call") boolean internalCall);
}




