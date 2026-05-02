package com.resumeai.resume.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.resumeai.resume.dto.ResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.service.ResumeService;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    @Mock
    private ResumeService resumeService;

    @InjectMocks
    private ResumeController resumeController;

    @Test
    void createResume_usesAuthenticatedUserHeader() {
        ResumeRequest request = ResumeRequest.builder()
                .title("Resume")
                .targetJobTitle("Engineer")
                .templateId(1L)
                .language("English")
                .sectionsJson("[]")
                .build();

        when(resumeService.createResume(eq(9L), any(ResumeRequest.class), eq("Bearer token")))
                .thenReturn(ResumeResponse.builder().resumeId(100L).userId(9L).build());

        var response = resumeController.createResume(9L, "Bearer token", null, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(resumeService).createResume(eq(9L), any(ResumeRequest.class), eq("Bearer token"));
    }

    @Test
    void incrementViewCount_callsService() {
        var response = resumeController.incrementViewCount(15L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(resumeService).incrementViewCount(15L);
    }
}
