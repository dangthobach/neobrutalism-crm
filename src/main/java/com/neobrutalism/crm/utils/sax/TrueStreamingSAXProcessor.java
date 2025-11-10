package com.neobrutalism.crm.utils.sax;

import com.neobrutalism.crm.utils.ExcelColumn;
import com.neobrutalism.crm.utils.config.ExcelConfig;
import com.neobrutalism.crm.utils.converter.TypeConverter;
import com.neobrutalism.crm.utils.reflection.MethodHandleMapper;
import com.neobrutalism.crm.utils.validation.ValidationRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * True streaming SAX processor - không tích lũy kết quả trong memory
 * Đẩy từng batch trực tiếp vào batchProcessor để xử lý ngay
 */
@Slf4j
public class TrueStreamingSAXProcessor<T> {
    
    private final Class<T> beanClass;
    private final ExcelConfig config;
    private final List<ValidationRule> validationRules;
    private final TypeConverter typeConverter;
    private final Consumer<List<T>> batchProcessor;
    private final MethodHandleMapper<T> methodHandleMapper;

    // Cache for ExcelColumn annotations per field name
    private final Map<String, ExcelColumn> fieldAnnotationCache = new ConcurrentHashMap<>();

    // ✅ Performance: Cache field types to avoid repeated lookups
    private final Map<String, Class<?>> fieldTypeCache = new ConcurrentHashMap<>();

    // Statistics
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final long startTime;

    // ✅ Error tracking: Store validation errors for reporting
    private final List<ValidationError> validationErrors = new ArrayList<>();
    
    public TrueStreamingSAXProcessor(Class<T> beanClass, ExcelConfig config, 
                                   List<ValidationRule> validationRules, 
                                   Consumer<List<T>> batchProcessor) {
        this.beanClass = beanClass;
        this.config = config;
        this.validationRules = validationRules != null ? validationRules : new ArrayList<>();
        this.typeConverter = TypeConverter.getInstance();
        this.batchProcessor = batchProcessor;
        this.methodHandleMapper = MethodHandleMapper.forClass(beanClass);
        this.startTime = System.currentTimeMillis();
        
        // Pre-cache ExcelColumn annotations for all fields
        initializeAnnotationCache();
        
        log.info("Initialized TrueStreamingSAXProcessor with MethodHandle optimization for class: {}", 
                 beanClass.getSimpleName());
    }
    
    /**
     * Pre-cache ExcelColumn annotations for all fields to avoid repeated reflection
     */
    private void initializeAnnotationCache() {
        try {
            for (Field field : beanClass.getDeclaredFields()) {
                ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                if (annotation != null) {
                    fieldAnnotationCache.put(field.getName(), annotation);
                    // Also cache by Excel column name
                    if (!annotation.name().isEmpty()) {
                        fieldAnnotationCache.put(annotation.name(), annotation);
                    }
                }
            }
            log.debug("Cached {} ExcelColumn annotations for class {}", 
                     fieldAnnotationCache.size(), beanClass.getSimpleName());
        } catch (Exception e) {
            log.warn("Failed to initialize annotation cache: {}", e.getMessage());
        }
    }
    
    /**
     * Get ExcelColumn annotation for a field name
     */
    private ExcelColumn getExcelColumnAnnotation(String fieldName) {
        return fieldAnnotationCache.get(fieldName);
    }
    
    /**
     * Custom DataFormatter that returns serial number for date cells instead of formatted string
     * This allows us to parse date in any format we want from the serial number
     * Made public static for use in multi-sheet processor
     */
    public static class SerialNumberDataFormatter extends DataFormatter {
        
        public SerialNumberDataFormatter() {
            this.setUseCachedValuesForFormulaCells(false);
        }
        
        /**
         * Override formatRawCellContents to return serial number for date cells
         * For date cells: returns serial number as string (e.g., "45234.0")
         * For non-date cells: uses default formatting
         */
        @Override
        public String formatRawCellContents(double cellValue, int formatIndex, String formatString) {
            // Check if this is a date format
            if (isDateFormat(formatIndex, formatString)) {
                // Return serial number as string so we can parse it later
                return String.valueOf(cellValue);
            }
            
            // For non-date cells, use default formatting
            return super.formatRawCellContents(cellValue, formatIndex, formatString);
        }
        
        /**
         * Check if format index/string represents a date format
         */
        private boolean isDateFormat(int formatIndex, String formatString) {
            if (formatString == null || formatString.isEmpty()) {
                // Check built-in date formats
                String builtinFormat = BuiltinFormats.getBuiltinFormat(formatIndex);
                if (builtinFormat != null) {
                    return DateUtil.isADateFormat(formatIndex, builtinFormat);
                }
                return false;
            }
            
            // Check if format string is a date format
            return DateUtil.isADateFormat(formatIndex, formatString);
        }
    }
    
    /**
     * Process Excel với true streaming - không tích lũy kết quả
     */
    public ProcessingResult processExcelStreamTrue(InputStream inputStream) throws Exception {
        
        try (OPCPackage opcPackage = OPCPackage.open(inputStream)) {
            XSSFReader xssfReader = new XSSFReader(opcPackage);
            org.apache.poi.xssf.model.SharedStringsTable sharedStringsTable = 
                (org.apache.poi.xssf.model.SharedStringsTable) xssfReader.getSharedStringsTable();
            StylesTable stylesTable = xssfReader.getStylesTable();
            
            // True streaming content handler - xử lý từng batch ngay
            TrueStreamingContentHandler contentHandler = new TrueStreamingContentHandler();
            
            // Setup SAX parser with proper date formatting
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            
            // Create custom DataFormatter that returns serial number for date cells
            // This allows us to parse date in any format we want from the serial number
            SerialNumberDataFormatter dataFormatter = new SerialNumberDataFormatter();
            
            XSSFSheetXMLHandler sheetHandler = new XSSFSheetXMLHandler(
                stylesTable, sharedStringsTable, contentHandler, dataFormatter, false
            );
            xmlReader.setContentHandler(sheetHandler);
            
            // Process first sheet với true streaming
            XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
            if (sheetIterator.hasNext()) {
                try (InputStream sheetStream = sheetIterator.next()) {
                    xmlReader.parse(new InputSource(sheetStream));
                }
            }
            
            // Flush remaining batch
            contentHandler.flushRemainingBatch();
        }

        long processingTime = System.currentTimeMillis() - startTime;

        // Throw if no data rows were processed
        if (totalProcessed.get() == 0) {
            throw new RuntimeException("Tập không có dữ liệu");
        }

        return new ProcessingResult(
            totalProcessed.get(),
            totalErrors.get(),
            processingTime,
            new ArrayList<>(validationErrors)
        );
    }
    
    /**
     * Process a single sheet stream with provided shared resources (styles, strings, formatter)
     * This is used by TrueStreamingMultiSheetProcessor to avoid reopening OPCPackage for each sheet
     */
    public ProcessingResult processSheetStream(
            InputStream sheetStream,
            StylesTable stylesTable,
            org.apache.poi.xssf.model.SharedStringsTable sharedStringsTable,
            DataFormatter dataFormatter) throws Exception {
        
        // True streaming content handler - xử lý từng batch ngay
        TrueStreamingContentHandler contentHandler = new TrueStreamingContentHandler();
        
        // Setup SAX parser
        XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        
        XSSFSheetXMLHandler sheetHandler = new XSSFSheetXMLHandler(
            stylesTable, sharedStringsTable, contentHandler, dataFormatter, false
        );
        xmlReader.setContentHandler(sheetHandler);
        
        // Process sheet stream directly
        xmlReader.parse(new InputSource(sheetStream));
        
        // Flush remaining batch
        contentHandler.flushRemainingBatch();

        long processingTime = System.currentTimeMillis() - startTime;

        // Throw if no data rows were processed
        if (totalProcessed.get() == 0) {
            throw new RuntimeException("Tập không có dữ liệu");
        }

        return new ProcessingResult(
            totalProcessed.get(),
            totalErrors.get(),
            processingTime,
            new ArrayList<>(validationErrors)
        );
    }

    // fieldMapping removed; MethodHandleMapper handles both Excel column names and direct field names
    
    /**
     * True streaming content handler - xử lý batch ngay, không tích lũy
     */
    private class TrueStreamingContentHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        
        private final List<T> currentBatch = new ArrayList<>();
        private final Map<String, Integer> headerMapping = new HashMap<>();
        private final Set<String> seenUniqueValues = new HashSet<>();
        private final AtomicLong errorCount = new AtomicLong(0);
        private Object currentInstance;
        private int currentRowNum = 0;
        private boolean headerProcessed = false;
        private boolean rowHasValue = false;
        
        @Override
        public void startRow(int rowNum) {
            this.currentRowNum = rowNum;
            
            // Skip rows before start row
            if (rowNum < config.getStartRow()) {
                return;
            }
            
            // Create new instance for data rows using MethodHandle (5x faster)
            if (headerProcessed) {
                rowHasValue = false;
                try {
                    currentInstance = methodHandleMapper.createInstance();
                    
                    // Set rowNum if field exists
                    @SuppressWarnings("unchecked")
                    T typedInstance = (T) currentInstance;
                    if (methodHandleMapper.hasField("rowNum")) {
                        methodHandleMapper.setFieldValue(typedInstance, "rowNum", rowNum + 1);
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to create instance for row {}: {}", rowNum, e.getMessage());
                    totalErrors.incrementAndGet();
                }
            }
        }
        
        @Override
        public void cell(String cellReference, String formattedValue, 
                        org.apache.poi.xssf.usermodel.XSSFComment comment) {
            if (currentRowNum < config.getStartRow()) {
                return;
            }
            
            int colIndex = getColumnIndex(cellReference);
            
            // Process header row
            if (currentRowNum == config.getStartRow() && !headerProcessed) {
                if (formattedValue != null && !formattedValue.trim().isEmpty()) {
                    headerMapping.put(formattedValue.trim(), colIndex);
                }
                return;
            }
            
            // Process data rows
            if (headerProcessed && currentInstance != null) {
                processDataCell(colIndex, formattedValue);
            }
        }
        
        @Override
        public void endRow(int rowNum) {
            // Mark header as processed
            if (rowNum == config.getStartRow() && !headerProcessed) {
                headerProcessed = true;
                log.debug("Header processed with {} columns", headerMapping.size());
                return;
            }

            // Process completed data row
            if (headerProcessed && currentInstance != null) {
                try {
                    // Skip completely empty data rows
                    if (!rowHasValue) {
                        log.debug("Skipping empty row {}", rowNum);
                        currentInstance = null;
                        return;
                    }
                    // ✅ INLINE maxRows VALIDATION (during streaming, NO buffering)
                    if (config.getMaxRows() > 0) {
                        int dataRowsProcessed = (int) totalProcessed.get() + 1; // +1 for current row
                        if (dataRowsProcessed > config.getMaxRows()) {
                            throw new RuntimeException(String.format(
                                "Số lượng bản ghi trong file (%d) vượt quá giới hạn cho phép (%d). " +
                                "Vui lòng chia nhỏ file hoặc tăng giới hạn xử lý.",
                                dataRowsProcessed, config.getMaxRows()));
                        }
                    }

                    // Run validations
                    runValidations(currentInstance, rowNum);

                    // Add to current batch
                    @SuppressWarnings("unchecked")
                    T typedInstance = (T) currentInstance;
                    currentBatch.add(typedInstance);
                    totalProcessed.incrementAndGet();

                    // Process batch khi đủ size
                    if (currentBatch.size() >= config.getBatchSize()) {
                        processBatch();
                    }

                    // Progress tracking - respects config.enableProgressTracking and configurable interval
                    if (config.isEnableProgressTracking() &&
                        totalProcessed.get() % config.getProgressReportInterval() == 0) {
                        log.info("Processed {} rows in streaming mode", totalProcessed.get());
                    }

                } catch (Exception e) {
                    totalErrors.incrementAndGet();
                    log.warn("Error processing row {}: {}", rowNum, e.getMessage());
                    // Re-throw if it's a maxRows violation (don't continue processing)
                    if (e.getMessage() != null && e.getMessage().contains("vượt quá giới hạn")) {
                        throw new RuntimeException(e);
                    }
                }

                currentInstance = null;
            }
        }
        
        /**
         * Process current batch và clear ngay để tiếp tục streaming
         */
        private void processBatch() {
            if (!currentBatch.isEmpty() && batchProcessor != null) {
                try {
                    // Tạo copy để xử lý
                    List<T> batchToProcess = new ArrayList<>(currentBatch);
                    
                    // Process batch ngay lập tức
                    batchProcessor.accept(batchToProcess);
                    
                    // Clear batch để tiếp tục streaming
                    currentBatch.clear();
                    
                    log.debug("Processed batch of {} records", batchToProcess.size());
                    
                } catch (Exception e) {
                    log.error("Error processing batch: {}", e.getMessage(), e);
                    totalErrors.addAndGet(currentBatch.size());
                    currentBatch.clear();
                }
            }
        }
        
        /**
         * Flush remaining batch cuối file
         */
        public void flushRemainingBatch() {
            if (!currentBatch.isEmpty()) {
                log.info("Flushing final batch of {} records", currentBatch.size());
                processBatch();
            }
        }
        
        /**
         * Process a single cell value and set to instance field
         * ✅ FIX: Full error context tracking with ValidationError
         * ✅ FIX: Check required fields BEFORE processing
         */
        private void processDataCell(int colIndex, String formattedValue) {
            String fieldName = findFieldNameByColumnIndex(colIndex);
            if (fieldName == null) {
                return;
            }

            // ✅ Performance: Trim once at start
            String trimmedValue = (formattedValue != null) ? formattedValue.trim() : null;

            try {
                // ✅ Performance: Cache field type lookups
                Class<?> fieldType = fieldTypeCache.computeIfAbsent(
                    fieldName,
                    methodHandleMapper::getFieldType
                );

                if (fieldType != null) {
                    // ✅ FIX: Check required fields BEFORE processing
                    ExcelColumn annotation = getExcelColumnAnnotation(fieldName);
                    if (annotation != null && annotation.required()) {
                        if (trimmedValue == null || trimmedValue.isEmpty()) {
                            String errorMsg = String.format(
                                "Row %d, Column %s: Required field '%s' is empty",
                                currentRowNum + 1,
                                getColumnName(colIndex),
                                fieldName
                            );

                            log.warn(errorMsg);

                            synchronized (validationErrors) {
                                validationErrors.add(new ValidationError(
                                    currentRowNum + 1,
                                    fieldName,
                                    null,
                                    "REQUIRED_FIELD_EMPTY"
                                ));
                            }

                            totalErrors.incrementAndGet();

                            // ✅ Check abort threshold
                            checkErrorAbortThreshold();

                            // ✅ Skip processing this cell
                            return;
                        }
                    }

                    if (trimmedValue != null && !trimmedValue.isEmpty()) {
                        rowHasValue = true;
                    }

                    String processedValue = smartProcessCellValue(trimmedValue, fieldName, fieldType);
                    Object convertedValue = typeConverter.convert(processedValue, fieldType);

                    @SuppressWarnings("unchecked")
                    T typedInstance = (T) currentInstance;
                    methodHandleMapper.setFieldValue(typedInstance, fieldName, convertedValue);
                }

            } catch (Exception e) {
                // ✅ FIX: Track error with full context
                String errorMsg = String.format(
                    "Row %d, Column %s (%s): Failed to process value '%s' - %s",
                    currentRowNum + 1,
                    getColumnName(colIndex),
                    fieldName,
                    trimmedValue,
                    e.getMessage()
                );

                log.warn(errorMsg);

                // ✅ Store error for reporting
                synchronized (validationErrors) {
                    validationErrors.add(new ValidationError(
                        currentRowNum + 1,
                        fieldName,
                        trimmedValue,
                        e.getMessage()
                    ));
                }

                // ✅ Increment error counter
                totalErrors.incrementAndGet();

                // ✅ Check abort threshold
                checkErrorAbortThreshold();
            }
        }

        /**
         * Check if error count exceeds threshold and abort if needed
         * ✅ FIX: Use dedicated config instead of calculating from maxRows
         */
        private void checkErrorAbortThreshold() {
            int maxErrors = config.getMaxErrorsBeforeAbort(); // Dedicated config
            if (totalErrors.get() > maxErrors) {
                throw new RuntimeException(String.format(
                    "Quá nhiều lỗi (%d/%d). Dừng xử lý tại row %d. " +
                    "Vui lòng kiểm tra và sửa lỗi trước khi tải lại.",
                    totalErrors.get(),
                    maxErrors,
                    currentRowNum + 1
                ));
            }
        }

        /**
         * Helper method to convert column index to Excel column name (A, B, ..., Z, AA, AB, ...)
         */
        private String getColumnName(int colIndex) {
            StringBuilder columnName = new StringBuilder();
            int index = colIndex + 1; // Convert 0-based to 1-based
            while (index > 0) {
                int remainder = (index - 1) % 26;
                columnName.insert(0, (char) ('A' + remainder));
                index = (index - 1) / 26;
            }
            return columnName.toString();
        }

        /**
         * Smart cell value processing based on field type
         * - Date fields: Parse Excel serial number to ISO format
         * - Identifier fields: Normalize scientific notation
         * - Number fields: Handle currency, accounting, percentage formats
         * - Other fields: Return as-is for TypeConverter
         */
        private String smartProcessCellValue(String rawValue, String fieldName, Class<?> fieldType) {
            if (rawValue == null || rawValue.trim().isEmpty()) {
                return rawValue;
            }

            String value = rawValue.trim();

            if (isDateField(fieldType)) {
                return normalizeDateValue(value, fieldType);
            }

            if (isIdentifierField(fieldName, fieldType, value)) {
                return normalizeIdentifierValue(value);
            }

            // ✅ FIX: Handle number formats (currency, accounting, percentage)
            if (isNumericField(fieldType)) {
                return normalizeNumericValue(value, fieldName, fieldType);
            }

            return value;
        }

        /**
         * Check if field is numeric type
         */
        private boolean isNumericField(Class<?> fieldType) {
            return fieldType == Integer.class || fieldType == int.class ||
                   fieldType == Long.class || fieldType == long.class ||
                   fieldType == Double.class || fieldType == double.class ||
                   fieldType == Float.class || fieldType == float.class ||
                   fieldType == java.math.BigDecimal.class;
        }

        /**
         * Check if field is percentage type
         */
        private boolean isPercentageField(String fieldName) {
            String normalized = normalizeFieldName(fieldName);
            return normalized.contains("percent") ||
                   normalized.contains("rate") ||
                   normalized.contains("ratio") ||
                   normalized.contains("tile") || // Vietnamese: tỷ lệ
                   normalized.contains("phantram"); // Vietnamese: phần trăm
        }

        /**
         * Normalize numeric values (currency, accounting, percentage)
         */
        private String normalizeNumericValue(String value, String fieldName, Class<?> fieldType) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            // ✅ Step 1: Handle accounting format: (123.45) → -123.45
            if (value.matches("\\(\\d+\\.?\\d*\\)")) {
                value = "-" + value.replaceAll("[()]", "");
                log.debug("Normalized accounting format: (123) → {}", value);
                return value;
            }

            // ✅ Step 2: Handle currency symbols: $1,234.56 → 1234.56
            if (value.matches(".*[^0-9.\\-+Ee].*\\d.*")) {
                String normalized = value.replaceAll("[^0-9.\\-+Ee]", "");
                if (!normalized.isEmpty() && !normalized.equals(value)) {
                    log.debug("Normalized currency: {} → {}", value, normalized);
                    return normalized;
                }
            }

            // ✅ Step 3: Handle percentage fields: 0.15 → 15 (if field name suggests percentage)
            if (isPercentageField(fieldName) &&
                (fieldType == Double.class || fieldType == double.class)) {
                try {
                    double numValue = Double.parseDouble(value);
                    if (numValue >= 0 && numValue <= 1) {
                        String percentage = String.valueOf(numValue * 100);
                        log.debug("Normalized percentage: {} → {}%", value, percentage);
                        return percentage;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            return value;
        }

        /**
         * Check if field is an identifier (CMND, phone, tax code, etc.)
         */
        private boolean isIdentifierField(String fieldName, Class<?> fieldType, String value) {
            if (fieldType != String.class) {
                return false;
            }

            String normalizedFieldName = normalizeFieldName(fieldName);
            boolean matchesPattern = normalizedFieldName.contains("identity") ||
                                   normalizedFieldName.contains("identitycard") ||
                                   normalizedFieldName.contains("cmnd") ||
                                   normalizedFieldName.contains("cccd") ||
                                   normalizedFieldName.contains("passport") ||
                                   normalizedFieldName.contains("phone") ||
                                   normalizedFieldName.contains("phonenumber") ||
                                   normalizedFieldName.contains("mobile") ||
                                   normalizedFieldName.contains("tax") ||
                                   normalizedFieldName.contains("taxcode") ||
                                   normalizedFieldName.contains("mst") ||
                                   normalizedFieldName.contains("account") ||
                                   normalizedFieldName.contains("accountnumber") ||
                                   normalizedFieldName.contains("code") ||
                                   (normalizedFieldName.contains("number") && normalizedFieldName.contains("card"));

            return matchesPattern || (value != null && !value.trim().isEmpty() && looksLikeIdentifierValue(value));
        }

        /**
         * Normalize field name: remove Vietnamese accents, spaces, lowercase
         */
        private String normalizeFieldName(String fieldName) {
            if (fieldName == null || fieldName.isEmpty()) {
                return "";
            }

            String normalized = Normalizer.normalize(fieldName, Normalizer.Form.NFD);
            normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            return normalized.toLowerCase().replaceAll("\\s+", "");
        }

        /**
         * Check if value looks like an identifier (scientific notation or long digit string)
         */
        private boolean looksLikeIdentifierValue(String value) {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }

            String trimmed = value.trim();

            if (trimmed.contains("E") || trimmed.contains("e")) {
                try {
                    java.math.BigDecimal bd = new java.math.BigDecimal(trimmed);
                    if (bd.scale() == 0 && bd.precision() > 9) {
                        log.debug("Identifier detected (scientific): {}", value);
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (trimmed.matches("\\d{9,15}") || trimmed.matches("\\d+\\.0+")) {
                log.debug("Identifier detected (pattern): {}", value);
                return true;
            }

            return false;
        }

        /**
         * Normalize identifier values (remove scientific notation and trailing .0)
         */
        private String normalizeIdentifierValue(String value) {
            if (value.contains("E") || value.contains("e")) {
                try {
                    java.math.BigDecimal bd = new java.math.BigDecimal(value);
                    String plainString = bd.toPlainString();

                    if (plainString.endsWith(".0")) {
                        plainString = plainString.substring(0, plainString.length() - 2);
                    }

                    log.debug("Normalized identifier: {} → {}", value, plainString);
                    return plainString;

                } catch (NumberFormatException e) {
                    log.warn("Failed to normalize identifier: {}", value);
                    return value;
                }
            }

            if (value.matches("\\d+\\.0+")) {
                return value.substring(0, value.indexOf('.'));
            }

            return value;
        }

        /**
         * Check if field is a date type
         */
        private boolean isDateField(Class<?> fieldType) {
            return fieldType == LocalDate.class ||
                   fieldType == java.time.LocalDateTime.class ||
                   fieldType == Date.class;
        }

        /**
         * Normalize date values - Parse Excel serial date to ISO format
         * ✅ FIX: Handle edge cases (before 1900, time-only, invalid dates)
         */
        private String normalizeDateValue(String value, Class<?> fieldType) {
            // Step 1: Parse Excel serial date (from SerialNumberDataFormatter)
            if (value != null && !value.contains("/") && !value.contains("-") &&
                (value.matches("\\d+\\.?\\d*") || value.matches("-?\\d+\\.?\\d*"))) {
                try {
                    double serialDate = Double.parseDouble(value);

                    // ✅ FIX: Handle dates before 1900 and up to year 9999
                    // Excel serial dates: -693593 = 0001-01-01, 2958465 = 9999-12-31
                    if (serialDate >= -693593 && serialDate < 2958465) {
                        Date javaDate = DateUtil.getJavaDate(serialDate, false); // use1904windowing=false

                        if (fieldType == LocalDate.class) {
                            LocalDate localDate = javaDate.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            String isoDate = localDate.toString();
                            log.debug("Converted Excel serial {} → {}", value, isoDate);
                            return isoDate;
                        } else if (fieldType == java.time.LocalDateTime.class) {
                            java.time.LocalDateTime localDateTime = javaDate.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                            String isoDateTime = localDateTime.toString();
                            log.debug("Converted Excel serial {} → {}", value, isoDateTime);
                            return isoDateTime;
                        } else if (fieldType == Date.class || fieldType == java.sql.Date.class) {
                            LocalDate localDate = javaDate.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            String isoDate = localDate.toString();
                            log.debug("Converted Excel serial {} → {}", value, isoDate);
                            return isoDate;
                        } else {
                            log.debug("Unknown date type for serial: {}", value);
                            return value;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse serial date '{}': {}", value, e.getMessage());
                }
            }

            // Step 2: Fallback - Handle text date formats (backward compatibility)
            if (value != null && value.matches("\\d{1,2}/\\d{1,2}/\\d{2}")) {
                String[] parts = value.split("/");
                if (parts.length == 3) {
                    String month = parts[0];
                    String day = parts[1];
                    String year = parts[2];

                    int yearInt = Integer.parseInt(year);
                    year = (yearInt <= 30) ? "20" + year : "19" + year;

                    String normalized = month + "/" + day + "/" + year;
                    log.debug("Normalized short year: {} → {}", value, normalized);
                    return normalized;
                }
            }

            // Step 3: Handle dd-MM-yy format
            if (value != null && value.matches("\\d{1,2}-\\d{1,2}-\\d{2}")) {
                String[] parts = value.split("-");
                if (parts.length == 3) {
                    String day = parts[0];
                    String month = parts[1];
                    String year = parts[2];

                    int yearInt = Integer.parseInt(year);
                    year = (yearInt <= 30) ? "20" + year : "19" + year;

                    return day + "/" + month + "/" + year;
                }
            }

            // Step 4: Handle dd-MMM-yyyy format
            if (value != null && value.matches("\\d{1,2}-[A-Za-z]+-\\d{4}")) {
                String[] parts = value.split("-");
                if (parts.length == 3) {
                    String day = parts[0];
                    String monthName = parts[1];
                    String year = parts[2];

                    String monthNumber = parseMonthName(monthName);
                    if (monthNumber != null) {
                        String normalized = day + "/" + monthNumber + "/" + year;
                        log.debug("Normalized dd-MMM-yyyy: {} → {}", value, normalized);
                        return normalized;
                    }
                }
            }

            // Step 5: Handle dd-MMM-yy format
            if (value != null && value.matches("\\d{1,2}-[A-Za-z]+-\\d{2}")) {
                String[] parts = value.split("-");
                if (parts.length == 3) {
                    String day = parts[0];
                    String monthName = parts[1];
                    String year = parts[2];

                    String monthNumber = parseMonthName(monthName);
                    if (monthNumber != null) {
                        int yearInt = Integer.parseInt(year);
                        year = (yearInt <= 30) ? "20" + year : "19" + year;

                        String normalized = day + "/" + monthNumber + "/" + year;
                        log.debug("Normalized dd-MMM-yy: {} → {}", value, normalized);
                        return normalized;
                    }
                }
            }

            return value;
        }

        /**
         * Parse month name (English or Vietnamese) to month number (01-12)
         * Supports both full names and abbreviations
         */
        private String parseMonthName(String monthName) {
            if (monthName == null || monthName.trim().isEmpty()) {
                return null;
            }
            
            String normalized = monthName.trim().toLowerCase();
            
            // English month names (full and abbreviated)
            Map<String, String> englishMonths = new HashMap<>();
            englishMonths.put("january", "01");
            englishMonths.put("jan", "01");
            englishMonths.put("february", "02");
            englishMonths.put("feb", "02");
            englishMonths.put("march", "03");
            englishMonths.put("mar", "03");
            englishMonths.put("april", "04");
            englishMonths.put("apr", "04");
            englishMonths.put("may", "05");
            englishMonths.put("june", "06");
            englishMonths.put("jun", "06");
            englishMonths.put("july", "07");
            englishMonths.put("jul", "07");
            englishMonths.put("august", "08");
            englishMonths.put("aug", "08");
            englishMonths.put("september", "09");
            englishMonths.put("sep", "09");
            englishMonths.put("sept", "09");
            englishMonths.put("october", "10");
            englishMonths.put("oct", "10");
            englishMonths.put("november", "11");
            englishMonths.put("nov", "11");
            englishMonths.put("december", "12");
            englishMonths.put("dec", "12");
            
            // Vietnamese month names (full and abbreviated)
            Map<String, String> vietnameseMonths = new HashMap<>();
            vietnameseMonths.put("tháng một", "01");
            vietnameseMonths.put("tháng 1", "01");
            vietnameseMonths.put("tháng hai", "02");
            vietnameseMonths.put("tháng 2", "02");
            vietnameseMonths.put("tháng ba", "03");
            vietnameseMonths.put("tháng 3", "03");
            vietnameseMonths.put("tháng tư", "04");
            vietnameseMonths.put("tháng 4", "04");
            vietnameseMonths.put("tháng năm", "05");
            vietnameseMonths.put("tháng 5", "05");
            vietnameseMonths.put("tháng sáu", "06");
            vietnameseMonths.put("tháng 6", "06");
            vietnameseMonths.put("tháng bảy", "07");
            vietnameseMonths.put("tháng 7", "07");
            vietnameseMonths.put("tháng tám", "08");
            vietnameseMonths.put("tháng 8", "08");
            vietnameseMonths.put("tháng chín", "09");
            vietnameseMonths.put("tháng 9", "09");
            vietnameseMonths.put("tháng mười", "10");
            vietnameseMonths.put("tháng 10", "10");
            vietnameseMonths.put("tháng mười một", "11");
            vietnameseMonths.put("tháng 11", "11");
            vietnameseMonths.put("tháng mười hai", "12");
            vietnameseMonths.put("tháng 12", "12");
            
            // Check English months first
            String monthNumber = englishMonths.get(normalized);
            if (monthNumber != null) {
                return monthNumber;
            }
            
            // Check Vietnamese months
            monthNumber = vietnameseMonths.get(normalized);
            if (monthNumber != null) {
                return monthNumber;
            }
            
            // Try to parse as number (01-12)
            try {
                int monthInt = Integer.parseInt(normalized);
                if (monthInt >= 1 && monthInt <= 12) {
                    return String.format("%02d", monthInt);
                }
            } catch (NumberFormatException ignored) {
                // Not a number
            }
            
            log.debug("Failed to parse month name: {}", monthName);
            return null;
        }
        
        /**
         * Find actual field name by column index
         * Always returns the actual Java field name, not Excel column name
         */
        private String findFieldNameByColumnIndex(int colIndex) {
            for (Map.Entry<String, Integer> entry : headerMapping.entrySet()) {
                if (entry.getValue().equals(colIndex)) {
                    String headerName = entry.getKey();
                    
                    // Step 1: Check if headerName is a direct field name (camelCase pattern)
                    if (methodHandleMapper.hasField(headerName) && isFieldNamePattern(headerName)) {
                        return headerName;
                    }
                    
                    // Step 2: Resolve Excel column name to actual field name via annotation
                    String actualFieldName = resolveExcelColumnToFieldName(headerName);
                    if (actualFieldName != null && methodHandleMapper.hasField(actualFieldName)) {
                        return actualFieldName;
                    }
                    
                    // Step 3: Fallback to headerName if mapper knows it (for backward compatibility)
                    if (methodHandleMapper.hasField(headerName)) {
                        return headerName;
                    }
                }
            }
            return null;
        }
        
        /**
         * Check if a string matches Java field name pattern (camelCase, no spaces, no special chars)
         */
        private boolean isFieldNamePattern(String name) {
            if (name == null || name.isEmpty()) {
                return false;
            }
            // Field names are typically camelCase, no spaces, no special characters except underscore
            return name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
        }
        
        /**
         * Resolve Excel column name to actual Java field name by checking ExcelColumn annotations
         */
        private String resolveExcelColumnToFieldName(String excelColumnName) {
            try {
                for (Field field : beanClass.getDeclaredFields()) {
                    ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                    if (annotation != null && excelColumnName.equals(annotation.name())) {
                        return field.getName();
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to resolve Excel column '{}' to field name: {}", excelColumnName, e.getMessage());
            }
            return null;
        }
        
        private void runValidations(Object instance, int rowNum) {
            try {
                // Required fields validation
                for (String requiredField : config.getRequiredFields()) {
                    if (methodHandleMapper.hasField(requiredField)) {
                        @SuppressWarnings("unchecked")
                        T typedInstance = (T) instance;
                        Object value = methodHandleMapper.getFieldValue(typedInstance, requiredField);
                        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                            log.warn("Required field '{}' is empty at row {}", requiredField, rowNum);
                            errorCount.incrementAndGet();
                        }
                    }
                }
                
                // Unique fields validation (simple memory-based check for current batch)
                for (String uniqueField : config.getUniqueFields()) {
                    if (methodHandleMapper.hasField(uniqueField)) {
                        @SuppressWarnings("unchecked")
                        T typedInstance = (T) instance;
                        Object value = methodHandleMapper.getFieldValue(typedInstance, uniqueField);
                        if (value != null) {
                            String key = uniqueField + ":" + value.toString();
                            if (seenUniqueValues.contains(key)) {
                                log.warn("Duplicate value '{}' for unique field '{}' at row {}", 
                                        value, uniqueField, rowNum);
                                errorCount.incrementAndGet();
                            } else {
                                seenUniqueValues.add(key);
                            }
                        }
                    }
                }
                
                // Custom field validation rules
                for (Map.Entry<String, ValidationRule> entry : config.getFieldValidationRules().entrySet()) {
                    String fieldName = entry.getKey();
                    ValidationRule rule = entry.getValue();
                    if (methodHandleMapper.hasField(fieldName)) {
                        @SuppressWarnings("unchecked")
                        T typedInstance = (T) instance;
                        Object value = methodHandleMapper.getFieldValue(typedInstance, fieldName);
                        if (value != null) {
                            var result = rule.validate(fieldName, value, rowNum, 0);
                            if (!result.isValid()) {
                                log.warn("Validation failed for field '{}' with value '{}' at row {}: {}", 
                                        fieldName, value, rowNum, result.getErrorMessage());
                                errorCount.incrementAndGet();
                            }
                        }
                    }
                }
                
                // Global validation rules
                for (ValidationRule rule : config.getGlobalValidationRules()) {
                    var result = rule.validate("global", instance, rowNum, 0);
                    if (!result.isValid()) {
                        log.warn("Global validation failed for instance at row {}: {}", rowNum, result.getErrorMessage());
                        errorCount.incrementAndGet();
                    }
                }
                
            } catch (Exception e) {
                log.error("Validation error at row {}: {}", rowNum, e.getMessage());
                errorCount.incrementAndGet();
            }
        }
        
        private int getColumnIndex(String cellReference) {
            // Extract column index from cell reference (e.g., "A1" -> 0, "B1" -> 1)
            String colRef = cellReference.replaceAll("\\d", "");
            int colIndex = 0;
            for (int i = 0; i < colRef.length(); i++) {
                colIndex = colIndex * 26 + (colRef.charAt(i) - 'A' + 1);
            }
            return colIndex - 1;
        }
    }
    
    /**
     * Result class for true streaming processing
     */
    public static class ProcessingResult {
        private final long processedRecords;
        private final long errorCount;
        private final long processingTimeMs;
        private final List<ValidationError> errors;

        public ProcessingResult(long processedRecords, long errorCount, long processingTimeMs) {
            this(processedRecords, errorCount, processingTimeMs, new ArrayList<>());
        }

        public ProcessingResult(long processedRecords, long errorCount, long processingTimeMs, List<ValidationError> errors) {
            this.processedRecords = processedRecords;
            this.errorCount = errorCount;
            this.processingTimeMs = processingTimeMs;
            this.errors = errors;
        }

        public long getProcessedRecords() { return processedRecords; }
        public long getErrorCount() { return errorCount; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public List<ValidationError> getErrors() { return errors; }
        public double getRecordsPerSecond() {
            return processingTimeMs > 0 ? (processedRecords * 1000.0) / processingTimeMs : 0;
        }

        @Override
        public String toString() {
            return String.format("ProcessingResult{processed=%d, errors=%d, time=%dms, rate=%.1f rec/sec}",
                    processedRecords, errorCount, processingTimeMs, getRecordsPerSecond());
        }
    }

    /**
     * Validation error with full context
     */
    public static class ValidationError {
        private final int rowNumber;
        private final String fieldName;
        private final String cellValue;
        private final String errorMessage;

        public ValidationError(int rowNumber, String fieldName, String cellValue, String errorMessage) {
            this.rowNumber = rowNumber;
            this.fieldName = fieldName;
            this.cellValue = cellValue;
            this.errorMessage = errorMessage;
        }

        public int getRowNumber() { return rowNumber; }
        public String getFieldName() { return fieldName; }
        public String getCellValue() { return cellValue; }
        public String getErrorMessage() { return errorMessage; }

        @Override
        public String toString() {
            return String.format("Row %d, Field '%s', Value '%s': %s",
                    rowNumber, fieldName, cellValue, errorMessage);
        }
    }
}