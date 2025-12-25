package com.neobrutalism.crm.domain.role.dto;

import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.model.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role response")
public class RoleResponse {

    @Schema(description = "Role ID")
    private UUID id;

    @Schema(description = "Role code")
    private String code;

    @Schema(description = "Role name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Is system role")
    private Boolean isSystem;

    @Schema(description = "Priority")
    private Integer priority;

    @Schema(description = "Status")
    private RoleStatus status;

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

    public static RoleResponse from(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .organizationId(role.getOrganizationId())
                .isSystem(role.getIsSystem())
                .priority(role.getPriority())
                .status(role.getStatus())
                .deleted(role.getDeleted())
                .createdAt(role.getCreatedAt())
                .createdBy(role.getCreatedBy())
                .updatedAt(role.getUpdatedAt())
                .updatedBy(role.getUpdatedBy())
                .build();
    }
}
