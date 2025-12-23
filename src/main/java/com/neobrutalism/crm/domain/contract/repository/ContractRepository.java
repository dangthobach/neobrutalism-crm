package com.neobrutalism.crm.domain.contract.repository;

import com.neobrutalism.crm.domain.contract.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    
    @Query("SELECT c FROM Contract c WHERE c.contractNumber = :contractNumber AND c.tenantId = :tenantId AND c.isDeleted = false")
    Optional<Contract> findByContractNumberAndTenantId(
        @Param("contractNumber") String contractNumber, 
        @Param("tenantId") UUID tenantId
    );
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Contract c " +
           "WHERE c.contractNumber = :contractNumber AND c.tenantId = :tenantId AND c.isDeleted = false")
    boolean existsByContractNumberAndTenantId(
        @Param("contractNumber") String contractNumber,
        @Param("tenantId") UUID tenantId
    );
}
