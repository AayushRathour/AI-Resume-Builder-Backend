package com.resumeai.notification.listener;

import com.resumeai.notification.dto.NotificationEvent;
import com.resumeai.notification.entity.NotificationType;
import com.resumeai.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ event listener for the notification-service.
 *
 * Listens to three queues:
 *  - export.completed  →  EXPORT_READY notification
 *  - ai.completed      →  AI_COMPLETED notification
 *  - job.match         →  JOB_MATCH notification
 *
 * Each handler:
 *  1. Persists a Notification record in MySQL
 *  2. Optionally sends an email via NotificationService#sendEmail
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    // ─────────────────────────────────────────────────────────────
    // export.completed → EXPORT_READY
    // ─────────────────────────────────────────────────────────────

    @RabbitListener(queues = "${rabbitmq.queue.export}")
    public void handleExportCompleted(NotificationEvent event) {
        log.info("[RabbitMQ] Received export.completed for userId={}", event.getUserId());

        if (!isValid(event)) return;

        String message = event.getMessage() != null
                ? event.getMessage()
                : "Your resume export is ready. You can now download it.";

        notificationService.createNotification(
                event.getUserId(),
                NotificationType.EXPORT_READY,
                message
        );

        notificationService.sendEmail(
                event.getUserId(),
                event.getSubject() != null ? event.getSubject() : "Export Ready — ResumeAI",
                message
        );
    }

    // ─────────────────────────────────────────────────────────────
    // ai.completed → AI_COMPLETED
    // ─────────────────────────────────────────────────────────────

    @RabbitListener(queues = "${rabbitmq.queue.ai}")
    public void handleAiCompleted(NotificationEvent event) {
        log.info("[RabbitMQ] Received ai.completed for userId={}", event.getUserId());

        if (!isValid(event)) return;

        String message = event.getMessage() != null
                ? event.getMessage()
                : "AI processing is complete. Your resume has been enhanced.";

        notificationService.createNotification(
                event.getUserId(),
                NotificationType.AI_DONE,
                message
        );

        notificationService.sendEmail(
                event.getUserId(),
                event.getSubject() != null ? event.getSubject() : "AI Enhancement Done — ResumeAI",
                message
        );
    }

    // ─────────────────────────────────────────────────────────────
    // job.match → JOB_MATCH
    // ─────────────────────────────────────────────────────────────

    @RabbitListener(queues = "${rabbitmq.queue.job}")
    public void handleJobMatch(NotificationEvent event) {
        log.info("[RabbitMQ] Received job.match for userId={}", event.getUserId());

        if (!isValid(event)) return;

        String message = event.getMessage() != null
                ? event.getMessage()
                : "New job matches are available for your resume. Check them out!";

        notificationService.createNotification(
                event.getUserId(),
                NotificationType.JOB_MATCH,
                message
        );

        notificationService.sendEmail(
                event.getUserId(),
                event.getSubject() != null ? event.getSubject() : "New Job Matches Found — ResumeAI",
                message
        );
    }

    // ─────────────────────────────────────────────────────────────
    // Guard
    // ─────────────────────────────────────────────────────────────

    private boolean isValid(NotificationEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("Received invalid or empty NotificationEvent — skipping.");
            return false;
        }
        return true;
    }
}
