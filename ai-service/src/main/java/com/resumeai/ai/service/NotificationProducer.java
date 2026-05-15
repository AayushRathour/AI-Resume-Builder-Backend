package com.resumeai.ai.service;

import com.resumeai.ai.dto.NotificationEvent;
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

    @Value("${rabbitmq.exchange:notification.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.ai:ai.completed}")
    private String aiRoutingKey;

    /**
     * Publishes an ai.completed event
     */
    public void publishAiCompletedEvent(Long userId, String aiTask) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .subject("AI Processing Completed")
                .message("Your " + aiTask + " AI task has been completed successfully.")
                .critical(false)
                .build();

        publishEvent(event, aiRoutingKey);
    }

    /**
     * Publishes an ATS score completion event
     */
    public void publishAtsScoreEvent(Long userId, Double score) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .subject("ATS Score Ready")
                .message("Your resume ATS score is " + String.format("%.1f", score * 100) + "%.")
                .critical(false)
                .build();

        publishEvent(event, aiRoutingKey);
    }

    /**
     * Generic event publisher
     */
    private void publishEvent(NotificationEvent event, String routingKey) {
        try {
            log.info("[RabbitMQ] Publishing event to exchange='{}', routingKey='{}', userId={}", 
                    exchange, routingKey, event.getUserId());
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("[RabbitMQ] Event published successfully for userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish event for userId={}: {}", event.getUserId(), e.getMessage(), e);
        }
    }
}
