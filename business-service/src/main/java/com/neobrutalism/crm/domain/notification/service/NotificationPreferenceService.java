package com.neobrutalism.crm.domain.notification.service;

import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.security.UserContext;
import com.neobrutalism.crm.domain.notification.dto.NotificationPreferenceRequest;
import com.neobrutalism.crm.domain.notification.model.NotificationPreference;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import com.neobrutalism.crm.domain.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing notification preferences
 * Implements caching for high-scale performance (1M users, 50K CCU)
 *
 * Cache Strategy:
 * - Read-heavy workload: Users check preferences frequently
 * - Cache TTL: 15 minutes (balanced between consistency and performance)
 * - Cache invalidation: On update/delete operations
 * - Cache key: user:org:preferences for user preferences
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserContext userContext;

    /**
     * Get all preferences for current user
     * Cached to reduce database load
     */
    @Cacheable(value = "notification-preferences", key = "#userId + ':' + #organizationId")
    @Transactional(readOnly = true)
    public List<NotificationPreference> getUserPreferences(UUID userId, UUID organizationId) {
        log.debug("Fetching notification preferences for user: {}, org: {}", userId, organizationId);
        List<NotificationPreference> preferences = preferenceRepository.findByUserIdAndOrganizationId(userId, organizationId);

        // If no preferences exist, create defaults
        if (preferences.isEmpty()) {
            log.info("No preferences found for user: {}, creating defaults", userId);
            return createDefaultPreferences(userId, organizationId);
        }

        return preferences;
    }

    /**
     * Get current user's preferences
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getMyPreferences() {
        UUID userId = UUID.fromString(userContext.getCurrentUserIdOrThrow());
        UUID organizationId = UUID.fromString(userContext.getCurrentOrganizationIdOrThrow());
        return getUserPreferences(userId, organizationId);
    }

    /**
     * Get specific preference by type
     */
    @Cacheable(value = "notification-preference", key = "#userId + ':' + #organizationId + ':' + #type")
    @Transactional(readOnly = true)
    public NotificationPreference getPreference(UUID userId, UUID organizationId, NotificationType type) {
        return preferenceRepository.findByUserIdAndNotificationTypeAndOrganizationId(userId, type, organizationId)
                .orElseGet(() -> createDefaultPreference(userId, organizationId, type));
    }

    /**
     * Update or create preference
     */
    @CacheEvict(value = {"notification-preferences", "notification-preference"},
                key = "#userId + ':' + #organizationId",
                allEntries = true)
    @Transactional
    public NotificationPreference updatePreference(UUID userId, UUID organizationId,
                                                   NotificationType type,
                                                   NotificationPreferenceRequest request) {
        log.info("Updating preference for user: {}, type: {}", userId, type);

        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationTypeAndOrganizationId(userId, type, organizationId)
                .orElse(NotificationPreference.builder()
                        .userId(userId)
                        .organizationId(organizationId)
                        .notificationType(type)
                        .build());

        // Update fields
        preference.setInAppEnabled(request.getInAppEnabled());
        preference.setEmailEnabled(request.getEmailEnabled());
        preference.setSmsEnabled(request.getSmsEnabled());
        preference.setQuietHoursStart(request.getQuietHoursStart());
        preference.setQuietHoursEnd(request.getQuietHoursEnd());
        preference.setDigestModeEnabled(request.getDigestModeEnabled());
        preference.setDigestTime(request.getDigestTime());

        return preferenceRepository.save(preference);
    }

    /**
     * Update current user's preference
     */
    @Transactional
    public NotificationPreference updateMyPreference(NotificationType type, NotificationPreferenceRequest request) {
        UUID userId = UUID.fromString(userContext.getCurrentUserIdOrThrow());
        UUID organizationId = UUID.fromString(userContext.getCurrentOrganizationIdOrThrow());
        return updatePreference(userId, organizationId, type, request);
    }

    /**
     * Batch update preferences
     */
    @CacheEvict(value = {"notification-preferences", "notification-preference"}, allEntries = true)
    @Transactional
    public List<NotificationPreference> batchUpdatePreferences(UUID userId, UUID organizationId,
                                                                List<NotificationPreferenceRequest> requests) {
        log.info("Batch updating {} preferences for user: {}", requests.size(), userId);

        return requests.stream()
                .map(request -> updatePreference(userId, organizationId, request.getNotificationType(), request))
                .toList();
    }

    /**
     * Delete preference
     */
    @CacheEvict(value = {"notification-preferences", "notification-preference"}, allEntries = true)
    @Transactional
    public void deletePreference(UUID userId, UUID organizationId, NotificationType type) {
        log.info("Deleting preference for user: {}, type: {}", userId, type);
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationTypeAndOrganizationId(userId, type, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preference not found"));

        preferenceRepository.delete(preference);
    }

    /**
     * Reset to defaults
     */
    @CacheEvict(value = {"notification-preferences", "notification-preference"}, allEntries = true)
    @Transactional
    public List<NotificationPreference> resetToDefaults(UUID userId, UUID organizationId) {
        log.info("Resetting preferences to defaults for user: {}", userId);

        // Delete existing
        preferenceRepository.deleteByUserIdAndOrganizationId(userId, organizationId);

        // Create defaults
        return createDefaultPreferences(userId, organizationId);
    }

    /**
     * Create default preferences for all notification types
     */
    private List<NotificationPreference> createDefaultPreferences(UUID userId, UUID organizationId) {
        log.info("Creating default preferences for user: {}", userId);

        List<NotificationPreference> defaults = List.of(
            createDefaultPreference(userId, organizationId, NotificationType.TASK_ASSIGNED),
            createDefaultPreference(userId, organizationId, NotificationType.TASK_UPDATED),
            createDefaultPreference(userId, organizationId, NotificationType.TASK_COMPLETED),
            createDefaultPreference(userId, organizationId, NotificationType.DEADLINE_APPROACHING),
            createDefaultPreference(userId, organizationId, NotificationType.TASK_OVERDUE),
            createDefaultPreference(userId, organizationId, NotificationType.COMMENT_ADDED),
            createDefaultPreference(userId, organizationId, NotificationType.MENTION),
            createDefaultPreference(userId, organizationId, NotificationType.SYSTEM)
        );

        return preferenceRepository.saveAll(defaults);
    }

    /**
     * Create default preference for specific type
     */
    private NotificationPreference createDefaultPreference(UUID userId, UUID organizationId, NotificationType type) {
        // High priority types: enable all channels by default
        boolean isHighPriority = type == NotificationType.TASK_ASSIGNED ||
                                type == NotificationType.DEADLINE_APPROACHING ||
                                type == NotificationType.TASK_OVERDUE;

        return NotificationPreference.builder()
                .userId(userId)
                .organizationId(organizationId)
                .notificationType(type)
                .inAppEnabled(true)  // Always enable in-app
                .emailEnabled(isHighPriority)  // Email for high priority
                .smsEnabled(false)  // SMS disabled by default (cost consideration)
                .digestModeEnabled(false)
                .build();
    }

    /**
     * Check if user should receive notification based on preferences
     * This is called frequently, so it's cached
     */
    @Transactional(readOnly = true)
    public boolean shouldSendNotification(UUID userId, UUID organizationId, NotificationType type, String channel) {
        NotificationPreference pref = getPreference(userId, organizationId, type);

        return switch (channel.toUpperCase()) {
            case "IN_APP" -> Boolean.TRUE.equals(pref.getInAppEnabled());
            case "EMAIL" -> Boolean.TRUE.equals(pref.getEmailEnabled());
            case "SMS" -> Boolean.TRUE.equals(pref.getSmsEnabled());
            default -> false;
        };
    }
}
