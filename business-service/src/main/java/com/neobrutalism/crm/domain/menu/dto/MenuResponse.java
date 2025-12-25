package com.neobrutalism.crm.domain.menu.dto;

import com.neobrutalism.crm.domain.menu.model.Menu;
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
@Schema(description = "Menu response")
public class MenuResponse {

    @Schema(description = "Menu ID")
    private UUID id;

    @Schema(description = "Menu code")
    private String code;

    @Schema(description = "Menu name")
    private String name;

    @Schema(description = "Icon")
    private String icon;

    @Schema(description = "Parent menu ID")
    private UUID parentId;

    @Schema(description = "Hierarchy level")
    private Integer level;

    @Schema(description = "Materialized path")
    private String path;

    @Schema(description = "Frontend route")
    private String route;

    @Schema(description = "Display order")
    private Integer displayOrder;

    @Schema(description = "Is visible")
    private Boolean isVisible;

    @Schema(description = "Requires authentication")
    private Boolean requiresAuth;

    @Schema(description = "Is deleted")
    private Boolean deleted;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .code(menu.getCode())
                .name(menu.getName())
                .icon(menu.getIcon())
                .parentId(menu.getParentId())
                .level(menu.getLevel())
                .path(menu.getPath())
                .route(menu.getRoute())
                .displayOrder(menu.getDisplayOrder())
                .isVisible(menu.getIsVisible())
                .requiresAuth(menu.getRequiresAuth())
                .deleted(menu.getDeleted())
                .createdAt(menu.getCreatedAt())
                .build();
    }
}
