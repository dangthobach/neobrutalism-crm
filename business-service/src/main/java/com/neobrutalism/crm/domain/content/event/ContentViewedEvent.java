package com.neobrutalism.crm.domain.content.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when content is viewed by a user
 * This triggers engagement scoring and analytics updates
 */
@Getter
@Setter
public class ContentViewedEvent extends DomainEvent {

    private UUID contentId;
    private String contentTitle;
    private UUID userId;
    private String sessionId;
    private Integer timeSpentSeconds;
    private Integer scrollPercentage;
    private String tenantId;

    public ContentViewedEvent() {
        super();
    }

    public ContentViewedEvent(UUID contentId, String contentTitle, UUID userId,
                             String sessionId, Integer timeSpentSeconds,
                             Integer scrollPercentage, String tenantId) {
        super("CONTENT_VIEWED", contentId.toString(), "Content", userId != null ? userId.toString() : "anonymous");
        this.contentId = contentId;
        this.contentTitle = contentTitle;
        this.userId = userId;
        this.sessionId = sessionId;
        this.timeSpentSeconds = timeSpentSeconds;
        this.scrollPercentage = scrollPercentage;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contentId", contentId);
        payload.put("contentTitle", contentTitle);
        payload.put("userId", userId);
        payload.put("sessionId", sessionId);
        payload.put("timeSpentSeconds", timeSpentSeconds);
        payload.put("scrollPercentage", scrollPercentage);
        payload.put("tenantId", tenantId);
        return payload;
    }

    /**
     * Check if this is an authenticated view
     */
    public boolean isAuthenticated() {
        return userId != null;
    }

    /**
     * Check if this is a significant view (>30 seconds)
     */
    public boolean isSignificantView() {
        return timeSpentSeconds != null && timeSpentSeconds > 30;
    }

    /**
     * Check if user fully read the content (>70% scroll)
     */
    public boolean isFullyRead() {
        return scrollPercentage != null && scrollPercentage > 70;
    }
}
