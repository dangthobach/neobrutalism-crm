package com.neobrutalism.crm.domain.menutab.service;

import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.menutab.model.MenuTab;
import com.neobrutalism.crm.domain.menutab.repository.MenuTabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuTabService extends BaseService<MenuTab> {

    private final MenuTabRepository menuTabRepository;

    @Override
    protected MenuTabRepository getRepository() {
        return menuTabRepository;
    }

    @Override
    protected String getEntityName() {
        return "MenuTab";
    }

    public Optional<MenuTab> findByCode(String code) {
        return menuTabRepository.findByCode(code);
    }

    public List<MenuTab> findByMenuId(UUID menuId) {
        return menuTabRepository.findByMenuIdOrderByDisplayOrderAsc(menuId);
    }

    public List<MenuTab> findVisibleByMenuId(UUID menuId) {
        return menuTabRepository.findByMenuIdAndIsVisibleTrueOrderByDisplayOrderAsc(menuId);
    }
}
