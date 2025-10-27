package com.neobrutalism.crm.domain.usergroup.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.usergroup.model.UserGroup;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends BaseRepository<UserGroup> {
    List<UserGroup> findByUserId(UUID userId);
    List<UserGroup> findByGroupId(UUID groupId);
    Optional<UserGroup> findByUserIdAndGroupId(UUID userId, UUID groupId);
    Optional<UserGroup> findByUserIdAndIsPrimaryTrue(UUID userId);
    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);
    void deleteByUserIdAndGroupId(UUID userId, UUID groupId);
}
