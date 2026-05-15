package com.resumeai.jobmatch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.resumeai.jobmatch.client.AiServiceClient;
import com.resumeai.jobmatch.client.ResumeClient;
import com.resumeai.jobmatch.client.SectionClient;
import com.resumeai.jobmatch.dto.*;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import com.resumeai.jobmatch.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobMatchServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobMatchRepository jobMatchRepository;
    @Mock private ResumeClient resumeClient;
    @Mock private SectionClient sectionClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private PdfParserService pdfParserService;
    @Mock private ResumeStructuringService resumeStructuringService;
    @Mock private AiServiceClient aiServiceClient;
    @Mock private AdzunaService adzunaService;
    @Mock private TheirStackService theirStackService;

    @InjectMocks private JobMatchService jobMatchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jobMatchService, "exchange", "test.exchange");
        ReflectionTestUtils.setField(jobMatchService, "jobRoutingKey", "job.match");
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void analyzeAndMatchDetailed_withFile_success() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        when(pdfParserService.extractTextFromPdf(any())).thenReturn("Raw text");
        
        setupSuccessMocks();

        AnalysisResponse resp = jobMatchService.analyzeAndMatchDetailed(file, null, 1L, "Dev", "NY");
        assertNotNull(resp);
        assertEquals(1, resp.getTotalMatches());
    }

    @Test
    void analyzeAndMatchDetailed_noFileOrResumeId_throws() {
        assertThrows(ResponseStatusException.class, 
            () -> jobMatchService.analyzeAndMatchDetailed(null, null, 1L, "Dev", "NY"));
    }

    @Test
    void analyzeAndMatchDetailed_withResumeId_success() {
        ResumeDTO resumeDTO = new ResumeDTO();
        resumeDTO.setSectionsJson("Java developer content");
        resumeDTO.setUserId(1L);
        when(resumeClient.getResumeById(anyLong(), anyLong())).thenReturn(resumeDTO);
        when(sectionClient.getSectionsByResumeId(anyLong(), anyLong())).thenReturn(List.of());
        
        setupSuccessMocks();

        AnalysisResponse resp = jobMatchService.analyzeAndMatchDetailed(null, 1L, 1L, "Dev", "NY");
        assertNotNull(resp);
    }
    
    @Test
    void analyzeAndMatchDetailed_fallbackToTheirStack() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        when(pdfParserService.extractTextFromPdf(any())).thenReturn("Raw text");
        
        setupSuccessMocks();
        // Force Adzuna to return empty, fallback to TheirStack
        when(adzunaService.fetchJobs(anyString(), anyString())).thenReturn(List.of());
        when(theirStackService.searchJobs(anyString(), anyList(), anyString())).thenReturn(List.of(
            Map.of("title", "Java Dev", "description", "Java role", "company", "Acme")
        ));

        AnalysisResponse resp = jobMatchService.analyzeAndMatchDetailed(file, null, 1L, "Dev", "NY");
        assertNotNull(resp);
        assertEquals(1, resp.getTotalMatches());
    }

    private void setupSuccessMocks() {
        ResumeStructuredData structured = new ResumeStructuredData();
        structured.setNormalizedText("Normalized text");
        structured.setSkills(List.of("Java"));
        when(resumeStructuringService.fromRawText(anyString())).thenReturn(structured);
        when(resumeStructuringService.normalizeText(anyString())).thenReturn("Normalized text");
        
        when(aiServiceClient.extractResume(any())).thenReturn(AiServiceResponse.<ResumeExtractResponse>builder()
                .status("success")
                .data(ResumeExtractResponse.builder().skills(List.of("Java")).roles(List.of("Dev")).build())
                .build());
        
        when(adzunaService.fetchJobs(anyString(), anyString())).thenReturn(List.of(
            Map.of("title", "Java Dev", "description", "Java role", "company", "Acme")
        ));
    }

    @Test
    void matchResumeWithJobs_success() {
        ResumeDTO resumeDTO = new ResumeDTO();
        resumeDTO.setSectionsJson("Java developer content");
        resumeDTO.setUserId(1L);
        when(resumeClient.getResumeById(anyLong(), anyLong())).thenReturn(resumeDTO);

        when(adzunaService.fetchJobs(anyString(), anyString())).thenReturn(List.of(
            Map.of("title", "Java Dev", "description", "Java role", "company", "Acme")
        ));
        
        ResumeStructuredData structured = new ResumeStructuredData();
        structured.setNormalizedText("java developer content");
        structured.setSkills(List.of("java"));
        when(resumeStructuringService.fromRawText(anyString())).thenReturn(structured);
        when(resumeStructuringService.normalizeText(anyString())).thenReturn("java developer content");
        
        when(aiServiceClient.extractResume(any())).thenReturn(AiServiceResponse.<ResumeExtractResponse>builder()
                .status("success")
                .data(ResumeExtractResponse.builder().skills(List.of("Java")).roles(List.of("Dev")).build())
                .build());

        List<MatchResponse> resp = jobMatchService.matchResumeWithJobs(1L, 10L, "Dev", "JD");
        assertNotNull(resp);
    }

    @Test
    void getRankedJobs() {
        when(jobMatchRepository.findByUserIdOrderByMatchScoreDescCreatedAtDesc(1L)).thenReturn(new ArrayList<>());
        List<MatchResponse> resp = jobMatchService.getRankedJobs(1L);
        assertNotNull(resp);
    }
    
    @Test
    void getTopMatches() {
        when(jobMatchRepository.findByUserIdOrderByMatchScoreDescCreatedAtDesc(1L)).thenReturn(List.of(new JobMatch()));
        List<MatchResponse> resp = jobMatchService.getTopMatches(1L, 1);
        assertNotNull(resp);
        assertEquals(1, resp.size());
    }

    @Test
    void updateBookmark() {
        UUID id = UUID.randomUUID();
        JobMatch match = JobMatch.builder().matchId(id).userId(1L).build();
        when(jobMatchRepository.findById(id)).thenReturn(Optional.of(match));
        
        MatchResponse resp = jobMatchService.updateBookmark(id, true);
        assertNotNull(resp);
        assertTrue(resp.isBookmarked());
    }

    @Test
    void toggleBookmark() {
        UUID id = UUID.randomUUID();
        JobMatch match = new JobMatch();
        match.setMatchId(id);
        match.setUserId(1L);
        match.setBookmarked(false);
        when(jobMatchRepository.findById(id)).thenReturn(Optional.of(match));
        
        MatchResponse resp = jobMatchService.toggleBookmark(id);
        assertNotNull(resp);
        assertTrue(resp.isBookmarked());
    }

    @Test
    void analyzeAndMatchDetailed_rawTextNull_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        when(pdfParserService.extractTextFromPdf(any())).thenReturn(null);
        assertThrows(ResponseStatusException.class, 
            () -> jobMatchService.analyzeAndMatchDetailed(file, null, 1L, "Dev", "NY"));
    }

    @Test
    void analyzeAndMatchDetailed_structuredResumeNull_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        when(pdfParserService.extractTextFromPdf(any())).thenReturn("text");
        when(resumeStructuringService.fromRawText(anyString())).thenReturn(null);
        assertThrows(ResponseStatusException.class, 
            () -> jobMatchService.analyzeAndMatchDetailed(file, null, 1L, "Dev", "NY"));
    }

    @Test
    void analyzeAndMatchDetailed_aiFailed_fallbackWorks() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        when(pdfParserService.extractTextFromPdf(any())).thenReturn("Raw text");
        setupSuccessMocks();
        when(aiServiceClient.extractResume(any())).thenThrow(new RuntimeException("AI error"));

        AnalysisResponse resp = jobMatchService.analyzeAndMatchDetailed(file, null, 1L, "Dev", "NY");
        assertNotNull(resp);
    }

    @Test
    void analyzeAndMatchDetailed_adzunaError_fallbackToTheirStack() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        when(pdfParserService.extractTextFromPdf(any())).thenReturn("Raw text");
        setupSuccessMocks();
        when(adzunaService.fetchJobs(anyString(), anyString())).thenThrow(new RuntimeException("Adzuna error"));
        when(theirStackService.searchJobs(anyString(), anyList(), anyString())).thenReturn(List.of(
            Map.of("title", "Java Dev", "description", "Java role", "company", "Acme")
        ));

        AnalysisResponse resp = jobMatchService.analyzeAndMatchDetailed(file, null, 1L, "Dev", "NY");
        assertNotNull(resp);
        assertEquals(1, resp.getTotalMatches());
    }

    @Test
    void analyzeAndMatchDetailed_noJobsFound() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        when(pdfParserService.extractTextFromPdf(any())).thenReturn("Raw text");
        setupSuccessMocks();
        when(adzunaService.fetchJobs(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(theirStackService.searchJobs(anyString(), anyList(), anyString())).thenReturn(new ArrayList<>());

        AnalysisResponse resp = jobMatchService.analyzeAndMatchDetailed(file, null, 1L, "Dev", "NY");
        assertEquals(0, resp.getTotalMatches());
    }

    @Test
    void getRankedJobs_invalidUser() {
        assertTrue(jobMatchService.getRankedJobs(null).isEmpty());
        assertTrue(jobMatchService.getRankedJobs(0L).isEmpty());
    }

    @Test
    void getTopMatches_invalidInputs() {
        assertTrue(jobMatchService.getTopMatches(null, 5).isEmpty());
        when(jobMatchRepository.findByUserIdOrderByMatchScoreDescCreatedAtDesc(1L)).thenReturn(List.of(new JobMatch()));
        assertEquals(1, jobMatchService.getTopMatches(1L, 0).size());
    }

    @Test
    void getMatchesForResume_invalidResume() {
        assertTrue(jobMatchService.getMatchesForResume(null).isEmpty());
        when(jobMatchRepository.findByResumeId(1L)).thenReturn(List.of(new JobMatch()));
        assertEquals(1, jobMatchService.getMatchesForResume(1L).size());
    }

    @Test
    void getMatchById_notFound_throws() {
        when(jobMatchRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> jobMatchService.getMatchById(UUID.randomUUID()));
    }

    @Test
    void updateBookmark_notFound_throws() {
        when(jobMatchRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> jobMatchService.updateBookmark(UUID.randomUUID(), true));
    }

    @Test
    void deleteMatch_notFound_throws() {
        when(jobMatchRepository.existsById(any())).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> jobMatchService.deleteMatch(UUID.randomUUID()));
    }

    @Test
    void extractRawResumeTextFromDb_resumeClientError() {
        when(resumeClient.getResumeById(anyLong(), anyLong())).thenThrow(new RuntimeException("Error"));
        when(sectionClient.getSectionsByResumeId(anyLong(), anyLong())).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> jobMatchService.analyzeAndMatchDetailed(null, 1L, 1L, "Dev", "NY"));
    }

    @Test
    void testHelperMethods() {
        // Covering readLong, readString, extractYears
        ReflectionTestUtils.invokeMethod(jobMatchService, "readLong", Map.of("id", "123"), "id");
        ReflectionTestUtils.invokeMethod(jobMatchService, "readLong", Map.of("id", 123), "id");
        ReflectionTestUtils.invokeMethod(jobMatchService, "readLong", Map.of("id", ""), "id");
        ReflectionTestUtils.invokeMethod(jobMatchService, "readLong", null, "id");
        
        ReflectionTestUtils.invokeMethod(jobMatchService, "readString", Map.of("k", "v"), "k");
        ReflectionTestUtils.invokeMethod(jobMatchService, "readString", Map.of("k", 1), "k");
        
        ReflectionTestUtils.invokeMethod(jobMatchService, "extractYears", "5 years experience");
        ReflectionTestUtils.invokeMethod(jobMatchService, "extractYears", "No years here");
        
        ReflectionTestUtils.invokeMethod(jobMatchService, "calculateExperienceScore", "5 yrs", List.of("2 years"), "Looking for 3 years");
        ReflectionTestUtils.invokeMethod(jobMatchService, "calculateExperienceScore", "1 yr", List.of("1 yr"), "Looking for 1 yr");
    }
}
