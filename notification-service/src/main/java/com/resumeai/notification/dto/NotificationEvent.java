package com.resumeai.notification.dto;

import lombok.*;

/**
 * Generic event message consumed from RabbitMQ queues.
 * Published by other microservices (export-service, ai-service, jobmatch-service).
 *
 * JSON shape expected:
 * {
 *   "userId": 1,
 *   "subject": "Your export is ready",
 *   "message": "Resume PDF has been generated successfully."
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    private Long userId;
    private String subject;
    private String message;
}
