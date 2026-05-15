package com.resumeai.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.resumeai.notification.dto.event.NotificationEvent;
import com.resumeai.notification.entity.NotificationType;
import com.resumeai.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener listener;

    private NotificationEvent event;

    @BeforeEach
    void setUp() {
        event = new NotificationEvent();
        event.setUserId(1L);
        event.setEventId("evt-1");
        event.setType("RESUME_CREATED");
        event.setMessage("Test message");
    }

    @Test
    void handleResumeEvent_success() {
        listener.handleResumeEvent(event);
        verify(notificationService).createNotification(eq(1L), eq(NotificationType.RESUME_CREATED), anyString(), eq("Test message"), eq("evt-1"), any());
    }

    @Test
    void handleResumeEvent_invalidEvent() {
        event.setUserId(null);
        listener.handleResumeEvent(event);
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleExportEvent_success() {
        event.setType("RESUME_EXPORTED");
        listener.handleExportEvent(event);
        verify(notificationService).createNotification(eq(1L), eq(NotificationType.RESUME_EXPORTED), anyString(), anyString(), eq("evt-1"), any());
    }

    @Test
    void handleTemplateEvent_success() {
        event.setType("TEMPLATE_UPDATED");
        listener.handleTemplateEvent(event);
        verify(notificationService).createNotification(eq(1L), eq(NotificationType.TEMPLATE_UPDATED), anyString(), anyString(), eq("evt-1"), any());
    }

    @Test
    void handleNotificationEvent_success() {
        event.setType("JOB_APPLIED");
        listener.handleNotificationEvent(event);
        verify(notificationService).createNotification(eq(1L), eq(NotificationType.JOB_APPLIED), anyString(), eq("Test message"), eq("evt-1"), any());
    }

    @Test
    void handleNotificationEvent_unknownType() {
        event.setType("UNKNOWN_TYPE");
        listener.handleNotificationEvent(event);
        verify(notificationService).createNotification(eq(1L), eq(NotificationType.GENERAL), anyString(), anyString(), eq("evt-1"), any());
    }
}
