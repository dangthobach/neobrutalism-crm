package com.neobrutalism.crm.domain.rolemenu.model;

import com.neobrutalism.crm.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "role_menus",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "menu_id"}),
        indexes = {
                @Index(name = "idx_rm_role", columnList = "role_id"),
                @Index(name = "idx_rm_menu", columnList = "menu_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenu extends TenantAwareEntity {

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "menu_id", nullable = false)
    private UUID menuId;

    @Column(name = "can_view", nullable = false)
    private Boolean canView = true;

    @Column(name = "can_create", nullable = false)
    private Boolean canCreate = false;

    @Column(name = "can_edit", nullable = false)
    private Boolean canEdit = false;

    @Column(name = "can_delete", nullable = false)
    private Boolean canDelete = false;

    @Column(name = "can_export", nullable = false)
    private Boolean canExport = false;

    @Column(name = "can_import", nullable = false)
    private Boolean canImport = false;
}
