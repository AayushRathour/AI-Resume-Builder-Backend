package com.resumeai.export.controller;

import com.resumeai.export.dto.ExportResponse;
import com.resumeai.export.service.ExportService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Exposes REST endpoints for export workflows. */

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;

    @PostMapping("/{resumeId}")
    public ResponseEntity<ExportResponse> exportResume(
            @PathVariable Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(defaultValue = "PDF") String format,
            @RequestParam(value = "templateId", required = false) Long templateId) {

        Long userId = parseUserId(userIdHeader);
        ExportResponse response = exportService.exportResume(userId, resumeId, format, templateId);

        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.unprocessableEntity().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<ExportResponse> getStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(exportService.getStatus(jobId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ExportResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(exportService.getByUser(userId));
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long userId) {
        return ResponseEntity.ok(exportService.getStats(userId));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> delete(@PathVariable UUID jobId) {
        exportService.delete(jobId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/file/{jobId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID jobId) {
        ExportResponse status = exportService.getStatus(jobId);
        if (status.getFilePath() == null || status.getFilePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Export file not found");
        }

        Path path = Path.of(status.getFilePath());
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Export file not found");
        }

        Resource fileResource = new FileSystemResource(path);
        String filename = path.getFileName().toString();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fileResource);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("export-service is UP");
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) return null;
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            log.warn("Could not parse X-User-Id header value: {}", userIdHeader);
            return null;
        }
    }
}



