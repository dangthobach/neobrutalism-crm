package com.neobrutalism.crm.application.migration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Migration sheet entity
 */
@Entity
@Table(name = "excel_migration_sheets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSheet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "job_id", nullable = false)
    private UUID jobId;
    
    @Column(name = "sheet_name", nullable = false, length = 100)
    private String sheetName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sheet_type", nullable = false, length = 50)
    private SheetType sheetType;
    
    @Column(name = "total_rows", nullable = false)
    private Long totalRows;
    
    @Column(name = "processed_rows", nullable = false)
    @Builder.Default
    private Long processedRows = 0L;
    
    @Column(name = "valid_rows", nullable = false)
    @Builder.Default
    private Long validRows = 0L;
    
    @Column(name = "invalid_rows", nullable = false)
    @Builder.Default
    private Long invalidRows = 0L;
    
    @Column(name = "skipped_rows", nullable = false)
    @Builder.Default
    private Long skippedRows = 0L;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private SheetStatus status = SheetStatus.PENDING;
    
    @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal progressPercent = BigDecimal.ZERO;
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "last_processed_row", nullable = false)
    @Builder.Default
    private Long lastProcessedRow = 0L;
    
    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}

