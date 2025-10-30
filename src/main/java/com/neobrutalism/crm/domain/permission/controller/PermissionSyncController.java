package com.neobrutalism.crm.domain.permission.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.permission.service.PermissionSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for permission consistency checking and synchronization
 */
@RestController
@RequestMapping("/api/permissions/sync")
@RequiredArgsConstructor
@Tag(name = "Permission Sync", description = "Check and synchronize permission consistency")
public class PermissionSyncController {

    private final PermissionSyncService permissionSyncService;

    @GetMapping("/check")
    @Operation(summary = "Check permission consistency",
               description = "Check for inconsistencies between Role-Menu permissions and Screen-API permissions")
    public ApiResponse<Map<String, Object>> checkConsistency() {
        Map<String, Object> report = permissionSyncService.checkPermissionConsistency();
        String status = (String) report.get("status");
        String message = "CONSISTENT".equals(status)
                ? "All permissions are consistent"
                : "Found " + report.get("issuesFound") + " permission inconsistencies";

        return ApiResponse.success(message, report);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get suggestions for fixing permission issues",
               description = "Get actionable suggestions for fixing permission inconsistencies")
    public ApiResponse<Map<String, Object>> getSuggestions() {
        Map<String, Object> suggestions = permissionSyncService.getSuggestions();
        return ApiResponse.success("Suggestions generated", suggestions);
    }
}
