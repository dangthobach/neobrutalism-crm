package com.neobrutalism.crm.domain.grouprole.model;

import com.neobrutalism.crm.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "role_id"}),
        indexes = {
                @Index(name = "idx_gr_group", columnList = "group_id"),
                @Index(name = "idx_gr_role", columnList = "role_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupRole extends TenantAwareEntity {

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "granted_by", length = 100)
    private String grantedBy;
}
