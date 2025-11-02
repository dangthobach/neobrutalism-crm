package com.neobrutalism.crm.domain.menu.dto;

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
@Schema(description = "Menu creation/update request")
public class MenuRequest {

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$")
    @Schema(description = "Menu code", example = "SALES", required = true)
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    @Schema(description = "Menu name", example = "Sales Management", required = true)
    private String name;

    @Size(max = 100)
    @Schema(description = "Icon class or URL")
    private String icon;

    @Schema(description = "Parent menu ID")
    private UUID parentId;

    @Size(max = 500)
    @Schema(description = "Frontend route")
    private String route;

    @Schema(description = "Display order")
    private Integer displayOrder = 0;

    @Schema(description = "Is visible")
    private Boolean isVisible = true;

    @Schema(description = "Requires authentication")
    private Boolean requiresAuth = true;
}
