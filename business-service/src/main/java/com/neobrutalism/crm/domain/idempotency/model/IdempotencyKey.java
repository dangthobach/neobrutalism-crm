package com.neobrutalism.crm.domain.idempotency.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Value Object representing an idempotency key for ensuring exactly-once execution.
 *
 * Stored in Redis with 24-hour TTL to prevent duplicate operations.
 *
 * @author Admin
 * @version 1.0
 * @since Phase 1
 */
@Entity
@Table(
    name = "idempotency_keys",
    indexes = {
        @Index(name = "idx_idempotency_tenant_key", columnList = "tenant_id, idempotency_key"),
        @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "operation_type", nullable = false, length = 100)
    private String operationType;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash; // SHA-256 hash of request body

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (expiresAt == null) {
            // 24-hour TTL
            expiresAt = createdAt.plusSeconds(86400);
        }
        if (status == null) {
            status = IdempotencyStatus.IN_PROGRESS;
        }
    }

    /**
     * Mark operation as completed successfully.
     */
    public void markCompleted(String responseBody, int httpStatusCode) {
        this.status = IdempotencyStatus.COMPLETED;
        this.responseBody = responseBody;
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Mark operation as failed.
     */
    public void markFailed(String errorMessage, int httpStatusCode) {
        this.status = IdempotencyStatus.FAILED;
        this.responseBody = errorMessage;
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Check if key has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if operation can be retried.
     */
    public boolean canRetry() {
        return status == IdempotencyStatus.FAILED && !isExpired();
    }
}
