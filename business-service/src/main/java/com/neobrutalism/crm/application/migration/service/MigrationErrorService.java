package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.migration.dto.MigrationErrorResponse;
import com.neobrutalism.crm.application.migration.entity.MigrationError;
import com.neobrutalism.crm.application.migration.model.MigrationSheet;
import com.neobrutalism.crm.application.migration.repository.MigrationErrorEntityRepository;
import com.neobrutalism.crm.application.migration.repository.MigrationErrorRepository;
import com.neobrutalism.crm.application.migration.repository.MigrationSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for retrieving migration errors
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationErrorService {
    
    private final MigrationErrorRepository errorRepository; // Native queries
    private final MigrationErrorEntityRepository errorEntityRepository; // JPA repository
    private final MigrationSheetRepository sheetRepository;
    
    /**
     * Get errors for a specific sheet
     */
    @Transactional(readOnly = true)
    public MigrationErrorResponse getSheetErrors(UUID sheetId, int page, int size) {
        MigrationSheet sheet = sheetRepository.findById(sheetId)
            .orElseThrow(() -> new IllegalArgumentException("Sheet not found: " + sheetId));
        
        // Use JPA repository for cleaner code
        long totalErrors = errorEntityRepository.countBySheetId(sheetId);
        
        Pageable pageable = PageRequest.of(page, size);
        List<MigrationError> errors = errorEntityRepository.findErrorsBySheetId(sheetId);
        
        // Apply pagination manually (or use Pageable if repository supports it)
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), errors.size());
        List<MigrationError> pagedErrors = errors.subList(start, end);
        
        List<MigrationErrorResponse.ErrorDetail> errorDetails = pagedErrors.stream()
            .map(this::mapToErrorDetail)
            .toList();
        
        return MigrationErrorResponse.builder()
            .jobId(sheet.getJobId())
            .sheetId(sheetId)
            .sheetName(sheet.getSheetName())
            .totalErrors(totalErrors)
            .errors(errorDetails)
            .build();
    }
    
    /**
     * Get all errors for a job (across all sheets)
     */
    @Transactional(readOnly = true)
    public List<MigrationErrorResponse> getJobErrors(UUID jobId, int page, int size) {
        long totalErrors = errorRepository.countErrorsByJobId(jobId);
        
        Pageable pageable = PageRequest.of(page, size);
        List<Object[]> errorRows = errorRepository.findErrorsByJobId(
            jobId,
            pageable.getPageSize(),
            (int) pageable.getOffset()
        );
        
        // Group errors by sheet
        var errorsBySheet = new java.util.HashMap<UUID, List<MigrationErrorResponse.ErrorDetail>>();
        
        for (Object[] row : errorRows) {
            UUID sheetId = UUID.fromString((String) row[0]); // First column is sheet_id
            MigrationErrorResponse.ErrorDetail error = mapToErrorDetailJob(row);
            
            errorsBySheet.computeIfAbsent(sheetId, k -> new ArrayList<>()).add(error);
        }
        
        // Build responses for each sheet
        List<MigrationErrorResponse> responses = new ArrayList<>();
        List<MigrationSheet> sheets = sheetRepository.findByJobId(jobId);
        
        for (MigrationSheet sheet : sheets) {
            List<MigrationErrorResponse.ErrorDetail> sheetErrors = 
                errorsBySheet.getOrDefault(sheet.getId(), new ArrayList<>());
            
            responses.add(MigrationErrorResponse.builder()
                .jobId(jobId)
                .sheetId(sheet.getId())
                .sheetName(sheet.getSheetName())
                .totalErrors(sheetErrors.size())
                .errors(sheetErrors)
                .build());
        }
        
        return responses;
    }
    
    private MigrationErrorResponse.ErrorDetail mapToErrorDetail(MigrationError error) {
        return MigrationErrorResponse.ErrorDetail.builder()
            .id(error.getId())
            .rowNumber(error.getRowNumber())
            .batchNumber(error.getBatchNumber())
            .errorCode(error.getErrorCode())
            .errorMessage(error.getErrorMessage())
            .validationRule(error.getValidationRule())
            .errorData(error.getErrorData())
            .createdAt(error.getCreatedAt())
            .build();
    }
    
    private MigrationErrorResponse.ErrorDetail mapToErrorDetail(Object[] row) {
        // Map based on query result structure for sheet errors (native query):
        // id::text, row_number, batch_number, error_code, error_message, validation_rule, error_data, created_at
        return MigrationErrorResponse.ErrorDetail.builder()
            .id(UUID.fromString((String) row[0]))
            .rowNumber(((Number) row[1]).longValue())
            .batchNumber(row[2] != null ? ((Number) row[2]).intValue() : null)
            .errorCode((String) row[3])
            .errorMessage((String) row[4])
            .validationRule((String) row[5])
            .errorData((String) row[6]) // JSONB as string
            .createdAt(row[7] != null ? ((java.sql.Timestamp) row[7]).toInstant() : null)
            .build();
    }
    
    private MigrationErrorResponse.ErrorDetail mapToErrorDetailJob(Object[] row) {
        // Map based on query result structure for job errors (native query):
        // sheet_id::text, id::text, row_number, batch_number, error_code, error_message, validation_rule, error_data, created_at
        return MigrationErrorResponse.ErrorDetail.builder()
            .id(UUID.fromString((String) row[1]))
            .rowNumber(((Number) row[2]).longValue())
            .batchNumber(row[3] != null ? ((Number) row[3]).intValue() : null)
            .errorCode((String) row[4])
            .errorMessage((String) row[5])
            .validationRule((String) row[6])
            .errorData((String) row[7]) // JSONB as string
            .createdAt(row[8] != null ? ((java.sql.Timestamp) row[8]).toInstant() : null)
            .build();
    }
}

