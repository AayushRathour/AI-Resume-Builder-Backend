package com.resumeai.jobmatch.dto;

import lombok.*;

/**
 * Event published to RabbitMQ after a job match is generated.
 * Consumed by notification-service via job.match routing key.
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
