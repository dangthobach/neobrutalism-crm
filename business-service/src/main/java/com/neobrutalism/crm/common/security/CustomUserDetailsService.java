package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import com.neobrutalism.crm.domain.userrole.model.UserRole;
import com.neobrutalism.crm.domain.userrole.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CustomUserDetailsService - Loads user from database with roles and permissions
 * Used by Spring Security for authentication
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Load user roles
        Set<String> roles = loadUserRoles(user.getId());

        // ✅ FIXED: Load permissions from Casbin
        // Note: Permissions are loaded dynamically by Casbin enforcer on each request
        // We don't pre-load all permissions here to avoid performance issues
        // Casbin will check: enforce(role, domain, resource, action)
        Set<String> permissions = new HashSet<>();
        // Permissions are managed by Casbin policies, not stored in UserPrincipal

        return UserPrincipal.create(user, roles, permissions);
    }

    /**
     * Load user by ID (used for token validation)
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found: " + userId));

        // Load user roles
        Set<String> roles = loadUserRoles(userId);

        // ✅ FIXED: Load permissions from Casbin
        // Permissions are loaded dynamically by Casbin enforcer on each request
        Set<String> permissions = new HashSet<>();
        // Permissions are managed by Casbin policies, not stored in UserPrincipal

        return UserPrincipal.create(user, roles, permissions);
    }

    /**
     * Load user roles from database
     */
    private Set<String> loadUserRoles(UUID userId) {
        // Get all active user-role mappings
        List<UserRole> userRoles = userRoleRepository.findByUserIdAndIsActiveTrue(userId);

        if (userRoles.isEmpty()) {
            log.debug("No roles found for user: {}", userId);
            return new HashSet<>();
        }

        // Get role IDs
        Set<UUID> roleIds = userRoles.stream()
                .filter(ur -> ur.getExpiresAt() == null || ur.getExpiresAt().isAfter(Instant.now()))
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());

        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }

        // Load roles from database
        List<Role> roles = roleRepository.findAllById(roleIds);

        // Extract role codes
        return roles.stream()
                .filter(role -> !role.getDeleted())
                .map(Role::getCode)
                .collect(Collectors.toSet());
    }
}
