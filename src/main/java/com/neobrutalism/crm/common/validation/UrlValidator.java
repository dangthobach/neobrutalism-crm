package com.neobrutalism.crm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * URL validator
 */
public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    private boolean requireProtocol;
    private List<String> allowedProtocols;

    @Override
    public void initialize(ValidUrl constraintAnnotation) {
        this.requireProtocol = constraintAnnotation.requireProtocol();
        this.allowedProtocols = Arrays.asList(constraintAnnotation.protocols());
    }

    @Override
    public boolean isValid(String urlString, ConstraintValidatorContext context) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return true; // Use @NotNull or @NotBlank for required validation
        }

        try {
            // Try to parse as URL
            URL url = new URL(urlString);

            // Check protocol if required
            if (requireProtocol && url.getProtocol() == null) {
                return false;
            }

            // Check if protocol is allowed
            if (url.getProtocol() != null && !allowedProtocols.contains(url.getProtocol())) {
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            // Try with protocol if not required
            if (!requireProtocol && !urlString.startsWith("http")) {
                try {
                    new URL("https://" + urlString);
                    return true;
                } catch (MalformedURLException ex) {
                    return false;
                }
            }
            return false;
        }
    }
}
