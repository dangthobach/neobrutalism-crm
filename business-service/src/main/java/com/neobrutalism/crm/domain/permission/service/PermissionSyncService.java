package com.neobrutalism.crm.domain.permission.service;

import com.neobrutalism.crm.common.enums.PermissionType;
import com.neobrutalism.crm.domain.apiendpoint.repository.ApiEndpointRepository;
import com.neobrutalism.crm.domain.menu.repository.MenuRepository;
import com.neobrutalism.crm.domain.menuscreen.model.MenuScreen;
import com.neobrutalism.crm.domain.menuscreen.repository.MenuScreenRepository;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import com.neobrutalism.crm.domain.rolemenu.repository.RoleMenuRepository;
import com.neobrutalism.crm.domain.screenapi.model.ScreenApiEndpoint;
import com.neobrutalism.crm.domain.screenapi.repository.ScreenApiEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for checking and synchronizing permissions between RoleMenu and ScreenApiEndpoint
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionSyncService {

    private final RoleMenuRepository roleMenuRepository;
    private final MenuScreenRepository menuScreenRepository;
    private final ScreenApiEndpointRepository screenApiEndpointRepository;
    private final MenuRepository menuRepository;
    private final ApiEndpointRepository apiEndpointRepository;

    /**
     * Check for inconsistencies between Role-Menu permissions and Screen-API permissions
     *
     * @return Report containing all inconsistencies found
     */
    public Map<String, Object> checkPermissionConsistency() {
        log.info("Starting permission consistency check...");

        List<RoleMenu> roleMenus = roleMenuRepository.findAll();
        List<Map<String, Object>> inconsistencies = new ArrayList<>();
        int totalChecked = 0;
        int issuesFound = 0;

        for (RoleMenu roleMenu : roleMenus) {
            totalChecked++;

            // Get all screens under this menu
            List<MenuScreen> screens = menuScreenRepository.findByMenuId(roleMenu.getMenuId());

            for (MenuScreen screen : screens) {
                // Get API endpoints linked to this screen
                List<ScreenApiEndpoint> screenApis = screenApiEndpointRepository.findByScreenId(screen.getId());

                if (screenApis.isEmpty()) {
                    // Warning: Screen has no API endpoints linked
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", "MISSING_API_LINKS");
                    issue.put("severity", "WARNING");
                    issue.put("menuId", roleMenu.getMenuId());
                    issue.put("screenId", screen.getId());
                    issue.put("screenCode", screen.getCode());
                    issue.put("screenRoute", screen.getRoute());
                    issue.put("message", "Screen has no API endpoints linked");

                    inconsistencies.add(issue);
                    issuesFound++;
                    continue;
                }

                // Check permission consistency
                Map<String, Object> permissionIssue = checkScreenPermissions(roleMenu, screen, screenApis);
                if (permissionIssue != null) {
                    inconsistencies.add(permissionIssue);
                    issuesFound++;
                }
            }
        }

        log.info("Permission consistency check completed. Checked: {}, Issues found: {}",
                 totalChecked, issuesFound);

        Map<String, Object> report = new HashMap<>();
        report.put("totalRoleMenusChecked", totalChecked);
        report.put("issuesFound", issuesFound);
        report.put("inconsistencies", inconsistencies);
        report.put("status", issuesFound == 0 ? "CONSISTENT" : "INCONSISTENT");

        return report;
    }

    /**
     * Check if Role-Menu permissions match Screen-API permissions
     */
    private Map<String, Object> checkScreenPermissions(RoleMenu roleMenu, MenuScreen screen,
                                                       List<ScreenApiEndpoint> screenApis) {
        // Determine what permissions are required based on screen APIs
        boolean needsRead = false;
        boolean needsWrite = false;
        boolean needsDelete = false;

        for (ScreenApiEndpoint screenApi : screenApis) {
            switch (screenApi.getRequiredPermission()) {
                case READ -> needsRead = true;
                case WRITE -> needsWrite = true;
                case DELETE -> needsDelete = true;
                case EXECUTE -> needsWrite = true; // EXECUTE implies WRITE capability
            }
        }

        // Check for inconsistencies
        List<String> issues = new ArrayList<>();

        if (needsRead && !Boolean.TRUE.equals(roleMenu.getCanView())) {
            issues.add("Screen requires READ permission but role has canView=false");
        }

        if (needsWrite && !Boolean.TRUE.equals(roleMenu.getCanCreate()) && !Boolean.TRUE.equals(roleMenu.getCanEdit())) {
            issues.add("Screen requires WRITE permission but role has canCreate=false and canEdit=false");
        }

        if (needsDelete && !Boolean.TRUE.equals(roleMenu.getCanDelete())) {
            issues.add("Screen requires DELETE permission but role has canDelete=false");
        }

        if (!issues.isEmpty()) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("type", "PERMISSION_MISMATCH");
            issue.put("severity", "ERROR");
            issue.put("roleId", roleMenu.getRoleId());
            issue.put("menuId", roleMenu.getMenuId());
            issue.put("screenId", screen.getId());
            issue.put("screenCode", screen.getCode());
            issue.put("screenRoute", screen.getRoute());
            issue.put("rolePermissions", Map.of(
                "canView", roleMenu.getCanView(),
                "canCreate", roleMenu.getCanCreate(),
                "canEdit", roleMenu.getCanEdit(),
                "canDelete", roleMenu.getCanDelete()
            ));
            issue.put("requiredPermissions", Map.of(
                "needsRead", needsRead,
                "needsWrite", needsWrite,
                "needsDelete", needsDelete
            ));
            issue.put("issues", issues);
            issue.put("message", String.join("; ", issues));

            return issue;
        }

        return null;
    }

    /**
     * Get suggestions for fixing permission inconsistencies
     *
     * @return Map containing suggestions grouped by type
     */
    public Map<String, Object> getSuggestions() {
        Map<String, Object> consistencyReport = checkPermissionConsistency();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inconsistencies =
                (List<Map<String, Object>>) consistencyReport.get("inconsistencies");

        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (Map<String, Object> inconsistency : inconsistencies) {
            String type = (String) inconsistency.get("type");

            if ("MISSING_API_LINKS".equals(type)) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("issue", inconsistency);
                suggestion.put("action", "AUTO_LINK");
                suggestion.put("description", "Use POST /api/screen-api/auto-link/screen/{screenId} to automatically link APIs");
                suggestions.add(suggestion);
            } else if ("PERMISSION_MISMATCH".equals(type)) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("issue", inconsistency);
                suggestion.put("action", "UPDATE_ROLE_MENU");
                suggestion.put("description", "Update RoleMenu permissions to match required API permissions");

                // Calculate suggested permission values
                @SuppressWarnings("unchecked")
                Map<String, Boolean> required =
                        (Map<String, Boolean>) inconsistency.get("requiredPermissions");

                Map<String, Boolean> suggestedPermissions = new HashMap<>();
                suggestedPermissions.put("canView", required.getOrDefault("needsRead", false));
                suggestedPermissions.put("canCreate", required.getOrDefault("needsWrite", false));
                suggestedPermissions.put("canEdit", required.getOrDefault("needsWrite", false));
                suggestedPermissions.put("canDelete", required.getOrDefault("needsDelete", false));

                suggestion.put("suggestedPermissions", suggestedPermissions);
                suggestions.add(suggestion);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalIssues", inconsistencies.size());
        result.put("suggestions", suggestions);

        return result;
    }
}
