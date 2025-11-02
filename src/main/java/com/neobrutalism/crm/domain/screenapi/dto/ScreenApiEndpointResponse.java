package com.neobrutalism.crm.domain.screenapi.dto;

import com.neobrutalism.crm.common.enums.PermissionType;
import com.neobrutalism.crm.domain.screenapi.model.ScreenApiEndpoint;
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
@Schema(description = "Screen API endpoint assignment response")
public class ScreenApiEndpointResponse {

    @Schema(description = "Screen API endpoint ID")
    private UUID id;

    @Schema(description = "Screen ID")
    private UUID screenId;

    @Schema(description = "Endpoint ID")
    private UUID endpointId;

    @Schema(description = "Required permission")
    private PermissionType requiredPermission;

    public static ScreenApiEndpointResponse from(ScreenApiEndpoint screenApiEndpoint) {
        return ScreenApiEndpointResponse.builder()
                .id(screenApiEndpoint.getId())
                .screenId(screenApiEndpoint.getScreenId())
                .endpointId(screenApiEndpoint.getEndpointId())
                .requiredPermission(screenApiEndpoint.getRequiredPermission())
                .build();
    }
}
