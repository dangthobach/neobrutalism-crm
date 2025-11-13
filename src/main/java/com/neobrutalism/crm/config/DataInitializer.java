package com.neobrutalism.crm.config;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.security.PermissionService;
import com.neobrutalism.crm.domain.organization.model.Organization;
import com.neobrutalism.crm.domain.organization.model.OrganizationStatus;
import com.neobrutalism.crm.domain.organization.repository.OrganizationRepository;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import com.neobrutalism.crm.domain.userrole.model.UserRole;
import com.neobrutalism.crm.domain.userrole.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Data Initializer
 * Automatically creates default admin user if not exists
 * Only runs in dev profile when Flyway is disabled
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Run early, before other initializers
public class DataInitializer {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionService permissionService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDefaultData() {
        log.info("🔧 Initializing default data...");

        try {
            // Set tenant context for this operation
            TenantContext.setCurrentTenant("default");
            
            // Check if admin user exists
            var adminUserOpt = userRepository.findByUsernameAndDeletedFalse("admin");
            
            if (adminUserOpt.isEmpty()) {
                log.info("📝 Creating default admin user...");
                createDefaultAdminUser();
            } else {
                log.info("✅ Admin user already exists, ensuring SUPER_ADMIN role...");
                User adminUser = adminUserOpt.get();
                ensureAdminHasSuperAdminRole(adminUser);
            }
        } catch (Exception e) {
            log.error("❌ Failed to initialize default data: {}", e.getMessage(), e);
            // Don't throw - application should still start even if initialization fails
        } finally {
            TenantContext.clear();
        }
    }

    private void createDefaultAdminUser() {
        // Create or get default organization
        Organization defaultOrg = organizationRepository.findByCode("SYSTEM_ORG")
                .orElseGet(() -> {
                    log.info("📝 Creating default organization...");
                    Organization org = new Organization();
                    // Don't set ID - let Hibernate generate it
                    org.setCode("SYSTEM_ORG");
                    org.setName("System Organization");
                    org.setTenantId("default");
                    // Set email for validation (Active organizations must have at least email or phone)
                    org.setEmail("admin@system.com");
                    // Phone is optional - email is sufficient for validation
                    // Set version = 0 for new entity
                    org.setVersion(0L);
                    // Organization starts with DRAFT status (from getInitialStatus)
                    // We'll activate it after saving
                    org = organizationRepository.save(org);
                    // Activate the organization
                    org.transitionTo(OrganizationStatus.ACTIVE, "system", "Initial system organization");
                    return organizationRepository.save(org);
                });

        // Create admin user
        User admin = new User();
        // Don't set ID - let Hibernate generate it
        // Or set version = 0 for new entity
        admin.setUsername("admin");
        admin.setEmail("admin@system.com");
        // Password: admin123 (BCrypt hash)
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setStatus(UserStatus.ACTIVE);
        admin.setOrganizationId(defaultOrg.getId());
        admin.setTenantId("default");
        admin.setDeleted(false);
        // Set version = 0 for new entity (required when ID is pre-set)
        admin.setVersion(0L);

        User savedAdmin = userRepository.save(admin);

        // Assign SUPER_ADMIN role to admin user
        assignSuperAdminRole(savedAdmin);

        log.info("✅ Default admin user created successfully!");
        log.info("   Username: admin");
        log.info("   Password: admin123");
        log.info("   Email: admin@system.com");
    }

    private void ensureAdminHasSuperAdminRole(User adminUser) {
        // Find SUPER_ADMIN role
        Role superAdminRole = roleRepository.findByCode("SUPER_ADMIN")
                .orElseGet(() -> {
                    log.warn("SUPER_ADMIN role not found, creating it...");
                    return createSuperAdminRole();
                });

        // Check if admin already has SUPER_ADMIN role
        boolean hasRole = userRoleRepository.findByUserIdAndRoleId(adminUser.getId(), superAdminRole.getId())
                .isPresent();

        if (!hasRole) {
            log.info("Assigning SUPER_ADMIN role to admin user...");
            assignRoleToUser(adminUser, superAdminRole);
        } else {
            log.info("Admin user already has SUPER_ADMIN role");
        }
    }

    private void assignSuperAdminRole(User adminUser) {
        // Find or create SUPER_ADMIN role
        Role superAdminRole = roleRepository.findByCode("SUPER_ADMIN")
                .orElseGet(() -> {
                    log.warn("SUPER_ADMIN role not found, creating it...");
                    return createSuperAdminRole();
                });

        assignRoleToUser(adminUser, superAdminRole);
    }

    private Role createSuperAdminRole() {
        // Find default organization
        Organization defaultOrg = organizationRepository.findByCode("SYSTEM_ORG")
                .orElseThrow(() -> new RuntimeException("SYSTEM_ORG not found"));

        Role role = new Role();
        role.setCode("SUPER_ADMIN");
        role.setName("Super Administrator");
        role.setDescription("Full system access with all permissions");
        role.setOrganizationId(defaultOrg.getId());
        role.setStatus(com.neobrutalism.crm.domain.role.model.RoleStatus.ACTIVE);
        role.setIsSystem(true);
        role.setPriority(100);
        role.setTenantId("default");
        role.setDeleted(false);
        role.setVersion(0L);

        return roleRepository.save(role);
    }

    private void assignRoleToUser(User user, Role role) {
        // Check if role is already assigned
        if (userRoleRepository.findByUserIdAndRoleId(user.getId(), role.getId()).isPresent()) {
            log.debug("User {} already has role {}", user.getUsername(), role.getCode());
            return;
        }

        // Create UserRole entity
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRole.setIsActive(true);
        userRole.setGrantedAt(Instant.now());
        userRole.setGrantedBy("system");
        userRole.setTenantId("default");
        userRole.setVersion(0L);

        userRoleRepository.save(userRole);

        // Also assign role in Casbin
        permissionService.assignRoleToUser(user.getId(), "ROLE_" + role.getCode(), "default");

        // Ensure migration API access for admin roles
        if ("SUPER_ADMIN".equals(role.getCode()) || "ADMIN".equals(role.getCode())) {
            ensureMigrationApiAccess(role.getCode());
        }

        log.info("✅ Assigned role {} to user {}", role.getCode(), user.getUsername());
    }

    private void ensureMigrationApiAccess(String roleCode) {
        String roleWithPrefix = "ROLE_" + roleCode;
        String tenantId = "default";
        String resource = "/api/migration.*";
        String action = "(GET)|(POST)|(PUT)|(DELETE)|(PATCH)";

        // Check if policy already exists
        boolean hasPermission = permissionService.hasPermission(roleWithPrefix, tenantId, "/api/migration/upload", "POST");
        
        if (!hasPermission) {
            // Add explicit policy for migration API
            permissionService.addPermissionForRole(roleWithPrefix, tenantId, resource, action);
            log.info("✅ Added migration API access for role {}", roleCode);
        }
    }
}

