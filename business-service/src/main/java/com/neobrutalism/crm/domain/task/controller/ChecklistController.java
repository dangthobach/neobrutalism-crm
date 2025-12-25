package com.neobrutalism.crm.domain.task.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.task.dto.ChecklistItemRequest;
import com.neobrutalism.crm.domain.task.dto.ChecklistItemResponse;
import com.neobrutalism.crm.domain.task.dto.ChecklistReorderRequest;
import com.neobrutalism.crm.domain.task.model.ChecklistItem;
import com.neobrutalism.crm.domain.task.service.ChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for task checklist items
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Checklist", description = "Task checklist management APIs")
public class ChecklistController {

    private final ChecklistService checklistService;

    @PostMapping("/{taskId}/checklist")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add checklist item", description = "Add a new item to task checklist")
    public ApiResponse<ChecklistItemResponse> addItem(
            @PathVariable UUID taskId,
            @Valid @RequestBody ChecklistItemRequest request
    ) {
        log.info("Add checklist item to task: {}", taskId);

        ChecklistItem item = checklistService.addItem(taskId, request.getTitle());

        return ApiResponse.success("Checklist item added successfully", ChecklistItemResponse.from(item));
    }

    @GetMapping("/{taskId}/checklist")
    @Operation(summary = "Get checklist items", description = "Get all checklist items for a task")
    public ApiResponse<List<ChecklistItemResponse>> getItems(@PathVariable UUID taskId) {
        List<ChecklistItem> items = checklistService.getItems(taskId);
        List<ChecklistItemResponse> responses = items.stream()
                .map(ChecklistItemResponse::from)
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }

    @GetMapping("/{taskId}/checklist/progress")
    @Operation(summary = "Get checklist progress", description = "Get completion progress for task checklist")
    public ApiResponse<Map<String, Object>> getProgress(@PathVariable UUID taskId) {
        Map<String, Object> progress = checklistService.calculateProgress(taskId);
        return ApiResponse.success(progress);
    }

    @PutMapping("/checklist/{itemId}")
    @Operation(summary = "Update checklist item", description = "Update title or completion status")
    public ApiResponse<ChecklistItemResponse> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody ChecklistItemRequest request
    ) {
        log.info("Update checklist item: {}", itemId);

        ChecklistItem item = checklistService.updateItem(
            itemId,
            request.getTitle(),
            request.getCompleted()
        );

        return ApiResponse.success("Checklist item updated successfully", ChecklistItemResponse.from(item));
    }

    @PutMapping("/checklist/{itemId}/toggle")
    @Operation(summary = "Toggle checklist item", description = "Toggle completion status")
    public ApiResponse<ChecklistItemResponse> toggleItem(@PathVariable UUID itemId) {
        log.info("Toggle checklist item: {}", itemId);

        ChecklistItem item = checklistService.toggleItem(itemId);

        return ApiResponse.success("Checklist item toggled", ChecklistItemResponse.from(item));
    }

    @PutMapping("/{taskId}/checklist/reorder")
    @Operation(summary = "Reorder checklist items", description = "Update order of checklist items")
    public ApiResponse<List<ChecklistItemResponse>> reorderItems(
            @PathVariable UUID taskId,
            @Valid @RequestBody ChecklistReorderRequest request
    ) {
        log.info("Reorder checklist items for task: {}", taskId);

        List<ChecklistItem> items = checklistService.reorderItems(taskId, request.getItemIds());
        List<ChecklistItemResponse> responses = items.stream()
                .map(ChecklistItemResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success("Checklist items reordered successfully", responses);
    }

    @DeleteMapping("/checklist/{itemId}")
    @Operation(summary = "Delete checklist item", description = "Soft delete a checklist item")
    public ApiResponse<Void> deleteItem(@PathVariable UUID itemId) {
        log.info("Delete checklist item: {}", itemId);

        checklistService.deleteItem(itemId);

        return ApiResponse.success("Checklist item deleted successfully");
    }
}
