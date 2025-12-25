package com.neobrutalism.crm.application.migration.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for migration errors
 */
@Entity
@Table(name = "excel_migration_errors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationError {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "sheet_id", nullable = false)
    private UUID sheetId;
    
    @Column(name = "row_number", nullable = false)
    private Long rowNumber;
    
    @Column(name = "batch_number")
    private Integer batchNumber;
    
    @Column(name = "error_code", nullable = false, length = 50)
    private String errorCode;
    
    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "error_data", columnDefinition = "JSONB")
    private String errorData; // JSON string
    
    @Column(name = "validation_rule", length = 100)
    private String validationRule;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

