package com.neobrutalism.crm.domain.usergroup.service;

import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.usergroup.model.UserGroup;
import com.neobrutalism.crm.domain.usergroup.repository.UserGroupRepository;
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
public class UserGroupService extends BaseService<UserGroup> {

    private final UserGroupRepository userGroupRepository;

    @Override
    protected UserGroupRepository getRepository() {
        return userGroupRepository;
    }

    @Override
    protected String getEntityName() {
        return "UserGroup";
    }

    public List<UserGroup> findByUserId(UUID userId) {
        return userGroupRepository.findByUserId(userId);
    }

    public List<UserGroup> findByGroupId(UUID groupId) {
        return userGroupRepository.findByGroupId(groupId);
    }

    public Optional<UserGroup> findByUserIdAndGroupId(UUID userId, UUID groupId) {
        return userGroupRepository.findByUserIdAndGroupId(userId, groupId);
    }

    public Optional<UserGroup> findPrimaryGroup(UUID userId) {
        return userGroupRepository.findByUserIdAndIsPrimaryTrue(userId);
    }

    @Transactional
    public void removeUserFromGroup(UUID userId, UUID groupId) {
        userGroupRepository.deleteByUserIdAndGroupId(userId, groupId);
    }
}
