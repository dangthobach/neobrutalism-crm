package com.neobrutalism.crm.infrastructure.scheduler;

import com.neobrutalism.crm.application.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for cleaning up expired idempotency keys.
 *
 * Runs daily at 3 AM.
 * Only enabled when IdempotencyService is available (requires Redis).
 *
 * @author Admin
 * @since Phase 1
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(IdempotencyService.class)
public class IdempotencyCleanupJob {

    private final IdempotencyService idempotencyService;

    /**
     * Clean up expired idempotency keys.
     *
     * Cron: Every day at 3:00 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredKeys() {
        log.info("Starting idempotency key cleanup job");

        try {
            idempotencyService.cleanupExpiredKeys();
            log.info("Idempotency key cleanup job completed successfully");
        } catch (Exception e) {
            log.error("Idempotency key cleanup job failed", e);
        }
    }
}
