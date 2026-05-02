package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.SectionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "section-service",
        url = "${services.section.base-url:http://localhost:8083}",
        fallback = SectionClientFallback.class
)
public interface SectionClient {

    @GetMapping("/api/sections/resume/{resumeId}")
    List<SectionDTO> getSectionsByResumeId(@PathVariable("resumeId") Long resumeId);
}
