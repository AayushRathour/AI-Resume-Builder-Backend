package com.resumeai.notification.dto;

import lombok.*;

/** Event DTO used for RabbitMQ-based asynchronous workflows. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Deprecated(since = "1.0", forRemoval = true)
public class NotificationEvent {

    private Long userId;
    private String subject;
    private String message;
    private boolean critical; // For broadcast notifications
}
