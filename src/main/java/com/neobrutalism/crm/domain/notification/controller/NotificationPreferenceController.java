package com.neobrutalism.crm.domain.notification.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.security.UserPrincipal;
import com.neobrutalism.crm.domain.notification.dto.NotificationPreferenceRequest;
import com.neobrutalism.crm.domain.notification.dto.NotificationPreferenceResponse;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import com.neobrutalism.crm.domain.notification.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Notification Preference management
 * Supports multi-channel notification preferences (in-app, email, SMS)
 * Optimized for high-scale operations with caching
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Manage notification channel preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping("/me")
    @Operation(summary = "Get my preferences", description = "Get all notification preferences for current user")
    public ApiResponse<List<NotificationPreferenceResponse>> getMyPreferences() {
        log.info("Fetching preferences for current user");

        List<NotificationPreferenceResponse> responses = preferenceService.getMyPreferences()
                .stream()
                .map(NotificationPreferenceResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/me/{type}")
    @Operation(summary = "Get preference by type", description = "Get specific notification preference by type")
    public ApiResponse<NotificationPreferenceResponse> getMyPreferenceByType(
            @PathVariable NotificationType type,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Fetching preference for user: {}, type: {}", userPrincipal.getId(), type);

        var preference = preferenceService.getPreference(
                userPrincipal.getId(),
                userPrincipal.getOrganizationId(),
                type
        );

        return ApiResponse.success(NotificationPreferenceResponse.from(preference));
    }

    @PutMapping("/me/{type}")
    @Operation(summary = "Update preference", description = "Update notification preference for specific type")
    public ApiResponse<NotificationPreferenceResponse> updateMyPreference(
            @PathVariable NotificationType type,
            @Valid @RequestBody NotificationPreferenceRequest request
    ) {
        log.info("Updating preference for type: {}", type);

        var preference = preferenceService.updateMyPreference(type, request);

        return ApiResponse.success(
                "Preference updated successfully",
                NotificationPreferenceResponse.from(preference)
        );
    }

    @PutMapping("/me/batch")
    @Operation(summary = "Batch update preferences", description = "Update multiple notification preferences at once")
    public ApiResponse<List<NotificationPreferenceResponse>> batchUpdatePreferences(
            @Valid @RequestBody List<NotificationPreferenceRequest> requests,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Batch updating {} preferences for user: {}", requests.size(), userPrincipal.getId());

        List<NotificationPreferenceResponse> responses = preferenceService.batchUpdatePreferences(
                        userPrincipal.getId(),
                        userPrincipal.getOrganizationId(),
                        requests
                )
                .stream()
                .map(NotificationPreferenceResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success("Preferences updated successfully", responses);
    }

    @DeleteMapping("/me/{type}")
    @Operation(summary = "Delete preference", description = "Delete notification preference (resets to default)")
    public ApiResponse<Void> deleteMyPreference(
            @PathVariable NotificationType type,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Deleting preference for user: {}, type: {}", userPrincipal.getId(), type);

        preferenceService.deletePreference(
                userPrincipal.getId(),
                userPrincipal.getOrganizationId(),
                type
        );

        return ApiResponse.success("Preference deleted successfully");
    }

    @PostMapping("/me/reset")
    @Operation(summary = "Reset to defaults", description = "Reset all preferences to default values")
    public ApiResponse<List<NotificationPreferenceResponse>> resetToDefaults(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Resetting preferences to defaults for user: {}", userPrincipal.getId());

        List<NotificationPreferenceResponse> responses = preferenceService.resetToDefaults(
                        userPrincipal.getId(),
                        userPrincipal.getOrganizationId()
                )
                .stream()
                .map(NotificationPreferenceResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success("Preferences reset to defaults", responses);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user preferences", description = "Get notification preferences for specific user (admin only)")
    public ApiResponse<List<NotificationPreferenceResponse>> getUserPreferences(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Fetching preferences for user: {}", userId);

        List<NotificationPreferenceResponse> responses = preferenceService.getUserPreferences(
                        userId,
                        userPrincipal.getOrganizationId()
                )
                .stream()
                .map(NotificationPreferenceResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @PutMapping("/user/{userId}/{type}")
    @Operation(summary = "Update user preference", description = "Update notification preference for specific user (admin only)")
    public ApiResponse<NotificationPreferenceResponse> updateUserPreference(
            @PathVariable UUID userId,
            @PathVariable NotificationType type,
            @Valid @RequestBody NotificationPreferenceRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Admin updating preference for user: {}, type: {}", userId, type);

        var preference = preferenceService.updatePreference(
                userId,
                userPrincipal.getOrganizationId(),
                type,
                request
        );

        return ApiResponse.success(
                "Preference updated successfully",
                NotificationPreferenceResponse.from(preference)
        );
    }
}
