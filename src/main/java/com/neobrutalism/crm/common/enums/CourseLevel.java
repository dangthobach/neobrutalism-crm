package com.neobrutalism.crm.common.enums;

/**
 * Course difficulty level enumeration
 */
public enum CourseLevel {
    /**
     * Beginner level - No prerequisites
     */
    BEGINNER("Beginner", "No prior knowledge required", 0),

    /**
     * Intermediate level - Basic knowledge required
     */
    INTERMEDIATE("Intermediate", "Basic knowledge required", 1),

    /**
     * Advanced level - Strong foundation required
     */
    ADVANCED("Advanced", "Strong foundation required", 2),

    /**
     * Expert level - Professional experience required
     */
    EXPERT("Expert", "Professional experience required", 3);

    private final String displayName;
    private final String description;
    private final int level;

    CourseLevel(String displayName, String description, int level) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Check if this level is higher than another
     */
    public boolean isHigherThan(CourseLevel other) {
        return this.level > other.level;
    }
}
