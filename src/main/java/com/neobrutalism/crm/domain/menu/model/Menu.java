package com.neobrutalism.crm.domain.menu.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
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
@Table(name = "menus", indexes = {
        @Index(name = "idx_menu_code", columnList = "code", unique = true),
        @Index(name = "idx_menu_parent", columnList = "parent_id"),
        @Index(name = "idx_menu_order", columnList = "display_order"),
        @Index(name = "idx_menu_deleted_id", columnList = "deleted, id"),
        @Index(name = "idx_menu_path", columnList = "path")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Menu extends SoftDeletableEntity {

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$")
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Size(max = 100)
    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Size(max = 500)
    @Column(name = "path", length = 500)
    private String path;

    @Size(max = 500)
    @Column(name = "route", length = 500)
    private String route;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "requires_auth", nullable = false)
    private Boolean requiresAuth = true;
}
