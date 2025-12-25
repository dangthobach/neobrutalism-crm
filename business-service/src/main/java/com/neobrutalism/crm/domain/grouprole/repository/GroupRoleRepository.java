package com.neobrutalism.crm.domain.grouprole.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.grouprole.model.GroupRole;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRoleRepository extends BaseRepository<GroupRole> {
    List<GroupRole> findByGroupId(UUID groupId);
    List<GroupRole> findByRoleId(UUID roleId);
    Optional<GroupRole> findByGroupIdAndRoleId(UUID groupId, UUID roleId);
    boolean existsByGroupIdAndRoleId(UUID groupId, UUID roleId);
    void deleteByGroupIdAndRoleId(UUID groupId, UUID roleId);
}
