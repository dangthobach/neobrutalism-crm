package com.neobrutalism.crm.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Permission Service - Quản lý permissions với Casbin
 *
 * Cấu trúc Policy:
 * - p, role, tenant, resource, action, effect
 *   VD: p, ROLE_ADMIN, default, /api/users/*, (GET)|(POST)|(PUT)|(DELETE), allow
 *
 * - g, user, role, tenant
 *   VD: g, user123, ROLE_ADMIN, default
 *
 * - g2, role, parent_role, tenant (Role Hierarchy)
 *   VD: g2, ROLE_MANAGER, ROLE_ADMIN, default
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final Enforcer enforcer;

    /**
     * Kiểm tra user có quyền truy cập resource không
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param resource Resource path (VD: /api/users, /api/organizations/*)
     * @param action HTTP method (GET, POST, PUT, DELETE)
     * @return true nếu có quyền
     */
    public boolean hasPermission(UUID userId, String tenantId, String resource, String action) {
        String userIdStr = userId.toString();
        boolean result = enforcer.enforce(userIdStr, tenantId, resource, action);

        log.debug("Permission check: user={}, tenant={}, resource={}, action={}, result={}",
                  userIdStr, tenantId, resource, action, result);

        return result;
    }

    /**
     * Kiểm tra subject (role/user) có quyền truy cập resource không
     * Overload version for String subject (role code)
     *
     * @param subject Subject (role code like "ROLE_ADMIN" or user ID)
     * @param tenantId Tenant ID
     * @param resource Resource path (VD: /api/users, /api/organizations/*)
     * @param action HTTP method (GET, POST, PUT, DELETE)
     * @return true nếu có quyền
     */
    public boolean hasPermission(String subject, String tenantId, String resource, String action) {
        boolean result = enforcer.enforce(subject, tenantId, resource, action);

        log.debug("Permission check: subject={}, tenant={}, resource={}, action={}, result={}",
                  subject, tenantId, resource, action, result);

        return result;
    }

    /**
     * Kiểm tra với scope (data scope)
     */
    public boolean hasPermissionWithScope(UUID userId, String tenantId, String resource,
                                          String action, String scope) {
        String userIdStr = userId.toString();
        boolean result = enforcer.enforce(userIdStr, tenantId, resource, action, scope);

        log.debug("Permission check with scope: user={}, tenant={}, resource={}, action={}, scope={}, result={}",
                  userIdStr, tenantId, resource, action, scope, result);

        return result;
    }

    /**
     * Gán role cho user
     */
    public boolean assignRoleToUser(UUID userId, String roleCode, String tenantId) {
        String userIdStr = userId.toString();
        boolean result = enforcer.addGroupingPolicy(userIdStr, roleCode, tenantId);

        log.info("Assigned role {} to user {} in tenant {}: {}", roleCode, userIdStr, tenantId, result);

        return result;
    }

    /**
     * Xóa role khỏi user
     */
    public boolean removeRoleFromUser(UUID userId, String roleCode, String tenantId) {
        String userIdStr = userId.toString();
        boolean result = enforcer.removeGroupingPolicy(userIdStr, roleCode, tenantId);

        log.info("Removed role {} from user {} in tenant {}: {}", roleCode, userIdStr, tenantId, result);

        return result;
    }

    /**
     * Gán permission cho role
     *
     * @param roleCode Role code (VD: ROLE_ADMIN, ROLE_USER)
     * @param tenantId Tenant ID
     * @param resource Resource pattern (VD: /api/users/*, /api/organizations)
     * @param action Action pattern (VD: GET, (GET)|(POST), *)
     */
    public boolean addPermissionForRole(String roleCode, String tenantId, String resource, String action) {
        boolean result = enforcer.addPolicy(roleCode, tenantId, resource, action, "allow");

        log.info("Added permission for role {}: tenant={}, resource={}, action={}, result={}",
                 roleCode, tenantId, resource, action, result);

        return result;
    }

    /**
     * Xóa permission khỏi role
     */
    public boolean removePermissionFromRole(String roleCode, String tenantId, String resource, String action) {
        boolean result = enforcer.removePolicy(roleCode, tenantId, resource, action, "allow");

        log.info("Removed permission from role {}: tenant={}, resource={}, action={}, result={}",
                 roleCode, tenantId, resource, action, result);

        return result;
    }

    /**
     * Lấy tất cả roles của user trong tenant
     */
    public List<String> getRolesForUser(UUID userId, String tenantId) {
        String userIdStr = userId.toString();
        return enforcer.getRolesForUserInDomain(userIdStr, tenantId);
    }

    /**
     * Lấy tất cả users có role trong tenant
     */
    public List<String> getUsersForRole(String roleCode, String tenantId) {
        return enforcer.getUsersForRoleInDomain(roleCode, tenantId);
    }

    /**
     * Lấy tất cả permissions của role trong tenant
     */
    public List<List<String>> getPermissionsForRole(String roleCode, String tenantId) {
        return enforcer.getFilteredPolicy(0, roleCode, tenantId);
    }

    /**
     * Xóa tất cả permissions của user trong tenant
     */
    public boolean deleteUser(UUID userId, String tenantId) {
        String userIdStr = userId.toString();
        boolean result = enforcer.deleteUser(userIdStr);

        log.info("Deleted all permissions for user {} in tenant {}: {}", userIdStr, tenantId, result);

        return result;
    }

    /**
     * Xóa tất cả permissions của role trong tenant
     */
    public boolean deleteRole(String roleCode, String tenantId) {
        boolean result = enforcer.removeFilteredGroupingPolicy(1, roleCode, tenantId);
        boolean result2 = enforcer.removeFilteredPolicy(0, roleCode, tenantId);

        log.info("Deleted all permissions for role {} in tenant {}: {}", roleCode, tenantId, result && result2);

        return result && result2;
    }

    /**
     * Load lại policies từ database
     */
    public void reloadPolicy() {
        enforcer.loadPolicy();
        log.info("Reloaded Casbin policies from database");
    }

    /**
     * Xóa tất cả policies (Cẩn thận!)
     */
    public void clearAllPolicies() {
        enforcer.clearPolicy();
        log.warn("Cleared all Casbin policies");
    }

    /**
     * Batch thêm permissions cho role
     * Hữu ích khi cần gán nhiều permissions cùng lúc
     */
    public boolean addPermissionsForRole(String roleCode, String tenantId,
                                         List<PermissionRequest> permissions) {
        boolean allSuccess = true;

        for (PermissionRequest perm : permissions) {
            boolean success = addPermissionForRole(roleCode, tenantId, perm.getResource(), perm.getAction());
            allSuccess = allSuccess && success;
        }

        return allSuccess;
    }

    /**
     * PermissionRequest DTO
     */
    public static class PermissionRequest {
        private String resource;
        private String action;

        public PermissionRequest() {}

        public PermissionRequest(String resource, String action) {
            this.resource = resource;
            this.action = action;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
