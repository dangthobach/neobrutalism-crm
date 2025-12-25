package com.neobrutalism.crm.domain.rolemenu.service;

import com.neobrutalism.crm.application.excel.service.ExcelWritingService;
import com.neobrutalism.crm.domain.menu.model.Menu;
import com.neobrutalism.crm.domain.menu.repository.MenuRepository;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import com.neobrutalism.crm.domain.rolemenu.dto.RoleMenuExportDto;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import com.neobrutalism.crm.domain.rolemenu.repository.RoleMenuRepository;
import com.neobrutalism.crm.utils.config.ExcelConfig;
import com.neobrutalism.crm.utils.config.ExcelConfigFactory;
import com.neobrutalism.crm.utils.exception.ExcelProcessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for exporting RoleMenu permissions to Excel
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMenuExportService {

    private final RoleMenuRepository roleMenuRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final ExcelWritingService excelWritingService;

    /**
     * Export permissions for a specific role to Excel
     */
    public byte[] exportRolePermissions(UUID roleId) throws ExcelProcessException {
        log.info("Exporting permissions for role: {}", roleId);

        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        List<RoleMenu> roleMenus = roleMenuRepository.findByRoleId(roleId);

        return exportToBytes(List.of(role), roleMenus);
    }

    /**
     * Export permissions for multiple roles to Excel
     */
    public byte[] exportMultipleRolePermissions(List<UUID> roleIds) throws ExcelProcessException {
        log.info("Exporting permissions for {} roles", roleIds.size());

        List<Role> roles = roleRepository.findAllById(roleIds);
        List<RoleMenu> roleMenus = roleMenuRepository.findByRoleIdIn(roleIds);

        return exportToBytes(roles, roleMenus);
    }

    /**
     * Export all role permissions to Excel
     */
    public byte[] exportAllRolePermissions() throws ExcelProcessException {
        log.info("Exporting all role permissions");

        List<Role> roles = roleRepository.findAll();
        List<RoleMenu> roleMenus = roleMenuRepository.findAll();

        return exportToBytes(roles, roleMenus);
    }

    /**
     * Internal method to export to bytes
     */
    private byte[] exportToBytes(List<Role> roles, List<RoleMenu> roleMenus) throws ExcelProcessException {
        // Build role and menu maps for lookup
        Map<UUID, Role> roleMap = roles.stream()
            .collect(Collectors.toMap(Role::getId, role -> role));

        List<UUID> menuIds = roleMenus.stream()
            .map(RoleMenu::getMenuId)
            .distinct()
            .toList();

        Map<UUID, Menu> menuMap = menuRepository.findAllById(menuIds).stream()
            .collect(Collectors.toMap(Menu::getId, menu -> menu));

        // Convert to export DTOs
        List<RoleMenuExportDto> exportData = new ArrayList<>();

        for (RoleMenu roleMenu : roleMenus) {
            Role role = roleMap.get(roleMenu.getRoleId());
            Menu menu = menuMap.get(roleMenu.getMenuId());

            if (role == null || menu == null) {
                log.warn("Skipping RoleMenu {} - missing role or menu", roleMenu.getId());
                continue;
            }

            RoleMenuExportDto dto = RoleMenuExportDto.builder()
                .roleCode(role.getCode())
                .roleName(role.getName())
                .menuCode(menu.getCode())
                .menuName(menu.getName())
                .menuPath(menu.getPath())
                .canView(RoleMenuExportDto.booleanToString(roleMenu.getCanView()))
                .canCreate(RoleMenuExportDto.booleanToString(roleMenu.getCanCreate()))
                .canEdit(RoleMenuExportDto.booleanToString(roleMenu.getCanEdit()))
                .canDelete(RoleMenuExportDto.booleanToString(roleMenu.getCanDelete()))
                .canExport(RoleMenuExportDto.booleanToString(roleMenu.getCanExport()))
                .canImport(RoleMenuExportDto.booleanToString(roleMenu.getCanImport()))
                .grantedPermissions(RoleMenuExportDto.buildGrantedPermissions(
                    roleMenu.getCanView(),
                    roleMenu.getCanCreate(),
                    roleMenu.getCanEdit(),
                    roleMenu.getCanDelete(),
                    roleMenu.getCanExport(),
                    roleMenu.getCanImport()
                ))
                .build();

            exportData.add(dto);
        }

        // Configure Excel
        ExcelConfig config = ExcelConfigFactory.createProductionConfig();
        config.setOutputBeanClassName(RoleMenuExportDto.class.getName());

        // Export to bytes
        byte[] excelBytes = excelWritingService.writeToBytes(exportData, config);

        log.info("Exported {} permission records to Excel", exportData.size());

        return excelBytes;
    }
}
