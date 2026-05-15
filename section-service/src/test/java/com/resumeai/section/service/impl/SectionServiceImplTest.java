package com.resumeai.section.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.resumeai.section.client.ResumePayload;
import com.resumeai.section.client.ResumeServiceClient;
import com.resumeai.section.dto.SectionReorderItemRequest;
import com.resumeai.section.dto.SectionRequest;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.dto.SectionUpdateItemRequest;
import com.resumeai.section.entity.Section;
import com.resumeai.section.entity.SectionType;
import com.resumeai.section.exception.InvalidInputException;
import com.resumeai.section.exception.SectionNotFoundException;
import com.resumeai.section.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SectionServiceImplTest {

    @Mock private SectionRepository sectionRepository;
    @Mock private ResumeServiceClient resumeServiceClient;

    @InjectMocks private SectionServiceImpl sectionService;

    private Section section;
    private SectionRequest request;
    private ResumePayload resumePayload;

    @BeforeEach
    void setUp() {
        section = Section.builder()
                .sectionId(1L).resumeId(10L)
                .sectionType(SectionType.EXPERIENCE).title("Experience")
                .content("{\"text\":\"5 years\"}").displayOrder(1)
                .isVisible(true).aiGenerated(false).build();

        request = SectionRequest.builder()
                .resumeId(10L).sectionType(SectionType.EXPERIENCE)
                .title("Experience").content("{\"text\":\"5 years\"}")
                .displayOrder(1).isVisible(true).aiGenerated(false).build();

        resumePayload = new ResumePayload();
        resumePayload.setResumeId(10L);
        resumePayload.setUserId(100L);
    }

    private void mockOwnership() {
        when(resumeServiceClient.getResumeById(10L, 100L)).thenReturn(resumePayload);
    }

    @Test
    void addSection_success() {
        mockOwnership();
        when(sectionRepository.save(any(Section.class))).thenReturn(section);

        SectionResponse resp = sectionService.addSection(request, 100L);

        assertNotNull(resp);
        assertEquals(1L, resp.getSectionId());
    }

    @Test
    void addSection_nullUser_throws() {
        assertThrows(InvalidInputException.class,
                () -> sectionService.addSection(request, null));
    }

    @Test
    void getSectionsByResume_success() {
        mockOwnership();
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(section));

        List<SectionResponse> list = sectionService.getSectionsByResume(10L, 100L);
        assertEquals(1, list.size());
    }

    @Test
    void getSectionById_success() {
        mockOwnership();
        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.of(section));

        SectionResponse resp = sectionService.getSectionById(1L, 100L);
        assertNotNull(resp);
    }

    @Test
    void getSectionById_notFound() {
        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.empty());

        assertThrows(SectionNotFoundException.class,
                () -> sectionService.getSectionById(1L, 100L));
    }

    @Test
    void updateSection_success() {
        mockOwnership();
        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.of(section));
        when(sectionRepository.save(any(Section.class))).thenReturn(section);

        SectionResponse resp = sectionService.updateSection(1L, request, 100L);
        assertNotNull(resp);
    }

    @Test
    void updateSection_resumeMismatch_throws() {
        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.of(section));
        when(resumeServiceClient.getResumeById(10L, 100L)).thenReturn(resumePayload);
        request.setResumeId(999L);

        assertThrows(InvalidInputException.class,
                () -> sectionService.updateSection(1L, request, 100L));
    }

    @Test
    void deleteSection_success() {
        mockOwnership();
        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.of(section));

        sectionService.deleteSection(1L, 100L);
        verify(sectionRepository).delete(section);
    }

    @Test
    void reorderSections_success() {
        mockOwnership();
        Section s2 = Section.builder().sectionId(2L).resumeId(10L)
                .sectionType(SectionType.EDUCATION).title("Edu")
                .displayOrder(2).isVisible(true).aiGenerated(false).build();

        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(section, s2));
        when(sectionRepository.saveAll(any())).thenReturn(List.of(section, s2));

        List<SectionReorderItemRequest> reorder = List.of(
                SectionReorderItemRequest.builder().sectionId(2L).displayOrder(1).build(),
                SectionReorderItemRequest.builder().sectionId(1L).displayOrder(2).build());

        List<SectionResponse> result = sectionService.reorderSections(10L, reorder, 100L);
        assertEquals(2, result.size());
    }

    @Test
    void reorderSections_sizeMismatch_throws() {
        mockOwnership();
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(section));

        List<SectionReorderItemRequest> reorder = List.of(
                SectionReorderItemRequest.builder().sectionId(1L).displayOrder(1).build(),
                SectionReorderItemRequest.builder().sectionId(2L).displayOrder(2).build());

        assertThrows(InvalidInputException.class,
                () -> sectionService.reorderSections(10L, reorder, 100L));
    }

    @Test
    void toggleVisibility_success() {
        mockOwnership();
        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.of(section));
        when(sectionRepository.save(any(Section.class))).thenReturn(section);

        SectionResponse resp = sectionService.toggleVisibility(1L, false, 100L);
        assertNotNull(resp);
    }

    @Test
    void deleteAllSections_success() {
        mockOwnership();

        sectionService.deleteAllSections(10L, 100L);
        verify(sectionRepository).deleteByResumeId(10L);
    }

    @Test
    void getSectionsByType_success() {
        mockOwnership();
        when(sectionRepository.findByResumeIdAndSectionType(10L, SectionType.EXPERIENCE))
                .thenReturn(List.of(section));

        List<SectionResponse> list = sectionService.getSectionsByType(10L, SectionType.EXPERIENCE, 100L);
        assertEquals(1, list.size());
    }

    @Test
    void bulkUpdateSections_success() {
        mockOwnership();
        when(sectionRepository.findByResumeId(10L)).thenReturn(List.of(section));
        when(sectionRepository.saveAll(any())).thenReturn(List.of(section));

        SectionUpdateItemRequest update = SectionUpdateItemRequest.builder()
                .sectionId(1L).sectionType(SectionType.EXPERIENCE)
                .title("Updated").content("{}")
                .displayOrder(1).isVisible(true).aiGenerated(false).build();

        List<SectionResponse> result = sectionService.bulkUpdateSections(10L, List.of(update), 100L);
        assertEquals(1, result.size());
    }

    @Test
    void bulkUpdateSections_sectionNotFound_throws() {
        mockOwnership();
        when(sectionRepository.findByResumeId(10L)).thenReturn(List.of(section));

        SectionUpdateItemRequest update = SectionUpdateItemRequest.builder()
                .sectionId(999L).sectionType(SectionType.EXPERIENCE)
                .title("Updated").content("{}")
                .displayOrder(1).isVisible(true).aiGenerated(false).build();

        assertThrows(SectionNotFoundException.class,
                () -> sectionService.bulkUpdateSections(10L, List.of(update), 100L));
    }

    @Test
    void addSection_accessDenied() {
        ResumePayload otherUser = new ResumePayload();
        otherUser.setResumeId(10L);
        otherUser.setUserId(999L);
        when(resumeServiceClient.getResumeById(10L, 100L)).thenReturn(otherUser);

        assertThrows(InvalidInputException.class,
                () -> sectionService.addSection(request, 100L));
    }

    @Test
    void addSection_normalizeContent_plainText() {
        mockOwnership();
        request.setContent("plain text content");
        when(sectionRepository.save(any(Section.class))).thenReturn(section);

        SectionResponse resp = sectionService.addSection(request, 100L);
        assertNotNull(resp);
    }

    @Test
    void addSection_normalizeContent_blankContent() {
        mockOwnership();
        request.setContent("");
        when(sectionRepository.save(any(Section.class))).thenReturn(section);

        SectionResponse resp = sectionService.addSection(request, 100L);
        assertNotNull(resp);
    }
}
