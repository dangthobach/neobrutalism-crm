package com.neobrutalism.crm.domain.menuscreen.dto;

import com.neobrutalism.crm.domain.menuscreen.model.MenuScreen;
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
@Schema(description = "Menu screen response")
public class MenuScreenResponse {

    @Schema(description = "Screen ID")
    private UUID id;

    @Schema(description = "Screen code")
    private String code;

    @Schema(description = "Screen name")
    private String name;

    @Schema(description = "Menu ID")
    private UUID menuId;

    @Schema(description = "Tab ID")
    private UUID tabId;

    @Schema(description = "Frontend route")
    private String route;

    @Schema(description = "Component path")
    private String component;

    @Schema(description = "Requires permission")
    private Boolean requiresPermission;

    public static MenuScreenResponse from(MenuScreen menuScreen) {
        return MenuScreenResponse.builder()
                .id(menuScreen.getId())
                .code(menuScreen.getCode())
                .name(menuScreen.getName())
                .menuId(menuScreen.getMenuId())
                .tabId(menuScreen.getTabId())
                .route(menuScreen.getRoute())
                .component(menuScreen.getComponent())
                .requiresPermission(menuScreen.getRequiresPermission())
                .build();
    }
}
