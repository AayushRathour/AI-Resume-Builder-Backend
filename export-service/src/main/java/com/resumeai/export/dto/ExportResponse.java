package com.resumeai.export.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/** Response payload for export operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportResponse {
    private UUID jobId;
    private Long userId;
    private Long resumeId;
    private String format;
    private String fileUrl;
    private String filePath;
    private Long fileSizeKb;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private String message;
    private String status;
}



