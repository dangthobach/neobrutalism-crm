package com.neobrutalism.crm.domain.customer.dto;

import com.neobrutalism.crm.domain.customer.model.CustomerStatus;
import com.neobrutalism.crm.domain.customer.model.CustomerType;
import com.neobrutalism.crm.domain.customer.model.Industry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for optimized Customer queries with joined data
 * Prevents N+1 query problem by fetching related entities in a single query
 */
public record CustomerWithDetailsDTO(
    UUID id,
    String code,
    String companyName,
    CustomerType customerType,
    CustomerStatus status,
    Industry industry,
    String email,
    String phone,
    boolean isVip,
    BigDecimal totalRevenue,
    LocalDate acquisitionDate,
    LocalDate lastContactDate,
    UUID organizationId,
    String organizationName,
    UUID ownerId,
    String ownerName,
    UUID branchId,
    String branchName,
    String tenantId,
    boolean deleted
) {
    /**
     * Constructor for JPQL queries without optional fields
     */
    public CustomerWithDetailsDTO(
        UUID id,
        String code,
        String companyName,
        CustomerType customerType,
        CustomerStatus status,
        String email,
        String phone,
        boolean isVip,
        UUID organizationId,
        String organizationName,
        String tenantId,
        boolean deleted
    ) {
        this(id, code, companyName, customerType, status, null, email, phone, isVip,
             null, null, null, organizationId, organizationName,
             null, null, null, null, tenantId, deleted);
    }
}
