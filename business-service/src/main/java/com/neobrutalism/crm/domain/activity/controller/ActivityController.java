package com.neobrutalism.crm.domain.activity.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.security.UserPrincipal;
import com.neobrutalism.crm.domain.activity.dto.ActivityRequest;
import com.neobrutalism.crm.domain.activity.dto.ActivityResponse;
import com.neobrutalism.crm.domain.activity.model.Activity;
import com.neobrutalism.crm.domain.activity.model.ActivityStatus;
import com.neobrutalism.crm.domain.activity.model.ActivityType;
import com.neobrutalism.crm.domain.activity.service.ActivityService;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Activity management
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activities", description = "Activity management APIs")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "Get all activities", description = "Retrieve all activities with pagination")
    public ApiResponse<PageResponse<ActivityResponse>> getAllActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scheduledStartAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Activity> activityPage = activityService.findAll(pageable);
        Page<ActivityResponse> responsePage = activityPage.map(ActivityResponse::fromEntity);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID", description = "Retrieve a specific activity by its ID")
    public ApiResponse<ActivityResponse> getActivityById(@PathVariable UUID id) {
        Activity activity = activityService.findById(id);
        return ApiResponse.success(ActivityResponse.fromEntity(activity));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new activity", description = "Create a new activity")
    public ApiResponse<ActivityResponse> createActivity(
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Activity activity = activityService.create(request, principal.getUsername());
        return ApiResponse.success("Activity created successfully", ActivityResponse.fromEntity(activity));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update activity", description = "Update an existing activity")
    public ApiResponse<ActivityResponse> updateActivity(
            @PathVariable UUID id,
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Activity activity = activityService.update(id, request, principal.getUsername());
        return ApiResponse.success("Activity updated successfully", ActivityResponse.fromEntity(activity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete activity", description = "Soft delete an activity")
    public ApiResponse<Void> deleteActivity(@PathVariable UUID id) {
        activityService.deleteById(id);
        return ApiResponse.success("Activity deleted successfully");
    }

    // State transition endpoints

    @PostMapping("/{id}/start")
    @Operation(summary = "Start activity", description = "Start a planned activity")
    public ApiResponse<ActivityResponse> startActivity(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Activity activity = activityService.start(id, principal.getUsername());
        return ApiResponse.success("Activity started", ActivityResponse.fromEntity(activity));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete activity", description = "Complete an activity")
    public ApiResponse<ActivityResponse> completeActivity(
            @PathVariable UUID id,
            @RequestParam String outcome,
            @AuthenticationPrincipal UserPrincipal principal) {

        Activity activity = activityService.complete(id, outcome, principal.getUsername());
        return ApiResponse.success("Activity completed", ActivityResponse.fromEntity(activity));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel activity", description = "Cancel an activity")
    public ApiResponse<ActivityResponse> cancelActivity(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserPrincipal principal) {

        Activity activity = activityService.cancel(id, reason, principal.getUsername());
        return ApiResponse.success("Activity cancelled", ActivityResponse.fromEntity(activity));
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule activity", description = "Reschedule an activity")
    public ApiResponse<ActivityResponse> rescheduleActivity(
            @PathVariable UUID id,
            @RequestParam Instant newStartAt,
            @RequestParam Instant newEndAt,
            @RequestParam String reason,
            @AuthenticationPrincipal UserPrincipal principal) {

        Activity activity = activityService.reschedule(id, newStartAt, newEndAt, reason, principal.getUsername());
        return ApiResponse.success("Activity rescheduled", ActivityResponse.fromEntity(activity));
    }

    // Query endpoints

    @GetMapping("/my-activities")
    @Operation(summary = "Get my activities", description = "Get activities owned by current user")
    public ApiResponse<List<ActivityResponse>> getMyActivities(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Activity> activities = activityService.findByOwner(principal.getId());
        List<ActivityResponse> responses = activities.stream()
                .map(ActivityResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming activities", description = "Get upcoming activities for user")
    public ApiResponse<List<ActivityResponse>> getUpcomingActivities(
            @RequestParam(defaultValue = "7") int daysAhead,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Activity> activities = activityService.findUpcomingActivities(principal.getId(), daysAhead);
        List<ActivityResponse> responses = activities.stream()
                .map(ActivityResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue activities", description = "Get all overdue activities")
    public ApiResponse<List<ActivityResponse>> getOverdueActivities() {
        List<Activity> activities = activityService.findOverdueActivities();
        List<ActivityResponse> responses = activities.stream()
                .map(ActivityResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/by-status")
    @Operation(summary = "Get activities by status", description = "Filter activities by status")
    public ApiResponse<List<ActivityResponse>> getActivitiesByStatus(
            @RequestParam ActivityStatus status) {

        List<Activity> activities = activityService.findByStatus(status);
        List<ActivityResponse> responses = activities.stream()
                .map(ActivityResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/by-type")
    @Operation(summary = "Get activities by type", description = "Filter activities by type")
    public ApiResponse<List<ActivityResponse>> getActivitiesByType(
            @RequestParam ActivityType type) {

        List<Activity> activities = activityService.findByType(type);
        List<ActivityResponse> responses = activities.stream()
                .map(ActivityResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/related")
    @Operation(summary = "Get related activities", description = "Get activities related to an entity")
    public ApiResponse<List<ActivityResponse>> getRelatedActivities(
            @RequestParam String relatedToType,
            @RequestParam UUID relatedToId) {

        List<Activity> activities = activityService.findByRelatedEntity(relatedToType, relatedToId);
        List<ActivityResponse> responses = activities.stream()
                .map(ActivityResponse::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }
}
