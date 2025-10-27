package com.neobrutalism.crm.domain.screenapi.dto;

import com.neobrutalism.crm.common.enums.PermissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Screen API endpoint assignment request")
public class ScreenApiEndpointRequest {

    @NotNull
    @Schema(description = "Screen ID", required = true)
    private UUID screenId;

    @NotNull
    @Schema(description = "Endpoint ID", required = true)
    private UUID endpointId;

    @NotNull
    @Schema(description = "Required permission", example = "READ", required = true)
    private PermissionType requiredPermission;
}
