package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.domain.permission.event.PermissionChangedEvent;
import com.neobrutalism.crm.domain.permission.service.PermissionAuditService;
import com.neobrutalism.crm.domain.user.model.DataScope;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import com.neobrutalism.crm.common.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final Enforcer enforcer;
    private final PermissionAuditService auditService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Kiểm tra user có quyền truy cập resource không
     * ✅ NEW: Automatically includes scope from DataScopeContext
     * ✅ NEW: Cached for performance (30 minutes TTL)
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param resource Resource path (VD: /api/users, /api/organizations/*)
     * @param action HTTP method (GET, POST, PUT, DELETE)
     * @return true nếu có quyền
     */
    @Cacheable(value = "userPermissions", key = "#userId + ':' + #tenantId + ':' + #resource + ':' + #action")
    public boolean hasPermission(UUID userId, String tenantId, String resource, String action) {
        String userIdStr = userId.toString();

        // ✅ NEW: Get scope from DataScopeContext
        String scope = getScopeFromContext();

        // Enforce with scope
        boolean result = enforcer.enforce(userIdStr, tenantId, resource, action, scope);

        log.debug("Permission check: user={}, tenant={}, resource={}, action={}, scope={}, result={}",
                  userIdStr, tenantId, resource, action, scope, result);

        return result;
    }

    /**
     * Kiểm tra subject (role/user) có quyền truy cập resource không
     * Overload version for String subject (role code)
     * ✅ NEW: Automatically includes scope from DataScopeContext
     *
     * @param subject Subject (role code like "ROLE_ADMIN" or user ID)
     * @param tenantId Tenant ID
     * @param resource Resource path (VD: /api/users, /api/organizations/*)
     * @param action HTTP method (GET, POST, PUT, DELETE)
     * @return true nếu có quyền
     */
    public boolean hasPermission(String subject, String tenantId, String resource, String action) {
        // ✅ NEW: Get scope from DataScopeContext
        String scope = getScopeFromContext();

        // Enforce with scope
        boolean result = enforcer.enforce(subject, tenantId, resource, action, scope);

        log.debug("Permission check: subject={}, tenant={}, resource={}, action={}, scope={}, result={}",
                  subject, tenantId, resource, action, scope, result);

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
        return assignRoleToUser(userId, roleCode, tenantId, null);
    }

    /**
     * Gán role cho user với lý do
     * ✅ NEW: Cache eviction on role assignment
     */
    @CacheEvict(value = {"userPermissions", "userRoles", "permissionMatrix"}, allEntries = true)
    public boolean assignRoleToUser(UUID userId, String roleCode, String tenantId, String reason) {
        String userIdStr = userId.toString();
        boolean result = enforcer.addGroupingPolicy(userIdStr, roleCode, tenantId);

        log.info("Assigned role {} to user {} in tenant {}: {}", roleCode, userIdStr, tenantId, result);

        // Audit logging
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(userId).orElse(null);

        if (currentUser != null && targetUser != null) {
            auditService.logRoleAssignment(
                currentUser.getId(),
                currentUser.getUsername(),
                userId,
                targetUser.getUsername(),
                roleCode,
                reason != null ? reason : "Role assigned via PermissionService"
            );

            // ✅ NEW: Publish permission changed event for cache invalidation
            if (result) {
                PermissionChangedEvent event = PermissionChangedEvent.roleAssigned(
                    tenantId,
                    userId,
                    roleCode,
                    currentUser.getId(),
                    currentUser.getUsername(),
                    reason
                );
                eventPublisher.publishEvent(event);
                log.debug("Published PermissionChangedEvent: ROLE_ASSIGNED");
            }
        }

        return result;
    }

    /**
     * Xóa role khỏi user
     */
    public boolean removeRoleFromUser(UUID userId, String roleCode, String tenantId) {
        return removeRoleFromUser(userId, roleCode, tenantId, null);
    }

    /**
     * Xóa role khỏi user với lý do
     * ✅ NEW: Cache eviction on role removal
     */
    @CacheEvict(value = {"userPermissions", "userRoles", "permissionMatrix"}, allEntries = true)
    public boolean removeRoleFromUser(UUID userId, String roleCode, String tenantId, String reason) {
        String userIdStr = userId.toString();
        boolean result = enforcer.removeGroupingPolicy(userIdStr, roleCode, tenantId);

        log.info("Removed role {} from user {} in tenant {}: {}", roleCode, userIdStr, tenantId, result);

        // Audit logging
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(userId).orElse(null);

        if (currentUser != null && targetUser != null) {
            auditService.logRoleRemoval(
                currentUser.getId(),
                currentUser.getUsername(),
                userId,
                targetUser.getUsername(),
                roleCode,
                reason != null ? reason : "Role removed via PermissionService"
            );

            // ✅ NEW: Publish permission changed event for cache invalidation
            if (result) {
                PermissionChangedEvent event = PermissionChangedEvent.roleRemoved(
                    tenantId,
                    userId,
                    roleCode,
                    currentUser.getId(),
                    currentUser.getUsername(),
                    reason
                );
                eventPublisher.publishEvent(event);
                log.debug("Published PermissionChangedEvent: ROLE_REMOVED");
            }
        }

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
        return addPermissionForRole(roleCode, tenantId, resource, action, null);
    }

    /**
     * Gán permission cho role với lý do
     * ✅ NEW: Cache eviction on policy addition
     */
    @CacheEvict(value = {"rolePermissions", "permissionMatrix"}, allEntries = true)
    public boolean addPermissionForRole(String roleCode, String tenantId, String resource, String action, String reason) {
        boolean result = enforcer.addPolicy(roleCode, tenantId, resource, action, "allow");

        log.info("Added permission for role {}: tenant={}, resource={}, action={}, result={}",
                 roleCode, tenantId, resource, action, result);

        // Audit logging
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            auditService.logPolicyCreation(
                currentUser.getId(),
                currentUser.getUsername(),
                roleCode,
                resource,
                action,
                reason != null ? reason : "Permission added for role via PermissionService"
            );

            // ✅ NEW: Publish permission changed event for cache invalidation
            if (result) {
                PermissionChangedEvent event = PermissionChangedEvent.policyAdded(
                    tenantId,
                    roleCode,
                    resource,
                    action,
                    currentUser.getId(),
                    currentUser.getUsername(),
                    reason
                );
                eventPublisher.publishEvent(event);
                log.debug("Published PermissionChangedEvent: POLICY_ADDED");
            }
        }

        return result;
    }

    /**
     * Xóa permission khỏi role
     */
    public boolean removePermissionFromRole(String roleCode, String tenantId, String resource, String action) {
        return removePermissionFromRole(roleCode, tenantId, resource, action, null);
    }

    /**
     * Xóa permission khỏi role với lý do
     * ✅ NEW: Cache eviction on policy deletion
     */
    @CacheEvict(value = {"rolePermissions", "permissionMatrix"}, allEntries = true)
    public boolean removePermissionFromRole(String roleCode, String tenantId, String resource, String action, String reason) {
        boolean result = enforcer.removePolicy(roleCode, tenantId, resource, action, "allow");

        log.info("Removed permission from role {}: tenant={}, resource={}, action={}, result={}",
                 roleCode, tenantId, resource, action, result);

        // Audit logging
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            auditService.logPolicyDeletion(
                currentUser.getId(),
                currentUser.getUsername(),
                roleCode,
                resource,
                action,
                reason != null ? reason : "Permission removed from role via PermissionService"
            );

            // ✅ NEW: Publish permission changed event for cache invalidation
            if (result) {
                PermissionChangedEvent event = PermissionChangedEvent.policyDeleted(
                    tenantId,
                    roleCode,
                    resource,
                    action,
                    currentUser.getId(),
                    currentUser.getUsername(),
                    reason
                );
                eventPublisher.publishEvent(event);
                log.debug("Published PermissionChangedEvent: POLICY_DELETED");
            }
        }

        return result;
    }

    /**
     * Lấy tất cả roles của user trong tenant
     * ✅ NEW: Cached for performance (30 minutes TTL)
     */
    @Cacheable(value = "userRoles", key = "#userId + ':' + #tenantId")
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
     * ✅ NEW: Cached for performance (1 hour TTL)
     */
    @Cacheable(value = "rolePermissions", key = "#roleCode + ':' + #tenantId")
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
     * Get current authenticated user from security context
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                return (User) authentication.getPrincipal();
            }
            // Try to get by username if principal is string
            if (authentication != null && authentication.getPrincipal() instanceof String) {
                String username = (String) authentication.getPrincipal();
                return userRepository.findByUsername(username).orElse(null);
            }
        } catch (Exception e) {
            log.warn("Failed to get current user from security context", e);
        }
        return null;
    }

    /**
     * ✅ NEW: Get scope from DataScopeContext
     * Returns user's data scope for permission checking
     *
     * @return Scope string (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY, or null for backward compatibility)
     */
    private String getScopeFromContext() {
        try {
            // Get scope from DataScopeContext (set by JwtAuthenticationFilter)
            DataScope dataScope = DataScopeContext.getCurrentDataScope();

            if (dataScope == null) {
                // No scope set - could be system operation or unauthenticated
                log.trace("No data scope in context - using null for backward compatibility");
                return null;
            }

            String scopeStr = dataScope.name(); // ALL_BRANCHES, CURRENT_BRANCH, or SELF_ONLY
            log.trace("Got scope from context: {}", scopeStr);

            return scopeStr;
        } catch (Exception e) {
            log.warn("Failed to get scope from DataScopeContext", e);
            return null; // Fallback to null for backward compatibility
        }
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
