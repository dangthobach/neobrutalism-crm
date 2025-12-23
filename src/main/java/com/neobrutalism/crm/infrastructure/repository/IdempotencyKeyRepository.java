package com.neobrutalism.crm.infrastructure.repository;

import com.neobrutalism.crm.domain.idempotency.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for IdempotencyKey entities.
 *
 * @author Admin
 * @since Phase 1
 */
@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    /**
     * Find idempotency key by tenant and key value.
     */
    Optional<IdempotencyKey> findByTenantIdAndIdempotencyKey(
        String tenantId,
        String idempotencyKey);

    /**
     * Delete expired idempotency keys.
     *
     * @param cutoff Cutoff timestamp
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
