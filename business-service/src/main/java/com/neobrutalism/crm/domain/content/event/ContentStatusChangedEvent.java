package com.neobrutalism.crm.domain.content.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.common.enums.ContentStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when content status changes
 */
@Getter
@Setter
public class ContentStatusChangedEvent extends DomainEvent {

    private UUID contentId;
    private String title;
    private ContentStatus oldStatus;
    private ContentStatus newStatus;
    private String reason;
    private String tenantId;

    public ContentStatusChangedEvent() {
        super();
    }

    public ContentStatusChangedEvent(UUID contentId, String title,
                                    ContentStatus oldStatus, ContentStatus newStatus,
                                    String reason, String tenantId, String changedBy) {
        super("CONTENT_STATUS_CHANGED", contentId.toString(), "Content", changedBy);
        this.contentId = contentId;
        this.title = title;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contentId", contentId);
        payload.put("title", title);
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        payload.put("reason", reason);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
