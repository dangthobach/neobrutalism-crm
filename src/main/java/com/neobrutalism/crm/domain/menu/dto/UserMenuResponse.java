package com.neobrutalism.crm.domain.menu.dto;

import com.neobrutalism.crm.domain.menu.model.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for user-specific menu tree with permissions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User menu with permissions")
public class UserMenuResponse {

    @Schema(description = "Menu ID")
    private UUID id;

    @Schema(description = "Menu code")
    private String code;

    @Schema(description = "Menu name")
    private String name;

    @Schema(description = "Menu icon")
    private String icon;

    @Schema(description = "Menu route")
    private String route;

    @Schema(description = "Display order")
    private Integer displayOrder;

    @Schema(description = "Parent menu ID")
    private UUID parentId;

    @Schema(description = "Menu level")
    private Integer level;

    @Schema(description = "Is visible")
    private Boolean isVisible;

    @Schema(description = "Requires authentication")
    private Boolean requiresAuth;

    @Schema(description = "Permissions for this menu")
    private MenuPermissions permissions;

    @Schema(description = "Child menus")
    private List<UserMenuResponse> children;

    /**
     * Permission details for a menu
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuPermissions {
        private Boolean canView;
        private Boolean canCreate;
        private Boolean canEdit;
        private Boolean canDelete;
        private Boolean canExport;
        private Boolean canImport;
    }

    /**
     * Create from Menu entity
     */
    public static UserMenuResponse from(Menu menu) {
        return UserMenuResponse.builder()
                .id(menu.getId())
                .code(menu.getCode())
                .name(menu.getName())
                .icon(menu.getIcon())
                .route(menu.getRoute())
                .displayOrder(menu.getDisplayOrder())
                .parentId(menu.getParentId())
                .level(menu.getLevel())
                .isVisible(menu.getIsVisible())
                .requiresAuth(menu.getRequiresAuth())
                .permissions(null) // Will be set by service
                .children(new ArrayList<>())
                .build();
    }
}
