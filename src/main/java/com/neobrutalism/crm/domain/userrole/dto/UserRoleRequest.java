package com.neobrutalism.crm.domain.userrole.dto;

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
@Schema(description = "User role assignment request")
public class UserRoleRequest {

    @Schema(description = "User ID", required = true)
    private UUID userId;

    @Schema(description = "Role ID", required = true)
    private UUID roleId;

    @Schema(description = "Is active")
    private Boolean isActive = true;

    @Schema(description = "Expiration timestamp")
    private Instant expiresAt;
}
