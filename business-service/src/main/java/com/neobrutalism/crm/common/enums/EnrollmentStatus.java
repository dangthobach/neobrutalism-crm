package com.neobrutalism.crm.common.enums;

public enum EnrollmentStatus {
    ACTIVE("Active", "Student is actively learning"),
    COMPLETED("Completed", "Course completed successfully"),
    DROPPED("Dropped", "Student dropped the course"),
    EXPIRED("Expired", "Enrollment has expired"),
    SUSPENDED("Suspended", "Enrollment temporarily suspended");

    private final String displayName;
    private final String description;

    EnrollmentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public boolean isActive() { return this == ACTIVE; }
    public boolean isCompleted() { return this == COMPLETED; }
}
