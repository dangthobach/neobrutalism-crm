package com.neobrutalism.crm.domain.rolemenu.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleMenuRepository extends BaseRepository<RoleMenu> {
    List<RoleMenu> findByRoleId(UUID roleId);
    List<RoleMenu> findByMenuId(UUID menuId);
    Optional<RoleMenu> findByRoleIdAndMenuId(UUID roleId, UUID menuId);
    boolean existsByRoleIdAndMenuId(UUID roleId, UUID menuId);
    void deleteByRoleIdAndMenuId(UUID roleId, UUID menuId);
}
