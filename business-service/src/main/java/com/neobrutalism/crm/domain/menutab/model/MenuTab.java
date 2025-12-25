package com.neobrutalism.crm.domain.menutab.model;

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
@Table(name = "menu_tabs", indexes = {
        @Index(name = "idx_tab_menu", columnList = "menu_id"),
        @Index(name = "idx_tab_code", columnList = "code", unique = true),
        @Index(name = "idx_tab_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuTab extends BaseEntity {

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$")
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "menu_id", nullable = false)
    private UUID menuId;

    @Size(max = 100)
    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;
}
