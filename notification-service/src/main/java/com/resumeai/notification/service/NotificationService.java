package com.resumeai.notification.service;

import com.resumeai.notification.dto.NotificationResponse;
import com.resumeai.notification.dto.UnreadCountResponse;
import com.resumeai.notification.entity.Notification;
import com.resumeai.notification.entity.NotificationType;
import com.resumeai.notification.exception.ResourceNotFoundException;
import com.resumeai.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.from:noreply@resumeai.com}")
    private String emailFrom;

    // ─────────────────────────────────────────────────────────────────────
    // 1. Create & persist a notification
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public NotificationResponse createNotification(Long userId, NotificationType type, String message) {
        log.info("Creating notification: userId={}, type={}", userId, type);

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.debug("Notification saved: id={}", saved.getNotificationId());
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2. Get all notifications for a user (newest first)
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        log.info("Fetching all notifications for userId={}", userId);
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3. Mark a single notification as read
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        log.info("Marking notification {} as read", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 4. Get unread count for a user
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsRead(userId, false);
        log.info("Unread count for userId={}: {}", userId, count);
        return UnreadCountResponse.builder()
                .userId(userId)
                .unreadCount(count)
                .build();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    @Transactional
    public void deleteNotification(UUID notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }
        notificationRepository.deleteById(notificationId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 5. Send email (optional – controlled by app.email.enabled flag)
    // ─────────────────────────────────────────────────────────────────────

    public void sendEmail(Long userId, String subject, String message) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. userId={} | Subject: {}", userId, subject);
            return;
        }

        // In a real system you'd look up the user's email from auth-service.
        // Here we log and send to a placeholder; replace with actual lookup.
        String recipientEmail = resolveUserEmail(userId);
        if (recipientEmail == null) {
            log.warn("Could not resolve email for userId={}. Skipping email send.", userId);
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(emailFrom);
            mail.setTo(recipientEmail);
            mail.setSubject(subject);
            mail.setText(message);
            mailSender.send(mail);
            log.info("Email sent to {} for userId={}", recipientEmail, userId);
        } catch (Exception e) {
            log.error("Failed to send email for userId={}: {}", userId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .type(n.getType())
                .message(n.getMessage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    /**
     * Placeholder email resolver.
     * Replace with a Feign call to auth-service when available.
     */
    private String resolveUserEmail(Long userId) {
        // TODO: inject ResumeClient / AuthClient and call GET /users/{userId}
        log.debug("resolveUserEmail called for userId={} — returning null (no auth-service client)", userId);
        return null;
    }
}
