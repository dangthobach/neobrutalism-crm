package com.neobrutalism.crm.common.repository;

import com.neobrutalism.crm.common.event.StateTransition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for state transitions
 */
@Repository
public interface StateTransitionRepository extends JpaRepository<StateTransition, Long> {

    /**
     * Find all state transitions for an entity
     */
    Page<StateTransition> findByEntityTypeAndEntityIdOrderByTransitionedAtDesc(
            String entityType, String entityId, Pageable pageable);

    /**
     * Find all state transitions for an entity without pagination
     */
    List<StateTransition> findByEntityTypeAndEntityIdOrderByTransitionedAtDesc(
            String entityType, String entityId);

    /**
     * Find transitions to a specific state
     */
    List<StateTransition> findByEntityTypeAndEntityIdAndToStatusOrderByTransitionedAtDesc(
            String entityType, String entityId, String toStatus);

    /**
     * Find latest transition for an entity
     */
    @Query("SELECT st FROM StateTransition st WHERE st.entityType = :entityType " +
           "AND st.entityId = :entityId ORDER BY st.transitionedAt DESC LIMIT 1")
    StateTransition findLatestByEntity(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId);

    /**
     * Count transitions for an entity
     */
    long countByEntityTypeAndEntityId(String entityType, String entityId);
}
