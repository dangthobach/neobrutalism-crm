package com.neobrutalism.crm.domain.userrole.dto;

import com.neobrutalism.crm.domain.userrole.model.UserRole;
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
@Schema(description = "User role assignment response")
public class UserRoleResponse {

    @Schema(description = "User role ID")
    private UUID id;

    @Schema(description = "User ID")
    private UUID userId;

    @Schema(description = "Role ID")
    private UUID roleId;

    @Schema(description = "Is active")
    private Boolean isActive;

    @Schema(description = "Granted timestamp")
    private Instant grantedAt;

    @Schema(description = "Granted by")
    private String grantedBy;

    @Schema(description = "Expiration timestamp")
    private Instant expiresAt;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    public static UserRoleResponse from(UserRole userRole) {
        return UserRoleResponse.builder()
                .id(userRole.getId())
                .userId(userRole.getUserId())
                .roleId(userRole.getRoleId())
                .isActive(userRole.getIsActive())
                .grantedAt(userRole.getGrantedAt())
                .grantedBy(userRole.getGrantedBy())
                .expiresAt(userRole.getExpiresAt())
                .createdAt(userRole.getCreatedAt())
                .createdBy(userRole.getCreatedBy())
                .build();
    }
}
