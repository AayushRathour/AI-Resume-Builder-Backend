package com.resumeai.jobmatch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class TheirStackServiceTest {

    @Mock private RestTemplate restTemplate;
    private TheirStackService theirStackService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        theirStackService = new TheirStackService(objectMapper, "http://api.com", "dummy-key", null);
        ReflectionTestUtils.setField(theirStackService, "restTemplate", restTemplate);
    }

    @Test
    void searchJobs_success() throws Exception {
        String jsonResponse = "{\"data\":[{\"title\":\"Java Dev\",\"company\":\"Acme\"}]}";
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        List<Map<String, Object>> jobs = theirStackService.searchJobs("java", List.of("spring"), "NY");
        assertFalse(jobs.isEmpty());
        assertEquals("Java Dev", jobs.get(0).get("title"));
    }

    @Test
    void searchJobs_missingKey() {
        TheirStackService serviceNoKey = new TheirStackService(objectMapper, "http://api.com", null, null);
        List<Map<String, Object>> jobs = serviceNoKey.searchJobs("java", null, null);
        assertTrue(jobs.isEmpty());
    }

    @Test
    void searchJobs_fallback_on_empty() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>("", HttpStatus.OK)) // first call empty
            .thenReturn(new ResponseEntity<>("{\"data\":[{\"title\":\"Fallback Dev\"}]}", HttpStatus.OK));

        List<Map<String, Object>> jobs = theirStackService.searchJobs("java", List.of("spring"), "NY");
        assertFalse(jobs.isEmpty());
        assertEquals("Fallback Dev", jobs.get(0).get("title"));
    }
}
