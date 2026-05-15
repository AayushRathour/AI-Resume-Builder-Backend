package com.resumeai.jobmatch.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.resumeai.jobmatch.dto.NotificationEvent;
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
    }

    @Test
    void publishAtsCompletedEvent() {
        notificationProducer.publishAtsCompletedEvent(1L, 2L, 85.0);
        verify(rabbitTemplate).convertAndSend(eq("test.exchange"), eq("ats.completed"), any(NotificationEvent.class));
    }

    @Test
    void publishJobAppliedEvent() {
        notificationProducer.publishJobAppliedEvent(1L, "Dev", "Acme");
        verify(rabbitTemplate).convertAndSend(eq("test.exchange"), eq("job.applied"), any(NotificationEvent.class));
    }

    @Test
    void publishJobMatchEvent() {
        notificationProducer.publishJobMatchEvent(1L, 5);
        verify(rabbitTemplate).convertAndSend(eq("test.exchange"), eq("ats.completed"), any(NotificationEvent.class));
    }

    @Test
    void publishAnalysisCompletedEvent() {
        notificationProducer.publishAnalysisCompletedEvent(1L);
        verify(rabbitTemplate).convertAndSend(eq("test.exchange"), eq("ats.completed"), any(NotificationEvent.class));
    }
}
