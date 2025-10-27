package com.neobrutalism.crm.domain.user.service;

import com.neobrutalism.crm.common.service.EventPublisher;
import com.neobrutalism.crm.common.service.StatefulService;
import com.neobrutalism.crm.domain.user.event.UserDeletedEvent;
import com.neobrutalism.crm.domain.user.event.UserUpdatedEvent;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for User entity
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
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email
     */
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
     */
    public List<User> findByOrganizationId(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId);
    }

    /**
     * Activate user
     */
    @Transactional
    public User activate(UUID id, String reason) {
        return transitionTo(id, UserStatus.ACTIVE, reason);
    }

    /**
     * Suspend user
     */
    @Transactional
    public User suspend(UUID id, String reason) {
        return transitionTo(id, UserStatus.SUSPENDED, reason);
    }

    /**
     * Lock user
     */
    @Transactional
    public User lock(UUID id, String reason) {
        return transitionTo(id, UserStatus.LOCKED, reason);
    }

    /**
     * Unlock user
     */
    @Transactional
    public User unlock(UUID id, String reason) {
        User user = findById(id);
        user.resetFailedLoginAttempts();
        user.transitionTo(UserStatus.ACTIVE, "system", reason);
        return update(id, user);
    }

    /**
     * Find users by status
     */
    public List<User> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status);
    }

    // ==================== Override CRUD with Integrity Check ====================

    /**
     * Create user with database integrity constraint checking
     */
    @Override
    @Transactional
    public User create(User entity) {
        return createWithIntegrityCheck(entity, CONSTRAINT_MESSAGES);
    }

    /**
     * Update user with database integrity constraint checking
     */
    @Override
    @Transactional
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
     */
    @Override
    @Transactional
    public void delete(User entity) {
        deleteWithIntegrityCheck(entity, "related entities (roles, groups, etc.)");
    }
}
