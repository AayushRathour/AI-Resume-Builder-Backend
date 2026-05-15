package com.resumeai.resume.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SectionServiceClientTest {

    @Mock
    private SectionServiceFeignClient feignClient;

    @InjectMocks
    private SectionServiceClient sectionServiceClient;

    private SectionPayload payload;

    @BeforeEach
    void setUp() {
        payload = new SectionPayload();
        payload.setSectionType("EXPERIENCE");
        payload.setTitle("Exp");
        payload.setContent("Text");
        payload.setDisplayOrder(1);
        payload.setIsVisible(true);
        payload.setAiGenerated(false);
    }

    @Test
    void copySections_success() {
        when(feignClient.getSectionsByResumeId(1L, 10L)).thenReturn(List.of(payload));

        sectionServiceClient.copySections(1L, 2L, 10L);

        verify(feignClient).createSection(any(SectionCreateRequest.class), eq(10L));
    }

    @Test
    void deleteAllSections_success() {
        sectionServiceClient.deleteAllSections(1L, 10L);
        verify(feignClient).deleteAllSections(1L, 10L);
    }
}
