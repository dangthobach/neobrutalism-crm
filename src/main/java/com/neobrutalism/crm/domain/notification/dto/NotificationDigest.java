package com.neobrutalism.crm.domain.notification.dto;

import com.neobrutalism.crm.domain.notification.model.Notification;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for notification digest summary
 * Contains aggregated information about notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDigest {

    /**
     * Total number of notifications in digest
     */
    private int totalCount;

    /**
     * Number of high priority notifications
     */
    private int highPriorityCount;

    /**
     * Count of notifications grouped by type
     */
    private Map<NotificationType, Long> countByType;

    /**
     * Count of notifications grouped by entity type
     */
    private Map<String, Long> countByEntity;

    /**
     * List of all notifications in the digest
     */
    private List<Notification> notifications;
}
