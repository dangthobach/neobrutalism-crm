package com.neobrutalism.crm.domain.content.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.content.dto.ContentSeriesDTO;
import com.neobrutalism.crm.domain.content.dto.SeriesRequest;
import com.neobrutalism.crm.domain.content.mapper.ContentMapper;
import com.neobrutalism.crm.domain.content.model.ContentSeries;
import com.neobrutalism.crm.domain.content.service.ContentSeriesService;
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
 * REST controller for Content Series management
 */
@RestController
@RequestMapping("/api/content-series")
@RequiredArgsConstructor
@Tag(name = "Content Series", description = "Content series management APIs")
public class ContentSeriesController {

    private final ContentSeriesService seriesService;
    private final ContentMapper contentMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create series", description = "Create new content series")
    public ApiResponse<ContentSeriesDTO> createSeries(
            @Valid @RequestBody SeriesRequest request,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        ContentSeries series = seriesService.createSeries(request, tenantId);
        return ApiResponse.success("Series created successfully", contentMapper.toSeriesDTO(series));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update series", description = "Update existing content series")
    public ApiResponse<ContentSeriesDTO> updateSeries(
            @PathVariable UUID id,
            @Valid @RequestBody SeriesRequest request) {

        ContentSeries series = seriesService.updateSeries(id, request);
        return ApiResponse.success("Series updated successfully", contentMapper.toSeriesDTO(series));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get series by ID", description = "Retrieve a specific series by its ID")
    public ApiResponse<ContentSeriesDTO> getSeriesById(@PathVariable UUID id) {
        ContentSeries series = seriesService.findById(id);
        return ApiResponse.success("Series retrieved successfully", contentMapper.toSeriesDTO(series));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get series by slug", description = "Retrieve a specific series by its slug")
    public ApiResponse<ContentSeriesDTO> getSeriesBySlug(
            @PathVariable String slug,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        ContentSeries series = seriesService.findBySlug(slug, tenantId);
        return ApiResponse.success("Series retrieved successfully", contentMapper.toSeriesDTO(series));
    }

    @GetMapping
    @Operation(summary = "Get all series", description = "Retrieve all series for tenant, ordered by sort order")
    public ApiResponse<List<ContentSeriesDTO>> getAllSeries(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        List<ContentSeries> seriesList = seriesService.findAllByTenant(tenantId);
        List<ContentSeriesDTO> dtos = seriesList.stream()
            .map(contentMapper::toSeriesDTO)
            .collect(Collectors.toList());

        return ApiResponse.success("Series retrieved successfully", dtos);
    }

    @GetMapping("/with-count")
    @Operation(summary = "Get series with content count", description = "Get all series with their content count")
    public ApiResponse<List<Object[]>> getSeriesWithCount(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        List<Object[]> result = seriesService.findAllWithContentCount(tenantId);
        return ApiResponse.success("Series with count retrieved successfully", result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete series", description = "Soft delete a content series")
    public ApiResponse<Void> deleteSeries(@PathVariable UUID id) {
        seriesService.deleteById(id);
        return ApiResponse.success("Series deleted successfully", null);
    }
}
