package com.neobrutalism.crm.common.validation;

import com.neobrutalism.crm.domain.organization.model.Organization;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Cross-field validator for Organization
 */
public class OrganizationValidator implements ConstraintValidator<ValidOrganization, Organization> {

    @Override
    public boolean isValid(Organization org, ConstraintValidatorContext context) {
        if (org == null) {
            return true;
        }

        boolean isValid = true;

        // Rule 1: If email is provided, it must be properly formatted (handled by @ValidEmail)
        // Rule 2: If website is provided, it must be properly formatted (handled by @ValidUrl)
        // Rule 3: Organization code must not contain special characters except dash and underscore
        if (org.getCode() != null && !org.getCode().matches("^[A-Z0-9_-]+$")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Organization code must contain only uppercase letters, numbers, dashes, and underscores"
            ).addPropertyNode("code").addConstraintViolation();
            isValid = false;
        }

        // Rule 4: Name and code must be different
        if (org.getName() != null && org.getCode() != null &&
                org.getName().equalsIgnoreCase(org.getCode())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Organization name and code must be different"
            ).addPropertyNode("code").addConstraintViolation();
            isValid = false;
        }

        // Rule 5: If ACTIVE status, must have at least email or phone
        if (org.getStatus() != null && org.getStatus().toString().equals("ACTIVE")) {
            if ((org.getEmail() == null || org.getEmail().trim().isEmpty()) &&
                    (org.getPhone() == null || org.getPhone().trim().isEmpty())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Active organizations must have at least an email or phone number"
                ).addConstraintViolation();
                isValid = false;
            }
        }

        return isValid;
    }
}
