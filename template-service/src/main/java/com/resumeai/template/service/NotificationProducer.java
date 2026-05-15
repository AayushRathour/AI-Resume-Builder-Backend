package com.resumeai.template.service;

import com.resumeai.template.dto.NotificationEvent;
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

    private static final String TEMPLATE_ID_JSON = "{\"templateId\":";

    /**
     * Publishes a template.created event
     */
    public void publishTemplateCreatedEvent(Long templateId, String templateName) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(0L) // System/admin event  no specific user
                .type("TEMPLATE_CREATED")
                .title("New Template Available")
                .message("A new template '" + templateName + "' is now available.")
                .subject("New Template Available")
                .metadataJson(TEMPLATE_ID_JSON + templateId + "}")
                .critical(false)
                .build();

        publishEvent(event, "template.created");
    }

    /**
     * Publishes a template.updated event
     */
    public void publishTemplateUpdatedEvent(Long templateId, String templateName) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(0L) // System/admin event
                .type("TEMPLATE_UPDATED")
                .title("Template Updated")
                .message("The template '" + templateName + "' has been updated.")
                .subject("Template Updated")
                .metadataJson(TEMPLATE_ID_JSON + templateId + "}")
                .critical(false)
                .build();

        publishEvent(event, "template.updated");
    }

    /**
     * Publishes a template.deleted event
     */
    public void publishTemplateDeletedEvent(Long templateId) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(0L)
                .type("TEMPLATE_DELETED")
                .title("Template Deleted")
                .message("A template has been removed.")
                .subject("Template Deleted")
                .metadataJson(TEMPLATE_ID_JSON + templateId + "}")
                .critical(false)
                .build();

        publishEvent(event, "template.deleted");
    }

    /**
     * Generic event publisher
     */
    private void publishEvent(NotificationEvent event, String routingKey) {
        try {
            log.info("[RabbitMQ] Publishing event: exchange='{}', routingKey='{}', type={}",
                    exchange, routingKey, event.getType());
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("[RabbitMQ] Event published successfully: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish template event: {}", e.getMessage(), e);
        }
    }
}
