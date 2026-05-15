package com.resumeai.resume.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.resumeai.resume.dto.NotificationEvent;
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

    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private NotificationProducer notificationProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationProducer, "exchange", "resumeai.exchange");
    }

    @Test
    void publishResumeCreatedEvent() {
        notificationProducer.publishResumeCreatedEvent(1L, 10L, "My Resume");
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.created"), any(NotificationEvent.class));
    }

    @Test
    void publishResumeUpdatedEvent() {
        notificationProducer.publishResumeUpdatedEvent(1L, 10L, "My Resume");
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.updated"), any(NotificationEvent.class));
    }

    @Test
    void publishResumeDeletedEvent() {
        notificationProducer.publishResumeDeletedEvent(1L, 10L, "My Resume");
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.deleted"), any(NotificationEvent.class));
    }

    @Test
    @SuppressWarnings("deprecation")
    void publishResumeSavedEvent_deprecated() {
        notificationProducer.publishResumeSavedEvent(1L, "My Resume");
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.created"), any(NotificationEvent.class));
    }

    @Test
    @SuppressWarnings("deprecation")
    void publishResumePublishedEvent_deprecated() {
        notificationProducer.publishResumePublishedEvent(1L, "My Resume");
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.updated"), any(NotificationEvent.class));
    }

    @Test
    void publishEvent_exception() {
        doThrow(new RuntimeException("RabbitMQ error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

        // Should not throw
        notificationProducer.publishResumeDeletedEvent(1L, 10L, "Resume");

        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.deleted"), any(NotificationEvent.class));
    }
}
