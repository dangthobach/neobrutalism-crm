package com.neobrutalism.crm.common.enums;

/**
 * Content type enumeration for different types of content in the CMS
 */
public enum ContentType {
    /**
     * Blog post - informal, personal, or news-style content
     */
    BLOG("Blog Post", "Informal or news-style content"),

    /**
     * Article - formal, in-depth content
     */
    ARTICLE("Article", "Formal, in-depth content"),

    /**
     * Page - static page content (About, Contact, etc.)
     */
    PAGE("Page", "Static page content"),

    /**
     * News - news and announcements
     */
    NEWS("News", "News and announcements"),

    /**
     * Guide - how-to guides and tutorials
     */
    GUIDE("Guide", "How-to guides and tutorials"),

    /**
     * Video - video content
     */
    VIDEO("Video", "Video content");

    private final String displayName;
    private final String description;

    ContentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
