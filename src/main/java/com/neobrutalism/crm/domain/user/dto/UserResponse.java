package com.neobrutalism.crm.domain.user.dto;

import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * User response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User response")
public class UserResponse {

    @Schema(description = "User ID (UUID v7)", example = "018d3f5c-7b44-7a90-a123-456789abcdef")
    private UUID id;

    @Schema(description = "Username", example = "john.doe")
    private String username;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Current status")
    private UserStatus status;

    @Schema(description = "Last login timestamp")
    private Instant lastLoginAt;

    @Schema(description = "Last login IP address")
    private String lastLoginIp;

    @Schema(description = "Is deleted")
    private Boolean deleted;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Last updated timestamp")
    private Instant updatedAt;

    @Schema(description = "Last updated by")
    private String updatedBy;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .organizationId(user.getOrganizationId())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .deleted(user.getDeleted())
                .createdAt(user.getCreatedAt())
                .createdBy(user.getCreatedBy())
                .updatedAt(user.getUpdatedAt())
                .updatedBy(user.getUpdatedBy())
                .build();
    }
}
