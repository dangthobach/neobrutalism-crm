package com.neobrutalism.crm.domain.permission.event;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Event: Permission Changed
 *
 * This event is published whenever permissions are modified, including:
 * - Role assignments/removals
 * - Policy additions/deletions
 * - Role hierarchy changes
 * - Permission matrix updates
 *
 * Listeners can use this event to:
 * - Invalidate permission caches
 * - Trigger audit logging
 * - Send notifications
 * - Reload Casbin policies
 *
 * @author Neobrutalism CRM Team
 */
@Getter
@Builder
@ToString
public class PermissionChangedEvent {

    /**
     * Event types
     */
    public enum ChangeType {
        /** Role assigned to user */
        ROLE_ASSIGNED,

        /** Role removed from user */
        ROLE_REMOVED,

        /** Policy added for role */
        POLICY_ADDED,

        /** Policy removed from role */
        POLICY_DELETED,

        /** Role hierarchy changed */
        HIERARCHY_CHANGED,

        /** Bulk permission update */
        BULK_UPDATE,

        /** Manual policy reload */
        POLICY_RELOAD
    }

    /**
     * Type of permission change
     */
    private final ChangeType changeType;

    /**
     * Tenant/Organization ID affected
     */
    private final String tenantId;

    /**
     * User ID affected (for role assignments)
     */
    private final UUID userId;

    /**
     * Role code affected
     */
    private final String roleCode;

    /**
     * Resource affected (for policy changes)
     */
    private final String resource;

    /**
     * Action affected (for policy changes)
     */
    private final String action;

    /**
     * User who made the change
     */
    private final UUID changedBy;

    /**
     * Username who made the change
     */
    private final String changedByUsername;

    /**
     * Reason for the change
     */
    private final String reason;

    /**
     * Timestamp of the change
     */
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Additional metadata (JSON serializable)
     */
    private final String metadata;

    /**
     * Whether this change affects multiple entities
     */
    @Builder.Default
    private final boolean bulk = false;

    /**
     * Factory method: Create event for role assignment
     */
    public static PermissionChangedEvent roleAssigned(
            String tenantId,
            UUID userId,
            String roleCode,
            UUID changedBy,
            String changedByUsername,
            String reason) {
        return PermissionChangedEvent.builder()
                .changeType(ChangeType.ROLE_ASSIGNED)
                .tenantId(tenantId)
                .userId(userId)
                .roleCode(roleCode)
                .changedBy(changedBy)
                .changedByUsername(changedByUsername)
                .reason(reason)
                .build();
    }

    /**
     * Factory method: Create event for role removal
     */
    public static PermissionChangedEvent roleRemoved(
            String tenantId,
            UUID userId,
            String roleCode,
            UUID changedBy,
            String changedByUsername,
            String reason) {
        return PermissionChangedEvent.builder()
                .changeType(ChangeType.ROLE_REMOVED)
                .tenantId(tenantId)
                .userId(userId)
                .roleCode(roleCode)
                .changedBy(changedBy)
                .changedByUsername(changedByUsername)
                .reason(reason)
                .build();
    }

    /**
     * Factory method: Create event for policy addition
     */
    public static PermissionChangedEvent policyAdded(
            String tenantId,
            String roleCode,
            String resource,
            String action,
            UUID changedBy,
            String changedByUsername,
            String reason) {
        return PermissionChangedEvent.builder()
                .changeType(ChangeType.POLICY_ADDED)
                .tenantId(tenantId)
                .roleCode(roleCode)
                .resource(resource)
                .action(action)
                .changedBy(changedBy)
                .changedByUsername(changedByUsername)
                .reason(reason)
                .build();
    }

    /**
     * Factory method: Create event for policy deletion
     */
    public static PermissionChangedEvent policyDeleted(
            String tenantId,
            String roleCode,
            String resource,
            String action,
            UUID changedBy,
            String changedByUsername,
            String reason) {
        return PermissionChangedEvent.builder()
                .changeType(ChangeType.POLICY_DELETED)
                .tenantId(tenantId)
                .roleCode(roleCode)
                .resource(resource)
                .action(action)
                .changedBy(changedBy)
                .changedByUsername(changedByUsername)
                .reason(reason)
                .build();
    }

    /**
     * Factory method: Create event for hierarchy change
     */
    public static PermissionChangedEvent hierarchyChanged(
            String tenantId,
            UUID changedBy,
            String changedByUsername,
            String reason) {
        return PermissionChangedEvent.builder()
                .changeType(ChangeType.HIERARCHY_CHANGED)
                .tenantId(tenantId)
                .changedBy(changedBy)
                .changedByUsername(changedByUsername)
                .reason(reason)
                .build();
    }

    /**
     * Factory method: Create event for bulk update
     */
    public static PermissionChangedEvent bulkUpdate(
            String tenantId,
            UUID changedBy,
            String changedByUsername,
            String reason,
            String metadata) {
        return PermissionChangedEvent.builder()
                .changeType(ChangeType.BULK_UPDATE)
                .tenantId(tenantId)
                .changedBy(changedBy)
                .changedByUsername(changedByUsername)
                .reason(reason)
                .metadata(metadata)
                .bulk(true)
                .build();
    }

    /**
     * Factory method: Create event for policy reload
     */
    public static PermissionChangedEvent policyReload(
            UUID changedBy,
            String changedByUsername,
            String reason) {
        return PermissionChangedEvent.builder()
                .changeType(ChangeType.POLICY_RELOAD)
                .changedBy(changedBy)
                .changedByUsername(changedByUsername)
                .reason(reason)
                .bulk(true)
                .build();
    }

    /**
     * Check if this event affects a specific user
     */
    public boolean affectsUser(UUID targetUserId) {
        return userId != null && userId.equals(targetUserId);
    }

    /**
     * Check if this event affects a specific role
     */
    public boolean affectsRole(String targetRoleCode) {
        return roleCode != null && roleCode.equals(targetRoleCode);
    }

    /**
     * Check if this event affects a specific tenant
     */
    public boolean affectsTenant(String targetTenantId) {
        return tenantId != null && tenantId.equals(targetTenantId);
    }

    /**
     * Get a human-readable description of the event
     */
    public String getDescription() {
        return switch (changeType) {
            case ROLE_ASSIGNED -> String.format("Role '%s' assigned to user %s", roleCode, userId);
            case ROLE_REMOVED -> String.format("Role '%s' removed from user %s", roleCode, userId);
            case POLICY_ADDED -> String.format("Policy added: %s can %s on %s", roleCode, action, resource);
            case POLICY_DELETED -> String.format("Policy deleted: %s can no longer %s on %s", roleCode, action, resource);
            case HIERARCHY_CHANGED -> "Role hierarchy updated";
            case BULK_UPDATE -> "Bulk permission update";
            case POLICY_RELOAD -> "Policies reloaded from database";
        };
    }
}
