package com.neobrutalism.crm.domain.user.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.util.SortValidator;
import com.neobrutalism.crm.domain.menu.dto.UserMenuResponse;
import com.neobrutalism.crm.domain.menu.service.MenuRenderingService;
import com.neobrutalism.crm.domain.user.dto.*;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import com.neobrutalism.crm.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for User management
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final MenuRenderingService menuRenderingService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve all active users with pagination")
    public ApiResponse<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        String validatedSortBy = SortValidator.validateUserSortField(sortBy);
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

        Page<User> userPage = userService.findAllActive(pageable);
        Page<UserResponse> responsePage = userPage.map(UserResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by its ID")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID id) {
        User user = userService.findById(id);
        return ApiResponse.success(UserResponse.from(user));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Retrieve a specific user by its unique username")
    public ApiResponse<UserResponse> getUserByUsername(@PathVariable String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.forResourceByField("User", "username", username));
        return ApiResponse.success(UserResponse.from(user));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", description = "Retrieve a specific user by its email address")
    public ApiResponse<UserResponse> getUserByEmail(@PathVariable String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> ResourceNotFoundException.forResourceByField("User", "email", email));
        return ApiResponse.success(UserResponse.from(user));
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get users by organization", description = "Retrieve all users belonging to a specific organization")
    public ApiResponse<List<UserResponse>> getUsersByOrganization(@PathVariable UUID organizationId) {
        List<User> users = userService.findByOrganizationId(organizationId);
        List<UserResponse> responses = users.stream()
                .map(UserResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user", description = "Create a new user account")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        // Get organization ID from current logged-in user
        User currentUser = userService.getCurrentUserEntity();
        UUID organizationId = currentUser.getOrganizationId();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());
        user.setOrganizationId(organizationId);

        // Set tenant ID: use organization ID if provided, otherwise use "default"
        String tenantId = organizationId != null
            ? organizationId.toString()
            : "default";
        user.setTenantId(tenantId);

        User created = userService.create(user);
        return ApiResponse.success("User created successfully", UserResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest request) {

        User user = userService.findById(id);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // Only update password if provided and different
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());
        // organizationId is not updated - it remains the same as the existing user

        User updated = userService.update(id, user);
        return ApiResponse.success("User updated successfully", UserResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Soft delete a user")
    public ApiResponse<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteById(id);
        return ApiResponse.success("User deleted successfully");
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate user", description = "Change user status to ACTIVE")
    public ApiResponse<UserResponse> activateUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        User activated = userService.activate(id, reason);
        return ApiResponse.success("User activated successfully", UserResponse.from(activated));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend user", description = "Change user status to SUSPENDED")
    public ApiResponse<UserResponse> suspendUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        User suspended = userService.suspend(id, reason);
        return ApiResponse.success("User suspended successfully", UserResponse.from(suspended));
    }

    @PostMapping("/{id}/lock")
    @Operation(summary = "Lock user", description = "Change user status to LOCKED")
    public ApiResponse<UserResponse> lockUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        User locked = userService.lock(id, reason);
        return ApiResponse.success("User locked successfully", UserResponse.from(locked));
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "Unlock user", description = "Unlock a locked user and reset failed login attempts")
    public ApiResponse<UserResponse> unlockUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        User unlocked = userService.unlock(id, reason);
        return ApiResponse.success("User unlocked successfully", UserResponse.from(unlocked));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get users by status", description = "Retrieve all users with a specific status")
    public ApiResponse<List<UserResponse>> getUsersByStatus(@PathVariable UserStatus status) {
        List<User> users = userService.findByStatus(status);
        List<UserResponse> responses = users.stream()
                .map(UserResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/check-username/{username}")
    @Operation(summary = "Check username availability", description = "Check if a username is already taken")
    public ApiResponse<Boolean> checkUsername(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ApiResponse.success(!exists); // Return true if available (not exists)
    }

    @GetMapping("/check-email/{email}")
    @Operation(summary = "Check email availability", description = "Check if an email is already registered")
    public ApiResponse<Boolean> checkEmail(@PathVariable String email) {
        boolean exists = userService.existsByEmail(email);
        return ApiResponse.success(!exists); // Return true if available (not exists)
    }

    @PostMapping("/search")
    @Operation(summary = "Search users", description = "Search and filter users with advanced criteria")
    public ApiResponse<PageResponse<UserResponse>> searchUsers(@Valid @RequestBody UserSearchRequest request) {
        String validatedSortBy = SortValidator.validateUserSortField(request.getSortBy());
        Sort.Direction direction = Sort.Direction.fromString(request.getSortDirection());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(direction, validatedSortBy));

        Page<User> userPage = userService.search(request, pageable);
        Page<UserResponse> responsePage = userPage.map(UserResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore deleted user", description = "Restore a soft-deleted user")
    public ApiResponse<UserResponse> restoreUser(@PathVariable UUID id) {
        User restored = userService.restore(id);
        return ApiResponse.success("User restored successfully", UserResponse.from(restored));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Get the profile of the currently authenticated user")
    public ApiResponse<UserResponse> getCurrentUserProfile() {
        User user = userService.getCurrentUserEntity();
        return ApiResponse.success(UserResponse.from(user));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update current user profile", description = "Update the profile of the currently authenticated user")
    public ApiResponse<UserResponse> updateCurrentUserProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        User user = userService.getCurrentUserEntity();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());

        User updated = userService.update(user.getId(), user);
        return ApiResponse.success("Profile updated successfully", UserResponse.from(updated));
    }

    @GetMapping("/me/menus")
    @Operation(summary = "Get current user's menus", description = "Get the menu tree with permissions for the currently authenticated user")
    public ApiResponse<List<UserMenuResponse>> getCurrentUserMenus() {
        User user = userService.getCurrentUserEntity();
        List<UserMenuResponse> menus = menuRenderingService.getUserMenuTree(user.getId());
        return ApiResponse.success(menus);
    }

    @GetMapping("/{id}/menus")
    @Operation(summary = "Get user's menus", description = "Get the menu tree with permissions for a specific user")
    public ApiResponse<List<UserMenuResponse>> getUserMenus(@PathVariable UUID id) {
        List<UserMenuResponse> menus = menuRenderingService.getUserMenuTree(id);
        return ApiResponse.success(menus);
    }
}
