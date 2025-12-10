package com.neobrutalism.crm.domain.role.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.model.RoleHierarchy;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Role Hierarchy
 */
@Repository
public interface RoleHierarchyRepository extends BaseRepository<RoleHierarchy> {

    /**
     * Find direct parent roles for a child role
     */
    @Query("SELECT rh FROM RoleHierarchy rh WHERE rh.childRole.id = :childRoleId AND rh.tenantId = :tenantId AND rh.deleted = false AND rh.active = true ORDER BY rh.hierarchyLevel")
    List<RoleHierarchy> findParentsByChildRole(@Param("childRoleId") UUID childRoleId, @Param("tenantId") String tenantId);

    /**
     * Find direct child roles for a parent role
     */
    @Query("SELECT rh FROM RoleHierarchy rh WHERE rh.parentRole.id = :parentRoleId AND rh.tenantId = :tenantId AND rh.deleted = false AND rh.active = true ORDER BY rh.hierarchyLevel")
    List<RoleHierarchy> findChildrenByParentRole(@Param("parentRoleId") UUID parentRoleId, @Param("tenantId") String tenantId);

    /**
     * Find all ancestors (parents, grandparents, etc.) for a role
     */
    @Query(value = """
        WITH RECURSIVE role_ancestors AS (
            -- Base case: direct parents
            SELECT rh.parent_role_id, rh.child_role_id, rh.hierarchy_level, 1 as depth
            FROM role_hierarchy rh
            WHERE rh.child_role_id = :childRoleId
              AND rh.tenant_id = :tenantId
              AND rh.deleted = false
              AND rh.active = true

            UNION ALL

            -- Recursive case: parents of parents
            SELECT rh.parent_role_id, rh.child_role_id, rh.hierarchy_level, ra.depth + 1
            FROM role_hierarchy rh
            JOIN role_ancestors ra ON ra.parent_role_id = rh.child_role_id
            WHERE rh.tenant_id = :tenantId
              AND rh.deleted = false
              AND rh.active = true
              AND ra.depth < 10
        )
        SELECT DISTINCT r.*
        FROM roles r
        JOIN role_ancestors ra ON ra.parent_role_id = r.id
        WHERE r.deleted = false
        ORDER BY r.code
    """, nativeQuery = true)
    List<Role> findAllAncestors(@Param("childRoleId") UUID childRoleId, @Param("tenantId") String tenantId);

    /**
     * Find all descendants (children, grandchildren, etc.) for a role
     */
    @Query(value = """
        WITH RECURSIVE role_descendants AS (
            -- Base case: direct children
            SELECT rh.parent_role_id, rh.child_role_id, rh.hierarchy_level, 1 as depth
            FROM role_hierarchy rh
            WHERE rh.parent_role_id = :parentRoleId
              AND rh.tenant_id = :tenantId
              AND rh.deleted = false
              AND rh.active = true

            UNION ALL

            -- Recursive case: children of children
            SELECT rh.parent_role_id, rh.child_role_id, rh.hierarchy_level, rd.depth + 1
            FROM role_hierarchy rh
            JOIN role_descendants rd ON rd.child_role_id = rh.parent_role_id
            WHERE rh.tenant_id = :tenantId
              AND rh.deleted = false
              AND rh.active = true
              AND rd.depth < 10
        )
        SELECT DISTINCT r.*
        FROM roles r
        JOIN role_descendants rd ON rd.child_role_id = r.id
        WHERE r.deleted = false
        ORDER BY r.code
    """, nativeQuery = true)
    List<Role> findAllDescendants(@Param("parentRoleId") UUID parentRoleId, @Param("tenantId") String tenantId);

    /**
     * Check if a hierarchy relationship exists
     */
    @Query("SELECT COUNT(rh) > 0 FROM RoleHierarchy rh WHERE rh.parentRole.id = :parentRoleId AND rh.childRole.id = :childRoleId AND rh.tenantId = :tenantId AND rh.deleted = false")
    boolean existsByParentAndChild(@Param("parentRoleId") UUID parentRoleId, @Param("childRoleId") UUID childRoleId, @Param("tenantId") String tenantId);

    /**
     * Find hierarchy relationship by parent and child
     */
    @Query("SELECT rh FROM RoleHierarchy rh WHERE rh.parentRole.id = :parentRoleId AND rh.childRole.id = :childRoleId AND rh.tenantId = :tenantId AND rh.deleted = false")
    Optional<RoleHierarchy> findByParentAndChild(@Param("parentRoleId") UUID parentRoleId, @Param("childRoleId") UUID childRoleId, @Param("tenantId") String tenantId);

    /**
     * Find all hierarchies for a tenant
     */
    @Query("SELECT rh FROM RoleHierarchy rh WHERE rh.tenantId = :tenantId AND rh.deleted = false ORDER BY rh.hierarchyLevel, rh.parentRole.code, rh.childRole.code")
    List<RoleHierarchy> findByTenant(@Param("tenantId") String tenantId);

    /**
     * Find active hierarchies for a tenant
     */
    @Query("SELECT rh FROM RoleHierarchy rh WHERE rh.tenantId = :tenantId AND rh.active = true AND rh.deleted = false ORDER BY rh.hierarchyLevel, rh.parentRole.code, rh.childRole.code")
    List<RoleHierarchy> findActiveByTenant(@Param("tenantId") String tenantId);

    /**
     * Count direct children for a parent role
     */
    @Query("SELECT COUNT(rh) FROM RoleHierarchy rh WHERE rh.parentRole.id = :parentRoleId AND rh.tenantId = :tenantId AND rh.hierarchyLevel = 1 AND rh.deleted = false AND rh.active = true")
    long countDirectChildren(@Param("parentRoleId") UUID parentRoleId, @Param("tenantId") String tenantId);

    /**
     * Count direct parents for a child role
     */
    @Query("SELECT COUNT(rh) FROM RoleHierarchy rh WHERE rh.childRole.id = :childRoleId AND rh.tenantId = :tenantId AND rh.hierarchyLevel = 1 AND rh.deleted = false AND rh.active = true")
    long countDirectParents(@Param("childRoleId") UUID childRoleId, @Param("tenantId") String tenantId);

    /**
     * Get maximum hierarchy depth for a role
     */
    @Query("SELECT COALESCE(MAX(rh.hierarchyLevel), 0) FROM RoleHierarchy rh WHERE rh.childRole.id = :roleId AND rh.tenantId = :tenantId AND rh.deleted = false AND rh.active = true")
    int getMaxHierarchyDepth(@Param("roleId") UUID roleId, @Param("tenantId") String tenantId);

    /**
     * Find roles at specific hierarchy level
     */
    @Query("SELECT rh FROM RoleHierarchy rh WHERE rh.hierarchyLevel = :level AND rh.tenantId = :tenantId AND rh.deleted = false AND rh.active = true")
    List<RoleHierarchy> findByHierarchyLevel(@Param("level") Integer level, @Param("tenantId") String tenantId);

    /**
     * Delete hierarchy relationship
     */
    @Query("UPDATE RoleHierarchy rh SET rh.deleted = true WHERE rh.parentRole.id = :parentRoleId AND rh.childRole.id = :childRoleId AND rh.tenantId = :tenantId")
    int deleteHierarchy(@Param("parentRoleId") UUID parentRoleId, @Param("childRoleId") UUID childRoleId, @Param("tenantId") String tenantId);
}
