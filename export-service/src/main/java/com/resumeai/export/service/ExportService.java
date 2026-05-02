package com.resumeai.export.service;

import com.resumeai.export.dto.ExportResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExportService {

    /**
     * Fetches resume + sections + template, merges into HTML,
     * saves a PDF file locally, then publishes export.completed RabbitMQ event.
     *
     * @param userId   the requesting user's ID (from X-User-Id gateway header)
     * @param resumeId the resume to export
     * @return ExportResponse with file path and status
     */
    ExportResponse exportResume(Long userId, Long resumeId, String format);

    ExportResponse getStatus(UUID jobId);

    List<ExportResponse> getByUser(Long userId);

    Map<String, Object> getStats(Long userId);

    void delete(UUID jobId);
}
