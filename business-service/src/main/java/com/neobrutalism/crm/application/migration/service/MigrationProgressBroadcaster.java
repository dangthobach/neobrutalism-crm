package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.migration.dto.JobProgressInfo;
import com.neobrutalism.crm.application.migration.model.MigrationStatus;
import com.neobrutalism.crm.application.migration.repository.MigrationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for broadcasting migration progress via WebSocket
 * Replaces SSE polling with efficient scheduled broadcasts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationProgressBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final MigrationProgressService progressService;
    private final MigrationJobRepository jobRepository;

    /**
     * Broadcast progress for all active migration jobs
     * Runs every 2 seconds to update all connected clients
     */
    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    public void broadcastActiveJobProgress() {
        try {
            // Find all active jobs
            List<UUID> activeJobIds = jobRepository.findByStatusIn(
                List.of(MigrationStatus.PENDING, MigrationStatus.PROCESSING)
            ).stream()
             .map(job -> job.getId())
             .toList();

            if (activeJobIds.isEmpty()) {
                return;
            }

            log.debug("Broadcasting progress for {} active jobs", activeJobIds.size());

            // Broadcast progress for each active job
            for (UUID jobId : activeJobIds) {
                try {
                    JobProgressInfo progress = progressService.getJobProgress(jobId);

                    // Send to topic: /topic/migration/{jobId}
                    messagingTemplate.convertAndSend(
                        "/topic/migration/" + jobId,
                        progress
                    );

                    log.trace("Broadcasted progress for job: {} - {}%",
                             jobId, progress.getOverallProgress());

                } catch (Exception e) {
                    log.error("Error broadcasting progress for job: {}", jobId, e);
                }
            }

        } catch (Exception e) {
            log.error("Error in progress broadcast scheduler", e);
        }
    }

    /**
     * Broadcast progress for a specific job immediately
     * Use this for important updates (job started, completed, failed)
     */
    public void broadcastJobProgress(UUID jobId) {
        try {
            JobProgressInfo progress = progressService.getJobProgress(jobId);

            messagingTemplate.convertAndSend(
                "/topic/migration/" + jobId,
                progress
            );

            log.debug("Broadcasted immediate progress update for job: {}", jobId);

        } catch (Exception e) {
            log.error("Error broadcasting immediate progress for job: {}", jobId, e);
        }
    }

    /**
     * Broadcast job completion notification
     */
    public void broadcastJobCompletion(UUID jobId, MigrationStatus finalStatus) {
        try {
            JobProgressInfo progress = progressService.getJobProgress(jobId);

            messagingTemplate.convertAndSend(
                "/topic/migration/" + jobId,
                progress
            );

            log.info("Broadcasted completion for job: {} with status: {}", jobId, finalStatus);

        } catch (Exception e) {
            log.error("Error broadcasting completion for job: {}", jobId, e);
        }
    }
}
