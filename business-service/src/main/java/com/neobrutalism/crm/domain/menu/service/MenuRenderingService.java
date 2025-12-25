package com.neobrutalism.crm.domain.menu.service;

import com.neobrutalism.crm.domain.menu.dto.UserMenuResponse;
import com.neobrutalism.crm.domain.menu.model.Menu;
import com.neobrutalism.crm.domain.menu.repository.MenuRepository;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import com.neobrutalism.crm.domain.rolemenu.repository.RoleMenuRepository;
import com.neobrutalism.crm.domain.userrole.model.UserRole;
import com.neobrutalism.crm.domain.userrole.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for rendering user-specific menu trees with permissions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuRenderingService {

    private final MenuRepository menuRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMenuRepository roleMenuRepository;

    /**
     * Get menu tree for a specific user with their permissions
     */
    @Transactional(readOnly = true)
    public List<UserMenuResponse> getUserMenuTree(UUID userId) {
        log.debug("Rendering menu tree for user: {}", userId);

        // Get user's roles
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            log.warn("User {} has no roles assigned", userId);
            return Collections.emptyList();
        }

        Set<UUID> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());

        // Get all menu permissions for user's roles
        List<RoleMenu> roleMenus = roleMenuRepository.findByRoleIdIn(roleIds);
        if (roleMenus.isEmpty()) {
            log.warn("User {} roles have no menu permissions", userId);
            return Collections.emptyList();
        }

        // Group permissions by menu ID and merge (OR logic)
        Map<UUID, UserMenuResponse.MenuPermissions> menuPermissionsMap = mergeMenuPermissions(roleMenus);

        // Get all accessible menu IDs
        Set<UUID> accessibleMenuIds = menuPermissionsMap.keySet();

        // Fetch all menus (including parents needed for tree structure)
        List<Menu> allMenus = menuRepository.findAllActive();

        // Build menu tree including parent menus even if not directly accessible
        Set<UUID> extendedMenuIds = collectMenuIdsWithParents(accessibleMenuIds, allMenus);

        // Filter menus
        List<Menu> visibleMenus = allMenus.stream()
                .filter(menu -> extendedMenuIds.contains(menu.getId()))
                .filter(Menu::getIsVisible)
                .filter(menu -> !menu.getDeleted())
                .collect(Collectors.toList());

        // Convert to DTOs with permissions
        List<UserMenuResponse> menuResponses = visibleMenus.stream()
                .map(menu -> {
                    UserMenuResponse response = UserMenuResponse.from(menu);
                    response.setPermissions(menuPermissionsMap.getOrDefault(
                            menu.getId(),
                            createDefaultPermissions() // Parent menus without direct permission
                    ));
                    return response;
                })
                .collect(Collectors.toList());

        // Build tree structure
        return buildMenuTree(menuResponses);
    }

    /**
     * Merge permissions from multiple roles (OR logic - most permissive wins)
     */
    private Map<UUID, UserMenuResponse.MenuPermissions> mergeMenuPermissions(List<RoleMenu> roleMenus) {
        Map<UUID, UserMenuResponse.MenuPermissions> permissionsMap = new HashMap<>();

        for (RoleMenu roleMenu : roleMenus) {
            UUID menuId = roleMenu.getMenuId();
            UserMenuResponse.MenuPermissions existingPerms = permissionsMap.get(menuId);

            if (existingPerms == null) {
                // First permission for this menu
                permissionsMap.put(menuId, UserMenuResponse.MenuPermissions.builder()
                        .canView(roleMenu.getCanView())
                        .canCreate(roleMenu.getCanCreate())
                        .canEdit(roleMenu.getCanEdit())
                        .canDelete(roleMenu.getCanDelete())
                        .canExport(roleMenu.getCanExport())
                        .canImport(roleMenu.getCanImport())
                        .build());
            } else {
                // Merge with OR logic (any role grants permission)
                existingPerms.setCanView(existingPerms.getCanView() || roleMenu.getCanView());
                existingPerms.setCanCreate(existingPerms.getCanCreate() || roleMenu.getCanCreate());
                existingPerms.setCanEdit(existingPerms.getCanEdit() || roleMenu.getCanEdit());
                existingPerms.setCanDelete(existingPerms.getCanDelete() || roleMenu.getCanDelete());
                existingPerms.setCanExport(existingPerms.getCanExport() || roleMenu.getCanExport());
                existingPerms.setCanImport(existingPerms.getCanImport() || roleMenu.getCanImport());
            }
        }

        return permissionsMap;
    }

    /**
     * Collect all menu IDs including their parent hierarchy
     */
    private Set<UUID> collectMenuIdsWithParents(Set<UUID> menuIds, List<Menu> allMenus) {
        Set<UUID> extendedMenuIds = new HashSet<>(menuIds);
        Map<UUID, Menu> menuMap = allMenus.stream()
                .collect(Collectors.toMap(Menu::getId, menu -> menu));

        // For each accessible menu, add all parent menus
        for (UUID menuId : menuIds) {
            UUID currentMenuId = menuId;
            while (currentMenuId != null) {
                Menu currentMenu = menuMap.get(currentMenuId);
                if (currentMenu == null) break;

                extendedMenuIds.add(currentMenuId);
                currentMenuId = currentMenu.getParentId();
            }
        }

        return extendedMenuIds;
    }

    /**
     * Build hierarchical menu tree
     */
    private List<UserMenuResponse> buildMenuTree(List<UserMenuResponse> allMenus) {
        Map<UUID, UserMenuResponse> menuMap = allMenus.stream()
                .collect(Collectors.toMap(UserMenuResponse::getId, menu -> menu));

        List<UserMenuResponse> rootMenus = new ArrayList<>();

        for (UserMenuResponse menu : allMenus) {
            if (menu.getParentId() == null) {
                rootMenus.add(menu);
            } else {
                UserMenuResponse parent = menuMap.get(menu.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(menu);
                }
            }
        }

        // Sort menus by display order
        sortMenus(rootMenus);

        return rootMenus;
    }

    /**
     * Recursively sort menus and their children by display order
     */
    private void sortMenus(List<UserMenuResponse> menus) {
        menus.sort(Comparator.comparing(UserMenuResponse::getDisplayOrder));
        menus.forEach(menu -> {
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                sortMenus(menu.getChildren());
            }
        });
    }

    /**
     * Create default permissions (view-only for parent menus)
     */
    private UserMenuResponse.MenuPermissions createDefaultPermissions() {
        return UserMenuResponse.MenuPermissions.builder()
                .canView(true)
                .canCreate(false)
                .canEdit(false)
                .canDelete(false)
                .canExport(false)
                .canImport(false)
                .build();
    }
}
