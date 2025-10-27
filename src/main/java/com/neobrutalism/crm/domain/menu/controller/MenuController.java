package com.neobrutalism.crm.domain.menu.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.menu.dto.MenuRequest;
import com.neobrutalism.crm.domain.menu.dto.MenuResponse;
import com.neobrutalism.crm.domain.menu.model.Menu;
import com.neobrutalism.crm.domain.menu.service.MenuService;
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
 * REST controller for Menu management
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Tag(name = "Menus", description = "Menu management APIs")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "Get all menus", description = "Retrieve all active menus with pagination")
    public ApiResponse<PageResponse<MenuResponse>> getAllMenus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Menu> menuPage = menuService.findAllActive(pageable);
        Page<MenuResponse> responsePage = menuPage.map(MenuResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get menu by ID", description = "Retrieve a specific menu by its ID")
    public ApiResponse<MenuResponse> getMenuById(@PathVariable UUID id) {
        Menu menu = menuService.findById(id);
        return ApiResponse.success(MenuResponse.from(menu));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get menu by code", description = "Retrieve a specific menu by its unique code")
    public ApiResponse<MenuResponse> getMenuByCode(@PathVariable String code) {
        Menu menu = menuService.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.forResourceByField("Menu", "code", code));
        return ApiResponse.success(MenuResponse.from(menu));
    }

    @GetMapping("/parent/{parentId}")
    @Operation(summary = "Get child menus", description = "Retrieve all menus that are children of a specific parent menu")
    public ApiResponse<List<MenuResponse>> getChildMenus(@PathVariable UUID parentId) {
        List<Menu> menus = menuService.findByParentId(parentId);
        List<MenuResponse> responses = menus.stream()
                .map(MenuResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/root")
    @Operation(summary = "Get root menus", description = "Retrieve all top-level menus (menus without parent)")
    public ApiResponse<List<MenuResponse>> getRootMenus() {
        List<Menu> menus = menuService.findRootMenus();
        List<MenuResponse> responses = menus.stream()
                .map(MenuResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/visible")
    @Operation(summary = "Get visible menus", description = "Retrieve all visible menus ordered by display order")
    public ApiResponse<List<MenuResponse>> getVisibleMenus() {
        List<Menu> menus = menuService.findVisibleMenus();
        List<MenuResponse> responses = menus.stream()
                .map(MenuResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create menu", description = "Create a new menu")
    public ApiResponse<MenuResponse> createMenu(@Valid @RequestBody MenuRequest request) {
        Menu menu = new Menu();
        menu.setCode(request.getCode());
        menu.setName(request.getName());
        menu.setIcon(request.getIcon());
        menu.setParentId(request.getParentId());
        menu.setRoute(request.getRoute());
        menu.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        menu.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : true);
        menu.setRequiresAuth(request.getRequiresAuth() != null ? request.getRequiresAuth() : true);

        // Calculate level and path based on parent
        if (request.getParentId() != null) {
            Menu parent = menuService.findById(request.getParentId());
            menu.setLevel(parent.getLevel() + 1);
            menu.setPath(parent.getPath() + "/" + request.getCode());
        } else {
            menu.setLevel(0);
            menu.setPath("/" + request.getCode());
        }

        Menu created = menuService.create(menu);
        return ApiResponse.success("Menu created successfully", MenuResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update menu", description = "Update an existing menu")
    public ApiResponse<MenuResponse> updateMenu(
            @PathVariable UUID id,
            @Valid @RequestBody MenuRequest request) {

        Menu menu = menuService.findById(id);
        menu.setCode(request.getCode());
        menu.setName(request.getName());
        menu.setIcon(request.getIcon());
        menu.setRoute(request.getRoute());
        menu.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        menu.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : true);
        menu.setRequiresAuth(request.getRequiresAuth() != null ? request.getRequiresAuth() : true);

        // Handle parent change - recalculate level and path
        if (request.getParentId() != null) {
            // Prevent circular reference
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("Menu cannot be its own parent");
            }

            Menu parent = menuService.findById(request.getParentId());

            // Prevent setting a child as parent
            if (parent.getPath() != null && parent.getPath().contains(menu.getPath())) {
                throw new IllegalArgumentException("Cannot set a descendant as parent");
            }

            menu.setParentId(request.getParentId());
            menu.setLevel(parent.getLevel() + 1);
            menu.setPath(parent.getPath() + "/" + request.getCode());
        } else {
            menu.setParentId(null);
            menu.setLevel(0);
            menu.setPath("/" + request.getCode());
        }

        Menu updated = menuService.update(id, menu);
        return ApiResponse.success("Menu updated successfully", MenuResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete menu", description = "Soft delete a menu")
    public ApiResponse<Void> deleteMenu(@PathVariable UUID id) {
        // Check if menu has children
        List<Menu> children = menuService.findByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete menu with children. Please delete or reassign child menus first.");
        }

        menuService.deleteById(id);
        return ApiResponse.success("Menu deleted successfully");
    }

    @PostMapping("/{id}/show")
    @Operation(summary = "Show menu", description = "Make menu visible")
    public ApiResponse<MenuResponse> showMenu(@PathVariable UUID id) {
        Menu menu = menuService.findById(id);
        menu.setIsVisible(true);
        Menu updated = menuService.update(id, menu);
        return ApiResponse.success("Menu is now visible", MenuResponse.from(updated));
    }

    @PostMapping("/{id}/hide")
    @Operation(summary = "Hide menu", description = "Make menu invisible")
    public ApiResponse<MenuResponse> hideMenu(@PathVariable UUID id) {
        Menu menu = menuService.findById(id);
        menu.setIsVisible(false);
        Menu updated = menuService.update(id, menu);
        return ApiResponse.success("Menu is now hidden", MenuResponse.from(updated));
    }

    @PostMapping("/{id}/reorder")
    @Operation(summary = "Reorder menu", description = "Change menu display order")
    public ApiResponse<MenuResponse> reorderMenu(
            @PathVariable UUID id,
            @RequestParam Integer newOrder) {
        Menu menu = menuService.findById(id);
        menu.setDisplayOrder(newOrder);
        Menu updated = menuService.update(id, menu);
        return ApiResponse.success("Menu order updated", MenuResponse.from(updated));
    }
}
