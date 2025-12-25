package com.neobrutalism.crm.application.migration.dto;

import com.neobrutalism.crm.application.migration.model.SheetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Progress information for a single sheet
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressInfo {
    
    private UUID sheetId;
    private String sheetName;
    private long totalRows;
    private long processedRows;
    private long validRows;
    private long invalidRows;
    private long skippedRows;
    private BigDecimal progressPercent;
    private SheetStatus status;
    private Duration elapsedTime;
    private Duration estimatedRemaining;
    private Instant lastHeartbeat;
}

