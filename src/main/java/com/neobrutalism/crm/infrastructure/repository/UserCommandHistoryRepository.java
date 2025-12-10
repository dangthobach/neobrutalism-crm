package com.neobrutalism.crm.infrastructure.repository;

import com.neobrutalism.crm.domain.command.model.UserCommandHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for UserCommandHistory entities.
 *
 * @author Admin
 * @since Phase 1
 */
@Repository
public interface UserCommandHistoryRepository extends JpaRepository<UserCommandHistory, UUID> {

    /**
     * Find command history for user.
     */
    List<UserCommandHistory> findByTenantIdAndUserId(
        String tenantId,
        UUID userId,
        Pageable pageable);

    /**
     * Find top commands executed by user (for suggestions).
     *
     * @return List of [commandId, executionCount] arrays
     */
    @Query("SELECT h.commandId, COUNT(h) as cnt " +
           "FROM UserCommandHistory h " +
           "WHERE h.userId = :userId " +
           "GROUP BY h.commandId " +
           "ORDER BY cnt DESC")
    List<Object[]> findTopCommandsByUser(
        @Param("userId") UUID userId,
        Pageable pageable);
}
