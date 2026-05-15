package com.resumeai.section.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import com.resumeai.section.dto.*;
import com.resumeai.section.entity.SectionType;
import com.resumeai.section.service.SectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SectionControllerTest {

    @Mock private SectionService sectionService;
    @InjectMocks private SectionController sectionController;

    private SectionResponse responseDto;

    @BeforeEach
    void setUp() {
        responseDto = SectionResponse.builder().sectionId(1L).resumeId(10L)
                .sectionType(SectionType.EXPERIENCE).title("Experience")
                .displayOrder(1).isVisible(true).aiGenerated(false).build();
    }

    @Test
    void addSection() {
        SectionRequest req = new SectionRequest();
        when(sectionService.addSection(req, 100L)).thenReturn(responseDto);

        ResponseEntity<SectionResponse> resp = sectionController.addSection(100L, req);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    void getSectionById() {
        when(sectionService.getSectionById(1L, 100L)).thenReturn(responseDto);

        ResponseEntity<SectionResponse> resp = sectionController.getSectionById(1L, 100L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void getSectionsByResume() {
        when(sectionService.getSectionsByResume(10L, 100L)).thenReturn(List.of(responseDto));

        ResponseEntity<List<SectionResponse>> resp = sectionController.getSectionsByResume(10L, 100L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void getSectionsByType() {
        when(sectionService.getSectionsByType(10L, SectionType.EXPERIENCE, 100L))
                .thenReturn(List.of(responseDto));

        ResponseEntity<List<SectionResponse>> resp = sectionController.getSectionsByType(10L, SectionType.EXPERIENCE, 100L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void updateSection() {
        SectionRequest req = new SectionRequest();
        when(sectionService.updateSection(1L, req, 100L)).thenReturn(responseDto);

        ResponseEntity<SectionResponse> resp = sectionController.updateSection(1L, 100L, req);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void reorderSections() {
        List<SectionReorderItemRequest> reqs = List.of();
        when(sectionService.reorderSections(10L, reqs, 100L)).thenReturn(List.of(responseDto));

        ResponseEntity<List<SectionResponse>> resp = sectionController.reorderSections(10L, 100L, reqs);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void toggleVisibility() {
        SectionVisibilityRequest visReq = new SectionVisibilityRequest();
        visReq.setIsVisible(false);
        when(sectionService.toggleVisibility(1L, false, 100L)).thenReturn(responseDto);

        ResponseEntity<SectionResponse> resp = sectionController.toggleVisibility(1L, 100L, visReq);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void bulkUpdateSections() {
        BulkSectionUpdateRequest bulkReq = new BulkSectionUpdateRequest();
        bulkReq.setSections(List.of());
        when(sectionService.bulkUpdateSections(10L, List.of(), 100L)).thenReturn(List.of(responseDto));

        ResponseEntity<List<SectionResponse>> resp = sectionController.bulkUpdateSections(10L, 100L, bulkReq);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void deleteSection() {
        ResponseEntity<Void> resp = sectionController.deleteSection(1L, 100L);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(sectionService).deleteSection(1L, 100L);
    }

    @Test
    void deleteAllSections() {
        ResponseEntity<Void> resp = sectionController.deleteAllSections(10L, 100L);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(sectionService).deleteAllSections(10L, 100L);
    }
}
