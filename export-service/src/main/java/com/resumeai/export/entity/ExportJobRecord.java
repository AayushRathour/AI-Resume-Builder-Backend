package com.resumeai.export.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent entity used by this service domain. */

@Entity
@Table(name = "export_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobRecord {

    @Id
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(nullable = false, length = 16)
    private String format;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "file_path", length = 1024)
    private String filePath;

    @Column(name = "file_size_kb")
    private Long fileSizeKb;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (jobId == null) {
            jobId = UUID.randomUUID();
        }
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }
}

