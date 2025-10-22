package com.neobrutalism.crm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Cross-field validation for Organization entity
 * Ensures business rules are met across multiple fields
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OrganizationValidator.class)
@Documented
public @interface ValidOrganization {
    String message() default "Organization validation failed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
