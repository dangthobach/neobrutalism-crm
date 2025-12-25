package com.neobrutalism.crm.domain.notification.dto;

import com.neobrutalism.crm.domain.notification.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Notification Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    private Integer priority = 0;

    private String actionUrl;

    private String entityType;

    private UUID entityId;
}
