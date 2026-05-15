package com.resumeai.template.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.resumeai.template.dto.NotificationEvent;
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
        ReflectionTestUtils.setField(notificationProducer, "exchange", "resumeai.exchange");
    }

    @Test
    void publishTemplateCreatedEvent() {
        notificationProducer.publishTemplateCreatedEvent(1L, "Template 1");
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("template.created"), any(NotificationEvent.class));
    }

    @Test
    void publishTemplateUpdatedEvent() {
        notificationProducer.publishTemplateUpdatedEvent(1L, "Template 1");
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("template.updated"), any(NotificationEvent.class));
    }

    @Test
    void publishTemplateDeletedEvent() {
        notificationProducer.publishTemplateDeletedEvent(1L);
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("template.deleted"), any(NotificationEvent.class));
    }

    @Test
    void publishEvent_exception() {
        doThrow(new RuntimeException("RabbitMQ Error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

        // This should log the error but not throw an exception
        notificationProducer.publishTemplateDeletedEvent(1L);
        
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("template.deleted"), any(NotificationEvent.class));
    }
}
