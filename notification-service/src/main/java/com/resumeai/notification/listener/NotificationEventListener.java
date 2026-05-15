package com.resumeai.notification.listener;

import com.resumeai.notification.dto.event.NotificationEvent;
import com.resumeai.notification.entity.NotificationType;
import com.resumeai.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Consumes RabbitMQ events and creates user-facing notifications.
 * Acts as the event-to-notification bridge for resume, export, template, and AI/job flows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    // Handles resume lifecycle events from resume-service.
    @RabbitListener(queues = "resume.queue")
    public void handleResumeEvent(NotificationEvent event) {
        log.info("[RabbitMQ] Received resume event: type={}, userId={}, eventId={}",
                event.getType(), event.getUserId(), event.getEventId());

        if (!isValid(event)) return;

        NotificationType type = resolveType(event.getType(), NotificationType.RESUME_CREATED);
        String title = resolveTitle(event, type.name().replace('_', ' '));
        String message = resolveMessage(event, getDefaultResumeMessage(type));

        notificationService.createNotification(
                event.getUserId(), type, title, message,
                event.getEventId(), event.getMetadataJson());

        log.info("[RabbitMQ] Resume event processed: type={}, userId={}", type, event.getUserId());
    }

    // Handles export completion and export failure events.
    @RabbitListener(queues = "export.queue")
    public void handleExportEvent(NotificationEvent event) {
        log.info("[RabbitMQ] Received export event: userId={}, eventId={}",
                event.getUserId(), event.getEventId());

        if (!isValid(event)) return;

        NotificationType type = resolveType(event.getType(), NotificationType.RESUME_EXPORTED);
        String title = resolveTitle(event,
                type == NotificationType.EXPORT_FAILED ? "Export Failed" : "Export Completed");
        String message = resolveMessage(event,
                type == NotificationType.EXPORT_FAILED
                        ? "Your resume export failed. Please try again."
                        : "Your resume export is ready for download.");

        notificationService.createNotification(
                event.getUserId(), type, title, message,
                event.getEventId(), event.getMetadataJson());

        log.info("[RabbitMQ] Export event processed: type={}, userId={}", type, event.getUserId());
    }

    // Handles template lifecycle events from template-service.
    @RabbitListener(queues = "template.queue")
    public void handleTemplateEvent(NotificationEvent event) {
        log.info("[RabbitMQ] Received template event: userId={}, eventId={}",
                event.getUserId(), event.getEventId());

        if (!isValid(event)) return;

        NotificationType type = resolveType(event.getType(), NotificationType.TEMPLATE_UPDATED);
        String title = resolveTitle(event, type.name().replace('_', ' '));
        String message = resolveMessage(event, "A template has been updated.");

        notificationService.createNotification(
                event.getUserId(), type, title, message,
                event.getEventId(), event.getMetadataJson());

        log.info("[RabbitMQ] Template event processed: type={}, userId={}", type, event.getUserId());
    }

    // Handles generic notifications such as ATS and job-related events.
    @RabbitListener(queues = "notification.queue")
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("[RabbitMQ] Received notification event: type={}, userId={}, eventId={}",
                event.getType(), event.getUserId(), event.getEventId());

        if (!isValid(event)) return;

        NotificationType type = resolveType(event.getType(), NotificationType.GENERAL);
        String title = resolveTitle(event, type.name().replace('_', ' '));
        String message = resolveMessage(event, getDefaultNotificationMessage(type));

        notificationService.createNotification(
                event.getUserId(), type, title, message,
                event.getEventId(), event.getMetadataJson());

        log.info("[RabbitMQ] Notification event processed: type={}, userId={}", type, event.getUserId());
    }

    // Shared validation and mapping helpers.

    private boolean isValid(NotificationEvent event) {
        if (event == null) {
            log.warn("[RabbitMQ] Received null event, skipping");
            return false;
        }
        if (event.getUserId() == null || event.getUserId() <= 0) {
            log.warn("[RabbitMQ] Event has invalid userId={}, skipping", event.getUserId());
            return false;
        }
        return true;
    }

    private NotificationType resolveType(String typeStr, NotificationType fallback) {
        if (typeStr == null || typeStr.isBlank()) return fallback;
        try {
            return NotificationType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[RabbitMQ] Unknown notification type: '{}', using fallback: {}", typeStr, fallback);
            return fallback;
        }
    }

    private String resolveTitle(NotificationEvent event, String fallback) {
        if (event.getTitle() != null && !event.getTitle().isBlank()) return event.getTitle();
        if (event.getSubject() != null && !event.getSubject().isBlank()) return event.getSubject();
        return fallback;
    }

    private String resolveMessage(NotificationEvent event, String fallback) {
        return (event.getMessage() != null && !event.getMessage().isBlank())
                ? event.getMessage() : fallback;
    }

    private String getDefaultResumeMessage(NotificationType type) {
        return switch (type) {
            case RESUME_CREATED -> "Your resume has been created successfully.";
            case RESUME_UPDATED -> "Your resume has been updated successfully.";
            case RESUME_DELETED -> "Your resume has been deleted.";
            default -> "A resume event occurred.";
        };
    }

    private String getDefaultNotificationMessage(NotificationType type) {
        return switch (type) {
            case ATS_COMPLETED -> "Your ATS score analysis is complete. Check your results!";
            case JOB_APPLIED -> "Your job application has been submitted successfully.";
            case JOB_MATCH -> "New job matches are available for your resume.";
            case AI_COMPLETED, AI_DONE -> "AI analysis is complete.";
            default -> "You have a new notification.";
        };
    }
}

