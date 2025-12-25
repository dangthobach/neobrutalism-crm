package com.neobrutalism.crm.domain.role.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Role Hierarchy - Defines parent-child relationships between roles
 *
 * Supports role inheritance where child roles automatically inherit
 * permissions from parent roles.
 *
 * Example hierarchy:
 * SUPER_ADMIN (top)
 *   └─> ADMIN
 *       └─> MANAGER
 *           └─> USER (bottom)
 *
 * In this hierarchy, MANAGER inherits all permissions from ADMIN,
 * which inherits from SUPER_ADMIN.
 */
@Entity
@Table(name = "role_hierarchy", indexes = {
    @Index(name = "idx_role_hierarchy_parent", columnList = "parent_role_id"),
    @Index(name = "idx_role_hierarchy_child", columnList = "child_role_id"),
    @Index(name = "idx_role_hierarchy_tenant", columnList = "tenant_id"),
    @Index(name = "idx_role_hierarchy_active", columnList = "active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_role_hierarchy_parent_child",
                     columnNames = {"parent_role_id", "child_role_id", "tenant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleHierarchy extends SoftDeletableEntity {

    /**
     * Parent role (inherits to child)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id", nullable = false)
    private Role parentRole;

    /**
     * Child role (inherits from parent)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_role_id", nullable = false)
    private Role childRole;

    /**
     * Depth in hierarchy
     * 1 = direct child, 2 = grandchild, etc.
     */
    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel = 1;

    /**
     * Type of inheritance
     * FULL - child inherits all parent permissions
     * PARTIAL - child inherits subset of parent permissions
     * OVERRIDE - child can override parent permissions
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "inheritance_type", nullable = false, length = 50)
    private InheritanceType inheritanceType = InheritanceType.FULL;

    /**
     * Whether this hierarchy relationship is active
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Organization ID for multi-org support
     */
    @Column(name = "organization_id")
    private UUID organizationId;

    /**
     * Tenant ID for multi-tenancy
     */
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /**
     * Inheritance Type Enum
     */
    public enum InheritanceType {
        /**
         * Child role inherits ALL permissions from parent
         */
        FULL,

        /**
         * Child role inherits a SUBSET of permissions from parent
         * (specific permissions can be excluded)
         */
        PARTIAL,

        /**
         * Child role can OVERRIDE parent permissions
         * (can remove or modify inherited permissions)
         */
        OVERRIDE
    }

    /**
     * Check if this is a direct parent-child relationship (level 1)
     */
    public boolean isDirectRelationship() {
        return hierarchyLevel != null && hierarchyLevel == 1;
    }

    /**
     * Check if inheritance is active and enabled
     */
    public boolean isInheritanceActive() {
        return Boolean.TRUE.equals(active) && !Boolean.TRUE.equals(getDeleted());
    }

    /**
     * Factory method to create a direct hierarchy relationship
     */
    public static RoleHierarchy createDirectHierarchy(
        Role parentRole,
        Role childRole,
        String tenantId,
        InheritanceType inheritanceType
    ) {
        return RoleHierarchy.builder()
            .parentRole(parentRole)
            .childRole(childRole)
            .hierarchyLevel(1)
            .inheritanceType(inheritanceType)
            .active(true)
            .tenantId(tenantId)
            .build();
    }

    /**
     * Factory method to create a multi-level hierarchy relationship
     */
    public static RoleHierarchy createMultiLevelHierarchy(
        Role parentRole,
        Role childRole,
        Integer level,
        String tenantId,
        InheritanceType inheritanceType
    ) {
        if (level < 1 || level > 10) {
            throw new IllegalArgumentException("Hierarchy level must be between 1 and 10");
        }

        return RoleHierarchy.builder()
            .parentRole(parentRole)
            .childRole(childRole)
            .hierarchyLevel(level)
            .inheritanceType(inheritanceType)
            .active(true)
            .tenantId(tenantId)
            .build();
    }
}
