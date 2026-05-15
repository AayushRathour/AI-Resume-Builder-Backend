package com.resumeai.notification.dto;

import com.resumeai.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response payload for notification operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID notificationId;
    private Long userId;
    private String title;
    private NotificationType type;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String eventId;
    private String metadataJson;
}



