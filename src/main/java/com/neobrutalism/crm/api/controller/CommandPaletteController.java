package com.neobrutalism.crm.api.controller;

import com.neobrutalism.crm.application.dto.command.*;
import com.neobrutalism.crm.application.service.CommandPaletteService;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.domain.command.model.CommandCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for Command Palette functionality.
 *
 * Endpoints:
 * - GET /api/commands/search - Search commands
 * - GET /api/commands/recent - Get recent commands
 * - GET /api/commands/favorites - Get favorite commands
 * - POST /api/commands/favorites/{commandId} - Add to favorites
 * - DELETE /api/commands/favorites/{commandId} - Remove from favorites
 * - POST /api/commands/execute - Record execution
 * - GET /api/commands/suggestions - Get suggested commands
 *
 * @author Admin
 * @since Phase 1
 */
@RestController
@RequestMapping("/api/commands")
@RequiredArgsConstructor
@Slf4j
public class CommandPaletteController {

    private final CommandPaletteService commandPaletteService;

    /**
     * Search commands.
     *
     * GET /api/commands/search?query=customer&category=CUSTOMER&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<CommandSearchResponse> searchCommands(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        String tenantId = TenantContext.getCurrentTenant();
        String userId = userDetails.getUsername();

        CommandSearchRequest request = CommandSearchRequest.builder()
            .tenantId(tenantId)
            .userId(userId)
            .query(query)
            .category(category != null ? CommandCategory.valueOf(category) : null)
            .page(page)
            .size(size)
            .build();

        CommandSearchResponse response = commandPaletteService.searchCommands(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get recent commands for current user.
     *
     * GET /api/commands/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<CommandDto>> getRecentCommands(
            @AuthenticationPrincipal UserDetails userDetails) {

        String tenantId = TenantContext.getCurrentTenant();
        UUID userId = UUID.fromString(userDetails.getUsername());

        List<CommandDto> commands = commandPaletteService.getRecentCommands(tenantId, userId);

        return ResponseEntity.ok(commands);
    }

    /**
     * Get favorite commands for current user.
     *
     * GET /api/commands/favorites
     */
    @GetMapping("/favorites")
    public ResponseEntity<List<CommandDto>> getFavoriteCommands(
            @AuthenticationPrincipal UserDetails userDetails) {

        String tenantId = TenantContext.getCurrentTenant();
        UUID userId = UUID.fromString(userDetails.getUsername());

        List<CommandDto> commands = commandPaletteService.getFavoriteCommands(tenantId, userId);

        return ResponseEntity.ok(commands);
    }

    /**
     * Add command to favorites.
     *
     * POST /api/commands/favorites/{commandId}
     */
    @PostMapping("/favorites/{commandId}")
    public ResponseEntity<Void> addToFavorites(
            @PathVariable UUID commandId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String tenantId = TenantContext.getCurrentTenant();
        UUID userId = UUID.fromString(userDetails.getUsername());

        commandPaletteService.addToFavorites(tenantId, userId, commandId);

        return ResponseEntity.ok().build();
    }

    /**
     * Remove command from favorites.
     *
     * DELETE /api/commands/favorites/{commandId}
     */
    @DeleteMapping("/favorites/{commandId}")
    public ResponseEntity<Void> removeFromFavorites(
            @PathVariable UUID commandId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());

        commandPaletteService.removeFromFavorites(userId, commandId);

        return ResponseEntity.ok().build();
    }

    /**
     * Record command execution.
     *
     * POST /api/commands/execute
     */
    @PostMapping("/execute")
    public ResponseEntity<Void> recordExecution(
            @RequestBody CommandExecutionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String tenantId = TenantContext.getCurrentTenant();
        UUID userId = UUID.fromString(userDetails.getUsername());

        request.setTenantId(tenantId);
        request.setUserId(userId);

        commandPaletteService.recordExecution(request);

        return ResponseEntity.ok().build();
    }

    /**
     * Get suggested commands for current user.
     *
     * GET /api/commands/suggestions?limit=10
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<CommandDto>> getSuggestedCommands(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {

        String tenantId = TenantContext.getCurrentTenant();
        UUID userId = UUID.fromString(userDetails.getUsername());

        List<CommandDto> commands = commandPaletteService.getSuggestedCommands(
            tenantId, userId, limit);

        return ResponseEntity.ok(commands);
    }
}
