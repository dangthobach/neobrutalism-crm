package com.neobrutalism.crm.domain.menuscreen.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.menuscreen.dto.MenuScreenRequest;
import com.neobrutalism.crm.domain.menuscreen.dto.MenuScreenResponse;
import com.neobrutalism.crm.domain.menuscreen.model.MenuScreen;
import com.neobrutalism.crm.domain.menuscreen.service.MenuScreenService;
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
 * REST controller for MenuScreen management
 */
@RestController
@RequestMapping("/api/menu-screens")
@RequiredArgsConstructor
@Tag(name = "Menu Screens", description = "Menu screen management APIs")
public class MenuScreenController {

    private final MenuScreenService menuScreenService;

    @GetMapping
    @Operation(summary = "Get all menu screens", description = "Retrieve all menu screens with pagination")
    public ApiResponse<PageResponse<MenuScreenResponse>> getAllMenuScreens(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<MenuScreen> menuScreenPage = menuScreenService.findAll(pageable);
        Page<MenuScreenResponse> responsePage = menuScreenPage.map(MenuScreenResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get menu screen by ID", description = "Retrieve a specific menu screen by its ID")
    public ApiResponse<MenuScreenResponse> getMenuScreenById(@PathVariable UUID id) {
        MenuScreen menuScreen = menuScreenService.findById(id);
        return ApiResponse.success(MenuScreenResponse.from(menuScreen));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get menu screen by code", description = "Retrieve a specific menu screen by its unique code")
    public ApiResponse<MenuScreenResponse> getMenuScreenByCode(@PathVariable String code) {
        MenuScreen menuScreen = menuScreenService.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.forResourceByField("MenuScreen", "code", code));
        return ApiResponse.success(MenuScreenResponse.from(menuScreen));
    }

    @GetMapping("/menu/{menuId}")
    @Operation(summary = "Get screens by menu", description = "Retrieve all screens belonging to a specific menu")
    public ApiResponse<List<MenuScreenResponse>> getScreensByMenu(@PathVariable UUID menuId) {
        List<MenuScreen> menuScreens = menuScreenService.findByMenuId(menuId);
        List<MenuScreenResponse> responses = menuScreens.stream()
                .map(MenuScreenResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/tab/{tabId}")
    @Operation(summary = "Get screens by tab", description = "Retrieve all screens belonging to a specific tab")
    public ApiResponse<List<MenuScreenResponse>> getScreensByTab(@PathVariable UUID tabId) {
        List<MenuScreen> menuScreens = menuScreenService.findByTabId(tabId);
        List<MenuScreenResponse> responses = menuScreens.stream()
                .map(MenuScreenResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create menu screen", description = "Create a new menu screen")
    public ApiResponse<MenuScreenResponse> createMenuScreen(@Valid @RequestBody MenuScreenRequest request) {
        MenuScreen menuScreen = new MenuScreen();
        menuScreen.setCode(request.getCode());
        menuScreen.setName(request.getName());
        menuScreen.setMenuId(request.getMenuId());
        menuScreen.setTabId(request.getTabId());
        menuScreen.setRoute(request.getRoute());
        menuScreen.setComponent(request.getComponent());
        menuScreen.setRequiresPermission(request.getRequiresPermission() != null ? request.getRequiresPermission() : true);

        MenuScreen created = menuScreenService.create(menuScreen);
        return ApiResponse.success("Menu screen created successfully", MenuScreenResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update menu screen", description = "Update an existing menu screen")
    public ApiResponse<MenuScreenResponse> updateMenuScreen(
            @PathVariable UUID id,
            @Valid @RequestBody MenuScreenRequest request) {

        MenuScreen menuScreen = menuScreenService.findById(id);
        menuScreen.setCode(request.getCode());
        menuScreen.setName(request.getName());
        menuScreen.setMenuId(request.getMenuId());
        menuScreen.setTabId(request.getTabId());
        menuScreen.setRoute(request.getRoute());
        menuScreen.setComponent(request.getComponent());
        menuScreen.setRequiresPermission(request.getRequiresPermission() != null ? request.getRequiresPermission() : true);

        MenuScreen updated = menuScreenService.update(id, menuScreen);
        return ApiResponse.success("Menu screen updated successfully", MenuScreenResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete menu screen", description = "Delete a menu screen")
    public ApiResponse<Void> deleteMenuScreen(@PathVariable UUID id) {
        menuScreenService.deleteById(id);
        return ApiResponse.success("Menu screen deleted successfully");
    }
}
