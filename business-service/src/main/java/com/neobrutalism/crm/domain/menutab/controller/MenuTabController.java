package com.neobrutalism.crm.domain.menutab.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.menutab.dto.MenuTabRequest;
import com.neobrutalism.crm.domain.menutab.dto.MenuTabResponse;
import com.neobrutalism.crm.domain.menutab.model.MenuTab;
import com.neobrutalism.crm.domain.menutab.service.MenuTabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for MenuTab management
 */
@RestController
@RequestMapping("/api/menu-tabs")
@RequiredArgsConstructor
@Tag(name = "Menu Tabs", description = "Menu tab management APIs")
public class MenuTabController {

    private final MenuTabService menuTabService;

    @GetMapping
    @Operation(summary = "Get all menu tabs", description = "Retrieve all menu tabs with pagination")
    public ApiResponse<PageResponse<MenuTabResponse>> getAllMenuTabs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<MenuTab> menuTabPage = menuTabService.findAll(pageable);
        Page<MenuTabResponse> responsePage = menuTabPage.map(MenuTabResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get menu tab by ID", description = "Retrieve a specific menu tab by its ID")
    public ApiResponse<MenuTabResponse> getMenuTabById(@PathVariable UUID id) {
        MenuTab menuTab = menuTabService.findById(id);
        return ApiResponse.success(MenuTabResponse.from(menuTab));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get menu tab by code", description = "Retrieve a specific menu tab by its unique code")
    public ApiResponse<MenuTabResponse> getMenuTabByCode(@PathVariable String code) {
        MenuTab menuTab = menuTabService.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.forResourceByField("MenuTab", "code", code));
        return ApiResponse.success(MenuTabResponse.from(menuTab));
    }

    @GetMapping("/menu/{menuId}")
    @Operation(summary = "Get tabs by menu", description = "Retrieve all tabs belonging to a specific menu, ordered by display order")
    public ApiResponse<List<MenuTabResponse>> getTabsByMenu(@PathVariable UUID menuId) {
        List<MenuTab> menuTabs = menuTabService.findByMenuId(menuId);
        List<MenuTabResponse> responses = menuTabs.stream()
                .map(MenuTabResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/menu/{menuId}/visible")
    @Operation(summary = "Get visible tabs by menu", description = "Retrieve all visible tabs for a specific menu")
    public ApiResponse<List<MenuTabResponse>> getVisibleTabsByMenu(@PathVariable UUID menuId) {
        List<MenuTab> menuTabs = menuTabService.findVisibleByMenuId(menuId);
        List<MenuTabResponse> responses = menuTabs.stream()
                .map(MenuTabResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create menu tab", description = "Create a new menu tab")
    public ApiResponse<MenuTabResponse> createMenuTab(@Valid @RequestBody MenuTabRequest request) {
        MenuTab menuTab = new MenuTab();
        menuTab.setCode(request.getCode());
        menuTab.setName(request.getName());
        menuTab.setMenuId(request.getMenuId());
        menuTab.setIcon(request.getIcon());
        menuTab.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        menuTab.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : true);

        MenuTab created = menuTabService.create(menuTab);
        return ApiResponse.success("Menu tab created successfully", MenuTabResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update menu tab", description = "Update an existing menu tab")
    public ApiResponse<MenuTabResponse> updateMenuTab(
            @PathVariable UUID id,
            @Valid @RequestBody MenuTabRequest request) {

        MenuTab menuTab = menuTabService.findById(id);
        menuTab.setCode(request.getCode());
        menuTab.setName(request.getName());
        menuTab.setMenuId(request.getMenuId());
        menuTab.setIcon(request.getIcon());
        menuTab.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        menuTab.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : true);

        MenuTab updated = menuTabService.update(id, menuTab);
        return ApiResponse.success("Menu tab updated successfully", MenuTabResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete menu tab", description = "Delete a menu tab")
    public ApiResponse<Void> deleteMenuTab(@PathVariable UUID id) {
        menuTabService.deleteById(id);
        return ApiResponse.success("Menu tab deleted successfully");
    }

    @PostMapping("/{id}/show")
    @Operation(summary = "Show menu tab", description = "Make menu tab visible")
    public ApiResponse<MenuTabResponse> showMenuTab(@PathVariable UUID id) {
        MenuTab menuTab = menuTabService.findById(id);
        menuTab.setIsVisible(true);
        MenuTab updated = menuTabService.update(id, menuTab);
        return ApiResponse.success("Menu tab is now visible", MenuTabResponse.from(updated));
    }

    @PostMapping("/{id}/hide")
    @Operation(summary = "Hide menu tab", description = "Make menu tab invisible")
    public ApiResponse<MenuTabResponse> hideMenuTab(@PathVariable UUID id) {
        MenuTab menuTab = menuTabService.findById(id);
        menuTab.setIsVisible(false);
        MenuTab updated = menuTabService.update(id, menuTab);
        return ApiResponse.success("Menu tab is now hidden", MenuTabResponse.from(updated));
    }

    @PostMapping("/{id}/reorder")
    @Operation(summary = "Reorder menu tab", description = "Change menu tab display order")
    public ApiResponse<MenuTabResponse> reorderMenuTab(
            @PathVariable UUID id,
            @RequestParam Integer newOrder) {
        MenuTab menuTab = menuTabService.findById(id);
        menuTab.setDisplayOrder(newOrder);
        MenuTab updated = menuTabService.update(id, menuTab);
        return ApiResponse.success("Menu tab order updated", MenuTabResponse.from(updated));
    }
}
