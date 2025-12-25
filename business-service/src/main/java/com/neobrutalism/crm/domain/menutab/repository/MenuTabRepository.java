package com.neobrutalism.crm.domain.menutab.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.menutab.model.MenuTab;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuTabRepository extends BaseRepository<MenuTab> {
    Optional<MenuTab> findByCode(String code);
    List<MenuTab> findByMenuIdOrderByDisplayOrderAsc(UUID menuId);
    List<MenuTab> findByMenuIdAndIsVisibleTrueOrderByDisplayOrderAsc(UUID menuId);
    boolean existsByCode(String code);
}
