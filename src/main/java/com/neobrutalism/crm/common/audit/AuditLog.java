package com.neobrutalism.crm.common.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity for storing audit logs
 * Records all create, update, delete operations with full context
 */
@Entity(name = "ApplicationAuditLog")
@Table(name = "audit_logs", 
    indexes = {
        @Index(name = "idx_audit_tenant", columnList = "tenant_id"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_date", columnList = "created_at"),
        @Index(name = "idx_audit_tenant_entity", columnList = "tenant_id, entity_type, entity_id"),
        @Index(name = "idx_audit_tenant_user_date", columnList = "tenant_id, user_id, created_at")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Tenant ID for multi-tenancy
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Entity type (e.g., "Customer", "Contract", "User")
     */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;
    
    /**
     * Entity ID that was affected
     */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    /**
     * Action performed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;
    
    /**
     * User who performed the action
     */
    @Column(name = "user_id")
    private UUID userId;
    
    /**
     * Username for display (denormalized for performance)
     */
    @Column(name = "username", length = 255)
    private String username;
    
    /**
     * Description of the action
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * Changes made (JSON format: {field: {old: value, new: value}})
     * Example: {"status": {"old": "DRAFT", "new": "ACTIVE"}, "price": {"old": 100, "new": 150}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", columnDefinition = "jsonb")
    private Map<String, Object> changes;
    
    /**
     * Old values (before change) - full entity snapshot for critical entities
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Map<String, Object> oldValues;
    
    /**
     * New values (after change) - full entity snapshot for critical entities
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues;
    
    /**
     * Request parameters (optional)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_params", columnDefinition = "jsonb")
    private Map<String, Object> requestParams;
    
    /**
     * IP address of the request
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    /**
     * User agent string
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    /**
     * Method name that was called
     */
    @Column(name = "method_name", length = 200)
    private String methodName;
    
    /**
     * Execution time in milliseconds
     */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    /**
     * Whether the operation was successful
     */
    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true;
    
    /**
     * Error message if operation failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Timestamp when the audit log was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
