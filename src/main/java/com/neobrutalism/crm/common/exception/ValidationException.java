package com.neobrutalism.crm.common.exception;

import java.util.Map;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends BaseException {

    private final Map<String, String> validationErrors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
        this.validationErrors = null;
    }

    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message, "VALIDATION_ERROR");
        this.validationErrors = validationErrors;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}
