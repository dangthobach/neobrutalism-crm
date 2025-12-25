package com.neobrutalism.crm.domain.usergroup.service;

import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.usergroup.model.UserGroup;
import com.neobrutalism.crm.domain.usergroup.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ✅ PHASE 1 WEEK 3: Service for UserGroup management with Redis caching
 * Cache region: "usergroups" with 10 minutes TTL
 */
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

    /**
     * Find user groups by user ID
     * Cached: 10 minutes TTL, key by user ID
     */
    @Cacheable(value = "usergroups", key = "'user:' + #userId")
    public List<UserGroup> findByUserId(UUID userId) {
        return userGroupRepository.findByUserId(userId);
    }

    /**
     * Find user groups by group ID
     * Cached: 10 minutes TTL, key by group ID
     */
    @Cacheable(value = "usergroups", key = "'group:' + #groupId")
    public List<UserGroup> findByGroupId(UUID groupId) {
        return userGroupRepository.findByGroupId(groupId);
    }

    /**
     * Find user group by user ID and group ID
     * Cached: 10 minutes TTL, key by user ID and group ID
     */
    @Cacheable(value = "usergroups", key = "'user:' + #userId + ':group:' + #groupId")
    public Optional<UserGroup> findByUserIdAndGroupId(UUID userId, UUID groupId) {
        return userGroupRepository.findByUserIdAndGroupId(userId, groupId);
    }

    /**
     * Find primary group for user
     * Cached: 10 minutes TTL, key by user ID
     */
    @Cacheable(value = "usergroups", key = "'primary:user:' + #userId")
    public Optional<UserGroup> findPrimaryGroup(UUID userId) {
        return userGroupRepository.findByUserIdAndIsPrimaryTrue(userId);
    }

    /**
     * Remove user from group
     * Cache eviction: Clears all usergroups cache
     */
    @Transactional
    @CacheEvict(value = "usergroups", allEntries = true)
    public void removeUserFromGroup(UUID userId, UUID groupId) {
        userGroupRepository.deleteByUserIdAndGroupId(userId, groupId);
    }

    /**
     * Create user group
     * Cache eviction: Clears all usergroups cache
     */
    @Override
    @Transactional
    @CacheEvict(value = "usergroups", allEntries = true)
    public UserGroup create(UserGroup entity) {
        return super.create(entity);
    }

    /**
     * Update user group
     * Cache eviction: Clears all usergroups cache
     */
    @Override
    @Transactional
    @CacheEvict(value = "usergroups", allEntries = true)
    public UserGroup update(UUID id, UserGroup entity) {
        return super.update(id, entity);
    }
}
