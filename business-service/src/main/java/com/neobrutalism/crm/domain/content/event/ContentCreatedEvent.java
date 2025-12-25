package com.neobrutalism.crm.domain.content.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.common.enums.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when content is created
 */
@Getter
@Setter
public class ContentCreatedEvent extends DomainEvent {

    private UUID contentId;
    private String title;
    private String slug;
    private ContentType contentType;
    private UUID authorId;
    private String tenantId;

    public ContentCreatedEvent() {
        super();
    }

    public ContentCreatedEvent(UUID contentId, String title, String slug,
                              ContentType contentType, UUID authorId,
                              String tenantId, String createdBy) {
        super("CONTENT_CREATED", contentId.toString(), "Content", createdBy);
        this.contentId = contentId;
        this.title = title;
        this.slug = slug;
        this.contentType = contentType;
        this.authorId = authorId;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contentId", contentId);
        payload.put("title", title);
        payload.put("slug", slug);
        payload.put("contentType", contentType);
        payload.put("authorId", authorId);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
