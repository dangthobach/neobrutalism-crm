package com.neobrutalism.crm.domain.menutab.dto;

import com.neobrutalism.crm.domain.menutab.model.MenuTab;
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
@Schema(description = "Menu tab response")
public class MenuTabResponse {

    @Schema(description = "Tab ID")
    private UUID id;

    @Schema(description = "Tab code")
    private String code;

    @Schema(description = "Tab name")
    private String name;

    @Schema(description = "Menu ID")
    private UUID menuId;

    @Schema(description = "Icon")
    private String icon;

    @Schema(description = "Display order")
    private Integer displayOrder;

    @Schema(description = "Is visible")
    private Boolean isVisible;

    public static MenuTabResponse from(MenuTab menuTab) {
        return MenuTabResponse.builder()
                .id(menuTab.getId())
                .code(menuTab.getCode())
                .name(menuTab.getName())
                .menuId(menuTab.getMenuId())
                .icon(menuTab.getIcon())
                .displayOrder(menuTab.getDisplayOrder())
                .isVisible(menuTab.getIsVisible())
                .build();
    }
}
