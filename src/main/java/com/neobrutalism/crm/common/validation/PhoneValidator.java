package com.neobrutalism.crm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Phone number validator
 */
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$"
    );

    private boolean international;

    @Override
    public void initialize(ValidPhone constraintAnnotation) {
        this.international = constraintAnnotation.international();
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // Use @NotNull or @NotBlank for required validation
        }

        return PHONE_PATTERN.matcher(phone).matches();
    }
}
