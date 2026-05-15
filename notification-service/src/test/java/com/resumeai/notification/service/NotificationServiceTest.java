package com.resumeai.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.resumeai.notification.dto.NotificationResponse;
import com.resumeai.notification.dto.UnreadCountResponse;
import com.resumeai.notification.entity.Notification;
import com.resumeai.notification.entity.NotificationType;
import com.resumeai.notification.exception.ResourceNotFoundException;
import com.resumeai.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private NotificationService notificationService;

    private Notification notification;
    private UUID notifId;

    @BeforeEach
    void setUp() {
        notifId = UUID.randomUUID();
        notification = Notification.builder()
                .notificationId(notifId).userId(1L)
                .type(NotificationType.RESUME_CREATED).title("Resume Created")
                .message("Your resume was created.").isRead(false)
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    void createNotification_simple() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse resp = notificationService.createNotification(
                1L, NotificationType.RESUME_CREATED, "Your resume was created.");

        assertNotNull(resp);
        assertEquals(1L, resp.getUserId());
    }

    @Test
    void createNotification_withEventId() {
        when(notificationRepository.existsByEventId("evt-1")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse resp = notificationService.createNotification(
                1L, NotificationType.RESUME_CREATED, "Resume Created",
                "msg", "evt-1", null);

        assertNotNull(resp);
    }

    @Test
    void createNotification_duplicate_skips() {
        when(notificationRepository.existsByEventId("evt-dup")).thenReturn(true);

        NotificationResponse resp = notificationService.createNotification(
                1L, NotificationType.RESUME_CREATED, "Resume Created",
                "msg", "evt-dup", null);

        assertNull(resp);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getNotifications() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> list = notificationService.getNotifications(1L);
        assertEquals(1, list.size());
    }

    @Test
    void getNotificationsPaginated() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getNotificationsPaginated(1L, 0, 20);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void markAsRead_success() {
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse resp = notificationService.markAsRead(notifId);
        assertNotNull(resp);
    }

    @Test
    void markAsRead_notFound() {
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(notifId));
    }

    @Test
    void getUnreadCount() {
        when(notificationRepository.countByUserIdAndIsRead(1L, false)).thenReturn(5L);

        UnreadCountResponse resp = notificationService.getUnreadCount(1L);
        assertEquals(5L, resp.getUnreadCount());
    }

    @Test
    void getAllNotifications() {
        when(notificationRepository.findAll()).thenReturn(List.of(notification));

        List<NotificationResponse> list = notificationService.getAllNotifications();
        assertEquals(1, list.size());
    }

    @Test
    void markAllAsRead() {
        Notification unread = Notification.builder()
                .notificationId(UUID.randomUUID()).userId(1L)
                .type(NotificationType.RESUME_UPDATED).title("Updated")
                .message("msg").isRead(false).createdAt(LocalDateTime.now()).build();

        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(1L, false))
                .thenReturn(List.of(unread));
        when(notificationRepository.saveAll(any())).thenReturn(List.of(unread));

        int count = notificationService.markAllAsRead(1L);
        assertEquals(1, count);
    }

    @Test
    void deleteNotification_success() {
        when(notificationRepository.existsById(notifId)).thenReturn(true);

        notificationService.deleteNotification(notifId);
        verify(notificationRepository).deleteById(notifId);
    }

    @Test
    void deleteNotification_notFound() {
        when(notificationRepository.existsById(notifId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.deleteNotification(notifId));
    }

    @Test
    void pushWebSocketNotification_exception() {
        NotificationResponse resp = NotificationResponse.builder()
                .userId(1L).message("test").build();

        doThrow(new RuntimeException("WS error")).when(messagingTemplate)
                .convertAndSend(any(String.class), any(NotificationResponse.class));

        // Should not throw
        notificationService.pushWebSocketNotification(1L, resp);
    }
}
