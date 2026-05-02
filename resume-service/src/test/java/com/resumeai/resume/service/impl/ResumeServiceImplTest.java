package com.resumeai.resume.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.resumeai.resume.client.SectionServiceClient;
import com.resumeai.resume.dto.ResumeRequest;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.entity.ResumeStatus;
import com.resumeai.resume.exception.InvalidInputException;
import com.resumeai.resume.repository.ResumeRepository;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private GeminiAtsClient geminiAtsClient;

    @Mock
    private SectionServiceClient sectionServiceClient;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private ResumeRequest request;

    @BeforeEach
    void setUp() {
        request = ResumeRequest.builder()
                .title("Backend Resume")
                .targetJobTitle("Java Developer")
                .templateId(2L)
                .language("English")
                .sectionsJson("[]")
                .build();
    }

    @Test
    void createResume_setsDefaultsAndOwner() {
        Resume saved = Resume.builder()
                .resumeId(10L)
                .userId(7L)
                .title(request.getTitle())
                .targetJobTitle(request.getTargetJobTitle())
                .templateId(request.getTemplateId())
                .language(request.getLanguage())
                .sectionsJson(request.getSectionsJson())
                .status(ResumeStatus.DRAFT)
                .atsScore(0.0)
                .isPublic(false)
                .viewCount(0L)
                .build();

        when(resumeRepository.save(any(Resume.class))).thenReturn(saved);

        var response = resumeService.createResume(7L, request, "Bearer test-token");

        assertEquals(10L, response.getResumeId());
        assertEquals(7L, response.getUserId());
        assertEquals(ResumeStatus.DRAFT, response.getStatus());
        assertEquals(false, response.getIsPublic());
    }

    @Test
    void updateResume_rejectsNonOwner() {
        Resume existing = Resume.builder().resumeId(3L).userId(11L).build();
        when(resumeRepository.findByResumeId(3L)).thenReturn(Optional.of(existing));

        assertThrows(InvalidInputException.class, () -> resumeService.updateResume(3L, 9L, request));
        verify(resumeRepository, never()).save(any(Resume.class));
    }

    @Test
    void duplicateResume_copiesSectionPayloadToTargetResume() {
        Resume source = Resume.builder()
                .resumeId(5L)
                .userId(2L)
                .title("Original")
                .targetJobTitle("SDE")
                .templateId(1L)
                .atsScore(88.0)
                .status(ResumeStatus.COMPLETE)
                .language("English")
                .sectionsJson("[]")
                .isPublic(true)
                .viewCount(50L)
                .build();

        Resume duplicate = Resume.builder()
                .resumeId(6L)
                .userId(2L)
                .title("Original (Copy)")
                .targetJobTitle("SDE")
                .templateId(1L)
                .atsScore(88.0)
                .status(ResumeStatus.DRAFT)
                .language("English")
                .sectionsJson("[]")
                .isPublic(false)
                .viewCount(0L)
                .build();

        when(resumeRepository.findByResumeId(5L)).thenReturn(Optional.of(source));
        when(resumeRepository.save(any(Resume.class))).thenReturn(duplicate);

        var response = resumeService.duplicateResume(5L, 2L);

        ArgumentCaptor<Long> sourceCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> targetCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);
        verify(sectionServiceClient).copySections(sourceCaptor.capture(), targetCaptor.capture(), userCaptor.capture());

        assertEquals(6L, response.getResumeId());
        assertEquals(5L, sourceCaptor.getValue());
        assertEquals(6L, targetCaptor.getValue());
        assertEquals(2L, userCaptor.getValue());
    }
}
