package com.neobrutalism.crm.domain.customer.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.domain.customer.model.Customer;
import com.neobrutalism.crm.domain.customer.model.CustomerStatus;
import com.neobrutalism.crm.domain.customer.model.CustomerType;
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
}
