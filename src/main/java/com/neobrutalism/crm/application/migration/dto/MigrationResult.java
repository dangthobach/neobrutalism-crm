package com.neobrutalism.crm.application.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of migration procedure execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationResult {

    private Integer totalProcessed;
    private Integer migratedCount;
    private Integer duplicateCount;
    private Integer errorCount;
    private Integer warningCount;

    /**
     * Calculate migration success rate
     */
    public double getSuccessRate() {
        if (totalProcessed == null || totalProcessed == 0) {
            return 0.0;
        }
        return (migratedCount != null ? migratedCount : 0) * 100.0 / totalProcessed;
    }

    /**
     * Check if migration was successful
     */
    public boolean isSuccessful() {
        return migratedCount != null && migratedCount > 0;
    }

    /**
     * Get total records that had issues (duplicates + errors)
     */
    public int getTotalIssues() {
        return (duplicateCount != null ? duplicateCount : 0) +
               (errorCount != null ? errorCount : 0);
    }
}
