package com.neobrutalism.crm.domain.content.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.content.dto.CategoryRequest;
import com.neobrutalism.crm.domain.content.dto.ContentCategoryDTO;
import com.neobrutalism.crm.domain.content.mapper.ContentMapper;
import com.neobrutalism.crm.domain.content.model.ContentCategory;
import com.neobrutalism.crm.domain.content.service.ContentCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Content Category management
 */
@RestController
@RequestMapping("/api/content-categories")
@RequiredArgsConstructor
@Tag(name = "Content Categories", description = "Content category management APIs")
public class ContentCategoryController {

    private final ContentCategoryService categoryService;
    private final ContentMapper contentMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create category", description = "Create new content category")
    public ApiResponse<ContentCategoryDTO> createCategory(
            @Valid @RequestBody CategoryRequest request,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        ContentCategory category = categoryService.createCategory(request, tenantId);
        return ApiResponse.success("Category created successfully", contentMapper.toCategoryDTO(category));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", description = "Update existing content category")
    public ApiResponse<ContentCategoryDTO> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {

        ContentCategory category = categoryService.updateCategory(id, request);
        return ApiResponse.success("Category updated successfully", contentMapper.toCategoryDTO(category));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve a specific category by its ID")
    public ApiResponse<ContentCategoryDTO> getCategoryById(@PathVariable UUID id) {
        ContentCategory category = categoryService.findById(id);
        return ApiResponse.success(contentMapper.toCategoryDTO(category));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieve a specific category by its slug")
    public ApiResponse<ContentCategoryDTO> getCategoryBySlug(
            @PathVariable String slug,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        ContentCategory category = categoryService.findBySlug(slug, tenantId);
        return ApiResponse.success(contentMapper.toCategoryDTO(category));
    }

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieve all categories")
    public ApiResponse<List<ContentCategoryDTO>> getAllCategories(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        List<ContentCategory> categories = categoryService.findAllByTenantId(tenantId);
        List<ContentCategoryDTO> dtos = categories.stream()
            .map(contentMapper::toCategoryDTO)
            .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    @GetMapping("/roots")
    @Operation(summary = "Get root categories", description = "Retrieve all root categories (no parent)")
    public ApiResponse<List<ContentCategoryDTO>> getRootCategories(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        List<ContentCategory> categories = categoryService.findRootCategories(tenantId);
        List<ContentCategoryDTO> dtos = categories.stream()
            .map(contentMapper::toCategoryDTO)
            .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get category children", description = "Get all child categories of a parent category")
    public ApiResponse<List<ContentCategoryDTO>> getCategoryChildren(@PathVariable UUID id) {
        List<ContentCategory> children = categoryService.findChildren(id);
        List<ContentCategoryDTO> dtos = children.stream()
            .map(contentMapper::toCategoryDTO)
            .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    @GetMapping("/with-count")
    @Operation(summary = "Get categories with content count", description = "Get all categories with their content count")
    public ApiResponse<List<Object[]>> getCategoriesWithCount(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        List<Object[]> result = categoryService.findAllWithContentCount(tenantId);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category", description = "Soft delete a content category")
    public ApiResponse<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
        return ApiResponse.success("Category deleted successfully");
    }
}
