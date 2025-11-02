package com.neobrutalism.crm.domain.content.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.content.dto.ContentTagDTO;
import com.neobrutalism.crm.domain.content.dto.TagRequest;
import com.neobrutalism.crm.domain.content.mapper.ContentMapper;
import com.neobrutalism.crm.domain.content.model.ContentTag;
import com.neobrutalism.crm.domain.content.service.ContentTagService;
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
 * REST controller for Content Tag management
 */
@RestController
@RequestMapping("/api/content-tags")
@RequiredArgsConstructor
@Tag(name = "Content Tags", description = "Content tag management APIs")
public class ContentTagController {

    private final ContentTagService tagService;
    private final ContentMapper contentMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create tag", description = "Create new content tag")
    public ApiResponse<ContentTagDTO> createTag(
            @Valid @RequestBody TagRequest request,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        ContentTag tag = tagService.createTag(request, tenantId);
        return ApiResponse.success(contentMapper.toTagDTO(tag), "Tag created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tag", description = "Update existing content tag")
    public ApiResponse<ContentTagDTO> updateTag(
            @PathVariable UUID id,
            @Valid @RequestBody TagRequest request) {

        ContentTag tag = tagService.updateTag(id, request);
        return ApiResponse.success(contentMapper.toTagDTO(tag), "Tag updated successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag by ID", description = "Retrieve a specific tag by its ID")
    public ApiResponse<ContentTagDTO> getTagById(@PathVariable UUID id) {
        ContentTag tag = tagService.findById(id);
        return ApiResponse.success(contentMapper.toTagDTO(tag));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get tag by slug", description = "Retrieve a specific tag by its slug")
    public ApiResponse<ContentTagDTO> getTagBySlug(
            @PathVariable String slug,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        ContentTag tag = tagService.findBySlug(slug, tenantId);
        return ApiResponse.success(contentMapper.toTagDTO(tag));
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get tag by name", description = "Retrieve a specific tag by its name")
    public ApiResponse<ContentTagDTO> getTagByName(
            @PathVariable String name,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        ContentTag tag = tagService.findByName(name, tenantId);
        return ApiResponse.success(contentMapper.toTagDTO(tag));
    }

    @GetMapping
    @Operation(summary = "Get all tags", description = "Retrieve all tags for tenant")
    public ApiResponse<List<ContentTagDTO>> getAllTags(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        List<ContentTag> tags = tagService.findAllByTenant(tenantId);
        List<ContentTagDTO> dtos = tags.stream()
            .map(contentMapper::toTagDTO)
            .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    @GetMapping("/search")
    @Operation(summary = "Search tags", description = "Search tags by name")
    public ApiResponse<List<ContentTagDTO>> searchTags(@RequestParam String name) {
        List<ContentTag> tags = tagService.searchByName(name);
        List<ContentTagDTO> dtos = tags.stream()
            .map(contentMapper::toTagDTO)
            .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular tags", description = "Get most used tags")
    public ApiResponse<List<Object[]>> getPopularTags(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        List<Object[]> result = tagService.findPopularTags(tenantId);
        return ApiResponse.success(result);
    }

    @GetMapping("/with-count")
    @Operation(summary = "Get tags with content count", description = "Get all tags with their content count")
    public ApiResponse<List<Object[]>> getTagsWithCount(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        List<Object[]> result = tagService.findAllWithContentCount(tenantId);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tag", description = "Soft delete a content tag")
    public ApiResponse<Void> deleteTag(@PathVariable UUID id) {
        tagService.delete(id);
        return ApiResponse.success(null, "Tag deleted successfully");
    }
}
