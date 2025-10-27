package com.neobrutalism.crm.domain.menu.repository;

import com.neobrutalism.crm.common.repository.SoftDeleteRepository;
import com.neobrutalism.crm.domain.menu.model.Menu;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuRepository extends SoftDeleteRepository<Menu> {
    Optional<Menu> findByCode(String code);
    List<Menu> findByParentId(UUID parentId);
    List<Menu> findByParentIdIsNullOrderByDisplayOrderAsc();
    List<Menu> findByIsVisibleTrueOrderByDisplayOrderAsc();
    boolean existsByCode(String code);
}
