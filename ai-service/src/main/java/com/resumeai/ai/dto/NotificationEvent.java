package com.resumeai.ai.dto;

import lombok.*;

/** Event DTO used for RabbitMQ-based asynchronous workflows. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    private Long userId;
    private String subject;
    private String message;
    private boolean critical;
}
