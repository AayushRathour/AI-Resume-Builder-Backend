package com.resumeai.section.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.resumeai.section.client.ResumePayload;
import com.resumeai.section.client.ResumeServiceClient;
import com.resumeai.section.dto.SectionReorderItemRequest;
import com.resumeai.section.dto.SectionRequest;
import com.resumeai.section.dto.SectionUpdateItemRequest;
import com.resumeai.section.entity.Section;
import com.resumeai.section.entity.SectionType;
import com.resumeai.section.repository.SectionRepository;

@ExtendWith(MockitoExtension.class)
class SectionServiceImplTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private ResumeServiceClient resumeServiceClient;

    @InjectMocks
    private SectionServiceImpl sectionService;

    @Test
    void addSection_wrapsPlainTextContentAsJson() {
        ResumePayload resumePayload = new ResumePayload();
        resumePayload.setResumeId(1L);
        resumePayload.setUserId(5L);
        when(resumeServiceClient.getResumeById(1L, 5L)).thenReturn(resumePayload);

        SectionRequest request = SectionRequest.builder()
                .resumeId(1L)
                .sectionType(SectionType.SUMMARY)
                .title("Summary")
                .content("plain text")
                .displayOrder(1)
                .isVisible(true)
                .aiGenerated(false)
                .build();

        Section saved = Section.builder()
                .sectionId(10L)
                .resumeId(1L)
                .sectionType(SectionType.SUMMARY)
                .title("Summary")
                .content("{\"text\":\"plain text\"}")
                .displayOrder(1)
                .isVisible(true)
                .aiGenerated(false)
                .build();
        when(sectionRepository.save(any(Section.class))).thenReturn(saved);

        var response = sectionService.addSection(request, 5L);

        assertTrue(response.getContent().contains("text"));
        assertEquals(1, response.getDisplayOrder());
    }

    @Test
    void reorderSections_setsSequentialDisplayOrder() {
        ResumePayload resumePayload = new ResumePayload();
        resumePayload.setResumeId(2L);
        resumePayload.setUserId(9L);
        when(resumeServiceClient.getResumeById(2L, 9L)).thenReturn(resumePayload);

        Section a = Section.builder().sectionId(21L).resumeId(2L).displayOrder(2).build();
        Section b = Section.builder().sectionId(22L).resumeId(2L).displayOrder(1).build();

        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc(2L)).thenReturn(List.of(a, b));
        when(sectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = sectionService.reorderSections(
                2L,
                List.of(
                        SectionReorderItemRequest.builder().sectionId(22L).displayOrder(1).build(),
                        SectionReorderItemRequest.builder().sectionId(21L).displayOrder(2).build()),
                9L);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getDisplayOrder());
        assertEquals(2, result.get(1).getDisplayOrder());
    }

    @Test
    void bulkUpdateSections_updatesMultipleRecords() {
        ResumePayload resumePayload = new ResumePayload();
        resumePayload.setResumeId(3L);
        resumePayload.setUserId(7L);
        when(resumeServiceClient.getResumeById(3L, 7L)).thenReturn(resumePayload);

        Section existing = Section.builder()
                .sectionId(30L)
                .resumeId(3L)
                .sectionType(SectionType.EXPERIENCE)
                .title("Old")
                .content("{}")
                .displayOrder(1)
                .isVisible(true)
                .aiGenerated(false)
                .build();

        when(sectionRepository.findByResumeId(3L)).thenReturn(List.of(existing));
        when(sectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var updates = List.of(SectionUpdateItemRequest.builder()
                .sectionId(30L)
                .sectionType(SectionType.PROJECTS)
                .title("Updated")
                .content("{\"items\":[]}")
                .displayOrder(1)
                .isVisible(false)
                .aiGenerated(true)
                .build());

        var result = sectionService.bulkUpdateSections(3L, updates, 7L);

        assertEquals(1, result.size());
        assertEquals("Updated", result.get(0).getTitle());
        assertEquals(SectionType.PROJECTS, result.get(0).getSectionType());
        assertEquals(true, result.get(0).getAiGenerated());
    }
}
