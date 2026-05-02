package com.resumeai.ai.dto;

import lombok.*;

/**
 * Event published to RabbitMQ after an AI generation completes.
 * Consumed by notification-service via ai.completed routing key.
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
