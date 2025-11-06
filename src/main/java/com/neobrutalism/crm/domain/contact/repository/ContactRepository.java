package com.neobrutalism.crm.domain.contact.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.domain.contact.model.Contact;
import com.neobrutalism.crm.domain.contact.model.ContactRole;
import com.neobrutalism.crm.domain.contact.model.ContactStatus;
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
 * Repository for Contact entity
 */
@Repository
public interface ContactRepository extends StatefulRepository<Contact, ContactStatus> {

    /**
     * Find contact by email
     */
    @Query("SELECT c FROM Contact c WHERE c.email = :email AND c.deleted = false")
    Optional<Contact> findByEmail(@Param("email") String email);

    /**
     * Find all contacts by customer
     */
    @Query("SELECT c FROM Contact c WHERE c.customerId = :customerId AND c.deleted = false ORDER BY c.isPrimary DESC, c.lastName, c.firstName")
    List<Contact> findByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Find primary contact for customer
     */
    @Query("SELECT c FROM Contact c WHERE c.customerId = :customerId AND c.isPrimary = true AND c.deleted = false")
    Optional<Contact> findPrimaryContactByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Find all contacts by owner
     */
    @Query("SELECT c FROM Contact c WHERE c.ownerId = :ownerId AND c.deleted = false ORDER BY c.lastName, c.firstName")
    List<Contact> findByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Find all contacts by organization
     */
    @Query("SELECT c FROM Contact c WHERE c.organizationId = :organizationId AND c.deleted = false")
    List<Contact> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find contacts by role
     */
    @Query("SELECT c FROM Contact c WHERE c.contactRole = :role AND c.tenantId = :tenantId AND c.deleted = false")
    List<Contact> findByContactRole(@Param("role") ContactRole role, @Param("tenantId") String tenantId);

    /**
     * Find contacts by tenant
     */
    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false ORDER BY c.lastName, c.firstName")
    List<Contact> findByTenantId(@Param("tenantId") String tenantId);

    /**
     * Search contacts by name
     */
    @Query("SELECT c FROM Contact c WHERE (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND c.deleted = false")
    List<Contact> searchByName(@Param("keyword") String keyword);

    /**
     * Find contacts by email domain
     */
    @Query("SELECT c FROM Contact c WHERE c.email LIKE CONCAT('%@', :domain) AND c.deleted = false")
    List<Contact> findByEmailDomain(@Param("domain") String domain);

    /**
     * Find contacts by tag
     */
    @Query("SELECT c FROM Contact c WHERE c.tags LIKE CONCAT('%', :tag, '%') AND c.deleted = false")
    List<Contact> findByTag(@Param("tag") String tag);

    /**
     * Find contacts requiring follow-up
     */
    @Query("SELECT c FROM Contact c WHERE c.lastContactDate < :date AND c.status = 'ACTIVE' AND c.deleted = false ORDER BY c.lastContactDate")
    List<Contact> findRequiringFollowup(@Param("date") LocalDate date);

    /**
     * Find contacts who opted out of email
     */
    @Query("SELECT c FROM Contact c WHERE c.emailOptOut = true AND c.deleted = false")
    List<Contact> findEmailOptOuts();

    /**
     * Find contacts by status and customer
     */
    @Query("SELECT c FROM Contact c WHERE c.customerId = :customerId AND c.status = :status AND c.deleted = false")
    List<Contact> findByCustomerIdAndStatus(@Param("customerId") UUID customerId, @Param("status") ContactStatus status);

    /**
     * Check if email exists
     */
    @Query("SELECT COUNT(c) > 0 FROM Contact c WHERE c.email = :email AND c.deleted = false")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Check if email exists (excluding specific contact)
     */
    @Query("SELECT COUNT(c) > 0 FROM Contact c WHERE c.email = :email AND c.id != :excludeId AND c.deleted = false")
    boolean existsByEmailExcluding(@Param("email") String email, @Param("excludeId") UUID excludeId);

    /**
     * Count contacts by customer
     */
    @Query("SELECT COUNT(c) FROM Contact c WHERE c.customerId = :customerId AND c.deleted = false")
    long countByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Count contacts by status
     */
    @Query("SELECT COUNT(c) FROM Contact c WHERE c.status = :status AND c.tenantId = :tenantId AND c.deleted = false")
    long countByStatusAndTenantId(@Param("status") ContactStatus status, @Param("tenantId") String tenantId);

    /**
     * Find contacts reporting to another contact
     */
    @Query("SELECT c FROM Contact c WHERE c.reportsToId = :reportsToId AND c.deleted = false ORDER BY c.lastName, c.firstName")
    List<Contact> findByReportsToId(@Param("reportsToId") UUID reportsToId);

    // ========================================
    // ✅ N+1 QUERY FIXES: Optimized pagination queries
    // ========================================

    /**
     * Find all active contacts with pagination (optimized - no N+1)
     * Uses EntityGraph to ensure efficient loading
     */
    @EntityGraph(attributePaths = {}) // No relationships to fetch, but prepared for future
    @Query("SELECT c FROM Contact c WHERE c.deleted = false")
    Page<Contact> findAllActiveOptimized(Pageable pageable);

    /**
     * Find contacts by customer with pagination (optimized)
     */
    @Query("SELECT c FROM Contact c WHERE c.customerId = :customerId AND c.deleted = false ORDER BY c.isPrimary DESC, c.lastName, c.firstName")
    Page<Contact> findByCustomerIdOptimized(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Find contacts by organization with pagination (optimized)
     */
    @Query("SELECT c FROM Contact c WHERE c.organizationId = :organizationId AND c.deleted = false")
    Page<Contact> findByOrganizationIdOptimized(@Param("organizationId") UUID organizationId, Pageable pageable);

    /**
     * Find contacts by status with pagination (optimized)
     */
    @Query("SELECT c FROM Contact c WHERE c.status = :status AND c.deleted = false")
    Page<Contact> findByStatusOptimized(@Param("status") ContactStatus status, Pageable pageable);
}
