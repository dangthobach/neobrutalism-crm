package com.neobrutalism.crm.domain.permission.model;

import com.neobrutalism.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Permission Audit Log - Tracks all permission-related changes
 *
 * Records who changed what permission, when, and why.
 * Essential for security compliance and troubleshooting.
 */
@Entity
@Table(name = "permission_audit_logs", indexes = {
    @Index(name = "idx_perm_audit_user", columnList = "changed_by_user_id"),
    @Index(name = "idx_perm_audit_target", columnList = "target_user_id"),
    @Index(name = "idx_perm_audit_action", columnList = "action_type"),
    @Index(name = "idx_perm_audit_timestamp", columnList = "changed_at"),
    @Index(name = "idx_perm_audit_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PermissionAuditLog extends BaseEntity {

    /**
     * Type of permission action performed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private PermissionActionType actionType;

    /**
     * User who performed the action
     */
    @Column(name = "changed_by_user_id", nullable = false)
    private UUID changedByUserId;

    /**
     * Username of person who made the change (denormalized for reporting)
     */
    @Column(name = "changed_by_username", length = 100)
    private String changedByUsername;

    /**
     * Target user affected by the permission change
     */
    @Column(name = "target_user_id")
    private UUID targetUserId;

    /**
     * Target username (denormalized for reporting)
     */
    @Column(name = "target_username", length = 100)
    private String targetUsername;

    /**
     * Target role code (for role-based changes)
     */
    @Column(name = "target_role_code", length = 100)
    private String targetRoleCode;

    /**
     * Permission resource (e.g., "customer", "task", "report")
     */
    @Column(name = "resource", length = 100)
    private String resource;

    /**
     * Permission action (e.g., "read", "write", "delete")
     */
    @Column(name = "action", length = 50)
    private String action;

    /**
     * Data scope level (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_scope", length = 50)
    private com.neobrutalism.crm.domain.user.model.DataScope dataScope;

    /**
     * Branch ID affected by scope change
     */
    @Column(name = "branch_id")
    private UUID branchId;

    /**
     * Old value before change (JSON format)
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * New value after change (JSON format)
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Reason for the change (optional, provided by admin)
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * IP address of user who made the change
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent / browser information
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * When the change occurred
     */
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    /**
     * Success or failure of the operation
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
     * Organization ID for multi-tenancy
     */
    @Column(name = "organization_id")
    private UUID organizationId;

    /**
     * Tenant ID for multi-tenancy
     */
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /**
     * Additional metadata (JSON format)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Session ID for correlating related changes
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }

    /**
     * Helper method to create an audit log for role assignment
     */
    public static PermissionAuditLog forRoleAssignment(
            UUID changedByUserId,
            String changedByUsername,
            UUID targetUserId,
            String targetUsername,
            String roleCode,
            String reason,
            String tenantId
    ) {
        return PermissionAuditLog.builder()
                .actionType(PermissionActionType.ROLE_ASSIGNED)
                .changedByUserId(changedByUserId)
                .changedByUsername(changedByUsername)
                .targetUserId(targetUserId)
                .targetUsername(targetUsername)
                .targetRoleCode(roleCode)
                .reason(reason)
                .tenantId(tenantId)
                .success(true)
                .build();
    }

    /**
     * Helper method to create an audit log for role removal
     */
    public static PermissionAuditLog forRoleRemoval(
            UUID changedByUserId,
            String changedByUsername,
            UUID targetUserId,
            String targetUsername,
            String roleCode,
            String reason,
            String tenantId
    ) {
        return PermissionAuditLog.builder()
                .actionType(PermissionActionType.ROLE_REMOVED)
                .changedByUserId(changedByUserId)
                .changedByUsername(changedByUsername)
                .targetUserId(targetUserId)
                .targetUsername(targetUsername)
                .targetRoleCode(roleCode)
                .reason(reason)
                .tenantId(tenantId)
                .success(true)
                .build();
    }

    /**
     * Helper method to create an audit log for data scope change
     */
    public static PermissionAuditLog forDataScopeChange(
            UUID changedByUserId,
            String changedByUsername,
            UUID targetUserId,
            String targetUsername,
            com.neobrutalism.crm.domain.user.model.DataScope oldScope,
            com.neobrutalism.crm.domain.user.model.DataScope newScope,
            String reason,
            String tenantId
    ) {
        return PermissionAuditLog.builder()
                .actionType(PermissionActionType.DATA_SCOPE_CHANGED)
                .changedByUserId(changedByUserId)
                .changedByUsername(changedByUsername)
                .targetUserId(targetUserId)
                .targetUsername(targetUsername)
                .dataScope(newScope)
                .oldValue(oldScope != null ? oldScope.name() : null)
                .newValue(newScope != null ? newScope.name() : null)
                .reason(reason)
                .tenantId(tenantId)
                .success(true)
                .build();
    }

    /**
     * Helper method to create an audit log for policy creation
     */
    public static PermissionAuditLog forPolicyCreation(
            UUID changedByUserId,
            String changedByUsername,
            String roleCode,
            String resource,
            String action,
            String reason,
            String tenantId
    ) {
        return PermissionAuditLog.builder()
                .actionType(PermissionActionType.POLICY_CREATED)
                .changedByUserId(changedByUserId)
                .changedByUsername(changedByUsername)
                .targetRoleCode(roleCode)
                .resource(resource)
                .action(action)
                .reason(reason)
                .tenantId(tenantId)
                .success(true)
                .build();
    }

    /**
     * Helper method to create an audit log for policy deletion
     */
    public static PermissionAuditLog forPolicyDeletion(
            UUID changedByUserId,
            String changedByUsername,
            String roleCode,
            String resource,
            String action,
            String reason,
            String tenantId
    ) {
        return PermissionAuditLog.builder()
                .actionType(PermissionActionType.POLICY_DELETED)
                .changedByUserId(changedByUserId)
                .changedByUsername(changedByUsername)
                .targetRoleCode(roleCode)
                .resource(resource)
                .action(action)
                .reason(reason)
                .tenantId(tenantId)
                .success(true)
                .build();
    }
}
