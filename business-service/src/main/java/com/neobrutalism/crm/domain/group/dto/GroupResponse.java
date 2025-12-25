package com.neobrutalism.crm.domain.group.dto;

import com.neobrutalism.crm.domain.group.model.Group;
import com.neobrutalism.crm.domain.group.model.GroupStatus;
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
@Schema(description = "Group response")
public class GroupResponse {

    @Schema(description = "Group ID")
    private UUID id;

    @Schema(description = "Group code")
    private String code;

    @Schema(description = "Group name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Parent group ID")
    private UUID parentId;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Hierarchy level")
    private Integer level;

    @Schema(description = "Materialized path")
    private String path;

    @Schema(description = "Status")
    private GroupStatus status;

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

    public static GroupResponse from(Group group) {
        return GroupResponse.builder()
                .id(group.getId())
                .code(group.getCode())
                .name(group.getName())
                .description(group.getDescription())
                .parentId(group.getParentId())
                .organizationId(group.getOrganizationId())
                .level(group.getLevel())
                .path(group.getPath())
                .status(group.getStatus())
                .deleted(group.getDeleted())
                .createdAt(group.getCreatedAt())
                .createdBy(group.getCreatedBy())
                .updatedAt(group.getUpdatedAt())
                .updatedBy(group.getUpdatedBy())
                .build();
    }
}
