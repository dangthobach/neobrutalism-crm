package com.neobrutalism.crm.domain.screenapi.service;

import com.neobrutalism.crm.common.enums.HttpMethod;
import com.neobrutalism.crm.common.enums.PermissionType;
import com.neobrutalism.crm.domain.apiendpoint.model.ApiEndpoint;
import com.neobrutalism.crm.domain.apiendpoint.repository.ApiEndpointRepository;
import com.neobrutalism.crm.domain.menuscreen.model.MenuScreen;
import com.neobrutalism.crm.domain.menuscreen.repository.MenuScreenRepository;
import com.neobrutalism.crm.domain.screenapi.model.ScreenApiEndpoint;
import com.neobrutalism.crm.domain.screenapi.repository.ScreenApiEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for automatically linking MenuScreens to ApiEndpoints based on route patterns
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenApiAutoLinkService {

    private final MenuScreenRepository menuScreenRepository;
    private final ApiEndpointRepository apiEndpointRepository;
    private final ScreenApiEndpointRepository screenApiEndpointRepository;

    /**
     * Auto-link a specific screen to relevant API endpoints
     *
     * @param screenId Screen ID to link
     * @return Map containing statistics about the linking operation
     */
    @Transactional
    public Map<String, Object> autoLinkScreen(UUID screenId) {
        MenuScreen screen = menuScreenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("Screen not found: " + screenId));

        return autoLinkScreen(screen);
    }

    /**
     * Auto-link all screens to relevant API endpoints
     *
     * @return Map containing statistics about the linking operation
     */
    @Transactional
    public Map<String, Object> autoLinkAllScreens() {
        List<MenuScreen> screens = menuScreenRepository.findAll();
        int totalLinked = 0;
        int totalSkipped = 0;
        List<String> errors = new ArrayList<>();

        for (MenuScreen screen : screens) {
            try {
                Map<String, Object> result = autoLinkScreen(screen);
                totalLinked += (Integer) result.get("linkedCount");
                totalSkipped += (Integer) result.get("skippedCount");
            } catch (Exception e) {
                log.error("Error auto-linking screen {}: {}", screen.getCode(), e.getMessage());
                errors.add(screen.getCode() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalScreens", screens.size());
        result.put("totalLinked", totalLinked);
        result.put("totalSkipped", totalSkipped);
        result.put("errors", errors);

        return result;
    }

    /**
     * Auto-link a screen to API endpoints based on route pattern matching
     */
    private Map<String, Object> autoLinkScreen(MenuScreen screen) {
        if (screen.getRoute() == null || screen.getRoute().isBlank()) {
            log.debug("Screen {} has no route, skipping auto-link", screen.getCode());
            return createEmptyResult();
        }

        // Extract base resource from route
        String baseResource = extractBaseResource(screen.getRoute());
        if (baseResource == null) {
            log.debug("Could not extract base resource from route: {}", screen.getRoute());
            return createEmptyResult();
        }

        log.info("Auto-linking screen {} (route: {}) with base resource: {}",
                 screen.getCode(), screen.getRoute(), baseResource);

        // Find matching API endpoints
        List<ApiEndpoint> endpoints = findMatchingEndpoints(baseResource);

        int linkedCount = 0;
        int skippedCount = 0;
        List<String> linkedPaths = new ArrayList<>();

        for (ApiEndpoint endpoint : endpoints) {
            // Check if link already exists
            if (screenApiEndpointRepository.findByScreenIdAndEndpointId(screen.getId(), endpoint.getId()).isPresent()) {
                log.debug("Link already exists: {} -> {}", screen.getCode(), endpoint.getPath());
                skippedCount++;
                continue;
            }

            // Determine permission type based on HTTP method
            PermissionType permission = determinePermissionType(endpoint.getMethod());

            // Create the link
            ScreenApiEndpoint link = new ScreenApiEndpoint();
            link.setScreenId(screen.getId());
            link.setEndpointId(endpoint.getId());
            link.setRequiredPermission(permission);

            screenApiEndpointRepository.save(link);

            linkedCount++;
            linkedPaths.add(endpoint.getMethod() + " " + endpoint.getPath());
            log.info("Linked: {} -> {} {} (permission: {})",
                     screen.getCode(), endpoint.getMethod(), endpoint.getPath(), permission);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("screenCode", screen.getCode());
        result.put("screenRoute", screen.getRoute());
        result.put("baseResource", baseResource);
        result.put("linkedCount", linkedCount);
        result.put("skippedCount", skippedCount);
        result.put("linkedPaths", linkedPaths);

        return result;
    }

    /**
     * Extract base resource from frontend route
     * Examples:
     * - /users -> users
     * - /admin/users/list -> users
     * - /users/{id}/edit -> users
     * - /settings/profile -> settings
     */
    private String extractBaseResource(String route) {
        if (route == null || route.isBlank()) {
            return null;
        }

        // Remove leading slash
        String cleanRoute = route.startsWith("/") ? route.substring(1) : route;

        // Remove admin prefix if exists
        if (cleanRoute.startsWith("admin/")) {
            cleanRoute = cleanRoute.substring(6);
        }

        // Get first segment
        String[] segments = cleanRoute.split("/");
        if (segments.length == 0) {
            return null;
        }

        String firstSegment = segments[0];

        // Ignore segments with path variables
        if (firstSegment.startsWith("{") || firstSegment.startsWith(":")) {
            return null;
        }

        return firstSegment;
    }

    /**
     * Find API endpoints that match the base resource
     * Examples:
     * - baseResource="users" matches /api/users, /api/users/{id}, etc.
     */
    private List<ApiEndpoint> findMatchingEndpoints(String baseResource) {
        List<ApiEndpoint> allEndpoints = apiEndpointRepository.findAll();
        List<ApiEndpoint> matchingEndpoints = new ArrayList<>();

        // Pattern to match /api/{baseResource}/**
        String patternStr = "^/api/" + Pattern.quote(baseResource) + "(/.*)?$";
        Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);

        for (ApiEndpoint endpoint : allEndpoints) {
            if (pattern.matcher(endpoint.getPath()).matches()) {
                matchingEndpoints.add(endpoint);
            }
        }

        log.debug("Found {} matching endpoints for base resource: {}", matchingEndpoints.size(), baseResource);
        return matchingEndpoints;
    }

    /**
     * Determine permission type based on HTTP method
     */
    private PermissionType determinePermissionType(HttpMethod method) {
        return switch (method) {
            case GET, HEAD, OPTIONS -> PermissionType.READ;
            case POST, PUT, PATCH -> PermissionType.WRITE;
            case DELETE -> PermissionType.DELETE;
        };
    }

    private Map<String, Object> createEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("linkedCount", 0);
        result.put("skippedCount", 0);
        result.put("linkedPaths", Collections.emptyList());
        return result;
    }
}
