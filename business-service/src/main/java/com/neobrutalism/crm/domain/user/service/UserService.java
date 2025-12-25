package com.neobrutalism.crm.domain.user.service;

import com.neobrutalism.crm.common.audit.AuditAction;
import com.neobrutalism.crm.common.audit.Audited;
import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.service.EventPublisher;
import com.neobrutalism.crm.common.service.StatefulService;
import com.neobrutalism.crm.domain.user.dto.UserSearchRequest;
import com.neobrutalism.crm.domain.user.event.UserDeletedEvent;
import com.neobrutalism.crm.domain.user.event.UserUpdatedEvent;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import com.neobrutalism.crm.domain.user.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ✅ PHASE 1 WEEK 3: Service for User management with Redis caching
 * Cache region: "users" with 10 minutes TTL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends StatefulService<User, UserStatus> {

    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Override
    protected UserRepository getRepository() {
        return userRepository;
    }

    @Override
    protected String getEntityName() {
        return "User";
    }

    // Constraint messages for database integrity violations
    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
            "username", "Username already exists. Please use a unique username.",
            "email", "Email already exists. Please use a unique email.",
            "users_username_key", "Username already exists. Please use a unique username.",
            "users_email_key", "Email already exists. Please use a unique email."
    );

    @Override
    protected void afterCreate(User entity) {
        super.afterCreate(entity);
        publishDomainEvents(entity);
    }

    @Override
    protected void afterUpdate(User entity) {
        super.afterUpdate(entity);
        publishDomainEvents(entity);
        eventPublisher.publish(new UserUpdatedEvent(
                entity.getId().toString(),
                entity.getUsername(),
                entity.getUpdatedBy()
        ));
    }

    @Override
    protected void afterDelete(User entity) {
        super.afterDelete(entity);
        eventPublisher.publish(new UserDeletedEvent(
                entity.getId().toString(),
                entity.getUsername(),
                entity.getUpdatedBy()
        ));
    }

    /**
     * Publish domain events
     */
    private void publishDomainEvents(User entity) {
        if (!entity.getDomainEvents().isEmpty()) {
            eventPublisher.publishAll(entity.getDomainEvents());
            entity.clearDomainEvents();
        }
    }

    /**
     * Find user by username
     * Cached: 10 minutes TTL, key by username and tenant
     */
    @Cacheable(value = "users", key = "'username:' + #username + ':tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email
     * Cached: 10 minutes TTL, key by email and tenant
     */
    @Cacheable(value = "users", key = "'email:' + #email + ':tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find active user by username
     */
    public Optional<User> findActiveByUsername(String username) {
        return userRepository.findByUsernameAndStatus(username, UserStatus.ACTIVE);
    }

    /**
     * Find active user by email
     */
    public Optional<User> findActiveByEmail(String email) {
        return userRepository.findByEmailAndStatus(email, UserStatus.ACTIVE);
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Find users by organization
     * Cached: 10 minutes TTL, key by organization ID
     */
    @Cacheable(value = "users", key = "'org:' + #organizationId")
    public List<User> findByOrganizationId(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId);
    }

    /**
     * Activate user
     * Cache eviction: Clears all users cache
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User activate(UUID id, String reason) {
        return transitionTo(id, UserStatus.ACTIVE, reason);
    }

    /**
     * Suspend user
     * Cache eviction: Clears all users cache
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User suspend(UUID id, String reason) {
        return transitionTo(id, UserStatus.SUSPENDED, reason);
    }

    /**
     * Lock user
     * Cache eviction: Clears all users cache
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User lock(UUID id, String reason) {
        return transitionTo(id, UserStatus.LOCKED, reason);
    }

    /**
     * Unlock user
     * Cache eviction: Clears all users cache
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User unlock(UUID id, String reason) {
        User user = findById(id);
        user.resetFailedLoginAttempts();
        user.transitionTo(UserStatus.ACTIVE, "system", reason);
        return update(id, user);
    }

    /**
     * Find users by status
     * Cached: 10 minutes TTL, key by status and tenant
     */
    @Cacheable(value = "users", key = "'status:' + #status + ':tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public List<User> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status);
    }

    // ==================== Override CRUD with Integrity Check ====================

    /**
     * Create user with database integrity constraint checking
     * Cache eviction: Clears all users cache
     */
    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    @Audited(entity = "User", action = AuditAction.CREATE, description = "User created")
    public User create(User entity) {
        return createWithIntegrityCheck(entity, CONSTRAINT_MESSAGES);
    }

    /**
     * Update user with database integrity constraint checking
     * Cache eviction: Clears all users cache
     */
    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    @Audited(entity = "User", action = AuditAction.UPDATE, description = "User updated")
    public User update(UUID id, User entity) {
        return updateWithIntegrityCheck(id, entity, CONSTRAINT_MESSAGES);
    }

    /**
     * Save user with database integrity constraint checking
     */
    @Override
    @Transactional
    public User save(User entity) {
        if (entity.isNew()) {
            return create(entity);
        } else {
            return update(entity.getId(), entity);
        }
    }

    /**
     * Delete user with foreign key constraint checking
     * Cache eviction: Clears all users cache
     */
    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    @Audited(entity = "User", action = AuditAction.DELETE, description = "User deleted")
    public void delete(User entity) {
        deleteWithIntegrityCheck(entity, "related entities (roles, groups, etc.)");
    }

    /**
     * Search users with dynamic filters
     */
    public Page<User> search(UserSearchRequest request, Pageable pageable) {
        Specification<User> spec = UserSpecification.fromSearchRequest(request);
        return userRepository.findAll(spec, pageable);
    }

    /**
     * Get current authenticated user entity
     */
    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("No authenticated user found");
        }

        String username = authentication.getName();
        return findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.forResourceByField("User", "username", username));
    }

    /**
     * Override to get current username from security context
     */
    @Override
    protected String getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Failed to get current user from security context", e);
        }
        return "system";
    }
}
