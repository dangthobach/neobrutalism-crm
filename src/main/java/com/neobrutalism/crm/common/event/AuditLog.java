package com.neobrutalism.crm.common.event;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log entity for detailed change tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_entity_type_id", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_changed_by", columnList = "changed_by"),
        @Index(name = "idx_audit_changed_at", columnList = "changed_at")
})
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE, etc.

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    public static AuditLog create(String entityType, String entityId, String action,
                                  String fieldName, String oldValue, String newValue,
                                  String changedBy, String reason) {
        return AuditLog.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(Instant.now())
                .changedBy(changedBy)
                .reason(reason)
                .build();
    }
}
