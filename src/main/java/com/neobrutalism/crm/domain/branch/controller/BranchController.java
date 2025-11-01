package com.neobrutalism.crm.domain.branch.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.domain.branch.Branch;
import com.neobrutalism.crm.domain.branch.BranchStatus;
import com.neobrutalism.crm.domain.branch.dto.BranchRequest;
import com.neobrutalism.crm.domain.branch.dto.BranchResponse;
import com.neobrutalism.crm.domain.branch.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Branch management
 */
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Branch management APIs")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "Get all branches", description = "Retrieve all branches with pagination")
    public ApiResponse<PageResponse<BranchResponse>> getAllBranches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Branch> branchPage = branchService.findAllActive(pageable);
        Page<BranchResponse> responsePage = branchPage.map(BranchResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get branch by ID", description = "Retrieve a specific branch by its ID")
    public ApiResponse<BranchResponse> getBranchById(@PathVariable UUID id) {
        Branch branch = branchService.findById(id);
        return ApiResponse.success(BranchResponse.from(branch));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get branch by code", description = "Retrieve a specific branch by its unique code")
    public ApiResponse<BranchResponse> getBranchByCode(@PathVariable String code) {
        Branch branch = branchService.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Branch not found with code: " + code));
        return ApiResponse.success(BranchResponse.from(branch));
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get branches by organization", description = "Retrieve all branches for a specific organization")
    public ApiResponse<List<BranchResponse>> getBranchesByOrganization(@PathVariable UUID organizationId) {
        List<Branch> branches = branchService.findByOrganizationId(organizationId);
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/root")
    @Operation(summary = "Get root branches", description = "Retrieve all root branches (branches with no parent)")
    public ApiResponse<List<BranchResponse>> getRootBranches() {
        List<Branch> branches = branchService.getRootBranches();
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get child branches", description = "Retrieve all direct children of a branch")
    public ApiResponse<List<BranchResponse>> getChildBranches(@PathVariable UUID id) {
        List<Branch> branches = branchService.getChildren(id);
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}/descendants")
    @Operation(summary = "Get all descendants", description = "Retrieve all descendants (children, grandchildren, etc.) of a branch")
    public ApiResponse<List<BranchResponse>> getDescendants(@PathVariable UUID id) {
        List<Branch> branches = branchService.getDescendants(id);
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}/ancestors")
    @Operation(summary = "Get all ancestors", description = "Retrieve all ancestors (parent, grandparent, etc.) of a branch")
    public ApiResponse<List<BranchResponse>> getAncestors(@PathVariable UUID id) {
        List<Branch> branches = branchService.getAncestors(id);
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/hierarchy")
    @Operation(summary = "Get branch hierarchy tree", description = "Retrieve complete branch hierarchy as a tree structure")
    public ApiResponse<List<BranchResponse>> getHierarchyTree() {
        List<Branch> branches = branchService.getHierarchyTree();
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get branches by type", description = "Retrieve branches by branch type (HQ, REGIONAL, LOCAL)")
    public ApiResponse<List<BranchResponse>> getBranchesByType(@PathVariable Branch.BranchType type) {
        List<Branch> branches = branchService.findByBranchType(type);
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/level/{level}")
    @Operation(summary = "Get branches by level", description = "Retrieve branches by hierarchy level")
    public ApiResponse<List<BranchResponse>> getBranchesByLevel(@PathVariable Integer level) {
        List<Branch> branches = branchService.findByLevel(level);
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/manager/{managerId}")
    @Operation(summary = "Get branches by manager", description = "Retrieve all branches managed by a specific user")
    public ApiResponse<List<BranchResponse>> getBranchesByManager(@PathVariable UUID managerId) {
        List<Branch> branches = branchService.findByManagerId(managerId);
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get branches by status", description = "Retrieve all branches with a specific status")
    public ApiResponse<List<BranchResponse>> getBranchesByStatus(@PathVariable BranchStatus status) {
        List<Branch> branches = branchService.findByStatus(status);
        List<BranchResponse> responses = branches.stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create branch", description = "Create a new branch")
    public ApiResponse<BranchResponse> createBranch(@Valid @RequestBody BranchRequest request) {
        Branch branch = new Branch();
        branch.setCode(request.getCode());
        branch.setName(request.getName());
        branch.setDescription(request.getDescription());
        branch.setOrganizationId(request.getOrganizationId());
        branch.setParentId(request.getParentId());
        branch.setBranchType(request.getBranchType() != null ? request.getBranchType() : Branch.BranchType.LOCAL);
        branch.setManagerId(request.getManagerId());
        branch.setEmail(request.getEmail());
        branch.setPhone(request.getPhone());
        branch.setAddress(request.getAddress());
        branch.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        Branch created = branchService.create(branch);
        return ApiResponse.success("Branch created successfully", BranchResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update branch", description = "Update an existing branch")
    public ApiResponse<BranchResponse> updateBranch(
            @PathVariable UUID id,
            @Valid @RequestBody BranchRequest request) {

        Branch branch = new Branch();
        branch.setCode(request.getCode());
        branch.setName(request.getName());
        branch.setDescription(request.getDescription());
        branch.setOrganizationId(request.getOrganizationId());
        branch.setParentId(request.getParentId());
        branch.setBranchType(request.getBranchType());
        branch.setManagerId(request.getManagerId());
        branch.setEmail(request.getEmail());
        branch.setPhone(request.getPhone());
        branch.setAddress(request.getAddress());
        branch.setDisplayOrder(request.getDisplayOrder());

        Branch updated = branchService.update(id, branch);
        return ApiResponse.success("Branch updated successfully", BranchResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete branch", description = "Soft delete a branch")
    public ApiResponse<Void> deleteBranch(@PathVariable UUID id) {
        branchService.deleteById(id);
        return ApiResponse.success("Branch deleted successfully");
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate branch", description = "Change branch status to ACTIVE")
    public ApiResponse<BranchResponse> activateBranch(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Branch activated = branchService.activate(id, reason);
        return ApiResponse.success("Branch activated successfully", BranchResponse.from(activated));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate branch", description = "Change branch status to INACTIVE")
    public ApiResponse<BranchResponse> deactivateBranch(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Branch deactivated = branchService.deactivate(id, reason);
        return ApiResponse.success("Branch deactivated successfully", BranchResponse.from(deactivated));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close branch", description = "Change branch status to CLOSED")
    public ApiResponse<BranchResponse> closeBranch(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Branch closed = branchService.close(id, reason);
        return ApiResponse.success("Branch closed successfully", BranchResponse.from(closed));
    }

    @PutMapping("/{id}/parent")
    @Operation(summary = "Update branch parent", description = "Change the parent branch and recalculate hierarchy")
    public ApiResponse<BranchResponse> updateParent(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID newParentId) {
        Branch branch = branchService.findById(id);
        branchService.updateParent(branch, newParentId);
        return ApiResponse.success("Branch parent updated successfully", BranchResponse.from(branch));
    }

    @GetMapping("/organization/{organizationId}/count")
    @Operation(summary = "Count branches by organization", description = "Get the total number of branches in an organization")
    public ApiResponse<Long> countBranchesByOrganization(@PathVariable UUID organizationId) {
        long count = branchService.countByOrganizationId(organizationId);
        return ApiResponse.success(count);
    }
}
