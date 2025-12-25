package com.neobrutalism.crm.domain.menu.service;

import com.neobrutalism.crm.common.service.SoftDeleteService;
import com.neobrutalism.crm.domain.menu.model.Menu;
import com.neobrutalism.crm.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService extends SoftDeleteService<Menu> {

    private final MenuRepository menuRepository;

    @Override
    protected MenuRepository getRepository() {
        return menuRepository;
    }

    @Override
    protected String getEntityName() {
        return "Menu";
    }

    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
            "code", "Menu code already exists.",
            "menus_code_key", "Menu code already exists."
    );

    public Optional<Menu> findByCode(String code) {
        return menuRepository.findByCode(code);
    }

    public List<Menu> findByParentId(UUID parentId) {
        return menuRepository.findByParentId(parentId);
    }

    public List<Menu> findRootMenus() {
        return menuRepository.findByParentIdIsNullOrderByDisplayOrderAsc();
    }

    public List<Menu> findVisibleMenus() {
        return menuRepository.findByIsVisibleTrueOrderByDisplayOrderAsc();
    }
}
