package com.neobrutalism.crm.common.validation;

import com.neobrutalism.crm.common.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Centralized validation service
 * Provides common validation patterns and reduces boilerplate
 */
@Service
public class ValidationService {

    private final Validator validator;

    public ValidationService(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validate entity and throw ValidationException if invalid
     */
    public <T> void validateEntity(T entity) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<T> violation : violations) {
                message.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; ");
            }
            throw new ValidationException(message.toString());
        }
    }

    /**
     * Validate entity for specific groups
     */
    public <T> void validateEntity(T entity, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity, groups);
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<T> violation : violations) {
                message.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; ");
            }
            throw new ValidationException(message.toString());
        }
    }

    /**
     * Validate property of an entity
     */
    public <T> void validateProperty(T entity, String propertyName) {
        Set<ConstraintViolation<T>> violations = validator.validateProperty(entity, propertyName);
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder("Validation failed for property " + propertyName + ": ");
            for (ConstraintViolation<T> violation : violations) {
                message.append(violation.getMessage()).append("; ");
            }
            throw new ValidationException(message.toString());
        }
    }

    /**
     * Validate property of an entity for specific groups
     */
    public <T> void validateProperty(T entity, String propertyName, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = validator.validateProperty(entity, propertyName, groups);
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder("Validation failed for property " + propertyName + ": ");
            for (ConstraintViolation<T> violation : violations) {
                message.append(violation.getMessage()).append("; ");
            }
            throw new ValidationException(message.toString());
        }
    }
}
