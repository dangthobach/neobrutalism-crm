package com.neobrutalism.crm.domain.organization.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.organization.model.OrganizationReadModel;
import com.neobrutalism.crm.domain.organization.model.OrganizationStatus;
import com.neobrutalism.crm.domain.organization.repository.OrganizationReadModelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Query controller for Organizations using Read Model (CQRS)
 * Optimized for read operations with denormalized data
 */
@Slf4j
@RestController
@RequestMapping("/api/organizations/query")
@RequiredArgsConstructor
@Tag(name = "Organization Queries", description = "Optimized read operations using CQRS read model")
public class OrganizationQueryController {

    private final OrganizationReadModelRepository readModelRepository;

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID (from read model)")
    public ResponseEntity<ApiResponse<OrganizationReadModel>> getById(@PathVariable UUID id) {
        log.debug("Query: Get organization by ID from read model: {}", id);

        return readModelRepository.findById(id)
                .map(org -> ResponseEntity.ok(ApiResponse.success("Organization retrieved", org)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active organizations")
    public ResponseEntity<ApiResponse<List<OrganizationReadModel>>> getActive() {
        log.debug("Query: Get all active organizations");

        List<OrganizationReadModel> organizations = readModelRepository.findByIsActiveTrue();
        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + organizations.size() + " active organizations",
                organizations
        ));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get organizations by status")
    public ResponseEntity<ApiResponse<List<OrganizationReadModel>>> getByStatus(
            @PathVariable OrganizationStatus status) {
        log.debug("Query: Get organizations by status: {}", status);

        List<OrganizationReadModel> organizations = readModelRepository.findByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + organizations.size() + " organizations with status " + status,
                organizations
        ));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search across organization data")
    public ResponseEntity<ApiResponse<List<OrganizationReadModel>>> search(
            @RequestParam String query) {
        log.debug("Query: Search organizations with term: {}", query);

        List<OrganizationReadModel> organizations = readModelRepository.search(query);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + organizations.size() + " organizations matching '" + query + "'",
                organizations
        ));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get organization by code")
    public ResponseEntity<ApiResponse<OrganizationReadModel>> getByCode(@PathVariable String code) {
        log.debug("Query: Get organization by code: {}", code);

        OrganizationReadModel organization = readModelRepository.findByCode(code);
        if (organization == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success("Organization retrieved", organization));
    }

    @GetMapping("/recent/{days}")
    @Operation(summary = "Get recently created organizations (within N days)")
    public ResponseEntity<ApiResponse<List<OrganizationReadModel>>> getRecent(
            @PathVariable int days) {
        log.debug("Query: Get organizations created within {} days", days);

        List<OrganizationReadModel> organizations = readModelRepository.findRecentlyCreated(days);
        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + organizations.size() + " organizations created within " + days + " days",
                organizations
        ));
    }

    @GetMapping("/with-contact")
    @Operation(summary = "Get organizations with contact information")
    public ResponseEntity<ApiResponse<List<OrganizationReadModel>>> getWithContactInfo() {
        log.debug("Query: Get organizations with contact information");

        List<OrganizationReadModel> organizations = readModelRepository.findByHasContactInfoTrue();
        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + organizations.size() + " organizations with contact info",
                organizations
        ));
    }

    @GetMapping("/active-with-contact")
    @Operation(summary = "Get active organizations with contact information")
    public ResponseEntity<ApiResponse<List<OrganizationReadModel>>> getActiveWithContactInfo() {
        log.debug("Query: Get active organizations with contact information");

        List<OrganizationReadModel> organizations = readModelRepository.findActiveWithContactInfo();
        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + organizations.size() + " active organizations with contact info",
                organizations
        ));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get organization statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatistics() {
        log.debug("Query: Get organization statistics");

        Map<String, Long> stats = readModelRepository.getStatistics();
        return ResponseEntity.ok(ApiResponse.success("Organization statistics retrieved", stats));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all non-deleted organizations")
    public ResponseEntity<ApiResponse<List<OrganizationReadModel>>> getAll() {
        log.debug("Query: Get all non-deleted organizations");

        List<OrganizationReadModel> organizations = readModelRepository.findByIsDeletedFalse();
        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + organizations.size() + " organizations",
                organizations
        ));
    }
}
