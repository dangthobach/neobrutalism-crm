package com.neobrutalism.crm.domain.user.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Find all admin user IDs for notifications
     * Uses native query to join through user_roles and roles tables
     */
    @Query(value = "SELECT DISTINCT u.id FROM users u " +
           "INNER JOIN user_roles ur ON ur.user_id = u.id " +
           "INNER JOIN roles r ON r.id = ur.role_id " +
           "WHERE r.code = 'ADMIN' AND u.deleted = false AND ur.is_active = true",
           nativeQuery = true)
    List<UUID> findAdminUserIds();

    /**
     * Find all security team user IDs (ADMIN + SECURITY_OFFICER roles)
     * Uses native query to join through user_roles and roles tables
     */
    @Query(value = "SELECT DISTINCT u.id FROM users u " +
           "INNER JOIN user_roles ur ON ur.user_id = u.id " +
           "INNER JOIN roles r ON r.id = ur.role_id " +
           "WHERE r.code IN ('ADMIN', 'SECURITY_OFFICER') AND u.deleted = false AND ur.is_active = true",
           nativeQuery = true)
    List<UUID> findSecurityTeamUserIds();
}
