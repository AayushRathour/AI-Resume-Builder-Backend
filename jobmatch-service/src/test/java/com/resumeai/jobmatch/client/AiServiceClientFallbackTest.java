package com.resumeai.jobmatch.client;

import static org.junit.jupiter.api.Assertions.*;

import com.resumeai.jobmatch.dto.AiServiceResponse;
import com.resumeai.jobmatch.dto.ResumeExtractResponse;
import org.junit.jupiter.api.Test;

class AiServiceClientFallbackTest {

    private AiServiceClientFallback fallback = new AiServiceClientFallback();

    @Test
    void extractResume() {
        AiServiceResponse<ResumeExtractResponse> result = fallback.extractResume(null);
        assertEquals("failed", result.getStatus());
        assertTrue(result.getMessage().contains("AI unavailable"));
    }
}
