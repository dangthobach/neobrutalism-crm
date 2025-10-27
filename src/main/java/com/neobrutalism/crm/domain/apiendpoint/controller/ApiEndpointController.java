package com.neobrutalism.crm.domain.apiendpoint.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.enums.HttpMethod;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.apiendpoint.dto.ApiEndpointRequest;
import com.neobrutalism.crm.domain.apiendpoint.dto.ApiEndpointResponse;
import com.neobrutalism.crm.domain.apiendpoint.model.ApiEndpoint;
import com.neobrutalism.crm.domain.apiendpoint.service.ApiEndpointService;
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
 * REST controller for ApiEndpoint management
 */
@RestController
@RequestMapping("/api/api-endpoints")
@RequiredArgsConstructor
@Tag(name = "API Endpoints", description = "API endpoint management APIs")
public class ApiEndpointController {

    private final ApiEndpointService apiEndpointService;

    @GetMapping
    @Operation(summary = "Get all API endpoints", description = "Retrieve all API endpoints with pagination")
    public ApiResponse<PageResponse<ApiEndpointResponse>> getAllApiEndpoints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "path") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ApiEndpoint> apiEndpointPage = apiEndpointService.findAll(pageable);
        Page<ApiEndpointResponse> responsePage = apiEndpointPage.map(ApiEndpointResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get API endpoint by ID", description = "Retrieve a specific API endpoint by its ID")
    public ApiResponse<ApiEndpointResponse> getApiEndpointById(@PathVariable UUID id) {
        ApiEndpoint apiEndpoint = apiEndpointService.findById(id);
        return ApiResponse.success(ApiEndpointResponse.from(apiEndpoint));
    }

    @GetMapping("/search")
    @Operation(summary = "Get API endpoint by method and path", description = "Retrieve a specific API endpoint by its HTTP method and path")
    public ApiResponse<ApiEndpointResponse> getApiEndpointByMethodAndPath(
            @RequestParam HttpMethod method,
            @RequestParam String path) {
        ApiEndpoint apiEndpoint = apiEndpointService.findByMethodAndPath(method, path)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ApiEndpoint not found with method: " + method + " and path: " + path));
        return ApiResponse.success(ApiEndpointResponse.from(apiEndpoint));
    }

    @GetMapping("/tag/{tag}")
    @Operation(summary = "Get API endpoints by tag", description = "Retrieve all API endpoints with a specific tag")
    public ApiResponse<List<ApiEndpointResponse>> getApiEndpointsByTag(@PathVariable String tag) {
        List<ApiEndpoint> apiEndpoints = apiEndpointService.findByTag(tag);
        List<ApiEndpointResponse> responses = apiEndpoints.stream()
                .map(ApiEndpointResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public API endpoints", description = "Retrieve all public API endpoints")
    public ApiResponse<List<ApiEndpointResponse>> getPublicApiEndpoints() {
        List<ApiEndpoint> apiEndpoints = apiEndpointService.findPublicEndpoints();
        List<ApiEndpointResponse> responses = apiEndpoints.stream()
                .map(ApiEndpointResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create API endpoint", description = "Register a new API endpoint")
    public ApiResponse<ApiEndpointResponse> createApiEndpoint(@Valid @RequestBody ApiEndpointRequest request) {
        ApiEndpoint apiEndpoint = new ApiEndpoint();
        apiEndpoint.setMethod(request.getMethod());
        apiEndpoint.setPath(request.getPath());
        apiEndpoint.setTag(request.getTag());
        apiEndpoint.setDescription(request.getDescription());
        apiEndpoint.setRequiresAuth(request.getRequiresAuth() != null ? request.getRequiresAuth() : true);
        apiEndpoint.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);

        ApiEndpoint created = apiEndpointService.create(apiEndpoint);
        return ApiResponse.success("API endpoint created successfully", ApiEndpointResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update API endpoint", description = "Update an existing API endpoint")
    public ApiResponse<ApiEndpointResponse> updateApiEndpoint(
            @PathVariable UUID id,
            @Valid @RequestBody ApiEndpointRequest request) {

        ApiEndpoint apiEndpoint = apiEndpointService.findById(id);
        apiEndpoint.setMethod(request.getMethod());
        apiEndpoint.setPath(request.getPath());
        apiEndpoint.setTag(request.getTag());
        apiEndpoint.setDescription(request.getDescription());
        apiEndpoint.setRequiresAuth(request.getRequiresAuth() != null ? request.getRequiresAuth() : true);
        apiEndpoint.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);

        ApiEndpoint updated = apiEndpointService.update(id, apiEndpoint);
        return ApiResponse.success("API endpoint updated successfully", ApiEndpointResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete API endpoint", description = "Delete an API endpoint")
    public ApiResponse<Void> deleteApiEndpoint(@PathVariable UUID id) {
        apiEndpointService.deleteById(id);
        return ApiResponse.success("API endpoint deleted successfully");
    }
}
