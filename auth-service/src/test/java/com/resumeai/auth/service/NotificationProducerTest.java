package com.resumeai.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.resumeai.auth.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationProducer notificationProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationProducer, "exchange", "test.exchange");
        ReflectionTestUtils.setField(notificationProducer, "authRoutingKey", "test.routing");
    }

    @Test
    void publishSignupEvent_success() {
        notificationProducer.publishSignupEvent(1L, "TestUser");

        verify(rabbitTemplate).convertAndSend(eq("test.exchange"), eq("test.routing"), any(NotificationEvent.class));
    }

    @Test
    void publishSubscriptionUpdateEvent_success() {
        notificationProducer.publishSubscriptionUpdateEvent(1L, "PREMIUM");

        verify(rabbitTemplate).convertAndSend(eq("test.exchange"), eq("test.routing"), any(NotificationEvent.class));
    }

    @Test
    void publishEvent_exception() {
        doThrow(new RuntimeException("RabbitMQ error")).when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        notificationProducer.publishSignupEvent(1L, "TestUser");
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }
}
