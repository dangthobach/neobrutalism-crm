package com.neobrutalism.crm.domain.usergroup.dto;

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
@Schema(description = "User group assignment request")
public class UserGroupRequest {

    @Schema(description = "User ID", required = true)
    private UUID userId;

    @Schema(description = "Group ID", required = true)
    private UUID groupId;

    @Schema(description = "Is primary group")
    private Boolean isPrimary = false;
}
