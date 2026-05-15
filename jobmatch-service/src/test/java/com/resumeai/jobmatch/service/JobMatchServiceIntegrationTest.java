package com.resumeai.jobmatch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.resumeai.jobmatch.client.AiServiceClient;
import com.resumeai.jobmatch.dto.AiServiceResponse;
import com.resumeai.jobmatch.dto.AnalysisResponse;
import com.resumeai.jobmatch.dto.ResumeExtractResponse;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class JobMatchServiceIntegrationTest {

    @Mock private PdfParserService pdfParser;
    @Mock private ResumeStructuringService structuringService;
    @Mock private AiServiceClient aiServiceClient;
    @Mock private AdzunaService adzunaService;
    @Mock private TheirStackService theirStackService;
    @Mock private JobMatchRepository jobMatchRepository;
    @Mock private NotificationProducer notificationProducer;

    @InjectMocks
    private JobMatchService jobMatchService;

    @Test
    void testAnalyzeAndMatchDetailed_fullFlow() throws Exception {
        // Mock PDF parsing
        when(pdfParser.extractTextFromPdf(any())).thenReturn("Raw resume text");
        
        // Mock structuring
        com.resumeai.jobmatch.dto.ResumeStructuredData mockData = new com.resumeai.jobmatch.dto.ResumeStructuredData();
        mockData.setNormalizedText("normalized content");
        when(structuringService.fromRawText(anyString())).thenReturn(mockData);
        
        // Mock AI extraction
        ResumeExtractResponse extractData = ResumeExtractResponse.builder()
                .skills(List.of("java", "spring"))
                .roles(List.of("developer"))
                .experience("5 years")
                .build();
        when(aiServiceClient.extractResume(any())).thenReturn(AiServiceResponse.<ResumeExtractResponse>builder()
                .status("success").data(extractData).build());
        
        // Mock Job Search
        List<Map<String, Object>> adzunaJobs = new ArrayList<>();
        adzunaJobs.add(Map.of("id", "1", "title", "Java Developer", "company", Map.of("display_name", "Acme"), "description", "Java and Spring Boot developer"));
        when(adzunaService.fetchJobs(anyString())).thenReturn(adzunaJobs);
        
        lenient().when(theirStackService.searchJobs(anyString(), anyList(), anyString())).thenReturn(new ArrayList<>());
        
        // Mock Repository
        lenient().when(jobMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "content".getBytes());
        
        AnalysisResponse response = jobMatchService.analyzeAndMatchDetailed(file, null, 1L, "Java Developer", "NY");
        
        assertNotNull(response);
        assertFalse(response.getJobs().isEmpty());
    }
}
