package com.neobrutalism.crm.application.migration.validation;

/**
 * Interface for sheet-specific validators
 */
public interface SheetValidator<T> {
    
    /**
     * Validate a single record
     * 
     * @param dto The DTO to validate
     * @param rowNumber Row number in Excel (1-based)
     * @return Validation result
     */
    ValidationResult validate(T dto, int rowNumber);
    
    /**
     * Get the sheet type this validator handles
     */
    String getSheetType();
}

