package com.resumeai.export.service;

import com.resumeai.export.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Publishes domain events to RabbitMQ for asynchronous processing. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange:resumeai.exchange}")
    private String exchange;

    /**
     * Publishes a resume.exported event (success)
     */
    public void publishExportCompletedEvent(Long userId, String format) {
        publishExportCompletedEvent(userId, null, format);
    }

    public void publishExportCompletedEvent(Long userId, Long resumeId, String format) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .resumeId(resumeId)
                .type("RESUME_EXPORTED")
                .title("Export Completed")
                .message("Your resume export to " + format + " format is ready for download.")
                .subject("Export Completed")
                .critical(false)
                .build();

        publishEvent(event, "resume.exported");
    }

    /**
     * Publishes an export error event
     */
    public void publishExportErrorEvent(Long userId, String errorMessage) {
        publishExportErrorEvent(userId, null, errorMessage);
    }

    public void publishExportErrorEvent(Long userId, Long resumeId, String errorMessage) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .resumeId(resumeId)
                .type("EXPORT_FAILED")
                .title("Export Failed")
                .message("Export failed: " + errorMessage)
                .subject("Export Failed")
                .critical(true)
                .build();

        publishEvent(event, "resume.exported");
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
        }
    }
}
