package com.neobrutalism.crm.domain.organization.repository;

import com.neobrutalism.crm.domain.organization.model.OrganizationReadModel;
import com.neobrutalism.crm.domain.organization.model.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for OrganizationReadModel - optimized for queries
 * Uses denormalized structure for faster reads
 */
@Repository
public interface OrganizationReadModelRepository extends JpaRepository<OrganizationReadModel, UUID> {

    /**
     * Find all active organizations
     * Uses idx_org_rm_active index
     */
    List<OrganizationReadModel> findByIsActiveTrue();

    /**
     * Find all non-deleted organizations
     */
    List<OrganizationReadModel> findByIsDeletedFalse();

    /**
     * Find organizations by status
     * Uses idx_org_rm_status index
     */
    List<OrganizationReadModel> findByStatus(OrganizationStatus status);

    /**
     * Find organizations by code (exact match)
     * Uses idx_org_rm_code index
     */
    OrganizationReadModel findByCode(String code);

    /**
     * Full-text search across organization data
     * Uses idx_org_rm_search index
     */
    @Query("SELECT o FROM OrganizationReadModel o WHERE LOWER(o.searchText) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND o.isDeleted = false")
    List<OrganizationReadModel> search(@Param("searchTerm") String searchTerm);

    /**
     * Find organizations created after a specific date
     * Uses idx_org_rm_created index
     */
    List<OrganizationReadModel> findByCreatedAtAfter(Instant date);

    /**
     * Find organizations with contact info
     */
    List<OrganizationReadModel> findByHasContactInfoTrue();

    /**
     * Find recently created organizations (within N days)
     */
    @Query("SELECT o FROM OrganizationReadModel o WHERE o.daysSinceCreated <= :days AND o.isDeleted = false ORDER BY o.createdAt DESC")
    List<OrganizationReadModel> findRecentlyCreated(@Param("days") int days);

    /**
     * Find active organizations with contact info (common query pattern)
     */
    @Query("SELECT o FROM OrganizationReadModel o WHERE o.isActive = true AND o.hasContactInfo = true AND o.isDeleted = false")
    List<OrganizationReadModel> findActiveWithContactInfo();

    /**
     * Count organizations by status
     */
    long countByStatus(OrganizationStatus status);

    /**
     * Get organization statistics
     */
    @Query("SELECT new map(" +
            "COUNT(o) as total, " +
            "SUM(CASE WHEN o.isActive = true THEN 1 ELSE 0 END) as active, " +
            "SUM(CASE WHEN o.hasContactInfo = true THEN 1 ELSE 0 END) as withContact, " +
            "SUM(CASE WHEN o.isDeleted = true THEN 1 ELSE 0 END) as deleted) " +
            "FROM OrganizationReadModel o")
    java.util.Map<String, Long> getStatistics();
}
