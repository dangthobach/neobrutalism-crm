package com.neobrutalism.crm.config.security;

import com.neobrutalism.crm.domain.menu.model.Menu;
import com.neobrutalism.crm.domain.menu.repository.MenuRepository;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import com.neobrutalism.crm.domain.rolemenu.repository.RoleMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casbin Policy Manager
 * Quản lý dynamic loading và syncing policies từ database vào Casbin
 * 
 * Features:
 * - Auto-load policies on application startup
 * - Sync role-menu permissions to Casbin
 * - Support multi-tenant authorization
 * - Handle hierarchical resources
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CasbinPolicyManager {

    private final Enforcer enforcer;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;

    /**
     * Load all policies from database when application starts
     * This ensures Casbin has all role-menu mappings for authorization
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void loadPoliciesOnStartup() {
        log.info("Loading Casbin policies from database...");
        
        try {
            // Clear existing policies to avoid duplicates
            enforcer.clearPolicy();
            
            // Load all roles with their menu permissions
            List<Role> roles = roleRepository.findAll();
            
            int policyCount = 0;
            for (Role role : roles) {
                policyCount += syncRolePolicies(role, false);
            }
            
            // Save all policies to database
            enforcer.savePolicy();
            
            log.info("Successfully loaded {} policies for {} roles", 
                policyCount, roles.size());
                
        } catch (Exception e) {
            log.error("Failed to load Casbin policies from database", e);
            throw new RuntimeException("Failed to initialize authorization system", e);
        }
    }

    /**
     * Sync a single role's policies to Casbin
     * Called when role permissions are updated
     * 
     * @param role Role to sync
     * @param save Whether to save policies immediately
     * @return Number of policies added
     */
    @Transactional(readOnly = true)
    public int syncRolePolicies(Role role, boolean save) {
        log.debug("Syncing policies for role: {}", role.getName());
        
        // Remove old policies for this role (all domains)
        enforcer.removeFilteredPolicy(0, role.getName());
        
        int count = 0;
        
        // Get role-menu mappings for this role
        List<RoleMenu> roleMenus = roleMenuRepository.findByRoleId(role.getId());
        
        // Add new policies from role-menu mappings
        for (RoleMenu roleMenu : roleMenus) {
            // Get menu details
            Menu menu = menuRepository.findById(roleMenu.getMenuId())
                .orElse(null);
            
            if (menu == null || menu.getPath() == null) {
                log.warn("Menu not found or has no path for roleMenu: {}", roleMenu.getId());
                continue;
            }
            
            String resource = menu.getPath();
            String domain = role.getOrganizationId().toString();
            
            // Add policy for each enabled action
            List<String> actions = buildActions(roleMenu);
            
            for (String action : actions) {
                // Format: p, role, domain, resource, action, allow
                boolean added = enforcer.addPolicy(
                    role.getName(),
                    domain,
                    resource,
                    action,
                    "allow"
                );
                
                if (added) {
                    count++;
                    log.trace("Added policy: role={}, domain={}, resource={}, action={}", 
                        role.getName(), domain, resource, action);
                }
            }
        }
        
        if (save) {
            enforcer.savePolicy();
        }
        
        log.debug("Synced {} policies for role: {}", count, role.getName());
        return count;
    }

    /**
     * Remove all policies for a role
     * Called when role is deleted
     */
    @Transactional
    public void removeRolePolicies(String roleName) {
        log.debug("Removing all policies for role: {}", roleName);
        
        enforcer.removeFilteredPolicy(0, roleName);
        enforcer.savePolicy();
        
        log.info("Removed all policies for role: {}", roleName);
    }

    /**
     * Add role hierarchy (role inheritance)
     * Example: g, user, admin, domain -> user inherits admin permissions in domain
     */
    @Transactional
    public void addRoleInheritance(String childRole, String parentRole, String domain) {
        log.debug("Adding role inheritance: {} -> {} in domain {}", 
            childRole, parentRole, domain);
        
        enforcer.addGroupingPolicy(childRole, parentRole, domain);
        enforcer.savePolicy();
    }

    /**
     * Check if user has permission
     * This is a convenience method for testing
     */
    public boolean checkPermission(String user, String domain, String resource, String action) {
        return enforcer.enforce(user, domain, resource, action);
    }

    /**
     * Get all policies for debugging
     */
    public List<List<String>> getAllPolicies() {
        return enforcer.getPolicy();
    }

    /**
     * Get all grouping policies (role inheritance)
     */
    public List<List<String>> getAllGroupingPolicies() {
        return enforcer.getGroupingPolicy();
    }

    /**
     * Build action list from RoleMenu permissions
     */
    private List<String> buildActions(RoleMenu roleMenu) {
        List<String> actions = new java.util.ArrayList<>();
        
        if (roleMenu.getCanCreate() != null && roleMenu.getCanCreate()) {
            actions.add("create");
        }
        if (roleMenu.getCanView() != null && roleMenu.getCanView()) {
            actions.add("read"); // Map canView to "read" action
        }
        if (roleMenu.getCanEdit() != null && roleMenu.getCanEdit()) {
            actions.add("update"); // Map canEdit to "update" action
        }
        if (roleMenu.getCanDelete() != null && roleMenu.getCanDelete()) {
            actions.add("delete");
        }
        if (roleMenu.getCanExport() != null && roleMenu.getCanExport()) {
            actions.add("export");
        }
        if (roleMenu.getCanImport() != null && roleMenu.getCanImport()) {
            actions.add("import");
        }
        
        return actions;
    }

    /**
     * Reload all policies from database
     * Useful for manual refresh or after bulk updates
     */
    @Transactional(readOnly = true)
    public void reloadAllPolicies() {
        log.info("Manually reloading all Casbin policies...");
        loadPoliciesOnStartup();
    }
}
