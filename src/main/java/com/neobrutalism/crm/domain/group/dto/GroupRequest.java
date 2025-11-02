package com.neobrutalism.crm.domain.group.dto;

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
@Schema(description = "Group creation/update request")
public class GroupRequest {

    @NotBlank(message = "Code is required")
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$")
    @Schema(description = "Group code", example = "SALES_TEAM", required = true)
    private String code;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 200)
    @Schema(description = "Group name", example = "Sales Team", required = true)
    private String name;

    @Size(max = 1000)
    @Schema(description = "Group description")
    private String description;

    @Schema(description = "Parent group ID")
    private UUID parentId;

    @Schema(description = "Organization ID", required = true)
    private UUID organizationId;
}
