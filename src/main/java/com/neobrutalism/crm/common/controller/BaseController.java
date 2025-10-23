package com.neobrutalism.crm.common.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.entity.BaseEntity;
import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.common.util.SortValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Base controller providing common CRUD operations
 * Reduces boilerplate for entity controllers
 */
public abstract class BaseController<Entity extends BaseEntity, CreateRequest, UpdateRequest, Response> {

    protected final BaseService<Entity> service;
    protected final Function<Entity, Response> toResponseMapper;
    protected final Set<String> allowedSortFields;

    protected BaseController(BaseService<Entity> service, 
                           Function<Entity, Response> toResponseMapper,
                           Set<String> allowedSortFields) {
        this.service = service;
        this.toResponseMapper = toResponseMapper;
        this.allowedSortFields = allowedSortFields;
    }

    @GetMapping
    public ApiResponse<PageResponse<Response>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        String validatedSortBy = SortValidator.validateSortField(sortBy, allowedSortFields, "id");
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

        Page<Entity> entityPage = service.findAll(pageable);
        Page<Response> responsePage = entityPage.map(toResponseMapper);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    public ApiResponse<Response> getById(@PathVariable UUID id) {
        Entity entity = service.findById(id);
        return ApiResponse.success(toResponseMapper.apply(entity));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Response> create(@RequestBody CreateRequest request) {
        Entity entity = mapFromCreateRequest(request);
        Entity created = service.create(entity);
        return ApiResponse.success(buildSuccessMessage("created"), toResponseMapper.apply(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<Response> update(@PathVariable UUID id, @RequestBody UpdateRequest request) {
        Entity entity = mapFromUpdateRequest(request);
        Entity updated = service.update(id, entity);
        return ApiResponse.success(buildSuccessMessage("updated"), toResponseMapper.apply(updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ApiResponse.success(buildSuccessMessage("deleted"));
    }

    // Abstract methods to be implemented by subclasses
    protected abstract Entity mapFromCreateRequest(CreateRequest request);
    protected abstract Entity mapFromUpdateRequest(UpdateRequest request);

    // Helper method for common success messages
    protected String buildSuccessMessage(String action) {
        return getEntityName() + " " + action + " successfully";
    }

    protected abstract String getEntityName();
}
