package com.neobrutalism.crm.domain.customer.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.common.security.DataScopeHelper;
import com.neobrutalism.crm.domain.customer.model.Customer;
import com.neobrutalism.crm.domain.customer.model.CustomerStatus;
import com.neobrutalism.crm.domain.customer.model.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Customer entity
 */
@Repository
public interface CustomerRepository extends StatefulRepository<Customer, CustomerStatus> {

    /**
     * Find customer by code and tenant ID
     */
    @Query("SELECT c FROM Customer c WHERE c.code = :code AND c.tenantId = :tenantId AND c.deleted = false")
    Optional<Customer> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") String tenantId);

    /**
     * Find customer by email
     */
    @Query("SELECT c FROM Customer c WHERE c.email = :email AND c.deleted = false")
    Optional<Customer> findByEmail(@Param("email") String email);

    /**
     * Find all customers by organization
     */
    @Query("SELECT c FROM Customer c WHERE c.organizationId = :organizationId AND c.deleted = false")
    List<Customer> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all customers by owner (account manager)
     */
    @Query("SELECT c FROM Customer c WHERE c.ownerId = :ownerId AND c.deleted = false ORDER BY c.companyName")
    List<Customer> findByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Find all customers by branch
     */
    @Query("SELECT c FROM Customer c WHERE c.branchId = :branchId AND c.deleted = false")
    List<Customer> findByBranchId(@Param("branchId") UUID branchId);

    /**
     * Find customers by type
     */
    @Query("SELECT c FROM Customer c WHERE c.customerType = :type AND c.tenantId = :tenantId AND c.deleted = false")
    List<Customer> findByCustomerType(@Param("type") CustomerType type, @Param("tenantId") String tenantId);

    /**
     * Find VIP customers
     */
    @Query("SELECT c FROM Customer c WHERE c.isVip = true AND c.tenantId = :tenantId AND c.deleted = false ORDER BY c.companyName")
    List<Customer> findVipCustomers(@Param("tenantId") String tenantId);

    /**
     * Search customers by company name
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND c.deleted = false")
    List<Customer> searchByCompanyName(@Param("keyword") String keyword);

    /**
     * Find customers acquired within date range
     */
    @Query("SELECT c FROM Customer c WHERE c.acquisitionDate BETWEEN :startDate AND :endDate AND c.deleted = false ORDER BY c.acquisitionDate DESC")
    List<Customer> findByAcquisitionDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find customers requiring follow-up
     */
    @Query("SELECT c FROM Customer c WHERE c.nextFollowupDate <= :date AND c.status = :status AND c.deleted = false ORDER BY c.nextFollowupDate")
    List<Customer> findRequiringFollowup(@Param("date") LocalDate date, @Param("status") CustomerStatus status);

    /**
     * Find customers by tag
     */
    @Query("SELECT c FROM Customer c WHERE c.tags LIKE CONCAT('%', :tag, '%') AND c.deleted = false")
    List<Customer> findByTag(@Param("tag") String tag);

    /**
     * Find customers by lead source
     */
    @Query("SELECT c FROM Customer c WHERE c.leadSource = :leadSource AND c.deleted = false")
    List<Customer> findByLeadSource(@Param("leadSource") String leadSource);

    /**
     * Check if customer code exists
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.code = :code AND c.organizationId = :organizationId AND c.deleted = false")
    boolean existsByCodeAndOrganizationId(@Param("code") String code, @Param("organizationId") UUID organizationId);

    /**
     * Check if customer code exists (excluding specific customer)
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.code = :code AND c.organizationId = :organizationId AND c.id != :excludeId AND c.deleted = false")
    boolean existsByCodeAndOrganizationIdExcluding(@Param("code") String code, @Param("organizationId") UUID organizationId, @Param("excludeId") UUID excludeId);

    /**
     * Count customers by status
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = :status AND c.tenantId = :tenantId AND c.deleted = false")
    long countByStatusAndTenantId(@Param("status") CustomerStatus status, @Param("tenantId") String tenantId);

    /**
     * Count customers by type
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.customerType = :type AND c.tenantId = :tenantId AND c.deleted = false")
    long countByTypeAndTenantId(@Param("type") CustomerType type, @Param("tenantId") String tenantId);

    /**
     * Get customers with no recent contact
     */
    @Query("SELECT c FROM Customer c WHERE c.lastContactDate < :date AND c.status IN :statuses AND c.deleted = false ORDER BY c.lastContactDate")
    List<Customer> findWithNoRecentContact(@Param("date") LocalDate date, @Param("statuses") List<CustomerStatus> statuses);

    /**
     * Count all customers by tenant ID
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.tenantId = :tenantId AND c.deleted = false")
    long countByTenantId(@Param("tenantId") String tenantId);

    /**
     * Count VIP customers by tenant ID
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isVip = :isVip AND c.tenantId = :tenantId AND c.deleted = false")
    long countByIsVipAndTenantId(@Param("isVip") boolean isVip, @Param("tenantId") String tenantId);

    // ========================================
    // ✅ PHASE 1: OPTIMIZED QUERIES WITH DTO PROJECTION
    // ========================================

    /**
     * Find customers by organization with full details (prevents N+1)
     * Uses DTO projection to fetch organization, owner, and branch data in single query
     */
    @Query("SELECT new com.neobrutalism.crm.domain.customer.dto.CustomerWithDetailsDTO(" +
           "c.id, c.code, c.companyName, c.customerType, c.status, " +
           "c.industry, c.email, c.phone, c.isVip, " +
           "c.annualRevenue, c.acquisitionDate, c.lastContactDate, " +
           "c.organizationId, o.name, " +
           "c.ownerId, CONCAT(owner.firstName, ' ', owner.lastName), " +
           "c.branchId, branch.name, " +
           "c.tenantId, c.deleted) " +
           "FROM Customer c " +
           "LEFT JOIN Organization o ON c.organizationId = o.id " +
           "LEFT JOIN User owner ON c.ownerId = owner.id " +
           "LEFT JOIN Branch branch ON c.branchId = branch.id " +
           "WHERE c.organizationId = :orgId AND c.deleted = false " +
           "ORDER BY c.companyName")
    List<com.neobrutalism.crm.domain.customer.dto.CustomerWithDetailsDTO> findByOrganizationWithDetails(@Param("orgId") UUID orgId);

    /**
     * Find customers by status with details (optimized for filtering)
     */
    @Query("SELECT new com.neobrutalism.crm.domain.customer.dto.CustomerWithDetailsDTO(" +
           "c.id, c.code, c.companyName, c.customerType, c.status, " +
           "c.email, c.phone, c.isVip, " +
           "c.organizationId, o.name, " +
           "c.tenantId, c.deleted) " +
           "FROM Customer c " +
           "LEFT JOIN Organization o ON c.organizationId = o.id " +
           "WHERE c.status = :status AND c.tenantId = :tenantId AND c.deleted = false " +
           "ORDER BY c.companyName")
    List<com.neobrutalism.crm.domain.customer.dto.CustomerWithDetailsDTO> findByStatusWithDetails(
        @Param("status") CustomerStatus status, 
        @Param("tenantId") String tenantId
    );

    /**
     * Find customers by type with details (optimized for filtering)
     */
    @Query("SELECT new com.neobrutalism.crm.domain.customer.dto.CustomerWithDetailsDTO(" +
           "c.id, c.code, c.companyName, c.customerType, c.status, " +
           "c.email, c.phone, c.isVip, " +
           "c.organizationId, o.name, " +
           "c.tenantId, c.deleted) " +
           "FROM Customer c " +
           "LEFT JOIN Organization o ON c.organizationId = o.id " +
           "WHERE c.customerType = :type AND c.tenantId = :tenantId AND c.deleted = false " +
           "ORDER BY c.companyName")
    List<com.neobrutalism.crm.domain.customer.dto.CustomerWithDetailsDTO> findByTypeWithDetails(
        @Param("type") CustomerType type, 
        @Param("tenantId") String tenantId
    );

    /**
     * Find VIP customers with details (optimized for reporting)
     */
    @Query("SELECT new com.neobrutalism.crm.domain.customer.dto.CustomerWithDetailsDTO(" +
           "c.id, c.code, c.companyName, c.customerType, c.status, " +
           "c.industry, c.email, c.phone, c.isVip, " +
           "c.annualRevenue, c.acquisitionDate, c.lastContactDate, " +
           "c.organizationId, o.name, " +
           "c.ownerId, CONCAT(owner.firstName, ' ', owner.lastName), " +
           "c.branchId, branch.name, " +
           "c.tenantId, c.deleted) " +
           "FROM Customer c " +
           "LEFT JOIN Organization o ON c.organizationId = o.id " +
           "LEFT JOIN User owner ON c.ownerId = owner.id " +
           "LEFT JOIN Branch branch ON c.branchId = branch.id " +
           "WHERE c.isVip = true AND c.tenantId = :tenantId AND c.deleted = false " +
           "ORDER BY c.annualRevenue DESC, c.companyName")
    List<com.neobrutalism.crm.domain.customer.dto.CustomerWithDetailsDTO> findVipCustomersWithDetails(@Param("tenantId") String tenantId);

    // ========================================
    // ✅ N+1 QUERY FIXES: Optimized pagination queries
    // ========================================

    /**
     * Find all active customers with pagination (optimized - no N+1)
     * Uses EntityGraph to ensure efficient loading
     */
    @EntityGraph(attributePaths = {}) // No relationships to fetch, but prepared for future
    @Query("SELECT c FROM Customer c WHERE c.deleted = false")
    Page<Customer> findAllActiveOptimized(Pageable pageable);

    /**
     * Find customers by organization with pagination (optimized)
     */
    @Query("SELECT c FROM Customer c WHERE c.organizationId = :organizationId AND c.deleted = false")
    Page<Customer> findByOrganizationIdOptimized(@Param("organizationId") UUID organizationId, Pageable pageable);

    /**
     * Find customers by status with pagination (optimized)
     */
    @Query("SELECT c FROM Customer c WHERE c.status = :status AND c.deleted = false")
    Page<Customer> findByStatusOptimized(@Param("status") CustomerStatus status, Pageable pageable);

    // ========================================
    // ✅ PHASE 2: DATA SCOPE ENFORCEMENT
    // ========================================

    /**
     * Find all customers with data scope filtering
     * Applies row-level security based on user's data scope (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)
     */
    default List<Customer> findAllWithScope() {
        return findAll(DataScopeHelper.applyDataScope());
    }

    /**
     * Find all customers with data scope filtering and pagination
     * Applies row-level security based on user's data scope (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)
     */
    default Page<Customer> findAllWithScope(Pageable pageable) {
        return findAll(DataScopeHelper.applyDataScope(), pageable);
    }

    /**
     * Find customers by organization with data scope filtering
     */
    default List<Customer> findByOrganizationIdWithScope(UUID organizationId) {
        return findAll(DataScopeHelper.applyScopeWith(
            DataScopeHelper.byOrganization(organizationId)
        ));
    }

    /**
     * Find customers by organization with data scope filtering and pagination
     */
    default Page<Customer> findByOrganizationIdWithScope(UUID organizationId, Pageable pageable) {
        return findAll(DataScopeHelper.applyScopeWith(
            DataScopeHelper.byOrganization(organizationId)
        ), pageable);
    }

    /**
     * Find customers by status with data scope filtering
     */
    default List<Customer> findByStatusWithScope(CustomerStatus status) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.equal(root.get("status"), status)
        ));
    }

    /**
     * Find customers by status with data scope filtering and pagination
     */
    default Page<Customer> findByStatusWithScope(CustomerStatus status, Pageable pageable) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.equal(root.get("status"), status)
        ), pageable);
    }

    /**
     * Find VIP customers with data scope filtering
     */
    default List<Customer> findVipCustomersWithScope() {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.isTrue(root.get("isVip"))
        ));
    }

    /**
     * Find customers by type with data scope filtering
     */
    default List<Customer> findByCustomerTypeWithScope(CustomerType type) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.equal(root.get("customerType"), type)
        ));
    }

    /**
     * Find customers by owner with data scope filtering
     * Useful for account managers viewing their own customers
     */
    default List<Customer> findByOwnerIdWithScope(UUID ownerId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId)
        ));
    }

    /**
     * Find customers by branch with data scope filtering
     */
    default List<Customer> findByBranchIdWithScope(UUID branchId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.equal(root.get("branchId"), branchId)
        ));
    }

    /**
     * Count customers with data scope filtering
     */
    default long countWithScope() {
        return count(DataScopeHelper.applyDataScope());
    }

    /**
     * Count VIP customers with data scope filtering
     */
    default long countVipCustomersWithScope() {
        return count(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.isTrue(root.get("isVip"))
        ));
    }
}
