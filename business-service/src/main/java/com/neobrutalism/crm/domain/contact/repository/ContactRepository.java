package com.neobrutalism.crm.domain.contact.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.common.security.DataScopeHelper;
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

    // ========================================
    // ✅ PHASE 2: DATA SCOPE ENFORCEMENT
    // ========================================

    /**
     * Find all contacts with data scope filtering
     * Applies row-level security based on user's data scope (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)
     */
    default List<Contact> findAllWithScope() {
        return findAll(DataScopeHelper.applyDataScope());
    }

    /**
     * Find all contacts with data scope filtering and pagination
     */
    default Page<Contact> findAllWithScope(Pageable pageable) {
        return findAll(DataScopeHelper.applyDataScope(), pageable);
    }

    /**
     * Find contacts by customer with data scope filtering
     */
    default List<Contact> findByCustomerIdWithScope(UUID customerId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> {
                query.orderBy(
                    cb.desc(root.get("isPrimary")),
                    cb.asc(root.get("lastName")),
                    cb.asc(root.get("firstName"))
                );
                return cb.and(
                    cb.equal(root.get("customerId"), customerId),
                    cb.isFalse(root.get("deleted"))
                );
            }
        ));
    }

    /**
     * Find contacts by customer with data scope filtering and pagination
     */
    default Page<Contact> findByCustomerIdWithScope(UUID customerId, Pageable pageable) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("customerId"), customerId),
                cb.isFalse(root.get("deleted"))
            )
        ), pageable);
    }

    /**
     * Find contacts by organization with data scope filtering
     */
    default List<Contact> findByOrganizationIdWithScope(UUID organizationId) {
        return findAll(DataScopeHelper.applyScopeWith(
            DataScopeHelper.byOrganization(organizationId)
        ));
    }

    /**
     * Find contacts by organization with data scope filtering and pagination
     */
    default Page<Contact> findByOrganizationIdWithScope(UUID organizationId, Pageable pageable) {
        return findAll(DataScopeHelper.applyScopeWith(
            DataScopeHelper.byOrganization(organizationId)
        ), pageable);
    }

    /**
     * Find contacts by status with data scope filtering
     */
    default List<Contact> findByStatusWithScope(ContactStatus status) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find contacts by status with data scope filtering and pagination
     */
    default Page<Contact> findByStatusWithScope(ContactStatus status, Pageable pageable) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ), pageable);
    }

    /**
     * Find contacts by owner with data scope filtering
     */
    default List<Contact> findByOwnerIdWithScope(UUID ownerId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> {
                query.orderBy(cb.asc(root.get("lastName")), cb.asc(root.get("firstName")));
                return cb.and(
                    cb.equal(root.get("ownerId"), ownerId),
                    cb.isFalse(root.get("deleted"))
                );
            }
        ));
    }

    /**
     * Find contacts by role with data scope filtering
     */
    default List<Contact> findByContactRoleWithScope(ContactRole role) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("contactRole"), role),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find primary contact for customer with data scope filtering
     */
    default Optional<Contact> findPrimaryContactByCustomerIdWithScope(UUID customerId) {
        List<Contact> contacts = findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("customerId"), customerId),
                cb.isTrue(root.get("isPrimary")),
                cb.isFalse(root.get("deleted"))
            )
        ));
        return contacts.isEmpty() ? Optional.empty() : Optional.of(contacts.get(0));
    }

    /**
     * Search contacts by name with data scope filtering
     */
    default List<Contact> searchByNameWithScope(String keyword) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> {
                String pattern = "%" + keyword.toLowerCase() + "%";
                return cb.and(
                    cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern)
                    ),
                    cb.isFalse(root.get("deleted"))
                );
            }
        ));
    }

    /**
     * Find contacts by email domain with data scope filtering
     */
    default List<Contact> findByEmailDomainWithScope(String domain) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.like(root.get("email"), "%@" + domain),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find contacts requiring follow-up with data scope filtering
     */
    default List<Contact> findRequiringFollowupWithScope(LocalDate date) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> {
                query.orderBy(cb.asc(root.get("lastContactDate")));
                return cb.and(
                    cb.lessThan(root.get("lastContactDate"), date),
                    cb.equal(root.get("status"), ContactStatus.ACTIVE),
                    cb.isFalse(root.get("deleted"))
                );
            }
        ));
    }

    /**
     * Count contacts by customer with data scope filtering
     */
    default long countByCustomerIdWithScope(UUID customerId) {
        return count(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("customerId"), customerId),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Count contacts with data scope filtering
     */
    default long countWithScope() {
        return count(DataScopeHelper.applyDataScope());
    }
}
