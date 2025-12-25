package com.neobrutalism.crm.common.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.event.OutboxEvent;
import com.neobrutalism.crm.common.service.OutboxEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for monitoring and managing outbox events
 */
@RestController
@RequestMapping("/api/outbox")
@RequiredArgsConstructor
@Tag(name = "Outbox Events", description = "Monitor and manage event outbox")
public class OutboxEventController {

    private final OutboxEventPublisher outboxEventPublisher;

    @GetMapping("/statistics")
    @Operation(summary = "Get outbox statistics", description = "Get pending and dead letter event counts")
    public ResponseEntity<ApiResponse<OutboxEventPublisher.OutboxStatistics>> getStatistics() {
        OutboxEventPublisher.OutboxStatistics stats = outboxEventPublisher.getStatistics();
        return ResponseEntity.ok(ApiResponse.success("Outbox statistics retrieved", stats));
    }

    @GetMapping("/dead-letter")
    @Operation(summary = "Get dead letter events", description = "Get events that exceeded max retries")
    public ResponseEntity<ApiResponse<List<OutboxEvent>>> getDeadLetterEvents() {
        List<OutboxEvent> deadLetterEvents = outboxEventPublisher.getDeadLetterEvents();
        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + deadLetterEvents.size() + " dead letter events",
                deadLetterEvents));
    }

    @PostMapping("/dead-letter/{eventId}/retry")
    @Operation(summary = "Retry dead letter event", description = "Reset and retry a failed event")
    public ResponseEntity<ApiResponse<String>> retryDeadLetterEvent(@PathVariable UUID eventId) {
        List<OutboxEvent> deadLetterEvents = outboxEventPublisher.getDeadLetterEvents();
        OutboxEvent event = deadLetterEvents.stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Dead letter event not found: " + eventId));

        outboxEventPublisher.retryDeadLetterEvent(event);
        return ResponseEntity.ok(ApiResponse.success("Event reset for retry"));
    }

    @PostMapping("/publish-now")
    @Operation(summary = "Trigger immediate publishing", description = "Manually trigger outbox event publishing")
    public ResponseEntity<ApiResponse<String>> publishNow() {
        outboxEventPublisher.publishPendingEvents();
        return ResponseEntity.ok(ApiResponse.success("Publishing triggered"));
    }
}
