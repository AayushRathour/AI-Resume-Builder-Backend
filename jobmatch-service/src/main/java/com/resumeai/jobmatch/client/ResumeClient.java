package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.ResumeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "resume-service",
    fallback = ResumeClientFallback.class
)

/** Feign client for synchronous calls to resume APIs. */
public interface ResumeClient {

    @GetMapping("/api/resumes/{resumeId}")
    ResumeDTO getResumeById(@PathVariable("resumeId") Long resumeId, @org.springframework.web.bind.annotation.RequestHeader("X-User-Id") Long userId);
}



