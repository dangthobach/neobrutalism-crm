package com.neobrutalism.crm.domain.apiendpoint.dto;

import com.neobrutalism.crm.common.enums.HttpMethod;
import com.neobrutalism.crm.domain.apiendpoint.model.ApiEndpoint;
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
@Schema(description = "API endpoint response")
public class ApiEndpointResponse {

    @Schema(description = "Endpoint ID")
    private UUID id;

    @Schema(description = "HTTP method")
    private HttpMethod method;

    @Schema(description = "API path")
    private String path;

    @Schema(description = "Tag/Group")
    private String tag;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Requires authentication")
    private Boolean requiresAuth;

    @Schema(description = "Is public API")
    private Boolean isPublic;

    public static ApiEndpointResponse from(ApiEndpoint apiEndpoint) {
        return ApiEndpointResponse.builder()
                .id(apiEndpoint.getId())
                .method(apiEndpoint.getMethod())
                .path(apiEndpoint.getPath())
                .tag(apiEndpoint.getTag())
                .description(apiEndpoint.getDescription())
                .requiresAuth(apiEndpoint.getRequiresAuth())
                .isPublic(apiEndpoint.getIsPublic())
                .build();
    }
}
