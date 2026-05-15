package com.resumeai.resume.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import com.resumeai.resume.dto.AtsScoreAiRequest;
import com.resumeai.resume.dto.ResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.entity.ResumeStatus;
import com.resumeai.resume.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    @Mock private ResumeService resumeService;

    @InjectMocks private ResumeController resumeController;

    private ResumeResponse responseDto;
    private ResumeRequest requestDto;

    @BeforeEach
    void setUp() {
        responseDto = ResumeResponse.builder().resumeId(1L).userId(10L)
                .title("My Resume").status(ResumeStatus.DRAFT).build();
        requestDto = ResumeRequest.builder().title("My Resume").targetJobTitle("Dev").build();
    }

    @Test
    void createResume() {
        when(resumeService.createResume(eq(10L), any(), any(), any())).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.createResume(10L, "Bearer x", null, null, requestDto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(1L, resp.getBody().getResumeId());
    }

    @Test
    void createResume_fallbackUserId() {
        when(resumeService.createResume(eq(5L), any(), any(), any())).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.createResume(null, null, null, 5L, requestDto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    void createResume_aliasPathBehavior() {
        when(resumeService.createResume(eq(10L), any(), any(), any())).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.createResume(10L, "Bearer x", null, null, requestDto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    void getResumeById() {
        when(resumeService.getResumeById(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.getResumeById(1L, 10L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1L, resp.getBody().getResumeId());
    }

    @Test
    void getResumesByUser() {
        when(resumeService.getResumesByUser(10L, 10L)).thenReturn(List.of(responseDto));

        ResponseEntity<List<ResumeResponse>> resp = resumeController.getResumesByUser(10L, 10L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void updateResume() {
        when(resumeService.updateResume(eq(1L), eq(10L), any())).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.updateResume(1L, 10L, requestDto);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void deleteResume() {
        ResponseEntity<Void> resp = resumeController.deleteResume(1L, 10L);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(resumeService).deleteResume(1L, 10L);
    }

    @Test
    void duplicateResume() {
        when(resumeService.duplicateResume(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.duplicateResume(1L, 10L);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    void publishResume() {
        when(resumeService.publishResume(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.publishResume(1L, 10L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void unpublishResume() {
        when(resumeService.unpublishResume(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.unpublishResume(1L, 10L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void updateAtsScore() {
        when(resumeService.updateAtsScore(1L, 85.0, 10L, false)).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.updateAtsScore(1L, 10L, false, 85.0);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void updateAtsScoreWithAi() {
        AtsScoreAiRequest aiReq = new AtsScoreAiRequest();
        aiReq.setResumeText("text");
        aiReq.setJobDescription("desc");
        when(resumeService.generateAtsScoreWithAi(1L, 10L, "text", "desc")).thenReturn(responseDto);

        ResponseEntity<ResumeResponse> resp = resumeController.updateAtsScoreWithAi(1L, 10L, aiReq);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void incrementViewCount() {
        ResponseEntity<Void> resp = resumeController.incrementViewCount(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(resumeService).incrementViewCount(1L);
    }

    @Test
    void getPublicResumes() {
        when(resumeService.getPublicResumes()).thenReturn(List.of(responseDto));

        ResponseEntity<List<ResumeResponse>> resp = resumeController.getPublicResumes();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void getResumesByTemplate() {
        when(resumeService.getResumesByTemplate(1L)).thenReturn(List.of(responseDto));

        ResponseEntity<List<ResumeResponse>> resp = resumeController.getResumesByTemplate(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}
