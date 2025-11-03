package com.neobrutalism.crm.common.enums;

/**
 * Course status enumeration for course lifecycle
 */
public enum CourseStatus {
    /**
     * Draft - course is being created
     */
    DRAFT("Draft", "Course is being created"),

    /**
     * Review - course is under review
     */
    REVIEW("Under Review", "Course is under review"),

    /**
     * Published - course is live and available
     */
    PUBLISHED("Published", "Course is live and available"),

    /**
     * Archived - course is no longer active
     */
    ARCHIVED("Archived", "Course is archived"),

    /**
     * Deleted - course is marked for deletion
     */
    DELETED("Deleted", "Course is deleted");

    private final String displayName;
    private final String description;

    CourseStatus(String displayName, String description) {
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
     * Check if course is publicly accessible
     */
    public boolean isPublic() {
        return this == PUBLISHED;
    }

    /**
     * Check if course can be edited
     */
    public boolean isEditable() {
        return this == DRAFT || this == REVIEW;
    }
}
