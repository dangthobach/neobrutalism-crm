package com.neobrutalism.crm.domain.notification.dto;

import com.neobrutalism.crm.domain.notification.model.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating notification preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {

    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    @Builder.Default
    private Boolean inAppEnabled = true;

    @Builder.Default
    private Boolean emailEnabled = true;

    @Builder.Default
    private Boolean smsEnabled = false;

    private String quietHoursStart;

    private String quietHoursEnd;

    @Builder.Default
    private Boolean digestModeEnabled = false;

    private String digestTime;
}
