package com.neobrutalism.crm.application.migration.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Migration Performance Monitor
 * 
 * Tracks and alerts on migration performance metrics:
 * - Batch processing duration
 * - Memory usage
 * - Throughput (records/second)
 * - Error rates
 * 
 * Integrates with Micrometer for Prometheus/Grafana monitoring
 */
@Slf4j
@Component
public class MigrationMonitor {

    private final MeterRegistry meterRegistry;
    
    // Metrics storage
    private final ConcurrentHashMap<UUID, SheetMetrics> sheetMetricsMap = new ConcurrentHashMap<>();
    private final AtomicLong totalRecordsProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    
    // Alert thresholds
    private static final long SLOW_BATCH_THRESHOLD_MS = 10_000; // 10 seconds
    private static final long HIGH_MEMORY_THRESHOLD_PERCENT = 80; // 80%
    private static final long CRITICAL_MEMORY_THRESHOLD_PERCENT = 90; // 90%
    
    public MigrationMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        registerGauges();
    }
    
    /**
     * Register Prometheus gauges for continuous monitoring
     */
    private void registerGauges() {
        // Memory usage gauge
        Gauge.builder("migration.memory.used.bytes", this::getCurrentMemoryUsage)
            .description("Current JVM memory usage in bytes")
            .register(meterRegistry);
        
        Gauge.builder("migration.memory.used.percent", this::getCurrentMemoryUsagePercent)
            .description("Current JVM memory usage percentage")
            .register(meterRegistry);
        
        Gauge.builder("migration.memory.max.bytes", this::getMaxMemory)
            .description("Maximum JVM memory in bytes")
            .register(meterRegistry);
        
        // Active sheets gauge
        Gauge.builder("migration.sheets.active", sheetMetricsMap::size)
            .description("Number of sheets currently being processed")
            .register(meterRegistry);
        
        // Total records processed
        Gauge.builder("migration.records.total", totalRecordsProcessed::get)
            .description("Total records processed across all migrations")
            .register(meterRegistry);
        
        // Total errors
        Gauge.builder("migration.errors.total", totalErrors::get)
            .description("Total errors encountered across all migrations")
            .register(meterRegistry);
        
        log.info("Migration monitoring gauges registered");
    }
    
    /**
     * Record batch processing metrics
     * 
     * @param sheetId Sheet being processed
     * @param batchSize Number of records in batch
     * @param durationMs Time taken to process batch
     * @param validCount Number of valid records
     * @param invalidCount Number of invalid records
     */
    public void recordBatchProcessing(UUID sheetId, int batchSize, long durationMs, 
                                     int validCount, int invalidCount) {
        // Update counters
        Counter.builder("migration.batch.processed")
            .tag("sheet_id", sheetId.toString())
            .description("Number of batches processed")
            .register(meterRegistry)
            .increment();
        
        Counter.builder("migration.records.processed")
            .tag("sheet_id", sheetId.toString())
            .tag("status", "valid")
            .description("Number of records processed")
            .register(meterRegistry)
            .increment(validCount);
        
        Counter.builder("migration.records.processed")
            .tag("sheet_id", sheetId.toString())
            .tag("status", "invalid")
            .description("Number of records processed")
            .register(meterRegistry)
            .increment(invalidCount);
        
        // Record timing
        Timer.builder("migration.batch.duration")
            .tag("sheet_id", sheetId.toString())
            .description("Time taken to process a batch")
            .register(meterRegistry)
            .record(Duration.ofMillis(durationMs));
        
        // Update global counters
        totalRecordsProcessed.addAndGet(batchSize);
        totalErrors.addAndGet(invalidCount);
        
        // Update sheet metrics
        SheetMetrics metrics = sheetMetricsMap.computeIfAbsent(sheetId, k -> new SheetMetrics());
        metrics.recordBatch(batchSize, durationMs, validCount, invalidCount);
        
        // Calculate throughput
        double throughput = (double) batchSize / (durationMs / 1000.0); // records/second
        
        // Alert on slow batches
        if (durationMs > SLOW_BATCH_THRESHOLD_MS) {
            log.warn("⚠️ Slow batch detected - Sheet: {}, Duration: {}ms, Size: {}, Throughput: {:.0f} records/sec",
                     sheetId, durationMs, batchSize, throughput);
            
            Counter.builder("migration.alerts.slow_batch")
                .tag("sheet_id", sheetId.toString())
                .description("Number of slow batch alerts")
                .register(meterRegistry)
                .increment();
        }
        
        log.debug("Batch metrics - Sheet: {}, Size: {}, Duration: {}ms, Throughput: {:.0f} rec/sec, Valid: {}, Invalid: {}",
                  sheetId, batchSize, durationMs, throughput, validCount, invalidCount);
    }
    
    /**
     * Monitor memory usage and alert if thresholds exceeded
     * Called periodically by scheduled task
     */
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    public void monitorMemoryUsage() {
        long usedMemory = getCurrentMemoryUsage();
        long maxMemory = getMaxMemory();
        long usedPercent = getCurrentMemoryUsagePercent();
        
        // Alert on high memory
        if (usedPercent >= CRITICAL_MEMORY_THRESHOLD_PERCENT) {
            log.error("🔴 CRITICAL: Memory usage at {}% ({} MB / {} MB) - OOM risk!",
                      usedPercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
            
            Counter.builder("migration.alerts.critical_memory")
                .description("Number of critical memory alerts")
                .register(meterRegistry)
                .increment();
            
            // Suggest GC
            System.gc();
            log.info("Triggered System.gc() to reclaim memory");
            
        } else if (usedPercent >= HIGH_MEMORY_THRESHOLD_PERCENT) {
            log.warn("⚠️ WARNING: Memory usage at {}% ({} MB / {} MB)",
                     usedPercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
            
            Counter.builder("migration.alerts.high_memory")
                .description("Number of high memory alerts")
                .register(meterRegistry)
                .increment();
        }
    }
    
    /**
     * Record sheet completion
     */
    public void recordSheetCompletion(UUID sheetId, boolean success) {
        SheetMetrics metrics = sheetMetricsMap.remove(sheetId);
        
        if (metrics != null) {
            long totalDuration = System.currentTimeMillis() - metrics.getStartTime();
            double avgThroughput = (double) metrics.getTotalRecords() / (totalDuration / 1000.0);
            
            log.info("📊 Sheet {} completed - Total: {} records, Duration: {}s, Avg Throughput: {:.0f} rec/sec, Errors: {}",
                     sheetId, 
                     metrics.getTotalRecords(),
                     totalDuration / 1000,
                     avgThroughput,
                     metrics.getTotalErrors());
        }
        
        Counter.builder("migration.sheets.completed")
            .tag("status", success ? "success" : "failed")
            .description("Number of sheets completed")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Get current memory usage in bytes
     */
    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Get current memory usage as percentage
     */
    private long getCurrentMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return (usedMemory * 100) / runtime.maxMemory();
    }
    
    /**
     * Get maximum memory in bytes
     */
    private long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }
    
    /**
     * Internal metrics tracking per sheet
     */
    private static class SheetMetrics {
        private final long startTime = System.currentTimeMillis();
        private final AtomicLong totalRecords = new AtomicLong(0);
        private final AtomicLong totalErrors = new AtomicLong(0);
        private final AtomicLong batchCount = new AtomicLong(0);
        
        public void recordBatch(int batchSize, long durationMs, int validCount, int invalidCount) {
            totalRecords.addAndGet(batchSize);
            totalErrors.addAndGet(invalidCount);
            batchCount.incrementAndGet();
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getTotalRecords() {
            return totalRecords.get();
        }
        
        public long getTotalErrors() {
            return totalErrors.get();
        }
    }
}
