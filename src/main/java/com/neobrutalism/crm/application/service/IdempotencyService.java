package com.neobrutalism.crm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobrutalism.crm.domain.idempotency.model.IdempotencyKey;
import com.neobrutalism.crm.domain.idempotency.model.IdempotencyStatus;
import com.neobrutalism.crm.infrastructure.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Service for handling idempotent operations.
 *
 * Ensures exactly-once execution of critical operations using:
 * - Redis for fast lookups (24-hour TTL)
 * - PostgreSQL for persistence and audit trail
 * - SHA-256 hashing for request deduplication
 *
 * @author Admin
 * @version 1.0
 * @since Phase 1
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(RedisTemplate.class)
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String REDIS_KEY_PREFIX = "idempotency:";

    private final IdempotencyKeyRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Execute operation idempotently.
     *
     * If the same idempotency key is used again within 24 hours:
     * - If operation is IN_PROGRESS: wait and poll
     * - If operation is COMPLETED: return cached response
     * - If operation is FAILED: retry operation
     *
     * @param tenantId Tenant ID
     * @param idempotencyKey Unique idempotency key
     * @param operationType Operation type (e.g., "customer.create")
     * @param requestBody Request body (will be hashed)
     * @param operation Operation to execute
     * @param <T> Response type
     * @return Operation result
     */
    @Transactional
    public <T> T executeIdempotent(
            String tenantId,
            String idempotencyKey,
            String operationType,
            Object requestBody,
            Supplier<T> operation) {

        String requestHash = hashRequest(requestBody);
        String redisKey = buildRedisKey(tenantId, idempotencyKey);

        // Check Redis cache first
        IdempotencyKey cached = (IdempotencyKey) redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            return handleCachedKey(cached, operation);
        }

        // Check database
        Optional<IdempotencyKey> existing = repository.findByTenantIdAndIdempotencyKey(
            tenantId, idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();

            // Validate request hash matches
            if (!key.getRequestHash().equals(requestHash)) {
                throw new IllegalArgumentException(
                    "Idempotency key reused with different request body");
            }

            return handleExistingKey(key, redisKey, operation);
        }

        // Create new idempotency key
        IdempotencyKey newKey = IdempotencyKey.builder()
            .tenantId(tenantId)
            .idempotencyKey(idempotencyKey)
            .operationType(operationType)
            .requestHash(requestHash)
            .status(IdempotencyStatus.IN_PROGRESS)
            .build();

        newKey = repository.save(newKey);

        // Cache in Redis
        redisTemplate.opsForValue().set(redisKey, newKey, TTL);

        try {
            // Execute operation
            T result = operation.get();

            // Mark as completed
            String responseBody = serializeResponse(result);
            newKey.markCompleted(responseBody, 200);
            repository.save(newKey);

            // Update Redis cache
            redisTemplate.opsForValue().set(redisKey, newKey, TTL);

            log.info("Idempotent operation completed: tenantId={}, key={}, operation={}",
                tenantId, idempotencyKey, operationType);

            return result;

        } catch (Exception e) {
            // Mark as failed
            newKey.markFailed(e.getMessage(), 500);
            repository.save(newKey);

            // Update Redis cache
            redisTemplate.opsForValue().set(redisKey, newKey, TTL);

            log.error("Idempotent operation failed: tenantId={}, key={}, operation={}",
                tenantId, idempotencyKey, operationType, e);

            throw e;
        }
    }

    /**
     * Handle cached idempotency key from Redis.
     */
    @SuppressWarnings("unchecked")
    private <T> T handleCachedKey(IdempotencyKey key, Supplier<T> operation) {
        switch (key.getStatus()) {
            case COMPLETED:
                log.info("Returning cached response for idempotency key: {}",
                    key.getIdempotencyKey());
                return deserializeResponse(key.getResponseBody());

            case IN_PROGRESS:
                throw new IllegalStateException(
                    "Operation already in progress for idempotency key: " +
                    key.getIdempotencyKey());

            case FAILED:
                if (key.canRetry()) {
                    log.info("Retrying failed operation: {}", key.getIdempotencyKey());
                    return operation.get();
                } else {
                    throw new IllegalStateException(
                        "Previous operation failed and cannot be retried: " +
                        key.getIdempotencyKey());
                }

            default:
                throw new IllegalStateException("Unknown idempotency status: " + key.getStatus());
        }
    }

    /**
     * Handle existing idempotency key from database.
     */
    @SuppressWarnings("unchecked")
    private <T> T handleExistingKey(
            IdempotencyKey key,
            String redisKey,
            Supplier<T> operation) {

        // Cache in Redis for future lookups
        redisTemplate.opsForValue().set(redisKey, key, TTL);

        return handleCachedKey(key, operation);
    }

    /**
     * Hash request body using SHA-256.
     */
    private String hashRequest(Object requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash request body", e);
        }
    }

    /**
     * Serialize response to JSON string.
     */
    private String serializeResponse(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("Failed to serialize response", e);
            return null;
        }
    }

    /**
     * Deserialize response from JSON string.
     */
    @SuppressWarnings("unchecked")
    private <T> T deserializeResponse(String json) {
        try {
            return (T) objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize response", e);
            return null;
        }
    }

    /**
     * Build Redis key for idempotency.
     */
    private String buildRedisKey(String tenantId, String idempotencyKey) {
        return REDIS_KEY_PREFIX + tenantId + ":" + idempotencyKey;
    }

    /**
     * Clean up expired idempotency keys from database.
     *
     * Should be called by scheduled job daily.
     */
    @Transactional
    public void cleanupExpiredKeys() {
        Instant cutoff = Instant.now().minus(TTL);
        int deleted = repository.deleteByExpiresAtBefore(cutoff);
        log.info("Cleaned up {} expired idempotency keys", deleted);
    }
}
