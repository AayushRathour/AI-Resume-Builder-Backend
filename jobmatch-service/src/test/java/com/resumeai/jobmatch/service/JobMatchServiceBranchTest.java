package com.resumeai.jobmatch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.resumeai.jobmatch.dto.GeminiExtractionResponse;
import com.resumeai.jobmatch.dto.MatchResponse;
import com.resumeai.jobmatch.dto.ResumeStructuredData;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class JobMatchServiceBranchTest {

    @InjectMocks
    private JobMatchService jobMatchService;

    @Mock
    private JobMatchRepository jobMatchRepository;

    @Test
    void testCalculateImprovedScore() {
        GeminiExtractionResponse extracted = GeminiExtractionResponse.builder()
                .skills(List.of("Java", "Spring"))
                .roles(List.of("Developer"))
                .build();
        
        ResumeStructuredData structured = new ResumeStructuredData();
        
        // Calling private method via reflection
        double score = (double) ReflectionTestUtils.invokeMethod(jobMatchService, "computeAdvancedScore",
                extracted, structured, "Java Developer", "Looking for a Java Developer with Spring skills");
        
        assertTrue(score >= 70, "Score should be decent for match. Actual: " + score);
    }

    @Test
    void testDeriveRoles() {
        ResumeStructuredData structured = new ResumeStructuredData();
        structured.setExperience(List.of("Frontend developer at Google", "Backend engineer", "fullstack developer"));
        
        List<String> roles = (List<String>) ReflectionTestUtils.invokeMethod(jobMatchService, "deriveRoles",
                structured, "Full stack developer", "React");
        
        assertTrue(roles.contains("Frontend Developer"));
        assertTrue(roles.contains("Backend Developer"));
        assertTrue(roles.contains("Full Stack Developer"));
        assertTrue(roles.contains("React Developer"));
    }

    @Test
    void testDeriveExperienceLevel() {
        String rawText = "I have 6 years of experience in Java";
        ResumeStructuredData structured = new ResumeStructuredData();
        
        String level = (String) ReflectionTestUtils.invokeMethod(jobMatchService, "deriveExperienceLevel",
                rawText, structured);
        
        assertEquals("5+ years", level);
        
        rawText = "3 years in Python";
        level = (String) ReflectionTestUtils.invokeMethod(jobMatchService, "deriveExperienceLevel",
                rawText, structured);
        assertEquals("3-5 years", level);
    }

    @Test
    void getTopMatches_invalidLimit() {
        // Method returns early for invalid limit, no repository call expected
        List<MatchResponse> results = jobMatchService.getTopMatches(1L, -5);
        assertTrue(results.isEmpty());
    }

    @Test
    void getRankedJobs_noUser() {
        lenient().when(jobMatchRepository.findTopMatches(anyLong(), any())).thenReturn(new ArrayList<>());
        List<MatchResponse> results = jobMatchService.getRankedJobs(999L);
        assertTrue(results.isEmpty());
    }

    @Test
    void testExtractYears() {
        int years = (int) ReflectionTestUtils.invokeMethod(jobMatchService, "extractYears", "5 years exp");
        assertEquals(5, years);
        
        years = (int) ReflectionTestUtils.invokeMethod(jobMatchService, "extractYears", "10 yrs");
        assertEquals(10, years);
        
        years = (int) ReflectionTestUtils.invokeMethod(jobMatchService, "extractYears", "No experience");
        assertEquals(0, years);
    }

    @Test
    void testCoalesceSkills() {
        List<String> ext = List.of("Java", "Spring");
        List<String> parsed = List.of("spring", "Docker");
        
        List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(jobMatchService, "coalesceSkills", ext, parsed);
        
        assertTrue(result.contains("java"));
        assertTrue(result.contains("spring"));
        assertTrue(result.contains("docker"));
    }

    @Test
    void testDeriveKeywords() {
        ResumeStructuredData structured = new ResumeStructuredData();
        structured.setEducation(List.of("B.S. Computer Science"));
        
        List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(jobMatchService, "deriveKeywords",
                structured, "microservices and kubernetes", List.of("Engineer"), List.of("Java"));
        
        assertTrue(result.contains("microservices"));
        assertTrue(result.contains("kubernetes"));
        assertTrue(result.contains("Java"));
        assertTrue(result.contains("Engineer"));
    }

    @Test
    void testFirstNonBlank() {
        // ReflectionTestUtils struggles with varargs. Use direct array.
        String[] args = new String[]{"", "  ", "target", "other"};
        String result = (String) ReflectionTestUtils.invokeMethod(jobMatchService, "firstNonBlank", (Object) args);
        assertEquals("target", result);
    }
}
