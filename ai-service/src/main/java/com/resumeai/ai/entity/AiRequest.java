package com.resumeai.ai.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRequest {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String requestId;

    @Column(nullable = false)
    private Long userId;

    private Long resumeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequestType requestType;

    @Column(columnDefinition = "TEXT")
    private String inputPrompt;

    @Column(columnDefinition = "LONGTEXT")
    private String aiResponse;

    @Column(length = 64)
    private String model;

    private Integer tokensUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequestStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (this.requestId == null || this.requestId.isBlank()) {
            this.requestId = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }
}
