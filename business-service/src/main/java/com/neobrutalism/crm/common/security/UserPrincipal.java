package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.domain.user.model.DataScope;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UserPrincipal - Represents authenticated user in Spring Security context
 * Contains user information, roles, and permissions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private UUID id;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatar;

    // Organization and Branch
    private UUID organizationId;
    private UUID branchId;
    private DataScope dataScope;

    // Tenant
    private String tenantId;

    // Status and security
    private UserStatus status;
    private boolean accountLocked;
    private Instant lockedUntil;
    private Integer failedLoginAttempts;

    // Roles and permissions
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @Builder.Default
    private Set<String> permissions = new HashSet<>();

    // Accessible branch IDs for data scope filtering
    @Builder.Default
    private Set<UUID> accessibleBranchIds = new HashSet<>();

    /**
     * Create UserPrincipal from User entity
     */
    public static UserPrincipal create(User user, Set<String> roles, Set<String> permissions) {
        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .organizationId(user.getOrganizationId())
                .branchId(user.getBranchId())
                .dataScope(user.getDataScope())
                .tenantId(user.getTenantId())
                .status(user.getStatus())
                .accountLocked(user.isAccountLocked())
                .lockedUntil(user.getLockedUntil())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Combine roles and permissions as authorities
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add roles with ROLE_ prefix
        authorities.addAll(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet()));

        // Add permissions as-is
        authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet()));

        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (accountLocked && lockedUntil != null) {
            return Instant.now().isAfter(lockedUntil);
        }
        return !accountLocked && status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Check if user has specific permission
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
