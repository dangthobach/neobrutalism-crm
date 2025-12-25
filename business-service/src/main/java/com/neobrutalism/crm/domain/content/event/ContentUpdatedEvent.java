package com.neobrutalism.crm.domain.content.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when content is updated
 */
@Getter
@Setter
public class ContentUpdatedEvent extends DomainEvent {

    private UUID contentId;
    private String title;
    private String slug;
    private String tenantId;

    public ContentUpdatedEvent() {
        super();
    }

    public ContentUpdatedEvent(UUID contentId, String title, String slug,
                              String tenantId, String updatedBy) {
        super("CONTENT_UPDATED", contentId.toString(), "Content", updatedBy);
        this.contentId = contentId;
        this.title = title;
        this.slug = slug;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contentId", contentId);
        payload.put("title", title);
        payload.put("slug", slug);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
