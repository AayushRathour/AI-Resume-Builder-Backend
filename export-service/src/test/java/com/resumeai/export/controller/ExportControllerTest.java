package com.resumeai.export.controller;

import com.resumeai.export.dto.ExportResponse;
import com.resumeai.export.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController exportController;

    private UUID jobId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
    }

    // ── exportResume Tests ──────────────────────────────────────────────

    @Test
    void exportResume_success_returnsOk() {
        ExportResponse response = ExportResponse.builder()
                .jobId(jobId)
                .status("COMPLETED")
                .message("Resume exported successfully")
                .build();
        when(exportService.exportResume(eq(1L), eq(100L), eq("PDF"), isNull()))
                .thenReturn(response);

        ResponseEntity<ExportResponse> result = exportController.exportResume(100L, "1", "PDF", null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("COMPLETED", result.getBody().getStatus());
    }

    @Test
    void exportResume_failed_returnsUnprocessableEntity() {
        ExportResponse response = ExportResponse.builder()
                .jobId(jobId)
                .status("FAILED")
                .message("Resume not found")
                .build();
        when(exportService.exportResume(eq(1L), eq(100L), eq("PDF"), isNull()))
                .thenReturn(response);

        ResponseEntity<ExportResponse> result = exportController.exportResume(100L, "1", "PDF", null);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        assertEquals("FAILED", result.getBody().getStatus());
    }

    @Test
    void exportResume_withTemplateId_passesTemplateId() {
        ExportResponse response = ExportResponse.builder()
                .jobId(jobId)
                .status("COMPLETED")
                .build();
        when(exportService.exportResume(eq(1L), eq(100L), eq("DOCX"), eq(5L)))
                .thenReturn(response);

        ResponseEntity<ExportResponse> result = exportController.exportResume(100L, "1", "DOCX", 5L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(exportService).exportResume(1L, 100L, "DOCX", 5L);
    }

    @Test
    void exportResume_nullUserIdHeader_parsesToNull() {
        ExportResponse response = ExportResponse.builder()
                .status("COMPLETED")
                .build();
        when(exportService.exportResume(isNull(), eq(100L), eq("PDF"), isNull()))
                .thenReturn(response);

        ResponseEntity<ExportResponse> result = exportController.exportResume(100L, null, "PDF", null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(exportService).exportResume(null, 100L, "PDF", null);
    }

    @Test
    void exportResume_blankUserIdHeader_parsesToNull() {
        ExportResponse response = ExportResponse.builder()
                .status("COMPLETED")
                .build();
        when(exportService.exportResume(isNull(), eq(100L), eq("PDF"), isNull()))
                .thenReturn(response);

        ResponseEntity<ExportResponse> result = exportController.exportResume(100L, "   ", "PDF", null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void exportResume_invalidUserIdHeader_parsesToNull() {
        ExportResponse response = ExportResponse.builder()
                .status("COMPLETED")
                .build();
        when(exportService.exportResume(isNull(), eq(100L), eq("PDF"), isNull()))
                .thenReturn(response);

        ResponseEntity<ExportResponse> result = exportController.exportResume(100L, "not-a-number", "PDF", null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ── getStatus Tests ─────────────────────────────────────────────────

    @Test
    void getStatus_returnsOk() {
        ExportResponse response = ExportResponse.builder()
                .jobId(jobId)
                .status("COMPLETED")
                .build();
        when(exportService.getStatus(jobId)).thenReturn(response);

        ResponseEntity<ExportResponse> result = exportController.getStatus(jobId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(jobId, result.getBody().getJobId());
    }

    // ── getByUser Tests ─────────────────────────────────────────────────

    @Test
    void getByUser_returnsListOfExports() {
        List<ExportResponse> exports = List.of(
                ExportResponse.builder().jobId(UUID.randomUUID()).build(),
                ExportResponse.builder().jobId(UUID.randomUUID()).build()
        );
        when(exportService.getByUser(1L)).thenReturn(exports);

        ResponseEntity<List<ExportResponse>> result = exportController.getByUser(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
    }

    // ── getStats Tests ──────────────────────────────────────────────────

    @Test
    void getStats_returnsStatsMap() {
        Map<String, Object> stats = Map.of(
                "userId", 1L,
                "totalExports", 10L,
                "completedExports", 8L,
                "failedExports", 2L
        );
        when(exportService.getStats(1L)).thenReturn(stats);

        ResponseEntity<Map<String, Object>> result = exportController.getStats(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10L, result.getBody().get("totalExports"));
    }

    // ── delete Tests ────────────────────────────────────────────────────

    @Test
    void delete_returnsNoContent() {
        doNothing().when(exportService).delete(jobId);

        ResponseEntity<Void> result = exportController.delete(jobId);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(exportService).delete(jobId);
    }

    // ── health Tests ────────────────────────────────────────────────────

    @Test
    void health_returnsOk() {
        ResponseEntity<String> result = exportController.health();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().contains("UP"));
    }

    // ── downloadFile Tests ──────────────────────────────────────────────

    @Test
    void downloadFile_noFilePath_throwsNotFound() {
        ExportResponse response = ExportResponse.builder()
                .jobId(jobId)
                .filePath(null)
                .build();
        when(exportService.getStatus(jobId)).thenReturn(response);

        assertThrows(Exception.class, () -> exportController.downloadFile(jobId));
    }

    @Test
    void downloadFile_blankFilePath_throwsNotFound() {
        ExportResponse response = ExportResponse.builder()
                .jobId(jobId)
                .filePath("   ")
                .build();
        when(exportService.getStatus(jobId)).thenReturn(response);

        assertThrows(Exception.class, () -> exportController.downloadFile(jobId));
    }

    @Test
    void downloadFile_fileDoesNotExist_throwsNotFound() {
        ExportResponse response = ExportResponse.builder()
                .jobId(jobId)
                .filePath("/nonexistent/path/file.pdf")
                .build();
        when(exportService.getStatus(jobId)).thenReturn(response);

        assertThrows(Exception.class, () -> exportController.downloadFile(jobId));
    }
}
