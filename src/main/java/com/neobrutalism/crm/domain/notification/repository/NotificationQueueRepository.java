package com.neobrutalism.crm.domain.notification.repository;

import com.neobrutalism.crm.domain.notification.model.NotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for NotificationQueue entities
 */
@Repository
public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, UUID> {

    /**
     * Find all queued notifications that are ready to be sent
     *
     * @param now Current time
     * @return List of notifications ready to send
     */
    @Query("SELECT nq FROM NotificationQueue nq WHERE nq.status = 'QUEUED' AND nq.scheduledAt <= :now ORDER BY nq.scheduledAt ASC")
    List<NotificationQueue> findQueuedNotificationsReadyToSend(@Param("now") Instant now);

    /**
     * Find failed notifications that can be retried
     *
     * @return List of retryable notifications
     */
    @Query("SELECT nq FROM NotificationQueue nq WHERE nq.status = 'FAILED' AND nq.attemptCount < 3 ORDER BY nq.createdAt ASC")
    List<NotificationQueue> findRetryableNotifications();

    /**
     * Count queued notifications for a recipient
     *
     * @param recipientId Recipient user ID
     * @return Count of queued notifications
     */
    long countByRecipientIdAndStatus(UUID recipientId, NotificationQueue.QueueStatus status);

    /**
     * Delete old sent notifications (cleanup)
     *
     * @param before Delete notifications sent before this time
     * @return Number of deleted records
     */
    @Query("DELETE FROM NotificationQueue nq WHERE nq.status = 'SENT' AND nq.sentAt < :before")
    int deleteOldSentNotifications(@Param("before") Instant before);
}
