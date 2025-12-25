package com.neobrutalism.crm.domain.grouprole.service;

import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.grouprole.model.GroupRole;
import com.neobrutalism.crm.domain.grouprole.repository.GroupRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupRoleService extends BaseService<GroupRole> {

    private final GroupRoleRepository groupRoleRepository;

    @Override
    protected GroupRoleRepository getRepository() {
        return groupRoleRepository;
    }

    @Override
    protected String getEntityName() {
        return "GroupRole";
    }

    public List<GroupRole> findByGroupId(UUID groupId) {
        return groupRoleRepository.findByGroupId(groupId);
    }

    public List<GroupRole> findByRoleId(UUID roleId) {
        return groupRoleRepository.findByRoleId(roleId);
    }

    public Optional<GroupRole> findByGroupIdAndRoleId(UUID groupId, UUID roleId) {
        return groupRoleRepository.findByGroupIdAndRoleId(groupId, roleId);
    }

    @Transactional
    public void removeGroupRole(UUID groupId, UUID roleId) {
        groupRoleRepository.deleteByGroupIdAndRoleId(groupId, roleId);
    }
}
