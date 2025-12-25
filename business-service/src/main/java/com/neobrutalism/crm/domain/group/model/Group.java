package com.neobrutalism.crm.domain.group.model;

import com.neobrutalism.crm.common.entity.TenantAwareAggregateRoot;
import com.neobrutalism.crm.domain.group.event.GroupCreatedEvent;
import com.neobrutalism.crm.domain.group.event.GroupStatusChangedEvent;
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

/**
 * Group/Team entity with hierarchy support
 */
@Entity
@Table(name = "groups", indexes = {
        @Index(name = "idx_group_code", columnList = "code", unique = true),
        @Index(name = "idx_group_parent", columnList = "parent_id"),
        @Index(name = "idx_group_org", columnList = "organization_id"),
        @Index(name = "idx_group_deleted_id", columnList = "deleted, id"),
        @Index(name = "idx_group_path", columnList = "path")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Group extends TenantAwareAggregateRoot<GroupStatus> {

    @NotBlank(message = "Group code is required")
    @Size(min = 2, max = 50, message = "Group code must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Group code must contain only uppercase letters, numbers, dashes, and underscores")
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 200, message = "Group name must be between 1 and 200 characters")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "path", length = 500)
    private String path;

    @Override
    protected Set<GroupStatus> getAllowedTransitions(GroupStatus currentStatus) {
        return switch (currentStatus) {
            case ACTIVE -> Set.of(GroupStatus.INACTIVE);
            case INACTIVE -> Set.of(GroupStatus.ACTIVE);
        };
    }

    @Override
    protected GroupStatus getInitialStatus() {
        return GroupStatus.ACTIVE;
    }

    @Override
    protected void onStatusChanged(GroupStatus oldStatus, GroupStatus newStatus) {
        super.onStatusChanged(oldStatus, newStatus);
        registerEvent(new GroupStatusChangedEvent(
                this.getId().toString(),
                oldStatus,
                newStatus,
                this.getStatusChangedBy()
        ));
    }

    @PostPersist
    protected void onCreated() {
        registerEvent(new GroupCreatedEvent(
                this.getId().toString(),
                this.getName(),
                this.getCode(),
                this.getCreatedBy()
        ));
    }

    /**
     * Build materialized path
     */
    public void buildPath() {
        if (parentId == null) {
            this.path = "/" + this.getId();
            this.level = 1;
        } else {
            // Path will be updated by service after parent is loaded
        }
    }
}
