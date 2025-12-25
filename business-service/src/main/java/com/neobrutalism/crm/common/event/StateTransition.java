package com.neobrutalism.crm.common.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * State transition tracking entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "state_transitions", indexes = {
        @Index(name = "idx_st_entity_type_id", columnList = "entity_type, entity_id"),
        @Index(name = "idx_transitioned_at", columnList = "transitioned_at")
})
public class StateTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 50)
    private String toStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "transitioned_at", nullable = false)
    private Instant transitionedAt;

    @Column(name = "transitioned_by", length = 100)
    private String transitionedBy;

    public static StateTransition create(String entityType, String entityId,
                                         String fromStatus, String toStatus,
                                         String reason, String transitionedBy) {
        return StateTransition.builder()
                .entityType(entityType)
                .entityId(entityId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .transitionedAt(Instant.now())
                .transitionedBy(transitionedBy)
                .build();
    }
}
