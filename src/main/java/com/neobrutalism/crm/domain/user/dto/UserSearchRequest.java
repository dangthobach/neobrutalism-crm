package com.neobrutalism.crm.domain.user.dto;

import com.neobrutalism.crm.domain.user.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for User search/filter requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User search and filter criteria")
public class UserSearchRequest {

    @Schema(description = "Search keyword (searches in username, email, firstName, lastName)", example = "john")
    private String keyword;

    @Schema(description = "Filter by username", example = "john.doe")
    private String username;

    @Schema(description = "Filter by email", example = "john@example.com")
    private String email;

    @Schema(description = "Filter by first name", example = "John")
    private String firstName;

    @Schema(description = "Filter by last name", example = "Doe")
    private String lastName;

    @Schema(description = "Filter by organization ID")
    private UUID organizationId;

    @Schema(description = "Filter by status", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "Filter by tenant ID")
    private String tenantId;

    @Schema(description = "Include deleted users", example = "false")
    private Boolean includeDeleted;

    @Builder.Default
    @Schema(description = "Page number (0-indexed)", example = "0")
    private Integer page = 0;

    @Builder.Default
    @Schema(description = "Page size", example = "20")
    private Integer size = 20;

    @Builder.Default
    @Schema(description = "Sort field", example = "createdAt")
    private String sortBy = "createdAt";

    @Builder.Default
    @Schema(description = "Sort direction (ASC or DESC)", example = "DESC")
    private String sortDirection = "DESC";
}
