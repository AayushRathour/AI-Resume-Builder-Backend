package com.resumeai.notification.dto;

import com.resumeai.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID notificationId;
    private Long userId;
    private NotificationType type;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
}
