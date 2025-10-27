package com.neobrutalism.crm.common.exception;

import com.neobrutalism.crm.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all controllers
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle resource not found exceptions (404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStateTransitionException(InvalidStateTransitionException ex) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode(), ex.getValidationErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", "VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(jakarta.validation.ValidationException.class)
    public ResponseEntity<ApiResponse<String>> handleJakartaValidationException(
            jakarta.validation.ValidationException ex) {
        log.warn("Entity validation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "ENTITY_VALIDATION_ERROR"));
    }

    /**
     * Handle Jakarta constraint violation exceptions (400)
     * Occurs during entity validation with custom validators
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            jakarta.validation.ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Constraint violation: {}", errors);

        // If there's only one error, return a simple message
        if (errors.size() == 1) {
            String errorMessage = errors.values().iterator().next();
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(errorMessage, "VALIDATION_ERROR"));
        }

        // Multiple errors - return the map
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", "VALIDATION_ERROR", errors));
    }

    /**
     * Handle database integrity constraint violations
     * This is a fallback handler if the exception is not caught by service layer
     * Returns 409 CONFLICT for duplicate/unique constraints
     * Returns 400 BAD_REQUEST for other constraint violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {
        log.warn("Database integrity violation: {}", ex.getMessage());

        // Check if it's a unique constraint violation (duplicate)
        boolean isUniqueViolation = DatabaseExceptionHandler.isUniqueConstraintViolation(ex);

        // Extract entity name from the exception message (e.g., "ORGANIZATIONS" -> "Organization")
        String entityName = extractEntityName(ex.getMessage());

        // Try to provide a user-friendly message
        ValidationException validationException = DatabaseExceptionHandler.handleDataIntegrityViolation(
            ex, entityName, Map.of()
        );

        // Return 409 CONFLICT for duplicates, 400 BAD_REQUEST for other violations
        HttpStatus status = isUniqueViolation ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        String errorCode = isUniqueViolation ? "DUPLICATE_RESOURCE" : "DATA_INTEGRITY_VIOLATION";

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(validationException.getMessage(), errorCode));
    }

    /**
     * Extract entity name from database exception message
     * Example: "ON PUBLIC.ORGANIZATIONS(CODE" -> "Organization"
     */
    private String extractEntityName(String message) {
        if (message == null) {
            return "Entity";
        }

        // Try to find table name pattern: "ON [SCHEMA.]TABLE_NAME"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "ON\\s+(?:[A-Z_]+\\.)?([A-Z_]+)\\s*\\(",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            String tableName = matcher.group(1);
            // Convert "ORGANIZATIONS" to "Organization"
            // Remove trailing 'S' if it's a plural table name
            if (tableName.endsWith("S") && tableName.length() > 1) {
                tableName = tableName.substring(0, tableName.length() - 1);
            }
            // Capitalize first letter, lowercase the rest
            return tableName.charAt(0) + tableName.substring(1).toLowerCase();
        }

        return "Entity";
    }

    /**
     * Handle optimistic locking failures (409)
     * Occurs when an entity is modified by another transaction
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                    "The resource has been modified by another user. Please refresh and try again.",
                    "OPTIMISTIC_LOCK_ERROR"
                ));
    }

    /**
     * Handle illegal argument exceptions (400)
     * Occurs when invalid arguments are passed to methods
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_ARGUMENT"));
    }

    /**
     * Handle illegal state exceptions (400)
     * Occurs when an operation is performed in an invalid state
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_STATE"));
    }

    /**
     * Handle business logic exceptions (422 Unprocessable Entity)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business logic error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    /**
     * Handle base application exceptions (500)
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        log.error("Application error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    /**
     * Handle transaction system exceptions (may wrap validation errors)
     */
    @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
    public ResponseEntity<ApiResponse<?>> handleTransactionSystemException(
            org.springframework.transaction.TransactionSystemException ex) {

        // Check if it's a wrapped ConstraintViolationException
        Throwable cause = ex.getRootCause();
        if (cause instanceof jakarta.validation.ConstraintViolationException) {
            jakarta.validation.ConstraintViolationException cve =
                (jakarta.validation.ConstraintViolationException) cause;

            Map<String, String> errors = new HashMap<>();
            cve.getConstraintViolations().forEach(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String errorMessage = violation.getMessage();
                errors.put(fieldName, errorMessage);
            });

            log.warn("Constraint violation during transaction commit: {}", errors);

            // If there's only one error, return a simple message
            if (errors.size() == 1) {
                String errorMessage = errors.values().iterator().next();
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(errorMessage, "VALIDATION_ERROR"));
            }

            // Multiple errors - return the map
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed", "VALIDATION_ERROR", errors));
        }

        // If not a validation exception, treat as generic error
        log.error("Transaction system error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Transaction error occurred", "TRANSACTION_ERROR"));
    }

    /**
     * Handle all other unexpected exceptions (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", "INTERNAL_ERROR"));
    }
}
