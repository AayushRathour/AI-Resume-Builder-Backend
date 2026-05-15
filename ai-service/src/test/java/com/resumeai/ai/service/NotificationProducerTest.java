package com.resumeai.ai.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.resumeai.ai.dto.NotificationEvent;
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
        ReflectionTestUtils.setField(notificationProducer, "exchange", "notification.exchange");
        ReflectionTestUtils.setField(notificationProducer, "aiRoutingKey", "ai.completed");
    }

    @Test
    void publishAiCompletedEvent() {
        notificationProducer.publishAiCompletedEvent(1L, "Summary");
        verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("ai.completed"), any(NotificationEvent.class));
    }

    @Test
    void publishAtsScoreEvent() {
        notificationProducer.publishAtsScoreEvent(1L, 0.85);
        verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("ai.completed"), any(NotificationEvent.class));
    }

    @Test
    void publishEvent_exception() {
        doThrow(new RuntimeException("Rabbit error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

        // Should not throw
        notificationProducer.publishAiCompletedEvent(1L, "Task");
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }
}
