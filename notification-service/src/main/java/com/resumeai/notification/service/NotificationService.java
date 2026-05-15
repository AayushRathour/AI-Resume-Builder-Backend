package com.resumeai.notification.service;

import com.resumeai.notification.dto.NotificationResponse;
import com.resumeai.notification.dto.UnreadCountResponse;
import com.resumeai.notification.entity.Notification;
import com.resumeai.notification.entity.NotificationType;
import com.resumeai.notification.exception.ResourceNotFoundException;
import com.resumeai.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages notification persistence, read-state updates, and realtime WebSocket delivery.
 * Used by RabbitMQ listeners and notification APIs.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    // Creates and persists notifications, then triggers realtime push.

    @Transactional
    public NotificationResponse createNotification(Long userId, NotificationType type, String message) {
        return createNotification(userId, type, type.name().replace('_', ' '), message, null, null);
    }

    @Transactional
    public NotificationResponse createNotification(Long userId, NotificationType type, String title,
                                                    String message, String eventId, String metadataJson) {
        // Dedup check: skip if same eventId already processed
        if (eventId != null && notificationRepository.existsByEventId(eventId)) {
            log.warn("Duplicate event detected: eventId={}, skipping", eventId);
            return null;
        }

        log.info("Creating notification: userId={}, type={}, title={}", userId, type, title);

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .eventId(eventId)
                .metadataJson(metadataJson)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved: id={}, eventId={}", saved.getNotificationId(), eventId);

        NotificationResponse response = toResponse(saved);

        // Push via WebSocket asynchronously
        pushWebSocketNotification(userId, response);

        return response;
    }
    // Pushes a single notification update to user-specific WebSocket topic.

    @Async
    public void pushWebSocketNotification(Long userId, NotificationResponse response) {
        try {
            String destination = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, response);
            log.info("[WebSocket] Pushed notification to {}", destination);
        } catch (Exception e) {
            log.error("[WebSocket] Failed to push notification for userId={}: {}",
                    userId, e.getMessage());
        }
    }
    // Fetches notifications for a user in reverse chronological order.

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        log.info("Fetching all notifications for userId={}", userId);
        try {
            return notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        } catch (Exception ex) {
            log.error("Failed to fetch notifications for userId={}. Returning empty list. Cause: {}",
                    userId, ex.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsPaginated(Long userId, int page, int size) {
        log.info("Fetching paginated notifications for userId={}, page={}, size={}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }
    // Marks a single notification as read.

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        log.info("Marking notification {} as read", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return toResponse(updated);
    }
    // Returns unread notification count for dashboard badges.

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        try {
            long count = notificationRepository.countByUserIdAndIsRead(userId, false);
            log.info("Unread count for userId={}: {}", userId, count);
            return UnreadCountResponse.builder()
                    .userId(userId)
                    .unreadCount(count)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to fetch unread count for userId={}. Returning 0. Cause: {}",
                    userId, ex.getMessage());
            return UnreadCountResponse.builder()
                    .userId(userId)
                    .unreadCount(0L)
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
        log.info("Marked {} notifications as read for userId={}", unread.size(), userId);
        return unread.size();
    }

    @Transactional
    public void deleteNotification(UUID notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }
        notificationRepository.deleteById(notificationId);
        log.info("Deleted notification: id={}", notificationId);
    }
    // Private mapping helper.

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .type(n.getType())
                .message(n.getMessage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .eventId(n.getEventId())
                .metadataJson(n.getMetadataJson())
                .build();
    }
}
