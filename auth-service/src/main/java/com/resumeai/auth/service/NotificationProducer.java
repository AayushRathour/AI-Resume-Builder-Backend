package com.resumeai.auth.service;

import com.resumeai.auth.dto.NotificationEvent;
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

    @Value("${rabbitmq.routing-key.auth:auth.signup}")
    private String authRoutingKey;

    /**
     * Publishes an auth.signup event (new user registration)
     */
    public void publishSignupEvent(Long userId, String username) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .subject("Welcome to ResumeAI")
                .message("Welcome " + username + "! Your account has been created successfully. Let's get started!")
                .critical(false)
                .build();

        publishEvent(event, authRoutingKey);
    }

    /**
     * Publishes a subscription change event
     */
    public void publishSubscriptionUpdateEvent(Long userId, String newPlan) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .subject("Subscription Updated")
                .message("Your subscription has been updated to " + newPlan + " plan.")
                .critical(false)
                .build();

        publishEvent(event, authRoutingKey);
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
