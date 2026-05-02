package com.resumeai.notification.repository;

import com.resumeai.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * All notifications for a user, newest first.
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Notifications for a user filtered by read status.
     */
    List<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, boolean isRead);

    /**
     * Count of unread (or read) notifications for a user.
     */
    long countByUserIdAndIsRead(Long userId, boolean isRead);
}
