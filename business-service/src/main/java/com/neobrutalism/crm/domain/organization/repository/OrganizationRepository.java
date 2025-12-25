package com.neobrutalism.crm.domain.organization.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.domain.organization.model.Organization;
import com.neobrutalism.crm.domain.organization.model.OrganizationStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Organization entity
 */
@Repository
public interface OrganizationRepository extends StatefulRepository<Organization, OrganizationStatus> {

    /**
     * Find organization by code
     */
    Optional<Organization> findByCode(String code);

    /**
     * Find active organization by code
     */
    @Query("SELECT o FROM Organization o WHERE o.code = :code AND o.deleted = false")
    Optional<Organization> findActiveByCode(@Param("code") String code);

    /**
     * Check if code exists
     */
    boolean existsByCode(String code);
}
