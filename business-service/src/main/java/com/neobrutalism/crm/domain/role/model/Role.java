package com.neobrutalism.crm.domain.role.model;

import com.neobrutalism.crm.common.entity.TenantAwareAggregateRoot;
import com.neobrutalism.crm.domain.role.event.RoleCreatedEvent;
import com.neobrutalism.crm.domain.role.event.RoleStatusChangedEvent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles", indexes = {
        @Index(name = "idx_role_code", columnList = "code", unique = true),
        @Index(name = "idx_role_org", columnList = "organization_id"),
        @Index(name = "idx_role_deleted_id", columnList = "deleted, id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role extends TenantAwareAggregateRoot<RoleStatus> {

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$")
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Override
    protected Set<RoleStatus> getAllowedTransitions(RoleStatus currentStatus) {
        return switch (currentStatus) {
            case ACTIVE -> Set.of(RoleStatus.INACTIVE);
            case INACTIVE -> Set.of(RoleStatus.ACTIVE);
        };
    }

    @Override
    protected RoleStatus getInitialStatus() {
        return RoleStatus.ACTIVE;
    }

    @Override
    protected void onStatusChanged(RoleStatus oldStatus, RoleStatus newStatus) {
        super.onStatusChanged(oldStatus, newStatus);
        registerEvent(new RoleStatusChangedEvent(
                this.getId().toString(),
                oldStatus,
                newStatus,
                this.getStatusChangedBy()
        ));
    }

    @PostPersist
    protected void onCreated() {
        registerEvent(new RoleCreatedEvent(
                this.getId().toString(),
                this.getName(),
                this.getCode(),
                this.getCreatedBy()
        ));
    }
}
