package com.resumeai.export.dto;

import lombok.*;

/**
 * Generic event published to RabbitMQ after an export completes.
 * Consumed by notification-service via export.completed routing key.
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
