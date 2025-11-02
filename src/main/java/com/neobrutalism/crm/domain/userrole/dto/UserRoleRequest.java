package com.neobrutalism.crm.domain.userrole.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @Builder.Default
    private Boolean isActive = true;

    @Schema(description = "Expiration timestamp (ISO-8601 format)", example = "2025-10-15T22:50:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant expiresAt;
}
