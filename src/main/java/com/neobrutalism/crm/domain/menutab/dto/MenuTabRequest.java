package com.neobrutalism.crm.domain.menutab.dto;

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
@Schema(description = "Menu tab creation/update request")
public class MenuTabRequest {

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$")
    @Schema(description = "Tab code", example = "OVERVIEW", required = true)
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    @Schema(description = "Tab name", example = "Overview", required = true)
    private String name;

    @Schema(description = "Menu ID", required = true)
    private UUID menuId;

    @Size(max = 100)
    @Schema(description = "Icon")
    private String icon;

    @Schema(description = "Display order")
    private Integer displayOrder = 0;

    @Schema(description = "Is visible")
    private Boolean isVisible = true;
}
