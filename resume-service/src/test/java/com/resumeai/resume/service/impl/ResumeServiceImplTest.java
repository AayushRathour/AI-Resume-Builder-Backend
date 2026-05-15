package com.resumeai.resume.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.resume.client.SectionServiceClient;
import com.resumeai.resume.dto.ResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.entity.ResumeStatus;
import com.resumeai.resume.exception.InvalidInputException;
import com.resumeai.resume.exception.ResumeNotFoundException;
import com.resumeai.resume.repository.ResumeRepository;
import com.resumeai.resume.service.NotificationProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private GeminiAtsClient geminiAtsClient;
    @Mock private SectionServiceClient sectionServiceClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private NotificationProducer notificationProducer;

    @InjectMocks private ResumeServiceImpl resumeService;

    private Resume resume;
    private ResumeRequest request;

    @BeforeEach
    void setUp() {
        resume = Resume.builder()
                .resumeId(1L).userId(10L).title("My Resume")
                .name("John").email("john@test.com").phone("1234567890")
                .location("NYC").targetJobTitle("Dev").templateId(1L)
                .language("English").summary("Summary").skills("Java")
                .experience("5 yrs").education("BS CS").projects("Proj")
                .sectionsJson("[]").atsScore(50.0)
                .status(ResumeStatus.DRAFT).isPublic(false).viewCount(0L)
                .build();

        request = ResumeRequest.builder()
                .title("My Resume").name("John").email("john@test.com")
                .phone("1234567890").location("NYC").targetJobTitle("Dev")
                .templateId(1L).language("English").summary("Summary")
                .skills("Java").experience("5 yrs").education("BS CS")
                .projects("Proj").sectionsJson("[]")
                .build();
    }

    // --- createResume ---
    @Test
    void createResume_success() {
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);
        when(resumeRepository.countByUserId(10L)).thenReturn(0L);

        ResumeResponse resp = resumeService.createResume(10L, request, null, null);

        assertNotNull(resp);
        assertEquals(1L, resp.getResumeId());
        verify(notificationProducer).publishResumeCreatedEvent(10L, 1L, "My Resume");
    }

    @Test
    void createResume_nullUser_throws() {
        assertThrows(InvalidInputException.class,
                () -> resumeService.createResume(null, request, null, null));
    }

    @Test
    void createResume_freePlanLimit_throws() {
        when(resumeRepository.countByUserId(10L)).thenReturn(3L);

        assertThrows(InvalidInputException.class,
                () -> resumeService.createResume(10L, request, null, null));
    }

    @Test
    void createResume_premiumByHeader_noLimit() {
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);

        ResumeResponse resp = resumeService.createResume(10L, request, null, "PREMIUM");

        assertNotNull(resp);
    }

    @Test
    void createResume_blankLanguage_defaultsToEnglish() {
        request.setLanguage("");
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);
        when(resumeRepository.countByUserId(10L)).thenReturn(0L);

        ResumeResponse resp = resumeService.createResume(10L, request, null, null);
        assertNotNull(resp);
    }

    // --- getResumeById ---
    @Test
    void getResumeById_success() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));

        ResumeResponse resp = resumeService.getResumeById(1L, 10L);

        assertNotNull(resp);
        assertEquals(1L, resp.getResumeId());
    }

    @Test
    void getResumeById_publicResume_noOwnerCheck() {
        resume.setIsPublic(true);
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));

        ResumeResponse resp = resumeService.getResumeById(1L, 999L);
        assertNotNull(resp);
    }

    @Test
    void getResumeById_notFound() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.empty());

        assertThrows(ResumeNotFoundException.class,
                () -> resumeService.getResumeById(1L, 10L));
    }

    @Test
    void getResumeById_notOwner_throws() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));

        assertThrows(InvalidInputException.class,
                () -> resumeService.getResumeById(1L, 999L));
    }

    // --- getResumesByUser ---
    @Test
    void getResumesByUser_success() {
        when(resumeRepository.findByUserId(10L)).thenReturn(List.of(resume));

        List<ResumeResponse> list = resumeService.getResumesByUser(10L, 10L);

        assertEquals(1, list.size());
    }

    @Test
    void getResumesByUser_accessDenied() {
        assertThrows(InvalidInputException.class,
                () -> resumeService.getResumesByUser(10L, 999L));
    }

    // --- updateResume ---
    @Test
    void updateResume_success() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);

        ResumeResponse resp = resumeService.updateResume(1L, 10L, request);

        assertNotNull(resp);
        verify(notificationProducer).publishResumeUpdatedEvent(10L, 1L, "My Resume");
    }

    @Test
    void updateResume_notOwner_throws() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));

        assertThrows(InvalidInputException.class,
                () -> resumeService.updateResume(1L, 999L, request));
    }

    // --- deleteResume ---
    @Test
    void deleteResume_success() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));

        resumeService.deleteResume(1L, 10L);

        verify(sectionServiceClient).deleteAllSections(1L, 10L);
        verify(resumeRepository).delete(resume);
        verify(notificationProducer).publishResumeDeletedEvent(10L, 1L, "My Resume");
    }

    // --- duplicateResume ---
    @Test
    void duplicateResume_success() {
        Resume dup = Resume.builder().resumeId(2L).userId(10L).title("My Resume (Copy)")
                .status(ResumeStatus.DRAFT).atsScore(50.0).isPublic(false).viewCount(0L).build();
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(dup);

        ResumeResponse resp = resumeService.duplicateResume(1L, 10L);

        assertEquals(2L, resp.getResumeId());
        verify(sectionServiceClient).copySections(1L, 2L, 10L);
    }

    // --- updateAtsScore ---
    @Test
    void updateAtsScore_success() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);

        ResumeResponse resp = resumeService.updateAtsScore(1L, 85.0, 10L, true);

        assertNotNull(resp);
    }

    @Test
    void updateAtsScore_negativeScore_throws() {
        assertThrows(InvalidInputException.class,
                () -> resumeService.updateAtsScore(1L, -1.0, 10L, true));
    }

    @Test
    void updateAtsScore_notInternal_throws() {
        assertThrows(InvalidInputException.class,
                () -> resumeService.updateAtsScore(1L, 85.0, 10L, false));
    }

    // --- generateAtsScoreWithAi ---
    @Test
    void generateAtsScoreWithAi_success() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));
        when(geminiAtsClient.generateAtsScore("resume text", "job desc")).thenReturn(92.0);
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);

        ResumeResponse resp = resumeService.generateAtsScoreWithAi(1L, 10L, "resume text", "job desc");

        assertNotNull(resp);
    }

    @Test
    void generateAtsScoreWithAi_emptyText_throws() {
        assertThrows(InvalidInputException.class,
                () -> resumeService.generateAtsScoreWithAi(1L, 10L, "", "job desc"));
    }

    // --- publishResume ---
    @Test
    void publishResume_success() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);

        ResumeResponse resp = resumeService.publishResume(1L, 10L);

        assertNotNull(resp);
    }

    // --- unpublishResume ---
    @Test
    void unpublishResume_success() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);

        ResumeResponse resp = resumeService.unpublishResume(1L, 10L);

        assertNotNull(resp);
    }

    // --- incrementViewCount ---
    @Test
    void incrementViewCount_success() {
        resume.setIsPublic(true);
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));

        resumeService.incrementViewCount(1L);

        verify(resumeRepository).save(resume);
        assertEquals(1L, resume.getViewCount());
    }

    @Test
    void incrementViewCount_notPublic_throws() {
        when(resumeRepository.findByResumeId(1L)).thenReturn(Optional.of(resume));

        assertThrows(InvalidInputException.class,
                () -> resumeService.incrementViewCount(1L));
    }

    // --- getPublicResumes ---
    @Test
    void getPublicResumes() {
        when(resumeRepository.findByIsPublic(true)).thenReturn(List.of(resume));

        List<ResumeResponse> list = resumeService.getPublicResumes();

        assertEquals(1, list.size());
    }

    // --- getResumesByTemplate ---
    @Test
    void getResumesByTemplate() {
        when(resumeRepository.findByTemplateId(1L)).thenReturn(List.of(resume));

        List<ResumeResponse> list = resumeService.getResumesByTemplate(1L);

        assertEquals(1, list.size());
    }
}
