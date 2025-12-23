package com.neobrutalism.crm.application.service;

import com.neobrutalism.crm.application.dto.command.*;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.security.PermissionService;
import com.neobrutalism.crm.domain.command.model.*;
import com.neobrutalism.crm.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for Command Palette functionality.
 *
 * Provides:
 * - Command search with permission filtering
 * - Recent commands per user
 * - Favorite commands management
 * - Command execution tracking
 * - Personalized suggestions based on usage
 *
 * @author Admin
 * @version 1.0
 * @since Phase 1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommandPaletteService {

    private final CommandRepository commandRepository;
    private final UserCommandHistoryRepository historyRepository;
    private final UserFavoriteCommandRepository favoriteRepository;
    private final PermissionService permissionService;

    /**
     * Search commands by query with permission filtering.
     *
     * Cached for 5 minutes per tenant.
     *
     * @param request Search request
     * @return Search results
     */
    @Cacheable(value = "commandSearch", key = "#request.tenantId + ':' + #request.query")
    @Transactional(readOnly = true)
    public CommandSearchResponse searchCommands(CommandSearchRequest request) {
        String tenantId = request.getTenantId();
        String query = request.getQuery();
        String userId = request.getUserId();
        CommandCategory category = request.getCategory();

        Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.by(Sort.Order.desc("executionCount"))
        );

        Page<Command> commandPage;

        if (category != null) {
            commandPage = commandRepository.findByTenantIdAndCategoryAndIsActiveTrue(
                tenantId, category, pageable);
        } else {
            commandPage = commandRepository.findByTenantIdAndIsActiveTrue(
                tenantId, pageable);
        }

        // Filter by query and permissions
        List<CommandDto> commands = commandPage.getContent().stream()
            .filter(cmd -> cmd.matchesSearch(query))
            .filter(cmd -> hasPermission(tenantId, userId, cmd.getRequiredPermission()))
            .map(this::toDto)
            .collect(Collectors.toList());

        log.debug("Command search: tenantId={}, query={}, results={}",
            tenantId, query, commands.size());

        return CommandSearchResponse.builder()
            .commands(commands)
            .totalCount(commands.size())
            .hasMore(commandPage.hasNext())
            .build();
    }

    /**
     * Get recent commands for user.
     *
     * Returns last 10 commands executed by the user.
     *
     * @param tenantId Tenant ID
     * @param userId User ID
     * @return Recent commands
     */
    @Transactional(readOnly = true)
    public List<CommandDto> getRecentCommands(String tenantId, UUID userId) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("executedAt")));

        List<UserCommandHistory> history = historyRepository
            .findByTenantIdAndUserId(tenantId, userId, pageable);

        return history.stream()
            .map(h -> commandRepository.findById(h.getCommandId()))
            .filter(opt -> opt.isPresent())
            .map(opt -> toDto(opt.get()))
            .collect(Collectors.toList());
    }

    /**
     * Get favorite commands for user.
     *
     * @param tenantId Tenant ID
     * @param userId User ID
     * @return Favorite commands in sort order
     */
    @Transactional(readOnly = true)
    public List<CommandDto> getFavoriteCommands(String tenantId, UUID userId) {
        List<UserFavoriteCommand> favorites = favoriteRepository
            .findByTenantIdAndUserIdOrderBySortOrderAsc(tenantId, userId);

        return favorites.stream()
            .map(f -> commandRepository.findById(f.getCommandId()))
            .filter(opt -> opt.isPresent())
            .map(opt -> toDto(opt.get()))
            .collect(Collectors.toList());
    }

    /**
     * Add command to favorites.
     *
     * @param tenantId Tenant ID
     * @param userId User ID
     * @param commandId Command ID
     */
    @Transactional
    public void addToFavorites(String tenantId, UUID userId, UUID commandId) {
        // Check if already favorited
        if (favoriteRepository.existsByUserIdAndCommandId(userId, commandId)) {
            log.debug("Command already in favorites: userId={}, commandId={}", userId, commandId);
            return;
        }

        // Get next sort order
        int maxSortOrder = favoriteRepository.findMaxSortOrderByUserId(userId)
            .orElse(0);

        UserFavoriteCommand favorite = UserFavoriteCommand.builder()
            .tenantId(tenantId)
            .userId(userId)
            .commandId(commandId)
            .sortOrder(maxSortOrder + 1)
            .build();

        favoriteRepository.save(favorite);

        log.info("Added command to favorites: userId={}, commandId={}", userId, commandId);
    }

    /**
     * Remove command from favorites.
     *
     * @param userId User ID
     * @param commandId Command ID
     */
    @Transactional
    public void removeFromFavorites(UUID userId, UUID commandId) {
        favoriteRepository.deleteByUserIdAndCommandId(userId, commandId);
        log.info("Removed command from favorites: userId={}, commandId={}", userId, commandId);
    }

    /**
     * Record command execution.
     *
     * Updates:
     * - Command execution count and avg execution time
     * - User command history
     *
     * @param request Execution request
     */
    @Transactional
    public void recordExecution(CommandExecutionRequest request) {
        UUID commandId = request.getCommandId();
        long executionTimeMs = request.getExecutionTimeMs();

        // Update command statistics
        commandRepository.findById(commandId).ifPresent(command -> {
            command.recordExecution(executionTimeMs);
            commandRepository.save(command);
        });

        // Record in user history
        UserCommandHistory history = UserCommandHistory.builder()
            .tenantId(request.getTenantId())
            .userId(request.getUserId())
            .commandId(commandId)
            .executionTimeMs(executionTimeMs)
            .contextData(request.getContextData())
            .build();

        historyRepository.save(history);

        log.debug("Recorded command execution: commandId={}, userId={}, time={}ms",
            commandId, request.getUserId(), executionTimeMs);
    }

    /**
     * Get suggested commands based on user history.
     *
     * Uses collaborative filtering:
     * - Most frequently executed commands
     * - Commands executed in similar contexts
     *
     * @param tenantId Tenant ID
     * @param userId User ID
     * @param limit Max suggestions
     * @return Suggested commands
     */
    @Transactional(readOnly = true)
    public List<CommandDto> getSuggestedCommands(
            String tenantId,
            UUID userId,
            int limit) {

        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> topCommands = historyRepository
            .findTopCommandsByUser(userId, pageable);

        return topCommands.stream()
            .map(arr -> (UUID) arr[0])
            .map(cmdId -> commandRepository.findById(cmdId))
            .filter(opt -> opt.isPresent())
            .map(opt -> toDto(opt.get()))
            .collect(Collectors.toList());
    }

    /**
     * Check if user has permission for command.
     * 
     * Permission format: "resource:action" (e.g., "customer:create")
     * Maps to Casbin resource="/api/{resource}" and action
     */
    private boolean hasPermission(String tenantId, String userId, String requiredPermission) {
        if (requiredPermission == null || requiredPermission.isBlank()) {
            return true; // No permission required
        }

        try {
            UUID userUuid = UUID.fromString(userId);
            
            // Parse permission format: "resource:action"
            String[] parts = requiredPermission.split(":");
            if (parts.length != 2) {
                log.warn("Invalid permission format: {}", requiredPermission);
                return false;
            }
            
            String resource = "/api/" + parts[0];
            String action = parts[1].toUpperCase();
            
            // Map action to HTTP method
            String httpMethod = mapActionToHttpMethod(action);
            
            return permissionService.hasPermission(userUuid, tenantId, resource, httpMethod);
        } catch (Exception e) {
            log.warn("Permission check failed: userId={}, permission={}",
                userId, requiredPermission, e);
            return false;
        }
    }

    /**
     * Map action to HTTP method.
     */
    private String mapActionToHttpMethod(String action) {
        return switch (action.toUpperCase()) {
            case "CREATE", "POST" -> "POST";
            case "READ", "VIEW", "GET" -> "GET";
            case "UPDATE", "EDIT", "PUT" -> "PUT";
            case "DELETE" -> "DELETE";
            default -> "GET";
        };
    }

    /**
     * Convert entity to DTO.
     */
    private CommandDto toDto(Command command) {
        return CommandDto.builder()
            .id(command.getId())
            .commandId(command.getCommandId())
            .label(command.getLabel())
            .description(command.getDescription())
            .category(command.getCategory())
            .icon(command.getIcon())
            .shortcutKey(command.getShortcutKey())
            .actionType(command.getActionType())
            .actionPayload(command.getActionPayload())
            .requiredPermission(command.getRequiredPermission())
            .executionCount(command.getExecutionCount())
            .avgExecutionTimeMs(command.getAvgExecutionTimeMs())
            .build();
    }
}
