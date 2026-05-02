package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.ResumeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "resume-service",
        url = "${services.resume.base-url:http://localhost:8082}",
        fallback = ResumeClientFallback.class
)
public interface ResumeClient {

    @GetMapping("/api/resumes/{resumeId}")
    ResumeDTO getResumeById(@PathVariable("resumeId") Long resumeId);
}
