package com.neobrutalism.crm.domain.branch.dto;

import com.neobrutalism.crm.domain.branch.BranchStatus;

import java.util.UUID;

/**
 * DTO for optimized Branch queries with joined data
 * Prevents N+1 query problem by fetching related entities in a single query
 */
public record BranchWithDetailsDTO(
    UUID id,
    String code,
    String name,
    String description,
    BranchStatus status,
    UUID organizationId,
    String organizationName,
    UUID parentId,
    String parentName,
    Integer level,
    String path,
    UUID managerId,
    String managerName,
    String tenantId,
    boolean deleted
) {
    /**
     * Constructor for JPQL queries without parent
     */
    public BranchWithDetailsDTO(
        UUID id,
        String code,
        String name,
        String description,
        BranchStatus status,
        UUID organizationId,
        String organizationName,
        UUID managerId,
        String managerName,
        Integer level,
        String path,
        String tenantId,
        boolean deleted
    ) {
        this(id, code, name, description, status, organizationId, organizationName,
             null, null, level, path, managerId, managerName, tenantId, deleted);
    }
}
