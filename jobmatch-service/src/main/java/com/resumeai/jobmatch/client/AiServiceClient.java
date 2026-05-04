package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.AiServiceResponse;
import com.resumeai.jobmatch.dto.MissingSkillsRequest;
import com.resumeai.jobmatch.dto.MissingSkillsResponse;
import com.resumeai.jobmatch.dto.ResumeExtractRequest;
import com.resumeai.jobmatch.dto.ResumeExtractResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "ai-service",
        url = "${services.ai.base-url:http://localhost:8085}",
        fallback = AiServiceClientFallback.class
)
public interface AiServiceClient {

    @PostMapping("/api/ai/resume-extract")
    AiServiceResponse<ResumeExtractResponse> extractResume(@RequestBody ResumeExtractRequest request);

    @PostMapping("/api/ai/missing-skills")
    AiServiceResponse<MissingSkillsResponse> analyzeMissingSkills(@RequestBody MissingSkillsRequest request);
}
