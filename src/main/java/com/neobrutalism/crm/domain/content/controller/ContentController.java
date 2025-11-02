package com.neobrutalism.crm.domain.content.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.enums.ContentStatus;
import com.neobrutalism.crm.common.enums.MemberTier;
import com.neobrutalism.crm.domain.content.dto.*;
import com.neobrutalism.crm.domain.content.mapper.ContentMapper;
import com.neobrutalism.crm.domain.content.model.Content;
import com.neobrutalism.crm.domain.content.model.ContentReadModel;
import com.neobrutalism.crm.domain.content.service.ContentReadModelService;
import com.neobrutalism.crm.domain.content.service.ContentService;
import com.neobrutalism.crm.domain.content.service.ContentViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * REST controller for Content management (CMS)
 */
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
@Tag(name = "Content", description = "Content management APIs for CMS")
public class ContentController {

    private final ContentService contentService;
    private final ContentReadModelService readModelService;
    private final ContentViewService viewService;
    private final ContentMapper contentMapper;

    // ==================== Admin APIs (Write Model) ====================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create content", description = "Create new content (admin)")
    public ApiResponse<ContentDTO> createContent(
            @Valid @RequestBody CreateContentRequest request,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        UUID authorId = getCurrentUserId();
        Content content = contentService.createContent(request, authorId, tenantId);
        return ApiResponse.success(contentMapper.toDTO(content), "Content created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update content", description = "Update existing content (admin)")
    public ApiResponse<ContentDTO> updateContent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContentRequest request) {

        Content content = contentService.updateContent(id, request);
        return ApiResponse.success(contentMapper.toDTO(content), "Content updated successfully");
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish content", description = "Publish content to make it publicly accessible")
    public ApiResponse<ContentDTO> publishContent(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {

        Content content = contentService.publishContent(id, reason);
        return ApiResponse.success(contentMapper.toDTO(content), "Content published successfully");
    }

    @PostMapping("/{id}/submit-review")
    @Operation(summary = "Submit for review", description = "Submit content for review before publishing")
    public ApiResponse<ContentDTO> submitForReview(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {

        Content content = contentService.submitForReview(id, reason);
        return ApiResponse.success(contentMapper.toDTO(content), "Content submitted for review");
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive content", description = "Archive content to remove from public view")
    public ApiResponse<ContentDTO> archiveContent(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {

        Content content = contentService.archiveContent(id, reason);
        return ApiResponse.success(contentMapper.toDTO(content), "Content archived successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete content", description = "Soft delete content")
    public ApiResponse<Void> deleteContent(@PathVariable UUID id) {
        contentService.delete(id);
        return ApiResponse.success(null, "Content deleted successfully");
    }

    // ==================== Public APIs (Read Model) ====================

    @GetMapping
    @Operation(summary = "List published content", description = "Get all published content with pagination")
    public ApiResponse<PageResponse<ContentDTO>> listPublishedContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ContentReadModel> contentPage = readModelService.findPublished(tenantId, pageable);
        Page<ContentDTO> responsePage = contentPage.map(contentMapper::fromReadModel);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get content by slug", description = "Get published content by URL slug")
    public ApiResponse<ContentDTO> getContentBySlug(
            @PathVariable String slug,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        ContentReadModel readModel = readModelService.findBySlug(slug, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Content not found with slug: " + slug));

        // For full content, get from write model
        Content content = contentService.findBySlug(slug, tenantId);

        return ApiResponse.success(contentMapper.toDTO(content));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get content by category", description = "Get all published content in a category")
    public ApiResponse<PageResponse<ContentDTO>> getContentByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Content> contentPage = contentService.findByCategory(categoryId, pageable);
        Page<ContentDTO> responsePage = contentPage.map(contentMapper::toDTO);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/tag/{tagId}")
    @Operation(summary = "Get content by tag", description = "Get all published content with a specific tag")
    public ApiResponse<PageResponse<ContentDTO>> getContentByTag(
            @PathVariable UUID tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Content> contentPage = contentService.findByTag(tagId, pageable);
        Page<ContentDTO> responsePage = contentPage.map(contentMapper::toDTO);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/search")
    @Operation(summary = "Search content", description = "Search published content by keyword")
    public ApiResponse<PageResponse<ContentDTO>> searchContent(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ContentReadModel> contentPage = readModelService.search(keyword, pageable);
        Page<ContentDTO> responsePage = contentPage.map(contentMapper::fromReadModel);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending content", description = "Get most viewed content in the last 7 days")
    public ApiResponse<PageResponse<ContentDTO>> getTrendingContent(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(page, size);

        Page<ContentReadModel> contentPage = readModelService.findTrending(since, pageable);
        Page<ContentDTO> responsePage = contentPage.map(contentMapper::fromReadModel);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recently published content", description = "Get content published in the last N days")
    public ApiResponse<PageResponse<ContentDTO>> getRecentContent(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(page, size);

        Page<ContentReadModel> contentPage = readModelService.findRecentlyPublished(since, pageable);
        Page<ContentDTO> responsePage = contentPage.map(contentMapper::fromReadModel);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/tier/{tier}")
    @Operation(summary = "Get content by tier", description = "Get content accessible to specific member tier")
    public ApiResponse<PageResponse<ContentDTO>> getContentByTier(
            @PathVariable MemberTier tier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<ContentReadModel> contentPage = readModelService.findAccessibleForTier(tier, pageable);
        Page<ContentDTO> responsePage = contentPage.map(contentMapper::fromReadModel);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    // ==================== View Tracking APIs ====================

    @PostMapping("/{id}/view")
    @Operation(summary = "Track content view", description = "Track when a user views content (for analytics)")
    public ApiResponse<Void> trackView(
            @PathVariable UUID id,
            @Valid @RequestBody TrackViewRequest request,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserIdOrNull();
        viewService.trackView(id, userId, request, httpRequest);

        return ApiResponse.success(null, "View tracked successfully");
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get content statistics", description = "Get view statistics for content (admin)")
    public ApiResponse<ContentViewService.ContentViewStats> getContentStats(@PathVariable UUID id) {
        ContentViewService.ContentViewStats stats = viewService.getContentViewStats(id);
        return ApiResponse.success(stats);
    }

    // ==================== Helper Methods ====================

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID) {
            return (UUID) auth.getPrincipal();
        }
        // For development/testing, return a default UUID
        // In production, this should throw an exception
        return UUID.randomUUID();
    }

    private UUID getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
