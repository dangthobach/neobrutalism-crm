package com.neobrutalism.crm.domain.content.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when content is published
 * This triggers notifications to subscribers and updates read model
 */
@Getter
@Setter
public class ContentPublishedEvent extends DomainEvent {

    private UUID contentId;
    private String title;
    private String slug;
    private UUID authorId;
    private Instant publishedAt;
    private String tenantId;

    public ContentPublishedEvent() {
        super();
    }

    public ContentPublishedEvent(UUID contentId, String title, String slug,
                                UUID authorId, Instant publishedAt,
                                String tenantId, String publishedBy) {
        super("CONTENT_PUBLISHED", contentId.toString(), "Content", publishedBy);
        this.contentId = contentId;
        this.title = title;
        this.slug = slug;
        this.authorId = authorId;
        this.publishedAt = publishedAt;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contentId", contentId);
        payload.put("title", title);
        payload.put("slug", slug);
        payload.put("authorId", authorId);
        payload.put("publishedAt", publishedAt);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
