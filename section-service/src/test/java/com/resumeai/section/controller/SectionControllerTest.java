package com.resumeai.section.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.resumeai.section.dto.SectionRequest;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.entity.SectionType;
import com.resumeai.section.service.SectionService;

@ExtendWith(MockitoExtension.class)
class SectionControllerTest {

    @Mock
    private SectionService sectionService;

    @InjectMocks
    private SectionController sectionController;

    @Test
    void addSection_usesAuthHeaderAndBodyContract() {
        SectionRequest request = SectionRequest.builder()
                .resumeId(11L)
                .sectionType(SectionType.SUMMARY)
                .title("Summary")
                .content("{}")
                .displayOrder(1)
                .isVisible(true)
                .aiGenerated(false)
                .build();

        when(sectionService.addSection(any(SectionRequest.class), eq(4L)))
                .thenReturn(SectionResponse.builder().sectionId(101L).resumeId(11L).build());

        var response = sectionController.addSection(4L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(sectionService).addSection(any(SectionRequest.class), eq(4L));
    }

    @Test
    void getSectionById_usesRequiredEndpointHandler() {
        when(sectionService.getSectionById(33L, 8L))
                .thenReturn(SectionResponse.builder().sectionId(33L).resumeId(12L).build());

        var response = sectionController.getSectionById(33L, 8L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sectionService).getSectionById(33L, 8L);
    }

    @Test
    void getSectionsByType_returnsList() {
        when(sectionService.getSectionsByType(50L, SectionType.EXPERIENCE, 9L))
                .thenReturn(List.of(SectionResponse.builder().sectionId(1L).resumeId(50L).build()));

        var response = sectionController.getSectionsByType(50L, SectionType.EXPERIENCE, 9L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sectionService).getSectionsByType(50L, SectionType.EXPERIENCE, 9L);
    }
}
