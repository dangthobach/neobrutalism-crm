package com.neobrutalism.crm.common.enums;

public enum QuestionType {
    MULTIPLE_CHOICE("Multiple Choice", "Select one correct answer"),
    TRUE_FALSE("True/False", "True or false question"),
    SHORT_ANSWER("Short Answer", "Brief text answer"),
    ESSAY("Essay", "Long form text answer");

    private final String displayName;
    private final String description;

    QuestionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
