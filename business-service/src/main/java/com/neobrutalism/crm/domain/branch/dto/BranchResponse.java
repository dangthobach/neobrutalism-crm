package com.neobrutalism.crm.domain.branch.dto;

import com.neobrutalism.crm.domain.branch.Branch;
import com.neobrutalism.crm.domain.branch.BranchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for Branch entity
 */
@Data
@Builder
@Schema(description = "Branch response data")
public class BranchResponse {

    @Schema(description = "Branch ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Branch code", example = "HN-001")
    private String code;

    @Schema(description = "Branch name", example = "Hanoi Branch")
    private String name;

    @Schema(description = "Branch description")
    private String description;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Parent branch ID")
    private UUID parentId;

    @Schema(description = "Branch level in hierarchy (0 = root)")
    private Integer level;

    @Schema(description = "Branch path in hierarchy", example = "/HN-001/HN-001-001")
    private String path;

    @Schema(description = "Branch type")
    private Branch.BranchType branchType;

    @Schema(description = "Branch status")
    private BranchStatus status;

    @Schema(description = "Manager user ID")
    private UUID managerId;

    @Schema(description = "Contact email")
    private String email;

    @Schema(description = "Contact phone")
    private String phone;

    @Schema(description = "Branch address")
    private String address;

    @Schema(description = "Display order")
    private Integer displayOrder;

    @Schema(description = "Tenant ID")
    private String tenantId;

    @Schema(description = "Entity version for optimistic locking")
    private Long version;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Created by user ID")
    private String createdBy;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;

    @Schema(description = "Last updated by user ID")
    private String updatedBy;

    /**
     * Convert Branch entity to response DTO
     */
    public static BranchResponse from(Branch branch) {
        if (branch == null) {
            return null;
        }

        return BranchResponse.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .description(branch.getDescription())
                .organizationId(branch.getOrganizationId())
                .parentId(branch.getParentId())
                .level(branch.getLevel())
                .path(branch.getPath())
                .branchType(branch.getBranchType())
                .status(branch.getStatus())
                .managerId(branch.getManagerId())
                .email(branch.getEmail())
                .phone(branch.getPhone())
                .address(branch.getAddress())
                .displayOrder(branch.getDisplayOrder())
                .tenantId(branch.getTenantId())
                .version(branch.getVersion())
                .createdAt(branch.getCreatedAt())
                .createdBy(branch.getCreatedBy())
                .updatedAt(branch.getUpdatedAt())
                .updatedBy(branch.getUpdatedBy())
                .build();
    }
}
