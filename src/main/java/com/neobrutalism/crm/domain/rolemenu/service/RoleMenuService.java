package com.neobrutalism.crm.domain.rolemenu.service;

import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import com.neobrutalism.crm.domain.rolemenu.repository.RoleMenuRepository;
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
public class RoleMenuService extends BaseService<RoleMenu> {

    private final RoleMenuRepository roleMenuRepository;

    @Override
    protected RoleMenuRepository getRepository() {
        return roleMenuRepository;
    }

    @Override
    protected String getEntityName() {
        return "RoleMenu";
    }

    public List<RoleMenu> findByRoleId(UUID roleId) {
        return roleMenuRepository.findByRoleId(roleId);
    }

    public List<RoleMenu> findByMenuId(UUID menuId) {
        return roleMenuRepository.findByMenuId(menuId);
    }

    public Optional<RoleMenu> findByRoleIdAndMenuId(UUID roleId, UUID menuId) {
        return roleMenuRepository.findByRoleIdAndMenuId(roleId, menuId);
    }

    @Transactional
    public void removeRoleMenu(UUID roleId, UUID menuId) {
        roleMenuRepository.deleteByRoleIdAndMenuId(roleId, menuId);
    }
}
