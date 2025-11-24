package com.neobrutalism.crm.domain.task.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.security.UserPrincipal;
import com.neobrutalism.crm.domain.task.dto.BulkAssignRequest;
import com.neobrutalism.crm.domain.task.dto.BulkOperationResponse;
import com.neobrutalism.crm.domain.task.dto.BulkStatusChangeRequest;
import com.neobrutalism.crm.domain.task.dto.TaskActivityResponse;
import com.neobrutalism.crm.domain.task.dto.TaskRequest;
import com.neobrutalism.crm.domain.task.dto.TaskResponse;
import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.model.TaskStatus;
import com.neobrutalism.crm.domain.task.service.TaskActivityService;
import com.neobrutalism.crm.domain.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Task management
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management APIs")
public class TaskController {

    private final TaskService taskService;
    private final TaskActivityService taskActivityService;

    @GetMapping
    @Operation(summary = "Get all tasks", description = "Retrieve all tasks with pagination")
    public ApiResponse<PageResponse<TaskResponse>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Task> taskPage = taskService.findAll(pageable);
        Page<TaskResponse> responsePage = taskPage.map(TaskResponse::fromEntity);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Retrieve a specific task by its ID")
    public ApiResponse<TaskResponse> getTaskById(@PathVariable UUID id) {
        Task task = taskService.findById(id);
        return ApiResponse.success(TaskResponse.fromEntity(task));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new task", description = "Create a new task")
    public ApiResponse<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = taskService.create(request, principal.getUsername());
        return ApiResponse.success("Task created successfully", TaskResponse.fromEntity(task));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Update an existing task")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = taskService.update(id, request, principal.getUsername());
        return ApiResponse.success("Task updated successfully", TaskResponse.fromEntity(task));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task", description = "Soft delete a task")
    public ApiResponse<Void> deleteTask(@PathVariable UUID id) {
        taskService.deleteById(id);
        return ApiResponse.success("Task deleted successfully");
    }

    // State transition endpoints

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign task", description = "Assign task to a user")
    public ApiResponse<TaskResponse> assignTask(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = taskService.assignTo(id, userId, principal.getUsername());
        return ApiResponse.success("Task assigned successfully", TaskResponse.fromEntity(task));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start task", description = "Start working on a task")
    public ApiResponse<TaskResponse> startTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = taskService.start(id, principal.getUsername());
        return ApiResponse.success("Task started", TaskResponse.fromEntity(task));
    }

    @PostMapping("/{id}/submit-for-review")
    @Operation(summary = "Submit task for review", description = "Submit task for review")
    public ApiResponse<TaskResponse> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = taskService.submitForReview(id, principal.getUsername());
        return ApiResponse.success("Task submitted for review", TaskResponse.fromEntity(task));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete task", description = "Complete a task")
    public ApiResponse<TaskResponse> completeTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = taskService.complete(id, principal.getUsername());
        return ApiResponse.success("Task completed", TaskResponse.fromEntity(task));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel task", description = "Cancel a task")
    public ApiResponse<TaskResponse> cancelTask(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = taskService.cancel(id, reason, principal.getUsername());
        return ApiResponse.success("Task cancelled", TaskResponse.fromEntity(task));
    }

    @PutMapping("/{id}/progress")
    @Operation(summary = "Update task progress", description = "Update task progress percentage")
    public ApiResponse<TaskResponse> updateProgress(
            @PathVariable UUID id,
            @RequestParam int percentage,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = taskService.updateProgress(id, percentage, principal.getUsername());
        return ApiResponse.success("Task progress updated", TaskResponse.fromEntity(task));
    }

    // Query endpoints

    @GetMapping("/my-tasks")
    @Operation(summary = "Get my tasks", description = "Get tasks assigned to current user")
    public ApiResponse<List<TaskResponse>> getMyTasks(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Task> tasks = taskService.getTasksAssignedTo(principal.getId());
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/assigned-by-me")
    @Operation(summary = "Get tasks assigned by me", description = "Get tasks assigned by current user")
    public ApiResponse<List<TaskResponse>> getTasksAssignedByMe(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Task> tasks = taskService.getTasksAssignedBy(principal.getId());
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/by-status")
    @Operation(summary = "Get tasks by status", description = "Filter tasks by status")
    public ApiResponse<List<TaskResponse>> getTasksByStatus(
            @RequestParam TaskStatus status) {

        List<Task> tasks = taskService.getTasksByStatus(status);
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/my-tasks/by-status")
    @Operation(summary = "Get my tasks by status", description = "Get current user's tasks filtered by status")
    public ApiResponse<List<TaskResponse>> getMyTasksByStatus(
            @RequestParam TaskStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Task> tasks = taskService.getTasksByUserAndStatus(principal.getId(), status);
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue tasks", description = "Get all overdue tasks")
    public ApiResponse<List<TaskResponse>> getOverdueTasks() {
        List<Task> tasks = taskService.getOverdueTasks();
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming tasks", description = "Get upcoming tasks for user")
    public ApiResponse<List<TaskResponse>> getUpcomingTasks(
            @RequestParam(defaultValue = "7") int daysAhead,
            @AuthenticationPrincipal UserPrincipal principal) {

        Instant startDate = Instant.now();
        Instant endDate = startDate.plus(daysAhead, ChronoUnit.DAYS);

        List<Task> tasks = taskService.getUpcomingTasks(principal.getId(), startDate, endDate);
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/related")
    @Operation(summary = "Get related tasks", description = "Get tasks related to an entity")
    public ApiResponse<List<TaskResponse>> getRelatedTasks(
            @RequestParam String relatedToType,
            @RequestParam UUID relatedToId) {

        List<Task> tasks = taskService.getRelatedTasks(relatedToType, relatedToId);
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/count")
    @Operation(summary = "Count tasks", description = "Count tasks by user and status")
    public ApiResponse<Long> countTasks(
            @RequestParam TaskStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long count = taskService.countTasksByUserAndStatus(principal.getId(), status);
        return ApiResponse.success(count);
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get tasks by organization", description = "Get all tasks for an organization")
    public ApiResponse<List<TaskResponse>> getTasksByOrganization(
            @PathVariable UUID organizationId) {

        List<Task> tasks = taskService.getTasksByOrganization(organizationId);
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get tasks by branch", description = "Get all tasks for a branch")
    public ApiResponse<List<TaskResponse>> getTasksByBranch(
            @PathVariable UUID branchId) {

        List<Task> tasks = taskService.getTasksByBranch(branchId);
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    // Bulk operation endpoints

    @PostMapping("/bulk/assign")
    @Operation(summary = "Bulk assign tasks", description = "Assign multiple tasks to a user")
    public ApiResponse<BulkOperationResponse> bulkAssignTasks(
            @Valid @RequestBody BulkAssignRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        BulkOperationResponse response = taskService.bulkAssign(
                request.getTaskIds(),
                request.getAssigneeId(),
                principal.getUsername());

        return ApiResponse.success("Bulk assign completed", response);
    }

    @PostMapping("/bulk/status")
    @Operation(summary = "Bulk change status", description = "Change status for multiple tasks")
    public ApiResponse<BulkOperationResponse> bulkChangeStatus(
            @Valid @RequestBody BulkStatusChangeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        BulkOperationResponse response = taskService.bulkStatusChange(
                request.getTaskIds(),
                request.getStatus(),
                principal.getUsername());

        return ApiResponse.success("Bulk status change completed", response);
    }

    @DeleteMapping("/bulk")
    @Operation(summary = "Bulk delete tasks", description = "Delete multiple tasks (soft delete)")
    public ApiResponse<BulkOperationResponse> bulkDeleteTasks(
            @RequestBody List<UUID> taskIds,
            @AuthenticationPrincipal UserPrincipal principal) {

        BulkOperationResponse response = taskService.bulkDelete(
                taskIds,
                principal.getUsername());

        return ApiResponse.success("Bulk delete completed", response);
    }

    // Activity timeline endpoints

    @GetMapping("/{taskId}/activities")
    @Operation(summary = "Get task activities", description = "Get activity timeline for a task")
    public ApiResponse<List<TaskActivityResponse>> getTaskActivities(
            @PathVariable UUID taskId) {

        List<TaskActivityResponse> activities = taskActivityService.getTaskActivities(taskId);
        return ApiResponse.success(activities);
    }

    @GetMapping("/{taskId}/activities/paginated")
    @Operation(summary = "Get task activities (paginated)", description = "Get activity timeline with pagination")
    public ApiResponse<PageResponse<TaskActivityResponse>> getTaskActivitiesPaginated(
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TaskActivityResponse> activities = taskActivityService.getTaskActivities(taskId, pageable);
        return ApiResponse.success(PageResponse.from(activities));
    }

    @GetMapping("/{taskId}/activities/count")
    @Operation(summary = "Count task activities", description = "Get total number of activities for a task")
    public ApiResponse<Long> countTaskActivities(
            @PathVariable UUID taskId) {

        long count = taskActivityService.countActivities(taskId);
        return ApiResponse.success(count);
    }
}
