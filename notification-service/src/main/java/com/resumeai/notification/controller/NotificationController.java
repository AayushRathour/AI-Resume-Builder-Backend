package com.resumeai.notification.controller;

import com.resumeai.notification.dto.NotificationRequest;
import com.resumeai.notification.dto.NotificationResponse;
import com.resumeai.notification.dto.UnreadCountResponse;
import com.resumeai.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;

/** Exposes REST endpoints for notification workflows. */

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /notifications/{userId}
     * Returns all notifications for a user, newest first.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @PathVariable Long userId) {
        log.info("GET /notifications/{}", userId);
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    /**
     * GET /notifications/{userId}/paged?page=0&size=20
     * Returns paginated notifications for a user.
     */
    @GetMapping("/{userId}/paged")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /notifications/{}/paged?page={}&size={}", userId, page, size);
        return ResponseEntity.ok(notificationService.getNotificationsPaginated(userId, page, size));
    }

    /**
     * PUT /notifications/read/{id}
     * Marks a single notification as read. Returns updated notification.
     */
    @PutMapping("/read/{id}")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id) {
        log.info("PUT /notifications/read/{}", id);
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    /**
     * GET /notifications/unread/{userId}
     * Returns unread notification count for a user.
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@PathVariable Long userId) {
        log.info("GET /notifications/unread/{}", userId);
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    /**
     * POST /notifications
     * Creates a notification via REST (internal/admin use).
     * This complements the async RabbitMQ path.
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationRequest request) {
        log.info("POST /notifications userId={}, type={}", request.getUserId(), request.getType());
        NotificationResponse response = notificationService.createNotification(
                request.getUserId(), request.getType(), request.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        log.info("GET /notifications");
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @PutMapping("/read-all/{userId}")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable Long userId) {
        log.info("PUT /notifications/read-all/{}", userId);
        int updated = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "updatedCount", updated,
                "message", "All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) {
        log.info("DELETE /notifications/{}", id);
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}



