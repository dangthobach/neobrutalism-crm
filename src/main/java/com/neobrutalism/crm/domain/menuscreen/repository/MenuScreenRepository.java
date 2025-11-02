package com.neobrutalism.crm.domain.menuscreen.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.menuscreen.model.MenuScreen;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuScreenRepository extends BaseRepository<MenuScreen> {
    Optional<MenuScreen> findByCode(String code);
    List<MenuScreen> findByMenuId(UUID menuId);
    List<MenuScreen> findByTabId(UUID tabId);
    boolean existsByCode(String code);
}
