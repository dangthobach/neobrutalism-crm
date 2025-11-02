package com.neobrutalism.crm.common.enums;

public enum AchievementType {
    COURSE_COMPLETION("Course Completion", "Complete a course"),
    QUIZ_MASTER("Quiz Master", "Perfect quiz scores"),
    STREAK("Learning Streak", "Consecutive days of learning"),
    SPEED_LEARNER("Speed Learner", "Complete course quickly"),
    PERFECT_SCORE("Perfect Score", "100% on all quizzes"),
    CUSTOM("Custom", "Custom achievement");

    private final String displayName;
    private final String description;

    AchievementType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
