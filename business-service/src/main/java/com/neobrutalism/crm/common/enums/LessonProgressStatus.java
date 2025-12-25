package com.neobrutalism.crm.common.enums;

public enum LessonProgressStatus {
    NOT_STARTED("Not Started", "Lesson not yet started"),
    IN_PROGRESS("In Progress", "Lesson in progress"),
    COMPLETED("Completed", "Lesson completed");

    private final String displayName;
    private final String description;

    LessonProgressStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return this == COMPLETED; }
}
