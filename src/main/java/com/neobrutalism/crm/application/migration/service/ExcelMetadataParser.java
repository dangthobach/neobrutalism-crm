package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.migration.model.SheetType;
import com.neobrutalism.crm.utils.validation.ExcelDimensionValidator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Service for parsing Excel metadata (sheet names, row counts)
 * OPTIMIZED: Uses streaming SAX parser to avoid loading entire file into memory
 */
@Slf4j
@Component
public class ExcelMetadataParser {

    /**
     * Parse Excel file to extract metadata using streaming approach
     *
     * BEFORE: WorkbookFactory.create() loads entire file into memory (1GB file = 1GB memory)
     * AFTER: ExcelDimensionValidator uses SAX streaming (~8MB constant memory)
     *
     * @param inputStream Excel file input stream (will be consumed)
     * @return Excel metadata with sheet names and row counts
     * @throws Exception if unable to parse metadata
     */
    public ExcelMetadata parseMetadata(InputStream inputStream) throws Exception {
        ExcelMetadata metadata = new ExcelMetadata();

        try {
            // Use ExcelDimensionValidator to get all sheet dimensions via streaming
            // This uses SAX parser and does NOT load the entire file into memory
            Map<String, ExcelDimensionValidator.DimensionInfo> allDimensions =
                ExcelDimensionValidator.readAllSheetDimensions(inputStream);

            List<String> sheetNames = new ArrayList<>();
            Map<String, Long> rowCounts = new HashMap<>();

            for (Map.Entry<String, ExcelDimensionValidator.DimensionInfo> entry : allDimensions.entrySet()) {
                String sheetName = entry.getKey();
                ExcelDimensionValidator.DimensionInfo dimensionInfo = entry.getValue();

                // Calculate data rows (excluding header row at index 0)
                // totalRows includes header, so subtract 1 for data rows
                long totalRows = dimensionInfo.getTotalRows();
                long dataRows = Math.max(0, totalRows - 1); // Subtract header row

                // Skip empty sheets (only header or no rows)
                if (dataRows == 0) {
                    log.debug("Skipping empty sheet: {}", sheetName);
                    continue;
                }

                sheetNames.add(sheetName);
                rowCounts.put(sheetName, dataRows);

                log.debug("Sheet '{}': {} total rows, {} data rows (dimension: {}:{})",
                         sheetName, totalRows, dataRows,
                         dimensionInfo.getFirstCellRef(), dimensionInfo.getLastCellRef());
            }

            metadata.setSheetCount(sheetNames.size());
            metadata.setSheetNames(sheetNames);
            metadata.setRowCounts(rowCounts);

            long totalDataRows = rowCounts.values().stream().mapToLong(Long::longValue).sum();
            log.info("Parsed Excel metadata using streaming: {} sheets, {} total data rows",
                     sheetNames.size(), totalDataRows);

            return metadata;

        } catch (Exception e) {
            log.error("Failed to parse Excel metadata using streaming approach", e);
            throw new Exception("Failed to parse Excel metadata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Detect sheet type from sheet name
     */
    public SheetType detectSheetType(String sheetName) {
        try {
            return SheetType.fromSheetName(sheetName);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown sheet name: {}", sheetName);
            return null;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExcelMetadata {
        private int sheetCount;
        private List<String> sheetNames;
        private Map<String, Long> rowCounts;
        
        public Long getRowCount(String sheetName) {
            return rowCounts != null ? rowCounts.getOrDefault(sheetName, 0L) : 0L;
        }
    }
}

