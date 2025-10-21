package com.neobrutalism.crm.domain.organization.model;

import com.neobrutalism.crm.common.entity.AggregateRoot;
import com.neobrutalism.crm.domain.organization.event.OrganizationCreatedEvent;
import com.neobrutalism.crm.domain.organization.event.OrganizationStatusChangedEvent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Organization entity example demonstrating use of AggregateRoot
 */
@Entity
@Table(name = "organizations", indexes = {
        @Index(name = "idx_org_name", columnList = "name"),
        @Index(name = "idx_org_code", columnList = "code"),
        @Index(name = "idx_org_status", columnList = "status"),
        @Index(name = "idx_org_deleted_id", columnList = "deleted, id"),
        @Index(name = "idx_org_deleted_created_at", columnList = "deleted, created_at"),
        @Index(name = "idx_org_deleted_status", columnList = "deleted, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organization extends AggregateRoot<OrganizationStatus> {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "website", length = 200)
    private String website;

    @Override
    protected Set<OrganizationStatus> getAllowedTransitions(OrganizationStatus currentStatus) {
        return switch (currentStatus) {
            case DRAFT -> Set.of(OrganizationStatus.ACTIVE, OrganizationStatus.ARCHIVED);
            case ACTIVE -> Set.of(OrganizationStatus.SUSPENDED, OrganizationStatus.INACTIVE);
            case SUSPENDED -> Set.of(OrganizationStatus.ACTIVE, OrganizationStatus.INACTIVE);
            case INACTIVE -> Set.of(OrganizationStatus.ACTIVE, OrganizationStatus.ARCHIVED);
            case ARCHIVED -> Set.of(); // No transitions from ARCHIVED
        };
    }

    @Override
    protected OrganizationStatus getInitialStatus() {
        return OrganizationStatus.DRAFT;
    }

    @Override
    protected void onStatusChanged(OrganizationStatus oldStatus, OrganizationStatus newStatus) {
        super.onStatusChanged(oldStatus, newStatus);
        // Register domain event when status changes
        registerEvent(new OrganizationStatusChangedEvent(
                this.getId().toString(),
                oldStatus,
                newStatus,
                this.getStatusChangedBy()
        ));
    }

    @PostPersist
    protected void onCreated() {
        // Register domain event when organization is created
        registerEvent(new OrganizationCreatedEvent(
                this.getId().toString(),
                this.getName(),
                this.getCode(),
                this.getCreatedBy()
        ));
    }
}
