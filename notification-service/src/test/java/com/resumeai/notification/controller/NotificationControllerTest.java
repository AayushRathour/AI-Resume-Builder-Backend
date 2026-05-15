package com.resumeai.notification.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.resumeai.notification.dto.NotificationRequest;
import com.resumeai.notification.dto.NotificationResponse;
import com.resumeai.notification.dto.UnreadCountResponse;
import com.resumeai.notification.entity.NotificationType;
import com.resumeai.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationController notificationController;

    private NotificationResponse responseDto;
    private UUID notifId;

    @BeforeEach
    void setUp() {
        notifId = UUID.randomUUID();
        responseDto = NotificationResponse.builder()
                .notificationId(notifId).userId(1L)
                .type(NotificationType.RESUME_CREATED)
                .message("Test").build();
    }

    @Test
    void getNotifications() {
        when(notificationService.getNotifications(1L)).thenReturn(List.of(responseDto));
        ResponseEntity<List<NotificationResponse>> resp = notificationController.getNotifications(1L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void getNotificationsPaged() {
        Page<NotificationResponse> page = new PageImpl<>(List.of(responseDto));
        when(notificationService.getNotificationsPaginated(1L, 0, 20)).thenReturn(page);
        ResponseEntity<Page<NotificationResponse>> resp = notificationController.getNotificationsPaged(1L, 0, 20);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void markAsRead() {
        when(notificationService.markAsRead(notifId)).thenReturn(responseDto);
        ResponseEntity<NotificationResponse> resp = notificationController.markAsRead(notifId);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void getUnreadCount() {
        UnreadCountResponse unread = UnreadCountResponse.builder().userId(1L).unreadCount(3).build();
        when(notificationService.getUnreadCount(1L)).thenReturn(unread);
        ResponseEntity<UnreadCountResponse> resp = notificationController.getUnreadCount(1L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(3, resp.getBody().getUnreadCount());
    }

    @Test
    void createNotification() {
        NotificationRequest req = NotificationRequest.builder()
                .userId(1L).type(NotificationType.RESUME_CREATED).message("test").build();
        when(notificationService.createNotification(1L, NotificationType.RESUME_CREATED, "test"))
                .thenReturn(responseDto);
        ResponseEntity<NotificationResponse> resp = notificationController.createNotification(req);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    void getAllNotifications() {
        when(notificationService.getAllNotifications()).thenReturn(List.of(responseDto));
        ResponseEntity<List<NotificationResponse>> resp = notificationController.getAllNotifications();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void markAllAsRead() {
        when(notificationService.markAllAsRead(1L)).thenReturn(2);
        ResponseEntity<Map<String, Object>> resp = notificationController.markAllAsRead(1L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(2, resp.getBody().get("updatedCount"));
    }

    @Test
    void deleteNotification() {
        ResponseEntity<Void> resp = notificationController.deleteNotification(notifId);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(notificationService).deleteNotification(notifId);
    }
}
