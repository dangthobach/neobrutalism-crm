package com.neobrutalism.crm.domain.command.model;

/**
 * Categories for grouping commands in the command palette.
 */
public enum CommandCategory {
    CUSTOMER("Customer Management"),
    CONTACT("Contact Management"),
    TASK("Task Management"),
    ACTIVITY("Activity Management"),
    USER("User Management"),
    ORGANIZATION("Organization Management"),
    REPORT("Reporting"),
    SETTINGS("Settings"),
    NAVIGATION("Navigation"),
    SEARCH("Search");

    private final String displayName;

    CommandCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
