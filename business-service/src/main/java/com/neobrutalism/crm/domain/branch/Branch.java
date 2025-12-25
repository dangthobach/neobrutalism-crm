package com.neobrutalism.crm.domain.branch;

import com.neobrutalism.crm.common.entity.TenantAwareAggregateRoot;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

/**
 * Branch Entity - Chi nhánh thuộc Organization
 * Dùng để quản lý phân quyền theo chi nhánh
 */
@Getter
@Setter
@Entity
@Table(
    name = "branches",
    indexes = {
        @Index(name = "idx_branch_org_id", columnList = "organization_id"),
        @Index(name = "idx_branch_parent_id", columnList = "parent_id"),
        @Index(name = "idx_branch_code", columnList = "code"),
        @Index(name = "idx_branch_deleted_id", columnList = "deleted, id"),
        // ✅ PHASE 1: Performance optimization indexes
        @Index(name = "idx_branch_tenant", columnList = "tenant_id"),
        @Index(name = "idx_branch_status", columnList = "status"),
        @Index(name = "idx_branch_manager", columnList = "manager_id"),
        @Index(name = "idx_branch_tenant_org_deleted", columnList = "tenant_id, organization_id, deleted"),
        @Index(name = "idx_branch_org_status", columnList = "organization_id, status, deleted"),
    }
)

public class Branch extends TenantAwareAggregateRoot<BranchStatus> {

    /**
     * Mã chi nhánh (unique trong organization)
     * VD: HN-001, HCM-002
     */
    @NotBlank(message = "Branch code is required")
    @Size(max = 50, message = "Branch code must be at most 50 characters")
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /**
     * Tên chi nhánh
     */
    @NotBlank(message = "Branch name is required")
    @Size(max = 200, message = "Branch name must be at most 200 characters")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Mô tả
     */
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Organization ID (FK)
     */
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /**
     * Parent Branch ID - Hỗ trợ cấu trúc phân cấp
     * VD: HN-001 (parent) -> HN-001-001 (child)
     */
    @Column(name = "parent_id")
    private UUID parentId;

    /**
     * Level trong hierarchy (0 = root, 1 = level 1, ...)
     */
    @Column(name = "level", nullable = false)
    private Integer level = 0;

    /**
     * Path in hierarchy - /HN-001/HN-001-001
     */
    @Size(max = 500, message = "Path must be at most 500 characters")
    @Column(name = "path", length = 500)
    private String path;

    /**
     * Branch Type - HQ (Head Quarter), REGIONAL, LOCAL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "branch_type", length = 20)
    private BranchType branchType = BranchType.LOCAL;

    /**
     * Status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BranchStatus status = BranchStatus.ACTIVE;

    /**
     * Manager User ID - Người quản lý chi nhánh
     */
    @Column(name = "manager_id")
    private UUID managerId;

    /**
     * Contact email
     */
    @Size(max = 255, message = "Email must be at most 255 characters")
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Contact phone
     */
    @Size(max = 50, message = "Phone must be at most 50 characters")
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Address
     */
    @Size(max = 500, message = "Address must be at most 500 characters")
    @Column(name = "address", length = 500)
    private String address;

    /**
     * Display order
     */
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    /**
     * Business logic: Unique code per organization
     */
    @PrePersist
    @PreUpdate
    public void validateBranch() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (path == null && code != null) {
            path = "/" + code;
        }
    }

    @Override
    protected Set<BranchStatus> getAllowedTransitions(BranchStatus currentStatus) {
        return switch (currentStatus) {
            case ACTIVE -> Set.of(BranchStatus.INACTIVE, BranchStatus.CLOSED);
            case INACTIVE -> Set.of(BranchStatus.ACTIVE, BranchStatus.CLOSED);
            case CLOSED -> Set.of(); // Closed là final state
        };
    }

    @Override
    protected BranchStatus getInitialStatus() {
        return BranchStatus.ACTIVE;
    }

    public enum BranchType {
        HQ,         // Head Quarter - Trụ sở chính
        REGIONAL,   // Chi nhánh vùng
        LOCAL       // Chi nhánh địa phương
    }
}
