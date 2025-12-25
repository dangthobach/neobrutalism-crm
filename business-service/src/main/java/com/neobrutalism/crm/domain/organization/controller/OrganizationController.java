package com.neobrutalism.crm.domain.organization.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.util.SortValidator;
import com.neobrutalism.crm.domain.organization.dto.OrganizationRequest;
import com.neobrutalism.crm.domain.organization.dto.OrganizationResponse;
import com.neobrutalism.crm.domain.organization.model.Organization;
import com.neobrutalism.crm.domain.organization.model.OrganizationStatus;
import com.neobrutalism.crm.domain.organization.service.OrganizationService;
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

/**
 * REST controller for Organization management
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management APIs")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "Get all organizations", description = "Retrieve all active organizations with pagination")
    public ApiResponse<PageResponse<OrganizationResponse>> getAllOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        String validatedSortBy = SortValidator.validateOrganizationSortField(sortBy);
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

        Page<Organization> organizationPage = organizationService.findAllActive(pageable);
        Page<OrganizationResponse> responsePage = organizationPage.map(OrganizationResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID", description = "Retrieve a specific organization by its ID")
    public ApiResponse<OrganizationResponse> getOrganizationById(@PathVariable UUID id) {
        Organization organization = organizationService.findById(id);
        return ApiResponse.success(OrganizationResponse.from(organization));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get organization by code", description = "Retrieve a specific organization by its unique code")
    public ApiResponse<OrganizationResponse> getOrganizationByCode(@PathVariable String code) {
        Organization organization = organizationService.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.forResourceByField("Organization", "code", code));
        return ApiResponse.success(OrganizationResponse.from(organization));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create organization", description = "Create a new organization")
    public ApiResponse<OrganizationResponse> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        Organization organization = new Organization();
        organization.setName(request.getName());
        organization.setCode(request.getCode());
        organization.setDescription(request.getDescription());
        organization.setEmail(request.getEmail());
        organization.setPhone(request.getPhone());
        organization.setAddress(request.getAddress());
        organization.setWebsite(request.getWebsite());

        Organization created = organizationService.create(organization);
        return ApiResponse.success("Organization created successfully", OrganizationResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update organization", description = "Update an existing organization")
    public ApiResponse<OrganizationResponse> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationRequest request) {

        Organization organization = organizationService.findById(id);
        organization.setName(request.getName());
        organization.setCode(request.getCode());
        organization.setDescription(request.getDescription());
        organization.setEmail(request.getEmail());
        organization.setPhone(request.getPhone());
        organization.setAddress(request.getAddress());
        organization.setWebsite(request.getWebsite());

        Organization updated = organizationService.update(id, organization);
        return ApiResponse.success("Organization updated successfully", OrganizationResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete organization", description = "Soft delete an organization")
    public ApiResponse<Void> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteById(id);
        return ApiResponse.success("Organization deleted successfully");
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate organization", description = "Change organization status to ACTIVE")
    public ApiResponse<OrganizationResponse> activateOrganization(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Organization activated = organizationService.activate(id, reason);
        return ApiResponse.success("Organization activated successfully", OrganizationResponse.from(activated));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend organization", description = "Change organization status to SUSPENDED")
    public ApiResponse<OrganizationResponse> suspendOrganization(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Organization suspended = organizationService.suspend(id, reason);
        return ApiResponse.success("Organization suspended successfully", OrganizationResponse.from(suspended));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive organization", description = "Change organization status to ARCHIVED")
    public ApiResponse<OrganizationResponse> archiveOrganization(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Organization archived = organizationService.archive(id, reason);
        return ApiResponse.success("Organization archived successfully", OrganizationResponse.from(archived));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get organizations by status", description = "Retrieve all organizations with a specific status")
    public ApiResponse<List<OrganizationResponse>> getOrganizationsByStatus(@PathVariable OrganizationStatus status) {
        List<Organization> organizations = organizationService.findByStatus(status);
        List<OrganizationResponse> responses = organizations.stream()
                .map(OrganizationResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}
