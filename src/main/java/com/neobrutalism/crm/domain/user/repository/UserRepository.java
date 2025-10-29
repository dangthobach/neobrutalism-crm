package com.neobrutalism.crm.domain.user.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends StatefulRepository<User, UserStatus> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find active user by username
     */
    Optional<User> findByUsernameAndStatus(String username, UserStatus status);

    /**
     * Find active user by email
     */
    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by organization
     */
    java.util.List<User> findByOrganizationId(UUID organizationId);

    /**
     * Count users by organization
     */
    long countByOrganizationId(UUID organizationId);

    /**
     * Find user by username (not deleted)
     */
    Optional<User> findByUsernameAndDeletedFalse(String username);

    /**
     * Find user by ID (not deleted)
     */
    Optional<User> findByIdAndDeletedFalse(UUID id);

    /**
     * Find user by email (not deleted)
     */
    Optional<User> findByEmailAndDeletedFalse(String email);
}
