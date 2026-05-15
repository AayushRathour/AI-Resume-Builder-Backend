package com.resumeai.notification.dto.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Event DTO used for RabbitMQ-based asynchronous workflows. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private Long userId;
    private Long resumeId;
    private String title;
    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String type; // RESUME_CREATED, RESUME_UPDATED, RESUME_EXPORTED, etc.

    private String metadataJson; // Optional JSON metadata

    // Legacy fields for backward compatibility
    private String subject;
    private boolean critical;
}
