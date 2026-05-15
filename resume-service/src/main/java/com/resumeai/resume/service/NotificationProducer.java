package com.resumeai.resume.service;

import com.resumeai.resume.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Publishes resume events to RabbitMQ for notification-service to consume.
 * Uses the centralized resumeai.exchange with standardized routing keys.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange:resumeai.exchange}")
    private String exchange;

    private static final String RESUME_PREFIX = "Your resume '";

    /**
     * Publishes a resume.created event
     */
    public void publishResumeCreatedEvent(Long userId, Long resumeId, String resumeTitle) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .resumeId(resumeId)
                .type("RESUME_CREATED")
                .title("Resume Created")
                .message(RESUME_PREFIX + resumeTitle + "' has been created successfully.")
                .subject("Resume Created")
                .critical(false)
                .build();

        publishEvent(event, "resume.created");
    }

    /**
     * Publishes a resume.updated event
     */
    public void publishResumeUpdatedEvent(Long userId, Long resumeId, String resumeTitle) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .resumeId(resumeId)
                .type("RESUME_UPDATED")
                .title("Resume Updated")
                .message(RESUME_PREFIX + resumeTitle + "' has been updated successfully.")
                .subject("Resume Updated")
                .critical(false)
                .build();

        publishEvent(event, "resume.updated");
    }

    /**
     * Publishes a resume.deleted event
     */
    public void publishResumeDeletedEvent(Long userId, Long resumeId, String resumeTitle) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .resumeId(resumeId)
                .type("RESUME_DELETED")
                .title("Resume Deleted")
                .message(RESUME_PREFIX + resumeTitle + "' has been deleted.")
                .subject("Resume Deleted")
                .critical(false)
                .build();

        publishEvent(event, "resume.deleted");
    }

    /**
     * @deprecated Use publishResumeCreatedEvent or publishResumeUpdatedEvent instead.
     * Kept for backward compatibility.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public void publishResumeSavedEvent(Long userId, String resumeTitle) {
        publishResumeCreatedEvent(userId, null, resumeTitle);
    }

    /**
     * @deprecated Use specific event methods instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public void publishResumePublishedEvent(Long userId, String resumeTitle) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .type("RESUME_UPDATED")
                .title("Resume Published")
                .message(RESUME_PREFIX + resumeTitle + "' is now publicly available.")
                .subject("Resume Published")
                .critical(false)
                .build();

        publishEvent(event, "resume.updated");
    }

    /**
     * Generic event publisher
     */
    private void publishEvent(NotificationEvent event, String routingKey) {
        try {
            log.info("[RabbitMQ] Publishing event: exchange='{}', routingKey='{}', type={}, userId={}",
                    exchange, routingKey, event.getType(), event.getUserId());
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("[RabbitMQ] Event published successfully: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish event for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
            // DO NOT rethrow — never crash the service due to notification failure
        }
    }
}
