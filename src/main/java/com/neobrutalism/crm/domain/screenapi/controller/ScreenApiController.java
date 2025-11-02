package com.neobrutalism.crm.domain.screenapi.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.screenapi.service.ScreenApiAutoLinkService;
import com.neobrutalism.crm.domain.screenapi.service.ScreenApiEndpointService;
import com.neobrutalism.crm.domain.screenapi.service.ScreenApiEndpointService.BulkAssignment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Screen-API management and automation
 */
@RestController
@RequestMapping("/api/screen-api")
@RequiredArgsConstructor
@Tag(name = "Screen API Management", description = "Manage and automate screen-to-API endpoint mappings")
public class ScreenApiController {

    private final ScreenApiAutoLinkService autoLinkService;
    private final ScreenApiEndpointService screenApiEndpointService;

    @PostMapping("/auto-link/screen/{screenId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Auto-link a screen to API endpoints",
               description = "Automatically link a screen to relevant API endpoints based on route pattern matching")
    public ApiResponse<Map<String, Object>> autoLinkScreen(@PathVariable UUID screenId) {
        Map<String, Object> result = autoLinkService.autoLinkScreen(screenId);
        return ApiResponse.success("Auto-link completed", result);
    }

    @PostMapping("/auto-link/all")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Auto-link all screens to API endpoints",
               description = "Automatically link all screens to their relevant API endpoints based on route patterns")
    public ApiResponse<Map<String, Object>> autoLinkAllScreens() {
        Map<String, Object> result = autoLinkService.autoLinkAllScreens();
        return ApiResponse.success("Bulk auto-link completed", result);
    }

    @PostMapping("/bulk-assign/{screenId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Bulk assign endpoints to a screen",
               description = "Assign multiple API endpoints to a screen at once")
    public ApiResponse<Map<String, Object>> bulkAssignEndpoints(
            @PathVariable UUID screenId,
            @Valid @RequestBody List<BulkAssignment> assignments) {
        int assigned = screenApiEndpointService.bulkAssignEndpoints(screenId, assignments);
        Map<String, Object> result = Map.of(
            "screenId", screenId,
            "assigned", assigned,
            "total", assignments.size(),
            "skipped", assignments.size() - assigned
        );
        return ApiResponse.success("Bulk assignment completed", result);
    }

    @DeleteMapping("/bulk-remove/{screenId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Bulk remove endpoints from a screen",
               description = "Remove multiple API endpoints from a screen at once")
    public ApiResponse<Map<String, Object>> bulkRemoveEndpoints(
            @PathVariable UUID screenId,
            @RequestBody List<UUID> endpointIds) {
        int removed = screenApiEndpointService.bulkRemoveEndpoints(screenId, endpointIds);
        Map<String, Object> result = Map.of(
            "screenId", screenId,
            "removed", removed,
            "total", endpointIds.size(),
            "notFound", endpointIds.size() - removed
        );
        return ApiResponse.success("Bulk removal completed", result);
    }
}
