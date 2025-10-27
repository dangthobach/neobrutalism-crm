package com.neobrutalism.crm.domain.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role creation/update request")
public class RoleRequest {

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$")
    @Schema(description = "Role code", example = "ADMIN", required = true)
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    @Schema(description = "Role name", example = "Administrator", required = true)
    private String name;

    @Size(max = 1000)
    @Schema(description = "Role description")
    private String description;

    @Schema(description = "Organization ID", required = true)
    private UUID organizationId;

    @Schema(description = "Is system role")
    private Boolean isSystem = false;

    @Schema(description = "Priority level")
    private Integer priority = 0;
}
