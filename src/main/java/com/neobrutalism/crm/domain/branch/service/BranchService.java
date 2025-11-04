package com.neobrutalism.crm.domain.branch.service;

import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.exception.ValidationException;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.branch.Branch;
import com.neobrutalism.crm.domain.branch.BranchStatus;
import com.neobrutalism.crm.domain.branch.repository.BranchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ PHASE 1 WEEK 2: Service for Branch management with Redis caching
 * Cache region: "branches" with 5 minutes TTL
 */
@Slf4j
@Service
public class BranchService extends BaseService<Branch> {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Override
    protected BranchRepository getRepository() {
        return branchRepository;
    }

    @Override
    protected String getEntityName() {
        return "Branch";
    }

    /**
     * Create a new branch
     * Cache eviction: Clears all branches cache for this organization
     */
    @Transactional
    @CacheEvict(value = "branches", allEntries = true)
    public Branch create(Branch branch) {
        log.info("Creating new branch: {}", branch.getCode());

        // Set tenant ID
        if (branch.getTenantId() == null) {
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr != null) {
                branch.setTenantId(tenantIdStr);
            }
        }

        // Validate branch code uniqueness
        if (branchRepository.existsByCodeAndOrganizationId(branch.getCode(), branch.getOrganizationId())) {
            throw new ValidationException("Branch code already exists in this organization: " + branch.getCode());
        }

        // Calculate level and path
        if (branch.getParentId() != null) {
            Branch parent = findById(branch.getParentId());
            branch.setLevel(parent.getLevel() + 1);
            branch.setPath(parent.getPath() + "/" + branch.getCode());
        } else {
            branch.setLevel(0);
            branch.setPath("/" + branch.getCode());
        }

        // Set initial status
        if (branch.getStatus() == null) {
            branch.setStatus(BranchStatus.ACTIVE);
        }

        return branchRepository.save(branch);
    }

    /**
     * Update existing branch
     * Cache eviction: Clears all branches cache
     */
    @Transactional
    @CacheEvict(value = "branches", allEntries = true)
    public Branch update(UUID id, Branch updatedBranch) {
        log.info("Updating branch: {}", id);

        Branch existingBranch = findById(id);

        // Validate code uniqueness (excluding current branch)
        if (!existingBranch.getCode().equals(updatedBranch.getCode())) {
            if (branchRepository.existsByCodeAndOrganizationIdExcluding(
                    updatedBranch.getCode(),
                    existingBranch.getOrganizationId(),
                    id)) {
                throw new ValidationException("Branch code already exists: " + updatedBranch.getCode());
            }
        }

        // Update fields
        existingBranch.setCode(updatedBranch.getCode());
        existingBranch.setName(updatedBranch.getName());
        existingBranch.setDescription(updatedBranch.getDescription());
        existingBranch.setEmail(updatedBranch.getEmail());
        existingBranch.setPhone(updatedBranch.getPhone());
        existingBranch.setAddress(updatedBranch.getAddress());
        existingBranch.setBranchType(updatedBranch.getBranchType());
        existingBranch.setManagerId(updatedBranch.getManagerId());
        existingBranch.setDisplayOrder(updatedBranch.getDisplayOrder());

        // Update parent if changed
        if (!Objects.equals(existingBranch.getParentId(), updatedBranch.getParentId())) {
            updateParent(existingBranch, updatedBranch.getParentId());
        }

        return branchRepository.save(existingBranch);
    }

    /**
     * Update branch parent and recalculate hierarchy
     */
    @Transactional
    public void updateParent(Branch branch, UUID newParentId) {
        log.info("Updating parent for branch: {} to parent: {}", branch.getId(), newParentId);

        // Validate not setting self as parent
        if (Objects.equals(branch.getId(), newParentId)) {
            throw new ValidationException("Branch cannot be its own parent");
        }

        // Validate not creating circular reference
        if (newParentId != null) {
            Branch newParent = findById(newParentId);
            if (newParent.getPath().contains(branch.getCode())) {
                throw new ValidationException("Cannot create circular reference in branch hierarchy");
            }

            // Update level and path
            branch.setLevel(newParent.getLevel() + 1);
            branch.setPath(newParent.getPath() + "/" + branch.getCode());
        } else {
            // Becoming a root branch
            branch.setLevel(0);
            branch.setPath("/" + branch.getCode());
        }

        branch.setParentId(newParentId);
        branchRepository.save(branch);

        // Update all descendants
        updateDescendantsPath(branch);
    }

    /**
     * Update paths of all descendants recursively
     */
    private void updateDescendantsPath(Branch branch) {
        List<Branch> children = branchRepository.findByParentId(branch.getId());
        for (Branch child : children) {
            child.setLevel(branch.getLevel() + 1);
            child.setPath(branch.getPath() + "/" + child.getCode());
            branchRepository.save(child);
            updateDescendantsPath(child);
        }
    }

    /**
     * Get all branches by organization
     * Cached: 5 minutes TTL, key by organization ID
     */
    @Cacheable(value = "branches", key = "'org:' + #organizationId")
    public List<Branch> findByOrganizationId(UUID organizationId) {
        return branchRepository.findByOrganizationId(organizationId);
    }

    /**
     * Get branch by code
     * Cached: 5 minutes TTL, key by code and tenant
     */
    @Cacheable(value = "branches", key = "'code:' + #code + ':tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public Optional<Branch> findByCode(String code) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        return branchRepository.findByCodeAndTenantId(code, tenantIdStr != null ? UUID.fromString(tenantIdStr) : null);
    }

    /**
     * Get all child branches
     */
    public List<Branch> getChildren(UUID branchId) {
        return branchRepository.findByParentId(branchId);
    }

    /**
     * Get all root branches (no parent)
     * Cached: 5 minutes TTL, key by tenant
     */
    @Cacheable(value = "branches", key = "'root:tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public List<Branch> getRootBranches() {
        String tenantIdStr = TenantContext.getCurrentTenant();
        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
        return branchRepository.findRootBranches(tenantId);
    }

    /**
     * Get all descendants (children, grandchildren, etc.)
     */
    public List<Branch> getDescendants(UUID branchId) {
        Branch branch = findById(branchId);
        String pathPrefix = branch.getPath();
        String tenantIdStr = TenantContext.getCurrentTenant();
        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;

        List<Branch> descendants = branchRepository.findByPathPrefix(pathPrefix + "/", tenantId);
        return descendants;
    }

    /**
     * Get all ancestors (parent, grandparent, etc.)
     */
    public List<Branch> getAncestors(UUID branchId) {
        Branch branch = findById(branchId);
        if (branch.getParentId() == null) {
            return Collections.emptyList();
        }

        // Parse codes from path
        String path = branch.getPath();
        String[] parts = path.split("/");
        List<String> codes = Arrays.stream(parts)
                .filter(p -> !p.isEmpty() && !p.equals(branch.getCode()))
                .collect(Collectors.toList());

        if (codes.isEmpty()) {
            return Collections.emptyList();
        }

        String tenantIdStr = TenantContext.getCurrentTenant();
        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
        return branchRepository.findByCodesInPath(codes, tenantId);
    }

    /**
     * Get branch hierarchy tree
     */
    public List<Branch> getHierarchyTree() {
        List<Branch> rootBranches = getRootBranches();
        for (Branch root : rootBranches) {
            loadChildren(root);
        }
        return rootBranches;
    }

    /**
     * Recursively load children for tree structure
     */
    private void loadChildren(Branch branch) {
        List<Branch> children = getChildren(branch.getId());
        // Note: In a real scenario, you'd need a way to attach children to branch
        // This might require a transient field in the Branch entity
        for (Branch child : children) {
            loadChildren(child);
        }
    }

    /**
     * Get branches by type
     * Cached: 5 minutes TTL, key by type and tenant
     */
    @Cacheable(value = "branches", key = "'type:' + #branchType + ':tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public List<Branch> findByBranchType(Branch.BranchType branchType) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
        return branchRepository.findByBranchType(branchType, tenantId);
    }

    /**
     * Get branches by level
     */
    public List<Branch> findByLevel(Integer level) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
        return branchRepository.findByLevel(level, tenantId);
    }

    /**
     * Get branches by manager
     */
    public List<Branch> findByManagerId(UUID managerId) {
        return branchRepository.findByManagerId(managerId);
    }

    /**
     * Get branches by status
     * Cached: 5 minutes TTL, key by status and tenant
     */
    @Cacheable(value = "branches", key = "'status:' + #status + ':tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public List<Branch> findByStatus(BranchStatus status) {
        return branchRepository.findByStatus(status);
    }

    /**
     * Get branches by status with pagination
     */
    public Page<Branch> findByStatus(BranchStatus status, Pageable pageable) {
        return branchRepository.findByStatus(status, pageable);
    }

    /**
     * Activate branch
     * Cache eviction: Clears all branches cache
     */
    @Transactional
    @CacheEvict(value = "branches", allEntries = true)
    public Branch activate(UUID branchId, String reason) {
        log.info("Activating branch: {}", branchId);
        Branch branch = findById(branchId);
        String currentUser = TenantContext.getCurrentTenant(); // In real scenario, get from SecurityContext
        branch.transitionTo(BranchStatus.ACTIVE, currentUser, reason);
        return branchRepository.save(branch);
    }

    /**
     * Deactivate branch
     * Cache eviction: Clears all branches cache
     */
    @Transactional
    @CacheEvict(value = "branches", allEntries = true)
    public Branch deactivate(UUID branchId, String reason) {
        log.info("Deactivating branch: {}", branchId);
        Branch branch = findById(branchId);
        String currentUser = TenantContext.getCurrentTenant();
        branch.transitionTo(BranchStatus.INACTIVE, currentUser, reason);
        return branchRepository.save(branch);
    }

    /**
     * Close branch
     * Cache eviction: Clears all branches cache
     */
    @Transactional
    @CacheEvict(value = "branches", allEntries = true)
    public Branch close(UUID branchId, String reason) {
        log.info("Closing branch: {}", branchId);
        Branch branch = findById(branchId);
        String currentUser = TenantContext.getCurrentTenant();
        branch.transitionTo(BranchStatus.CLOSED, currentUser, reason);
        return branchRepository.save(branch);
    }

    /**
     * Count branches by organization
     */
    public long countByOrganizationId(UUID organizationId) {
        return branchRepository.countByOrganizationId(organizationId);
    }

    /**
     * Get all active branches
     * Cached: 5 minutes TTL, key by active status and tenant
     */
    @Cacheable(value = "branches", key = "'active:tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public List<Branch> findAllActive() {
        return branchRepository.findByStatus(BranchStatus.ACTIVE);
    }

    /**
     * Get all active branches with pagination
     */
    public Page<Branch> findAllActive(Pageable pageable) {
        return branchRepository.findByStatus(BranchStatus.ACTIVE, pageable);
    }

    /**
     * Soft delete branch
     * Cache eviction: Clears all branches cache
     */
    @Transactional
    @CacheEvict(value = "branches", allEntries = true)
    public void deleteById(UUID id) {
        log.info("Soft deleting branch: {}", id);
        Branch branch = findById(id);

        // Check if branch has children
        List<Branch> children = getChildren(id);
        if (!children.isEmpty()) {
            throw new ValidationException("Cannot delete branch with child branches. Delete children first.");
        }

        branch.setDeleted(true);
        branchRepository.save(branch);
    }
}
