package com.neobrutalism.crm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async configuration for migration tasks
 * Optimized for large Excel file processing (5M+ records)
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Excel migration executor - Dedicated thread pool for Excel processing
     *
     * Configuration rationale:
     * - corePoolSize: 2 sheets can process concurrently
     * - maxPoolSize: 3 sheets maximum (prevent memory spike)
     * - queueCapacity: 10 jobs can be queued
     * - keepAliveTime: 60s for idle threads
     */
    @Bean(name = "excelMigrationExecutor")
    public ThreadPoolTaskExecutor excelMigrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core settings
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(60);

        // Thread naming
        executor.setThreadNamePrefix("excel-migration-");

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300); // 5 minutes max wait

        // Rejection policy: CallerRunsPolicy ensures no tasks are lost
        // When pool is full, the calling thread will execute the task
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Allow core threads to timeout
        executor.setAllowCoreThreadTimeOut(true);

        executor.initialize();

        log.info("Initialized excelMigrationExecutor with core={}, max={}, queue={}",
                 2, 3, 10);

        return executor;
    }

    /**
     * File upload executor - For async file upload processing
     *
     * Separate pool to prevent upload operations from blocking migration processing
     */
    @Bean(name = "fileUploadExecutor")
    public ThreadPoolTaskExecutor fileUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Allow more concurrent uploads since they're I/O bound
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setKeepAliveSeconds(30);

        executor.setThreadNamePrefix("file-upload-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);

        executor.initialize();

        log.info("Initialized fileUploadExecutor with core={}, max={}, queue={}",
                 4, 8, 20);

        return executor;
    }

    /**
     * Default async executor for other async operations
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Exception handler for uncaught exceptions in async methods
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Uncaught async exception in method: {}", method.getName(), throwable);
            log.error("Method parameters: {}", (Object[]) params);

            // You can add additional logic here:
            // - Send alerts
            // - Update job status to FAILED
            // - Retry mechanism
        };
    }
}
