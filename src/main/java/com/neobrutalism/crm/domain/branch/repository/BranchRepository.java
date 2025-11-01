package com.neobrutalism.crm.domain.branch.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.domain.branch.Branch;
import com.neobrutalism.crm.domain.branch.BranchStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Branch entity
 */
@Repository
public interface BranchRepository extends StatefulRepository<Branch, BranchStatus> {

    /**
     * Find branch by code and tenant ID
     */
    @Query("SELECT b FROM Branch b WHERE b.code = :code AND b.tenantId = :tenantId AND b.deleted = false")
    Optional<Branch> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") UUID tenantId);

    /**
     * Find all branches by organization ID
     */
    @Query("SELECT b FROM Branch b WHERE b.organizationId = :organizationId AND b.deleted = false")
    List<Branch> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all child branches by parent ID
     */
    @Query("SELECT b FROM Branch b WHERE b.parentId = :parentId AND b.deleted = false ORDER BY b.displayOrder, b.name")
    List<Branch> findByParentId(@Param("parentId") UUID parentId);

    /**
     * Find all root branches (no parent)
     */
    @Query("SELECT b FROM Branch b WHERE b.parentId IS NULL AND b.tenantId = :tenantId AND b.deleted = false ORDER BY b.displayOrder, b.name")
    List<Branch> findRootBranches(@Param("tenantId") UUID tenantId);

    /**
     * Find branches by branch type
     */
    @Query("SELECT b FROM Branch b WHERE b.branchType = :branchType AND b.tenantId = :tenantId AND b.deleted = false")
    List<Branch> findByBranchType(@Param("branchType") Branch.BranchType branchType, @Param("tenantId") UUID tenantId);

    /**
     * Find all branches in hierarchy (by path prefix)
     */
    @Query("SELECT b FROM Branch b WHERE b.path LIKE :pathPrefix% AND b.tenantId = :tenantId AND b.deleted = false ORDER BY b.path")
    List<Branch> findByPathPrefix(@Param("pathPrefix") String pathPrefix, @Param("tenantId") UUID tenantId);

    /**
     * Find branches by manager ID
     */
    @Query("SELECT b FROM Branch b WHERE b.managerId = :managerId AND b.deleted = false")
    List<Branch> findByManagerId(@Param("managerId") UUID managerId);

    /**
     * Check if branch code exists in organization
     */
    @Query("SELECT COUNT(b) > 0 FROM Branch b WHERE b.code = :code AND b.organizationId = :organizationId AND b.deleted = false")
    boolean existsByCodeAndOrganizationId(@Param("code") String code, @Param("organizationId") UUID organizationId);

    /**
     * Check if branch code exists (excluding specific ID)
     */
    @Query("SELECT COUNT(b) > 0 FROM Branch b WHERE b.code = :code AND b.organizationId = :organizationId AND b.id != :excludeId AND b.deleted = false")
    boolean existsByCodeAndOrganizationIdExcluding(@Param("code") String code, @Param("organizationId") UUID organizationId, @Param("excludeId") UUID excludeId);

    /**
     * Find branches by level
     */
    @Query("SELECT b FROM Branch b WHERE b.level = :level AND b.tenantId = :tenantId AND b.deleted = false ORDER BY b.displayOrder, b.name")
    List<Branch> findByLevel(@Param("level") Integer level, @Param("tenantId") UUID tenantId);

    /**
     * Get all ancestor branches (from path)
     */
    @Query("SELECT b FROM Branch b WHERE b.tenantId = :tenantId AND b.code IN :codes AND b.deleted = false ORDER BY b.level")
    List<Branch> findByCodesInPath(@Param("codes") List<String> codes, @Param("tenantId") UUID tenantId);

    /**
     * Count branches by organization
     */
    @Query("SELECT COUNT(b) FROM Branch b WHERE b.organizationId = :organizationId AND b.deleted = false")
    long countByOrganizationId(@Param("organizationId") UUID organizationId);
}
