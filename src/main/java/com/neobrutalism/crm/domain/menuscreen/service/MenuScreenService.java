package com.neobrutalism.crm.domain.menuscreen.service;

import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.menuscreen.model.MenuScreen;
import com.neobrutalism.crm.domain.menuscreen.repository.MenuScreenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuScreenService extends BaseService<MenuScreen> {

    private final MenuScreenRepository menuScreenRepository;

    @Override
    protected MenuScreenRepository getRepository() {
        return menuScreenRepository;
    }

    @Override
    protected String getEntityName() {
        return "MenuScreen";
    }

    public Optional<MenuScreen> findByCode(String code) {
        return menuScreenRepository.findByCode(code);
    }

    public List<MenuScreen> findByMenuId(UUID menuId) {
        return menuScreenRepository.findByMenuId(menuId);
    }

    public List<MenuScreen> findByTabId(UUID tabId) {
        return menuScreenRepository.findByTabId(tabId);
    }
}
