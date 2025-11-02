package com.neobrutalism.crm.domain.apiendpoint.dto;

import com.neobrutalism.crm.common.enums.HttpMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API endpoint creation/update request")
public class ApiEndpointRequest {

    @NotNull
    @Schema(description = "HTTP method", example = "GET", required = true)
    private HttpMethod method;

    @NotBlank
    @Size(min = 1, max = 500)
    @Schema(description = "API path", example = "/api/users/{id}", required = true)
    private String path;

    @Size(max = 100)
    @Schema(description = "Tag/Group", example = "User")
    private String tag;

    @Size(max = 500)
    @Schema(description = "Description")
    private String description;

    @Schema(description = "Requires authentication")
    private Boolean requiresAuth = true;

    @Schema(description = "Is public API")
    private Boolean isPublic = false;
}
