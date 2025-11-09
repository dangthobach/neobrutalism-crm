package com.neobrutalism.crm.application.migration.dto;

import com.neobrutalism.crm.application.migration.model.MigrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Overall progress information for a migration job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobProgressInfo {
    
    private UUID jobId;
    private String fileName;
    private int totalSheets;
    private long totalRows;
    private long processedRows;
    private BigDecimal overallProgress;
    private List<ProgressInfo> sheets;
    private MigrationStatus status;
}

