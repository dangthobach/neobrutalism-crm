package com.neobrutalism.crm.common.repository;

import com.neobrutalism.crm.common.event.EventStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for event store
 */
@Repository
public interface EventStoreRepository extends JpaRepository<EventStore, Long> {

    /**
     * Find all events for an aggregate
     */
    List<EventStore> findByAggregateIdOrderByOccurredAtAsc(String aggregateId);

    /**
     * Find all events for an aggregate by type
     */
    List<EventStore> findByAggregateIdAndAggregateTypeOrderByOccurredAtAsc(
            String aggregateId, String aggregateType);

    /**
     * Find all events by event type
     */
    List<EventStore> findByEventTypeOrderByOccurredAtAsc(String eventType);

    /**
     * Count events for an aggregate
     */
    long countByAggregateId(String aggregateId);

    /**
     * Find latest event for an aggregate
     */
    @Query("SELECT e FROM EventStore e WHERE e.aggregateId = :aggregateId " +
           "ORDER BY e.occurredAt DESC LIMIT 1")
    EventStore findLatestByAggregateId(@Param("aggregateId") String aggregateId);
}
