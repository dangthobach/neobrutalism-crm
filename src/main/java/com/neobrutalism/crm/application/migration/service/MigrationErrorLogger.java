package com.neobrutalism.crm.application.migration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobrutalism.crm.application.migration.entity.MigrationError;
import com.neobrutalism.crm.application.migration.repository.MigrationErrorEntityRepository;
import com.neobrutalism.crm.application.migration.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for logging migration errors to excel_migration_errors table
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationErrorLogger {
    
    private final MigrationErrorEntityRepository errorRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Log validation errors to excel_migration_errors table
     * REQUIRES_NEW ensures errors are committed immediately, independent of parent transaction
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void logValidationErrors(UUID sheetId, Long rowNumber, Integer batchNumber,
                                   ValidationResult validationResult) {
        if (validationResult == null || validationResult.isValid()) {
            return;
        }
        
        List<MigrationError> errors = new ArrayList<>();
        
        for (ValidationResult.ValidationError error : validationResult.getErrors()) {
            try {
                String errorDataJson = objectMapper.writeValueAsString(error);
                
                MigrationError migrationError = MigrationError.builder()
                    .sheetId(sheetId)
                    .rowNumber(rowNumber)
                    .batchNumber(batchNumber)
                    .errorCode(error.getCode())
                    .errorMessage(error.getMessage())
                    .errorData(errorDataJson)
                    .validationRule(error.getValidationRule())
                    .build();
                
                errors.add(migrationError);
            } catch (Exception e) {
                log.error("Failed to serialize error data for row {} in sheet {}", rowNumber, sheetId, e);
            }
        }
        
        if (!errors.isEmpty()) {
            errorRepository.saveAll(errors);
            log.debug("Logged {} validation errors for row {} in sheet {}", 
                     errors.size(), rowNumber, sheetId);
        }
    }
    
    /**
     * Log processing error (non-validation errors)
     * REQUIRES_NEW ensures errors are committed immediately, independent of parent transaction
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void logProcessingError(UUID sheetId, Long rowNumber, Integer batchNumber,
                                  String errorCode, String errorMessage, Throwable throwable) {
        try {
            String errorDataJson = objectMapper.writeValueAsString(
                java.util.Map.of(
                    "exception", throwable.getClass().getName(),
                    "message", throwable.getMessage(),
                    "stackTrace", getStackTrace(throwable)
                )
            );
            
            MigrationError error = MigrationError.builder()
                .sheetId(sheetId)
                .rowNumber(rowNumber)
                .batchNumber(batchNumber)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .errorData(errorDataJson)
                .build();
            
            errorRepository.save(error);
            log.debug("Logged processing error for row {} in sheet {}", rowNumber, sheetId);
        } catch (Exception e) {
            log.error("Failed to log processing error for row {} in sheet {}", rowNumber, sheetId, e);
        }
    }
    
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}

