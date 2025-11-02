package com.neobrutalism.crm.domain.userrole.service;

import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.userrole.model.UserRole;
import com.neobrutalism.crm.domain.userrole.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService extends BaseService<UserRole> {

    private final UserRoleRepository userRoleRepository;

    @Override
    protected UserRoleRepository getRepository() {
        return userRoleRepository;
    }

    @Override
    protected String getEntityName() {
        return "UserRole";
    }

    public List<UserRole> findByUserId(UUID userId) {
        return userRoleRepository.findByUserId(userId);
    }

    public List<UserRole> findActiveByUserId(UUID userId) {
        return userRoleRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public List<UserRole> findByRoleId(UUID roleId) {
        return userRoleRepository.findByRoleId(roleId);
    }

    public Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId) {
        return userRoleRepository.findByUserIdAndRoleId(userId, roleId);
    }

    @Transactional
    public void removeUserRole(UUID userId, UUID roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Transactional
    public void expireExpiredRoles() {
        List<UserRole> expiredRoles = userRoleRepository.findByExpiresAtBefore(Instant.now());
        expiredRoles.forEach(role -> {
            role.setIsActive(false);
            userRoleRepository.save(role);
        });
    }
}
