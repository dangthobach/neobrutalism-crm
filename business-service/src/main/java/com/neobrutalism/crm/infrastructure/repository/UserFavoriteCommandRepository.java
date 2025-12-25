package com.neobrutalism.crm.infrastructure.repository;

import com.neobrutalism.crm.domain.command.model.UserFavoriteCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserFavoriteCommand entities.
 *
 * @author Admin
 * @since Phase 1
 */
@Repository
public interface UserFavoriteCommandRepository extends JpaRepository<UserFavoriteCommand, UUID> {

    /**
     * Find favorite commands for user ordered by sort order.
     */
    List<UserFavoriteCommand> findByTenantIdAndUserIdOrderBySortOrderAsc(
        String tenantId,
        UUID userId);

    /**
     * Check if command is already favorited.
     */
    boolean existsByUserIdAndCommandId(UUID userId, UUID commandId);

    /**
     * Delete favorite by user and command.
     */
    void deleteByUserIdAndCommandId(UUID userId, UUID commandId);

    /**
     * Find max sort order for user.
     */
    @Query("SELECT MAX(f.sortOrder) FROM UserFavoriteCommand f WHERE f.userId = :userId")
    Optional<Integer> findMaxSortOrderByUserId(@Param("userId") UUID userId);
}
