package com.neobrutalism.crm.common.util;

import java.util.Set;

/**
 * Utility for validating sort parameters to prevent injection attacks
 */
public class SortValidator {

    private static final Set<String> ALLOWED_ORGANIZATION_SORT_FIELDS = Set.of(
            "id", "name", "code", "status", "createdAt", "updatedAt", "email", "phone"
    );

    /**
     * Validate sort field for Organization entity
     */
    public static String validateOrganizationSortField(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return "id"; // Default sort field
        }

        String trimmedField = sortBy.trim();
        if (ALLOWED_ORGANIZATION_SORT_FIELDS.contains(trimmedField)) {
            return trimmedField;
        }

        throw new IllegalArgumentException("Invalid sort field: " + sortBy + 
                ". Allowed fields: " + ALLOWED_ORGANIZATION_SORT_FIELDS);
    }

    /**
     * Generic method to validate sort field against allowed fields
     */
    public static String validateSortField(String sortBy, Set<String> allowedFields, String defaultField) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return defaultField;
        }

        String trimmedField = sortBy.trim();
        if (allowedFields.contains(trimmedField)) {
            return trimmedField;
        }

        throw new IllegalArgumentException("Invalid sort field: " + sortBy + 
                ". Allowed fields: " + allowedFields);
    }
}
