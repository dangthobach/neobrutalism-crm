package com.neobrutalism.crm.domain.notification.repository;

import com.neobrutalism.crm.domain.notification.model.NotificationPreference;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NotificationPreference entity
 * Optimized for high-scale read operations (1M users, 50K CCU)
 * Uses composite indexes for efficient lookups
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /**
     * Find all preferences for a user in an organization
     * Uses idx_notif_pref_user_org composite index
     */
    List<NotificationPreference> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * Find specific preference by user, type, and organization
     * Uses uk_user_type_org unique constraint
     */
    Optional<NotificationPreference> findByUserIdAndNotificationTypeAndOrganizationId(
        UUID userId,
        NotificationType notificationType,
        UUID organizationId
    );

    /**
     * Check if preference exists for user and type
     */
    boolean existsByUserIdAndNotificationTypeAndOrganizationId(
        UUID userId,
        NotificationType notificationType,
        UUID organizationId
    );

    /**
     * Delete all preferences for a user (for user deletion cleanup)
     */
    void deleteByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * Find users with specific notification type enabled for a channel
     * Used for bulk notification sending
     */
    @Query("""
        SELECT np.userId FROM NotificationPreference np
        WHERE np.organizationId = :organizationId
        AND np.notificationType = :notificationType
        AND (
            (:channel = 'IN_APP' AND np.inAppEnabled = true)
            OR (:channel = 'EMAIL' AND np.emailEnabled = true)
            OR (:channel = 'SMS' AND np.smsEnabled = true)
        )
    """)
    List<UUID> findUserIdsWithChannelEnabled(
        @Param("organizationId") UUID organizationId,
        @Param("notificationType") NotificationType notificationType,
        @Param("channel") String channel
    );

    /**
     * Count preferences by organization (for analytics)
     */
    long countByOrganizationId(UUID organizationId);
}
