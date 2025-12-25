package com.neobrutalism.crm.domain.grouprole.dto;

import com.neobrutalism.crm.domain.grouprole.model.GroupRole;
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
@Schema(description = "Group role assignment response")
public class GroupRoleResponse {

    @Schema(description = "Group role ID")
    private UUID id;

    @Schema(description = "Group ID")
    private UUID groupId;

    @Schema(description = "Role ID")
    private UUID roleId;

    @Schema(description = "Granted timestamp")
    private Instant grantedAt;

    @Schema(description = "Granted by")
    private String grantedBy;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    public static GroupRoleResponse from(GroupRole groupRole) {
        return GroupRoleResponse.builder()
                .id(groupRole.getId())
                .groupId(groupRole.getGroupId())
                .roleId(groupRole.getRoleId())
                .grantedAt(groupRole.getGrantedAt())
                .grantedBy(groupRole.getGrantedBy())
                .createdAt(groupRole.getCreatedAt())
                .createdBy(groupRole.getCreatedBy())
                .build();
    }
}
