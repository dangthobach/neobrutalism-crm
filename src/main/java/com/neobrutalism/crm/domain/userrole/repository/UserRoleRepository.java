package com.neobrutalism.crm.domain.userrole.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.userrole.model.UserRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends BaseRepository<UserRole> {
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByRoleId(UUID roleId);
    List<UserRole> findByUserIdAndIsActiveTrue(UUID userId);
    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
    List<UserRole> findByExpiresAtBefore(Instant now);

    @Query("SELECT ur.roleId FROM UserRole ur WHERE ur.userId = :userId AND ur.isActive = true AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    List<UUID> findActiveRoleIdsByUserId(@Param("userId") UUID userId);
}
