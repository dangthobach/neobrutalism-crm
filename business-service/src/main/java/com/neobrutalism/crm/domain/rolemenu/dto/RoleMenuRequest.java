package com.neobrutalism.crm.domain.rolemenu.dto;

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
@Schema(description = "Role menu permission assignment request")
public class RoleMenuRequest {

    @Schema(description = "Role ID", required = true)
    private UUID roleId;

    @Schema(description = "Menu ID", required = true)
    private UUID menuId;

    @Schema(description = "Can view")
    private Boolean canView = true;

    @Schema(description = "Can create")
    private Boolean canCreate = false;

    @Schema(description = "Can edit")
    private Boolean canEdit = false;

    @Schema(description = "Can delete")
    private Boolean canDelete = false;

    @Schema(description = "Can export")
    private Boolean canExport = false;

    @Schema(description = "Can import")
    private Boolean canImport = false;
}
