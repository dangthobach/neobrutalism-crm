package com.neobrutalism.crm.domain.userrole.model;

import com.neobrutalism.crm.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}),
        indexes = {
                @Index(name = "idx_ur_user", columnList = "user_id"),
                @Index(name = "idx_ur_role", columnList = "role_id"),
                @Index(name = "idx_ur_active", columnList = "is_active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRole extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "granted_by", length = 100)
    private String grantedBy;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
