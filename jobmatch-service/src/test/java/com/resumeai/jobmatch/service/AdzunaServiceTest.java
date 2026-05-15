package com.resumeai.jobmatch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.jobmatch.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AdzunaServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private RestTemplate restTemplate;
    private AdzunaService adzunaService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adzunaService = new AdzunaService(objectMapper, jobRepository, "appId", "appKey", null);
        ReflectionTestUtils.setField(adzunaService, "restTemplate", restTemplate);
    }

    @Test
    void searchJobs_success() {
        String jsonResponse = "{\"results\":[{\"title\":\"Java Dev\",\"company\":{\"display_name\":\"Acme\"},\"location\":{\"display_name\":\"NY\"},\"redirect_url\":\"http://url\",\"description\":\"Desc\"}]}";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        List<Map<String, Object>> jobs = adzunaService.searchJobs("java", List.of("spring"), "USA");
        assertFalse(jobs.isEmpty());
        assertEquals("Java Dev", jobs.get(0).get("title"));
    }
    
    @Test
    void searchJobs_emptyResponse() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(new ResponseEntity<>("", HttpStatus.OK));

        List<Map<String, Object>> jobs = adzunaService.searchJobs("java", List.of("spring"), "USA");
        assertTrue(jobs.isEmpty());
    }

    @Test
    void searchJobs_fallback() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(new ResponseEntity<>("", HttpStatus.OK)) // Primary query fails
            .thenReturn(new ResponseEntity<>("{\"results\":[{\"title\":\"Fallback Dev\"}]}", HttpStatus.OK)); // Fallback works

        List<Map<String, Object>> jobs = adzunaService.searchJobs("unknown title", List.of("spring"), "USA");
        assertFalse(jobs.isEmpty());
        assertEquals("Fallback Dev", jobs.get(0).get("title"));
    }
    
    @Test
    void fetchJobs_success() {
        String jsonResponse = "{\"results\":[{\"title\":\"Java Dev\",\"company\":{\"display_name\":\"Acme\"},\"location\":{\"display_name\":\"NY\"},\"redirect_url\":\"http://url\",\"description\":\"Desc\"}]}";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        List<Map<String, Object>> jobs = adzunaService.fetchJobs("java");
        assertFalse(jobs.isEmpty());
    }
}
