package com.neobrutalism.crm.domain.menuscreen.dto;

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
@Schema(description = "Menu screen creation/update request")
public class MenuScreenRequest {

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$")
    @Schema(description = "Screen code", example = "CUSTOMER_LIST", required = true)
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    @Schema(description = "Screen name", example = "Customer List", required = true)
    private String name;

    @Schema(description = "Menu ID")
    private UUID menuId;

    @Schema(description = "Tab ID")
    private UUID tabId;

    @Size(max = 500)
    @Schema(description = "Frontend route")
    private String route;

    @Size(max = 500)
    @Schema(description = "Component path")
    private String component;

    @Schema(description = "Requires permission")
    private Boolean requiresPermission = true;
}
