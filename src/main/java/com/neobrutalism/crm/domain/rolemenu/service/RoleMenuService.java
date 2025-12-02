package com.neobrutalism.crm.domain.rolemenu.service;

import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.config.security.CasbinPolicyManager;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import com.neobrutalism.crm.domain.rolemenu.repository.RoleMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMenuService extends BaseService<RoleMenu> {

    private final RoleMenuRepository roleMenuRepository;
    private final CasbinPolicyManager casbinPolicyManager;
    private final RoleRepository roleRepository;
    private final com.neobrutalism.crm.config.security.CasbinCacheService casbinCacheService;

    @Override
    protected RoleMenuRepository getRepository() {
        return roleMenuRepository;
    }

    @Override
    protected String getEntityName() {
        return "RoleMenu";
    }

    public List<RoleMenu> findByRoleId(UUID roleId) {
        return roleMenuRepository.findByRoleId(roleId);
    }

    public List<RoleMenu> findByMenuId(UUID menuId) {
        return roleMenuRepository.findByMenuId(menuId);
    }

    public Optional<RoleMenu> findByRoleIdAndMenuId(UUID roleId, UUID menuId) {
        return roleMenuRepository.findByRoleIdAndMenuId(roleId, menuId);
    }

    @Transactional
    public void removeRoleMenu(UUID roleId, UUID menuId) {
        roleMenuRepository.deleteByRoleIdAndMenuId(roleId, menuId);
        // Auto-sync Casbin policies after delete
        syncCasbinPoliciesForRole(roleId);
    }

    /**
     * Lifecycle hook: Auto-sync Casbin policies after RoleMenu created
     */
    @Override
    protected void afterCreate(RoleMenu entity) {
        super.afterCreate(entity);
        syncCasbinPoliciesForRole(entity.getRoleId());
    }

    /**
     * Lifecycle hook: Auto-sync Casbin policies after RoleMenu updated
     */
    @Override
    protected void afterUpdate(RoleMenu entity) {
        super.afterUpdate(entity);
        syncCasbinPoliciesForRole(entity.getRoleId());
    }

    /**
     * Lifecycle hook: Auto-sync Casbin policies after RoleMenu deleted
     */
    @Override
    protected void afterDelete(RoleMenu entity) {
        super.afterDelete(entity);
        syncCasbinPoliciesForRole(entity.getRoleId());
    }

    /**
     * Sync Casbin policies for a specific role
     * This ensures permission changes take effect immediately
     * Also invalidates L1 cache for affected role
     */
    private void syncCasbinPoliciesForRole(UUID roleId) {
        try {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null) {
                // Sync policies to Casbin
                int policyCount = casbinPolicyManager.syncRolePolicies(role, true);
                log.info("Auto-synced {} Casbin policies for role: {} (id: {})",
                    policyCount, role.getName(), roleId);

                // ✅ Invalidate L1 cache for this role
                // This ensures users with this role will get fresh permission checks
                casbinCacheService.invalidateRole(role.getName());
                log.debug("Invalidated L1 cache for role: {}", role.getName());
            } else {
                log.warn("Role not found for auto-sync: {}", roleId);
            }
        } catch (Exception e) {
            log.error("Failed to auto-sync Casbin policies for role: {}", roleId, e);
            // Don't throw exception to avoid breaking the transaction
            // Policies will be reloaded on next app restart
        }
    }
}
