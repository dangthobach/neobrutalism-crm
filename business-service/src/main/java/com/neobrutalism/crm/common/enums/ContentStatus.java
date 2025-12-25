package com.neobrutalism.crm.common.enums;

/**
 * Content status enumeration for content lifecycle management
 */
public enum ContentStatus {
    /**
     * Draft - content is being created/edited
     */
    DRAFT("Draft", "Content is being created or edited"),

    /**
     * Review - content is under review before publishing
     */
    REVIEW("Under Review", "Content is under review"),

    /**
     * Published - content is live and accessible
     */
    PUBLISHED("Published", "Content is live and accessible"),

    /**
     * Archived - content is no longer active but kept for reference
     */
    ARCHIVED("Archived", "Content is archived"),

    /**
     * Deleted - content is marked for deletion
     */
    DELETED("Deleted", "Content is deleted");

    private final String displayName;
    private final String description;

    ContentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if content is publicly accessible
     */
    public boolean isPublic() {
        return this == PUBLISHED;
    }

    /**
     * Check if content can be edited
     */
    public boolean isEditable() {
        return this == DRAFT || this == REVIEW;
    }
}
