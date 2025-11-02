package com.neobrutalism.crm.domain.rolemenu.dto;

import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role menu permission response")
public class RoleMenuResponse {

    @Schema(description = "Role menu ID")
    private UUID id;

    @Schema(description = "Role ID")
    private UUID roleId;

    @Schema(description = "Menu ID")
    private UUID menuId;

    @Schema(description = "Can view")
    private Boolean canView;

    @Schema(description = "Can create")
    private Boolean canCreate;

    @Schema(description = "Can edit")
    private Boolean canEdit;

    @Schema(description = "Can delete")
    private Boolean canDelete;

    @Schema(description = "Can export")
    private Boolean canExport;

    @Schema(description = "Can import")
    private Boolean canImport;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    public static RoleMenuResponse from(RoleMenu roleMenu) {
        return RoleMenuResponse.builder()
                .id(roleMenu.getId())
                .roleId(roleMenu.getRoleId())
                .menuId(roleMenu.getMenuId())
                .canView(roleMenu.getCanView())
                .canCreate(roleMenu.getCanCreate())
                .canEdit(roleMenu.getCanEdit())
                .canDelete(roleMenu.getCanDelete())
                .canExport(roleMenu.getCanExport())
                .canImport(roleMenu.getCanImport())
                .createdAt(roleMenu.getCreatedAt())
                .createdBy(roleMenu.getCreatedBy())
                .build();
    }
}
