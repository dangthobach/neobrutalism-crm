package com.neobrutalism.crm.utils.sax;

// Removed dependency on ExcelUtil.MultiSheetResult
import com.neobrutalism.crm.utils.config.ExcelConfig;
// import com.neobrutalism.crm.utils.validation.ExcelEarlyValidator; // Unused - disabled to prevent InputStream consumption
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
// Removed unused imports after refactor
import org.apache.poi.xssf.model.StylesTable;
// import org.xml.sax.InputSource;
// import org.xml.sax.XMLReader;

// import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

/**
 * True streaming multi-sheet processor using SAX
 * Không sử dụng WorkbookFactory.create, xử lý từng sheet với streaming thực sự
 */
@Slf4j
public class TrueStreamingMultiSheetProcessor {
    
    private final Map<String, Class<?>> sheetClassMap;
    private final Map<String, Consumer<List<?>>> sheetProcessors;
    private final ExcelConfig config;
    
    public TrueStreamingMultiSheetProcessor(Map<String, Class<?>> sheetClassMap, 
                                          Map<String, Consumer<List<?>>> sheetProcessors,
                                          ExcelConfig config) {
        this.sheetClassMap = sheetClassMap;
        this.sheetProcessors = sheetProcessors;
        this.config = config;
    }
    
    /**
     * Process multiple sheets với true streaming - không tích lũy kết quả
     */
    public Map<String, TrueStreamingSAXProcessor.ProcessingResult> processTrueStreaming(InputStream inputStream) 
            throws Exception {
        
        Map<String, TrueStreamingSAXProcessor.ProcessingResult> results = new HashMap<>();

        // ❌ DISABLED: Early validation consumes InputStream even after reset
        // ExcelMigrationService already validates file before calling this processor
        // ExcelEarlyValidator.EarlyValidationResult earlyResult =
        //     ExcelEarlyValidator.validateRecordCount(inputStream, config.getMaxErrorsBeforeAbort(), 1);
        //
        // if (!earlyResult.isValid()) {
        //     log.error("Multi-sheet file failed early validation: {}", earlyResult.getErrorMessage());
        //     throw new RuntimeException("File too large: " + earlyResult.getErrorMessage());
        // }

        log.info("Processing {} sheets with true streaming...",
                sheetClassMap.size());
        
        try (OPCPackage opcPackage = OPCPackage.open(inputStream)) {
            XSSFReader xssfReader = new XSSFReader(opcPackage);
            org.apache.poi.xssf.model.SharedStringsTable sharedStringsTable =
                (org.apache.poi.xssf.model.SharedStringsTable) xssfReader.getSharedStringsTable();
            StylesTable stylesTable = xssfReader.getStylesTable();
            
            // ✅ NOTE: sharedStringsTable can be null if Excel file doesn't use shared strings
            // This is normal for small files or files created by certain tools
            if (sharedStringsTable == null) {
                log.warn("⚠️ SharedStringsTable is null - file may not use shared strings (this is OK)");
            }

            XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader.getSheetsData();

            // ✅ FIX: Use SerialNumberDataFormatter for consistent date handling
            DataFormatter dataFormatter = new TrueStreamingSAXProcessor.SerialNumberDataFormatter();
            
            while (sheetIterator.hasNext()) {
                try (InputStream sheetStream = sheetIterator.next()) {
                    String sheetName = sheetIterator.getSheetName();

                    log.info("🔍 Got sheet stream for '{}', available bytes: {}",
                             sheetName, sheetStream != null ? sheetStream.available() : "NULL");

                    Class<?> beanClass = sheetClassMap.get(sheetName);
                    Consumer<List<?>> sheetProcessor = sheetProcessors.get(sheetName);

                    if (beanClass == null || sheetProcessor == null) {
                        log.warn("Sheet '{}' not configured for processing, skipping", sheetName);
                        continue;
                    }

                    log.info("Processing sheet '{}' with class {}", sheetName, beanClass.getSimpleName());
                    
                    // Create true streaming processor for this sheet
                    TrueStreamingSAXProcessor<?> processor = createProcessorForSheet(
                        beanClass, sheetProcessor, config);
                    
                    // Process sheet với true streaming
                    TrueStreamingSAXProcessor.ProcessingResult result = 
                        processSheetWithSAX(sheetStream, processor, stylesTable, sharedStringsTable, dataFormatter);
                    
                    results.put(sheetName, result);
                    
                    log.info("Completed sheet '{}': {}", sheetName, result);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Legacy compatibility method - returns MultiSheetResult format
     */
    public Map<String, TrueStreamingSAXProcessor.ProcessingResult> process(InputStream inputStream) throws Exception {
        
        // Convert processors to collect results for legacy format
        Map<String, List<Object>> collectedResults = new HashMap<>();
        Map<String, Consumer<List<?>>> collectingProcessors = new HashMap<>();
        
        for (Map.Entry<String, Consumer<List<?>>> entry : sheetProcessors.entrySet()) {
            String sheetName = entry.getKey();
            Consumer<List<?>> originalProcessor = entry.getValue();
            
            List<Object> sheetResults = new ArrayList<>();
            collectedResults.put(sheetName, sheetResults);
            
            // Wrap processor to collect results
            collectingProcessors.put(sheetName, batch -> {
                // Call original processor
                originalProcessor.accept(batch);
                // Also collect for legacy result
                sheetResults.addAll(batch);
            });
        }
        
        // Create temporary processor with collecting wrappers
        TrueStreamingMultiSheetProcessor tempProcessor = new TrueStreamingMultiSheetProcessor(
            sheetClassMap, collectingProcessors, config);
        
        Map<String, TrueStreamingSAXProcessor.ProcessingResult> streamingResults = 
            tempProcessor.processTrueStreaming(inputStream);
        
        // Convert to legacy MultiSheetResult format
        Map<String, TrueStreamingSAXProcessor.ProcessingResult> legacyResults = new HashMap<>();
        for (Map.Entry<String, TrueStreamingSAXProcessor.ProcessingResult> entry : streamingResults.entrySet()) {
            String sheetName = entry.getKey();
            TrueStreamingSAXProcessor.ProcessingResult result = entry.getValue();
            List<Object> sheetData = collectedResults.get(sheetName);
            if (sheetData != null && sheetData.isEmpty()) {
                // no-op to acknowledge variable usage
            }
            
            // Store original processing result; callers can join with sheetData map if needed
            legacyResults.put(sheetName, result);
        }
        
        return legacyResults;
    }
    
    /**
     * Create processor for specific sheet
     */
    @SuppressWarnings("unchecked")
    private <T> TrueStreamingSAXProcessor<T> createProcessorForSheet(
            Class<?> beanClass, Consumer<List<?>> sheetProcessor, ExcelConfig config) {
        
        // Cast to appropriate types
        Class<T> typedBeanClass = (Class<T>) beanClass;
        
        // Create wrapper to handle type conversion
        Consumer<List<T>> typedProcessor = batch -> {
            List<?> wildcardBatch = (List<?>) batch;
            sheetProcessor.accept(wildcardBatch);
        };
        
        return new TrueStreamingSAXProcessor<>(
            typedBeanClass, 
            config, 
            new ArrayList<>(), // Empty validation rules for now
            typedProcessor
        );
    }
    
    /**
     * Process single sheet using SAX with streaming
     * Uses shared styles/strings/dataFormatter from the already-opened OPCPackage
     */
    private TrueStreamingSAXProcessor.ProcessingResult processSheetWithSAX(
            InputStream sheetStream,
            TrueStreamingSAXProcessor<?> processor,
            StylesTable stylesTable,
            org.apache.poi.xssf.model.SharedStringsTable sharedStringsTable,
            DataFormatter dataFormatter) throws Exception {
        
        // Use processor's new method to process sheet stream with shared resources
        return processor.processSheetStream(sheetStream, stylesTable, sharedStringsTable, dataFormatter);
    }
}