package com.neobrutalism.crm.infrastructure.repository;

import com.neobrutalism.crm.domain.command.model.Command;
import com.neobrutalism.crm.domain.command.model.CommandCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Command entities.
 *
 * @author Admin
 * @since Phase 1
 */
@Repository
public interface CommandRepository extends JpaRepository<Command, UUID> {

    /**
     * Find active commands by tenant.
     */
    Page<Command> findByTenantIdAndIsActiveTrue(String tenantId, Pageable pageable);

    /**
     * Find active commands by tenant and category.
     */
    Page<Command> findByTenantIdAndCategoryAndIsActiveTrue(
        String tenantId,
        CommandCategory category,
        Pageable pageable);

    /**
     * Find command by command ID.
     */
    Optional<Command> findByCommandId(String commandId);

    /**
     * Search commands by label, description, or keywords.
     */
    @Query("SELECT c FROM Command c WHERE c.tenantId = :tenantId " +
           "AND c.isActive = true " +
           "AND (LOWER(c.label) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.searchKeywords) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Command> searchCommands(
        @Param("tenantId") String tenantId,
        @Param("query") String query,
        Pageable pageable);
}
