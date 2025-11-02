package com.neobrutalism.crm.common.enums;

public enum LessonType {
    TEXT("Text", "Text-based lesson"),
    VIDEO("Video", "Video lesson"),
    QUIZ("Quiz", "Quiz/Assessment"),
    ASSIGNMENT("Assignment", "Assignment/Homework"),
    LIVE_SESSION("Live Session", "Live class/webinar"),
    DOCUMENT("Document", "Downloadable document");

    private final String displayName;
    private final String description;

    LessonType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
