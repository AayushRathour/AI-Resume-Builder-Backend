package com.resumeai.ai.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.dto.*;
import com.resumeai.ai.entity.AiRequest;
import com.resumeai.ai.entity.RequestStatus;
import com.resumeai.ai.entity.RequestType;
import com.resumeai.ai.exception.QuotaExceededException;
import com.resumeai.ai.provider.AiProviderFactory;
import com.resumeai.ai.provider.AiProviderFactory.AiProviderResult;
import com.resumeai.ai.repository.AiRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiServiceImplTest {

    @Mock private AiRequestRepository repository;
    @Mock private ObjectMapper objectMapper;
    @Mock private AiProviderFactory providerFactory;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private AiServiceImpl aiService;

    private AiRequest aiRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "freeMonthlyCallsLimit", 5);
        ReflectionTestUtils.setField(aiService, "freeMonthlyAtsLimit", 5);
        ReflectionTestUtils.setField(aiService, "exchange", "test.exchange");
        ReflectionTestUtils.setField(aiService, "aiRoutingKey", "ai.completed");

        aiRequest = AiRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userId(1L).resumeId(10L)
                .requestType(RequestType.SUMMARY)
                .status(RequestStatus.QUEUED)
                .build();
        
        when(repository.save(any(AiRequest.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void checkAtsCompatibility_success() throws Exception {
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("{\"score\": 80, \"missingKeywords\": [], \"recommendations\": \"OK\"}", "model"));
        
        com.fasterxml.jackson.databind.JsonNode rootNode = new ObjectMapper().readTree("{\"score\": 80, \"missingKeywords\": [], \"recommendations\": \"OK\"}");
        when(objectMapper.readTree(anyString())).thenReturn(rootNode);
        when(objectMapper.convertValue(any(), eq(String[].class))).thenReturn(new String[0]);

        ATSRequest req = new ATSRequest();
        req.setResumeContent("Email: test@test.com. Experience: 5 years. Skills: Java. Education: BS. Summary: Hello.");
        req.setJobDescription("JD");

        ATSResponse resp = aiService.checkAtsCompatibility(1L, 10L, req);
        assertTrue(resp.getScore() > 0);
    }

    @Test
    void checkAtsCompatibility_hybrid_fallback() {
        when(providerFactory.generateWithFallback(anyString())).thenThrow(new RuntimeException("AI error"));

        ATSRequest req = new ATSRequest();
        req.setResumeContent("Java developer");
        req.setJobDescription("Looking for a Java developer with Python skills");

        ATSResponse resp = aiService.checkAtsCompatibility(1L, 10L, req);
        assertNotNull(resp);
        assertTrue(resp.getRecommendations().contains("keyword-based"));
    }

    @Test
    void extractResumeData_success() throws Exception {
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("{\"skills\":[\"Java\"], \"roles\":[\"Dev\"]}", "model"));
        when(objectMapper.readValue(anyString(), eq(ResumeExtractResponse.class)))
                .thenReturn(ResumeExtractResponse.builder().skills(List.of("Java")).build());
        
        ResumeExtractRequest req = ResumeExtractRequest.builder().userId(1L).resumeText("Raw text").build();
        ResumeExtractResponse resp = aiService.extractResumeData(req);
        assertNotNull(resp);
    }

    @Test
    void generateSummary_success() {
        when(repository.countByUserIdCurrentMonth(1L)).thenReturn(0L);
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("Summary", "model"));
        
        AIResponse resp = aiService.generateSummary(1L, 10L, new SummaryRequest());
        assertNotNull(resp);
    }

    @Test
    void generateBulletPoints_success() {
        when(repository.countByUserIdCurrentMonth(1L)).thenReturn(0L);
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("Bullet 1\nBullet 2", "model"));

        BulletRequest req = new BulletRequest();
        AIResponse resp = aiService.generateBulletPoints(1L, 10L, req);
        assertNotNull(resp);
    }

    @Test
    void generateCoverLetter_success() {
        when(repository.countByUserIdCurrentMonth(1L)).thenReturn(0L);
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("Cover Letter", "model"));

        CoverLetterRequest req = new CoverLetterRequest();
        AIResponse resp = aiService.generateCoverLetter(1L, 10L, req);
        assertNotNull(resp);
    }

    @Test
    void improveSection_success() {
        when(repository.countByUserIdCurrentMonth(1L)).thenReturn(0L);
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("Improved", "model"));

        ImproveRequest req = new ImproveRequest();
        AIResponse resp = aiService.improveSection(1L, 10L, req);
        assertNotNull(resp);
    }

    @Test
    void suggestSkills_success() {
        when(repository.countByUserIdCurrentMonth(1L)).thenReturn(0L);
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("Skill 1\nSkill 2", "model"));

        SkillRequest req = new SkillRequest();
        AIResponse resp = aiService.suggestSkills(1L, 10L, req);
        assertNotNull(resp);
    }

    @Test
    void tailorResume_success() {
        when(repository.countByUserIdCurrentMonth(1L)).thenReturn(0L);
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("Tailored", "model"));

        TailorRequest req = new TailorRequest();
        AIResponse resp = aiService.tailorResumeForJob(1L, 10L, req);
        assertNotNull(resp);
    }

    @Test
    void translateResume_success() {
        when(repository.countByUserIdCurrentMonth(1L)).thenReturn(0L);
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("Translated", "model"));

        TranslateRequest req = new TranslateRequest();
        AIResponse resp = aiService.translateResume(1L, 10L, req);
        assertNotNull(resp);
    }

    @Test
    void getAiHistory() {
        when(repository.findByUserId(1L)).thenReturn(List.of(aiRequest));
        List<AIHistoryResponse> history = aiService.getAiHistory(1L);
        assertEquals(1, history.size());
    }

    @Test
    void getRemainingQuota() {
        when(repository.countByUserIdCurrentMonth(1L)).thenReturn(2L);
        QuotaResponse quota = aiService.getRemainingQuota(1L);
        assertEquals(3, quota.getRemainingCalls());
    }
    @Test
    void analyzeMissingSkills_success() throws Exception {
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("{\"missingSkills\": \"Java\", \"recommendations\": \"Learn Java\"}", "model"));
        when(objectMapper.readValue(anyString(), eq(MissingSkillsResponse.class)))
                .thenReturn(new MissingSkillsResponse("Java", "Learn Java"));

        MissingSkillsRequest req = new MissingSkillsRequest();
        req.setUserId(1L);
        MissingSkillsResponse resp = aiService.analyzeMissingSkills(req);
        assertNotNull(resp);
    }

    @Test
    void chat_success() {
        when(repository.countByUserIdCurrentMonth(1L)).thenReturn(0L);
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("Chat response", "model"));

        ChatRequest req = new ChatRequest();
        req.setContext("");
        AIResponse resp = aiService.chat(1L, req);
        assertNotNull(resp);
    }

    @Test
    void extractResumeData_fallback() {
        when(providerFactory.generateWithFallback(anyString())).thenThrow(new RuntimeException("API error"));
        
        ResumeExtractRequest req = ResumeExtractRequest.builder().userId(1L).resumeText("Java dev").build();
        ResumeExtractResponse resp = aiService.extractResumeData(req);
        assertNotNull(resp);
        assertTrue(resp.getSkills().isEmpty());
    }

    @Test
    void checkAtsCompatibility_emptyResume() {
        ATSRequest req = new ATSRequest();
        req.setResumeContent("");
        ATSResponse resp = aiService.checkAtsCompatibility(1L, 10L, req);
        assertEquals(0, resp.getScore());
        assertTrue(resp.getRecommendations().contains("Could not extract"));
    }

    @Test
    void checkAtsCompatibility_parsingFailure() throws Exception {
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("invalid json", "model"));
        when(objectMapper.readTree(anyString())).thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "error"));

        ATSRequest req = new ATSRequest();
        req.setResumeContent("Experience: Java, Python. Skills: AWS, Docker.");
        req.setJobDescription("Looking for Java AWS expert.");

        ATSResponse resp = aiService.checkAtsCompatibility(1L, 10L, req);
        assertNotNull(resp);
        assertTrue(resp.getScore() > 0); // Hybrid score should still work
    }

    @Test
    void checkAtsCompatibility_quotaExceeded() {
        when(providerFactory.generateWithFallback(anyString())).thenThrow(new QuotaExceededException("Quota exceeded"));

        ATSRequest req = new ATSRequest();
        req.setResumeContent("Experience: Java. Skills: Python.");
        req.setJobDescription("Java Developer");

        ATSResponse resp = aiService.checkAtsCompatibility(1L, 10L, req);
        assertNotNull(resp);
        assertTrue(resp.getRecommendations().toLowerCase().contains("unavailable"));
    }

    @Test
    void checkAtsCompatibility_noJobDescription() throws Exception {
        // Mocking the AI response for standalone (no JD) path
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("{\"score\": 75, \"missingKeywords\": [], \"recommendations\": \"Structure is good\"}", "model"));
        
        com.fasterxml.jackson.databind.JsonNode rootNode = new ObjectMapper().readTree("{\"score\": 75, \"missingKeywords\": [], \"recommendations\": \"Structure is good\"}");
        when(objectMapper.readTree(anyString())).thenReturn(rootNode);
        when(objectMapper.convertValue(any(), eq(String[].class))).thenReturn(new String[0]);

        ATSRequest req = new ATSRequest();
        req.setResumeContent("Email: test@test.com. Experience: 5 years. Skills: Java. Education: BS. Summary: Hello.");
        req.setJobDescription(""); // Empty JD

        ATSResponse resp = aiService.checkAtsCompatibility(1L, 10L, req);
        assertTrue(resp.getScore() > 0);
    }

    @Test
    void analyzeMissingSkills_fallback() {
        when(providerFactory.generateWithFallback(anyString())).thenThrow(new RuntimeException("API error"));
        MissingSkillsRequest req = new MissingSkillsRequest();
        req.setResumeText("Java");
        MissingSkillsResponse resp = aiService.analyzeMissingSkills(req);
        assertEquals("AI TEMPORARILY UNAVAILABLE", resp.getRecommendations());
    }

    @Test
    void executeAndSave_fallback() {
        when(providerFactory.generateWithFallback(anyString())).thenThrow(new RuntimeException("API error"));
        AIResponse resp = aiService.generateSummary(1L, 10L, new SummaryRequest());
        assertEquals("AI TEMPORARILY UNAVAILABLE", resp.getText());
    }

    @Test
    void saveQueued_exceptionHandling() {
        // Test extractResumeData when repository.save fails initially
        when(repository.save(any(AiRequest.class))).thenThrow(new RuntimeException("DB Error")).thenAnswer(i -> i.getArguments()[0]);
        when(providerFactory.generateWithFallback(anyString())).thenReturn(new AiProviderResult("{\"skills\":[]}", "model"));
        
        ResumeExtractRequest req = ResumeExtractRequest.builder().userId(1L).resumeText("text").build();
        ResumeExtractResponse resp = aiService.extractResumeData(req);
        assertNotNull(resp);
    }
}
