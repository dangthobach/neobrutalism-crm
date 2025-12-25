package com.neobrutalism.crm.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unified handler for database constraint violations
 * Converts low-level database exceptions into user-friendly validation exceptions
 *
 * <p>This class provides reusable methods to handle common database integrity violations
 * such as unique constraints, foreign key violations, and null constraints.</p>
 *
 * @see ValidationException
 * @see DataIntegrityViolationException
 */
@Slf4j
@Component
public class DatabaseExceptionHandler {

    // Pattern to extract constraint name from exception message
    private static final Pattern CONSTRAINT_NAME_PATTERN =
        Pattern.compile("constraint [\"']?([^\"'\\s]+)[\"']?", Pattern.CASE_INSENSITIVE);

    private static final Pattern DUPLICATE_KEY_PATTERN =
        Pattern.compile("duplicate key.*violates unique constraint|Unique index or primary key violation",
            Pattern.CASE_INSENSITIVE);

    // Pattern to extract field name from H2 unique constraint violations
    // Example: "CONSTRAINT_INDEX_3 ON PUBLIC.ORGANIZATIONS(CODE NULLS FIRST)"
    private static final Pattern H2_FIELD_PATTERN =
        Pattern.compile("\\w+\\(([A-Z_]+)(?:\\s|\\))", Pattern.CASE_INSENSITIVE);

    /**
     * Handle DataIntegrityViolationException with custom field messages
     *
     * @param ex The database exception
     * @param entityName The name of the entity (e.g., "Organization")
     * @param fieldMessages Map of field/constraint names to user-friendly messages
     * @return ValidationException with appropriate message
     */
    public static ValidationException handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            String entityName,
            Map<String, String> fieldMessages) {

        String exceptionMessage = ex.getMessage();
        String rootCauseMessage = ex.getMostSpecificCause().getMessage();
        String fullMessage = exceptionMessage + " " + rootCauseMessage;

        log.warn("Data integrity violation for {}: {}", entityName, rootCauseMessage);

        // Try to match user-defined field messages
        for (Map.Entry<String, String> entry : fieldMessages.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (fullMessage.toLowerCase().contains(key)) {
                return new ValidationException(entry.getValue());
            }
        }

        // Check for common violation types FIRST before generic constraint handling
        if (DUPLICATE_KEY_PATTERN.matcher(fullMessage).find()) {
            // Try to extract field name from the error message
            String fieldName = extractFieldName(fullMessage);
            if (fieldName != null) {
                return new ValidationException(
                    String.format("%s with this %s already exists. Please use a unique %s.",
                        entityName, fieldName.toLowerCase(), fieldName.toLowerCase())
                );
            }
            return new ValidationException(
                String.format("Duplicate value detected for %s. Please use a unique value.", entityName)
            );
        }

        // Try to extract constraint name and provide generic message
        String constraintName = extractConstraintName(fullMessage);
        if (constraintName != null) {
            return new ValidationException(
                String.format("Data integrity violation: constraint '%s' violated for %s",
                    constraintName, entityName)
            );
        }

        // Generic fallback message
        return new ValidationException(
            String.format("Data integrity violation for %s: %s",
                entityName,
                sanitizeErrorMessage(rootCauseMessage))
        );
    }

    /**
     * Handle unique constraint violation with automatic field detection
     *
     * @param ex The database exception
     * @param entityName The name of the entity
     * @param fieldName The field that should be unique
     * @param fieldValue The duplicate value
     * @return ValidationException with specific message
     */
    public static ValidationException handleUniqueConstraintViolation(
            DataIntegrityViolationException ex,
            String entityName,
            String fieldName,
            Object fieldValue) {

        log.warn("Unique constraint violation for {}.{}: value '{}' already exists",
            entityName, fieldName, fieldValue);

        return new ValidationException(
            String.format("%s with %s '%s' already exists", entityName, fieldName, fieldValue)
        );
    }

    /**
     * Handle foreign key constraint violation
     *
     * @param ex The database exception
     * @param entityName The name of the entity
     * @param referencedEntity The entity being referenced
     * @return ValidationException with appropriate message
     */
    public static ValidationException handleForeignKeyViolation(
            DataIntegrityViolationException ex,
            String entityName,
            String referencedEntity) {

        String message = ex.getMostSpecificCause().getMessage();
        log.warn("Foreign key violation for {}: {}", entityName, message);

        if (message.toLowerCase().contains("delete") || message.toLowerCase().contains("update")) {
            return new ValidationException(
                String.format("Cannot delete or update %s because it is referenced by %s",
                    entityName, referencedEntity)
            );
        }

        return new ValidationException(
            String.format("Referenced %s does not exist", referencedEntity)
        );
    }

    /**
     * Handle null constraint violation
     *
     * @param ex The database exception
     * @param entityName The name of the entity
     * @param fieldName The field that cannot be null
     * @return ValidationException with appropriate message
     */
    public static ValidationException handleNotNullViolation(
            DataIntegrityViolationException ex,
            String entityName,
            String fieldName) {

        log.warn("Not null constraint violation for {}.{}", entityName, fieldName);

        return new ValidationException(
            String.format("Field '%s' is required for %s", fieldName, entityName)
        );
    }

    /**
     * Extract constraint name from exception message
     */
    private static String extractConstraintName(String message) {
        Matcher matcher = CONSTRAINT_NAME_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract field name from exception message
     * Works for H2 and PostgreSQL unique constraint violations
     */
    private static String extractFieldName(String message) {
        // Try H2 pattern first: "ORGANIZATIONS(CODE NULLS FIRST)"
        Matcher matcher = H2_FIELD_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Try to find field name in constraint name (e.g., "uk_organization_code" -> "code")
        String constraintName = extractConstraintName(message);
        if (constraintName != null) {
            // Extract last part after underscore (common naming convention)
            String[] parts = constraintName.toLowerCase().split("_");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }
        }

        return null;
    }

    /**
     * Sanitize error message to remove sensitive database information
     */
    private static String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Unknown database error";
        }

        // Limit message length
        if (message.length() > 200) {
            message = message.substring(0, 200) + "...";
        }

        // Remove SQL fragments (basic sanitization)
        message = message.replaceAll("SQL \\[.*?\\]", "");
        message = message.replaceAll("\\[.*?\\]", "");

        return message.trim();
    }

    /**
     * Check if exception is a unique constraint violation
     */
    public static boolean isUniqueConstraintViolation(DataIntegrityViolationException ex) {
        String message = ex.getMessage() + " " + ex.getMostSpecificCause().getMessage();
        return DUPLICATE_KEY_PATTERN.matcher(message).find();
    }

    /**
     * Check if exception is a foreign key violation
     */
    public static boolean isForeignKeyViolation(DataIntegrityViolationException ex) {
        String message = ex.getMessage().toLowerCase() + " " +
                        ex.getMostSpecificCause().getMessage().toLowerCase();
        return message.contains("foreign key") || message.contains("referential integrity");
    }

    /**
     * Check if exception is a not-null violation
     */
    public static boolean isNotNullViolation(DataIntegrityViolationException ex) {
        String message = ex.getMessage().toLowerCase() + " " +
                        ex.getMostSpecificCause().getMessage().toLowerCase();
        return message.contains("not null") || message.contains("null not allowed");
    }
}
