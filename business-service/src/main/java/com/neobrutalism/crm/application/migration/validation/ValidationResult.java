package com.neobrutalism.crm.application.migration.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validation for a single record
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    @Builder.Default
    private boolean valid = true;
    
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();
    
    public void addError(String code, String message) {
        this.valid = false;
        this.errors.add(ValidationError.builder()
            .code(code)
            .message(message)
            .build());
    }
    
    public void addError(String code, String message, String field) {
        this.valid = false;
        this.errors.add(ValidationError.builder()
            .code(code)
            .message(message)
            .field(field)
            .build());
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String code;
        private String message;
        private String field;
        private String validationRule;
    }
}

