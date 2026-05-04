package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.AiServiceResponse;
import com.resumeai.jobmatch.dto.MissingSkillsRequest;
import com.resumeai.jobmatch.dto.MissingSkillsResponse;
import com.resumeai.jobmatch.dto.ResumeExtractRequest;
import com.resumeai.jobmatch.dto.ResumeExtractResponse;
import org.springframework.stereotype.Component;

@Component
public class AiServiceClientFallback implements AiServiceClient {

    @Override
    public AiServiceResponse<ResumeExtractResponse> extractResume(ResumeExtractRequest request) {
        return AiServiceResponse.<ResumeExtractResponse>builder()
                .status("failed")
                .message("AI unavailable")
                .data(ResumeExtractResponse.builder()
                        .skills(java.util.List.of())
                        .roles(java.util.List.of())
                        .keywords(java.util.List.of())
                        .experience("AI TEMPORARILY UNAVAILABLE")
                        .build())
                .build();
    }

    @Override
    public AiServiceResponse<MissingSkillsResponse> analyzeMissingSkills(MissingSkillsRequest request) {
        return AiServiceResponse.<MissingSkillsResponse>builder()
                .status("failed")
                .message("AI unavailable")
                .data(MissingSkillsResponse.builder()
                        .missingSkills("")
                        .recommendations("AI TEMPORARILY UNAVAILABLE")
                        .build())
                .build();
    }
}
