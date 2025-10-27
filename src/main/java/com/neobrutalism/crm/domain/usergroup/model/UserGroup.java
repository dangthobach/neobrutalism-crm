package com.neobrutalism.crm.domain.usergroup.model;

import com.neobrutalism.crm.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_groups",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "group_id"}),
        indexes = {
                @Index(name = "idx_ug_user", columnList = "user_id"),
                @Index(name = "idx_ug_group", columnList = "group_id"),
                @Index(name = "idx_ug_primary", columnList = "is_primary")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGroup extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();
}
