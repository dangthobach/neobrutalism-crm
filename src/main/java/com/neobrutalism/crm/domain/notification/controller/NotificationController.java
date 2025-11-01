package com.neobrutalism.crm.domain.notification.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.security.UserPrincipal;
import com.neobrutalism.crm.domain.notification.dto.NotificationRequest;
import com.neobrutalism.crm.domain.notification.dto.NotificationResponse;
import com.neobrutalism.crm.domain.notification.model.Notification;
import com.neobrutalism.crm.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Notification management
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create notification", description = "Create and send a new notification")
    public ApiResponse<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationRequest request
    ) {
        log.info("Create notification request for user: {}", request.getRecipientId());

        Notification notification = notificationService.createNotification(
                request.getRecipientId(),
                request.getTitle(),
                request.getMessage(),
                request.getNotificationType(),
                request.getPriority(),
                request.getActionUrl(),
                request.getEntityType(),
                request.getEntityId()
        );

        return ApiResponse.success("Notification created successfully", NotificationResponse.from(notification));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Retrieve notification by ID")
    public ApiResponse<NotificationResponse> getNotification(@PathVariable UUID id) {
        Notification notification = notificationService.findById(id);
        return ApiResponse.success(NotificationResponse.from(notification));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my notifications", description = "Get all notifications for current user")
    public ApiResponse<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<Notification> notifications = notificationService.findByRecipient(userPrincipal.getId());
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/me/paginated")
    @Operation(summary = "Get my notifications (paginated)", description = "Get notifications with pagination")
    public ApiResponse<Page<NotificationResponse>> getMyNotificationsPaginated(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationService.findByRecipient(userPrincipal.getId(), pageable);
        Page<NotificationResponse> responsePage = notificationPage.map(NotificationResponse::from);
        return ApiResponse.success(responsePage);
    }

    @GetMapping("/me/unread")
    @Operation(summary = "Get unread notifications", description = "Get all unread notifications for current user")
    public ApiResponse<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<Notification> notifications = notificationService.findUnreadNotifications(userPrincipal.getId());
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/me/unread/count")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ApiResponse<Long> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long count = notificationService.countUnreadNotifications(userPrincipal.getId());
        return ApiResponse.success(count);
    }

    @GetMapping("/me/priority")
    @Operation(summary = "Get high priority notifications", description = "Get high priority notifications for current user")
    public ApiResponse<List<NotificationResponse>> getHighPriorityNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<Notification> notifications = notificationService.findHighPriorityNotifications(userPrincipal.getId());
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/me/recent")
    @Operation(summary = "Get recent notifications", description = "Get recent notifications (last 7 days)")
    public ApiResponse<List<NotificationResponse>> getRecentNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<Notification> notifications = notificationService.findRecentNotifications(userPrincipal.getId());
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark as read", description = "Mark notification as read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable UUID id) {
        Notification notification = notificationService.markAsRead(id);
        return ApiResponse.success("Notification marked as read", NotificationResponse.from(notification));
    }

    @PutMapping("/me/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    public ApiResponse<Integer> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        int count = notificationService.markAllAsRead(userPrincipal.getId());
        return ApiResponse.success("All notifications marked as read", count);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification", description = "Delete notification (soft delete)")
    public ApiResponse<Void> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
        return ApiResponse.success("Notification deleted successfully");
    }

    @DeleteMapping("/me/all")
    @Operation(summary = "Delete all notifications", description = "Delete all notifications for current user")
    public ApiResponse<Integer> deleteAllNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        int count = notificationService.deleteAllByRecipient(userPrincipal.getId());
        return ApiResponse.success("All notifications deleted", count);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get notifications by user", description = "Get all notifications for specific user (admin only)")
    public ApiResponse<List<NotificationResponse>> getNotificationsByUser(@PathVariable UUID userId) {
        List<Notification> notifications = notificationService.findByRecipient(userId);
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }
}
