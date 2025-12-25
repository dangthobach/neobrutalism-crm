package com.neobrutalism.crm.domain.menuscreen.model;

import com.neobrutalism.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "menu_screens", indexes = {
        @Index(name = "idx_screen_menu", columnList = "menu_id"),
        @Index(name = "idx_screen_tab", columnList = "tab_id"),
        @Index(name = "idx_screen_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuScreen extends BaseEntity {

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$")
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "menu_id")
    private UUID menuId;

    @Column(name = "tab_id")
    private UUID tabId;

    @Size(max = 500)
    @Column(name = "route", length = 500)
    private String route;

    @Size(max = 500)
    @Column(name = "component", length = 500)
    private String component;

    @Column(name = "requires_permission", nullable = false)
    private Boolean requiresPermission = true;
}
