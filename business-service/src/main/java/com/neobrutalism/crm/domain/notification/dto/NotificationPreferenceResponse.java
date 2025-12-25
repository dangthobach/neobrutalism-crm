package com.neobrutalism.crm.domain.notification.dto;

import com.neobrutalism.crm.domain.notification.model.NotificationPreference;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Response DTO for notification preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private UUID id;
    private UUID userId;
    private UUID organizationId;
    private NotificationType notificationType;
    private Boolean inAppEnabled;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
    private Boolean digestModeEnabled;
    private String digestTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .organizationId(preference.getOrganizationId())
                .notificationType(preference.getNotificationType())
                .inAppEnabled(preference.getInAppEnabled())
                .emailEnabled(preference.getEmailEnabled())
                .smsEnabled(preference.getSmsEnabled())
                .quietHoursStart(preference.getQuietHoursStart())
                .quietHoursEnd(preference.getQuietHoursEnd())
                .digestModeEnabled(preference.getDigestModeEnabled())
                .digestTime(preference.getDigestTime())
                .createdAt(toLocalDateTime(preference.getCreatedDate()))
                .updatedAt(toLocalDateTime(preference.getLastModifiedDate()))
                .build();
    }
    
    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
