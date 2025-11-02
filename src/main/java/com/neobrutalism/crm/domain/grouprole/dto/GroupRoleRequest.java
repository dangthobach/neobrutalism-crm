package com.neobrutalism.crm.domain.grouprole.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Group role assignment request")
public class GroupRoleRequest {

    @Schema(description = "Group ID", required = true)
    private UUID groupId;

    @Schema(description = "Role ID", required = true)
    private UUID roleId;
}
