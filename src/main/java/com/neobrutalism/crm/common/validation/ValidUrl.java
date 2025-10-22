package com.neobrutalism.crm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.hibernate.validator.constraints.URL;

import java.lang.annotation.*;

/**
 * URL validation with optional protocol requirement
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UrlValidator.class)
@Documented
public @interface ValidUrl {
    String message() default "Invalid URL format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * Require protocol (http/https)
     */
    boolean requireProtocol() default false;

    /**
     * Allowed protocols
     */
    String[] protocols() default {"http", "https"};
}
