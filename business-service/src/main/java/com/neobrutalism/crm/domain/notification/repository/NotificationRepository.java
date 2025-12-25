package com.neobrutalism.crm.domain.notification.repository;

import com.neobrutalism.crm.domain.notification.model.Notification;
import com.neobrutalism.crm.domain.notification.model.NotificationStatus;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Notification
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndDeletedFalse(UUID id);

    List<Notification> findByRecipientIdAndDeletedFalseOrderByCreatedAtDesc(UUID recipientId);

    Page<Notification> findByRecipientIdAndDeletedFalse(UUID recipientId, Pageable pageable);

    List<Notification> findByRecipientIdAndIsReadFalseAndDeletedFalseOrderByCreatedAtDesc(UUID recipientId);

    List<Notification> findByRecipientIdAndNotificationTypeAndDeletedFalse(
            UUID recipientId, NotificationType notificationType);

    List<Notification> findByStatusAndDeletedFalse(NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = false AND n.deleted = false")
    List<Notification> findUnreadNotifications(@Param("recipientId") UUID recipientId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = false AND n.deleted = false")
    Long countUnreadNotifications(@Param("recipientId") UUID recipientId);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.priority >= :priority AND n.deleted = false ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notification> findHighPriorityNotifications(@Param("recipientId") UUID recipientId, @Param("priority") Integer priority);

    @Query("SELECT n FROM Notification n WHERE n.entityType = :entityType AND n.entityId = :entityId AND n.deleted = false")
    List<Notification> findByEntity(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipientId = :recipientId AND n.isRead = false AND n.deleted = false")
    int markAllAsRead(@Param("recipientId") UUID recipientId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.deleted = true WHERE n.recipientId = :recipientId")
    int deleteAllByRecipient(@Param("recipientId") UUID recipientId);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.createdAt < :before AND n.deleted = false")
    List<Notification> findByStatusAndCreatedBefore(
            @Param("status") NotificationStatus status,
            @Param("before") Instant before);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.createdAt >= :since AND n.deleted = false ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(
            @Param("recipientId") UUID recipientId,
            @Param("since") Instant since);
}
