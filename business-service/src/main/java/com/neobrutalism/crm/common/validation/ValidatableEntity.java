package com.neobrutalism.crm.common.validation;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base entity with automatic validation on persist/update
 */
@MappedSuperclass
public abstract class ValidatableEntity {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    /**
     * Validate entity before persisting
     */
    @PrePersist
    @PreUpdate
    protected void validateEntity() {
        Set<ConstraintViolation<ValidatableEntity>> violations = validator.validate(this);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            throw new jakarta.validation.ValidationException("Validation failed: " + errors);
        }
    }

    /**
     * Manual validation method
     */
    public void validate() {
        validateEntity();
    }
}
