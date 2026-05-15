package com.resumeai.export.service;

import com.resumeai.export.dto.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationProducer notificationProducer;

    private void setExchange() {
        ReflectionTestUtils.setField(notificationProducer, "exchange", "resumeai.exchange");
    }

    @Test
    void publishExportCompletedEvent_withoutResumeId_sendsEvent() {
        setExchange();

        notificationProducer.publishExportCompletedEvent(1L, "PDF");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.exported"), captor.capture());

        NotificationEvent event = captor.getValue();
        assertEquals(1L, event.getUserId());
        assertEquals("RESUME_EXPORTED", event.getType());
        assertEquals("Export Completed", event.getTitle());
        assertFalse(event.isCritical());
        assertTrue(event.getMessage().contains("PDF"));
    }

    @Test
    void publishExportCompletedEvent_withResumeId_sendsEvent() {
        setExchange();

        notificationProducer.publishExportCompletedEvent(1L, 100L, "DOCX");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.exported"), captor.capture());

        NotificationEvent event = captor.getValue();
        assertEquals(1L, event.getUserId());
        assertEquals(100L, event.getResumeId());
        assertTrue(event.getMessage().contains("DOCX"));
    }

    @Test
    void publishExportErrorEvent_withoutResumeId_sendsErrorEvent() {
        setExchange();

        notificationProducer.publishExportErrorEvent(1L, "Out of memory");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.exported"), captor.capture());

        NotificationEvent event = captor.getValue();
        assertEquals("EXPORT_FAILED", event.getType());
        assertEquals("Export Failed", event.getTitle());
        assertTrue(event.isCritical());
        assertTrue(event.getMessage().contains("Out of memory"));
    }

    @Test
    void publishExportErrorEvent_withResumeId_sendsErrorEvent() {
        setExchange();

        notificationProducer.publishExportErrorEvent(1L, 200L, "Template not found");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("resumeai.exchange"), eq("resume.exported"), captor.capture());

        NotificationEvent event = captor.getValue();
        assertEquals(200L, event.getResumeId());
        assertTrue(event.getMessage().contains("Template not found"));
    }

    @Test
    void publishEvent_rabbitTemplateThrows_doesNotPropagate() {
        setExchange();
        doThrow(new RuntimeException("Connection refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

        // Should NOT throw — error is swallowed and logged
        assertDoesNotThrow(() ->
                notificationProducer.publishExportCompletedEvent(1L, "PDF"));
    }

    @Test
    void publishExportCompletedEvent_eventHasUniqueId() {
        setExchange();

        notificationProducer.publishExportCompletedEvent(1L, "PDF");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture());

        NotificationEvent event = captor.getValue();
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
    }
}
