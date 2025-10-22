package com.neobrutalism.crm.domain.organization.model;

import com.neobrutalism.crm.common.entity.AggregateRoot;
import com.neobrutalism.crm.common.validation.ValidEmail;
import com.neobrutalism.crm.common.validation.ValidOrganization;
import com.neobrutalism.crm.common.validation.ValidPhone;
import com.neobrutalism.crm.common.validation.ValidUrl;
import com.neobrutalism.crm.domain.organization.event.OrganizationCreatedEvent;
import com.neobrutalism.crm.domain.organization.event.OrganizationStatusChangedEvent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@ValidOrganization
public class Organization extends AggregateRoot<OrganizationStatus> {

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 200, message = "Organization name must be between 2 and 200 characters")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotBlank(message = "Organization code is required")
    @Size(min = 2, max = 50, message = "Organization code must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Organization code must contain only uppercase letters, numbers, dashes, and underscores")
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    @ValidEmail
    @Column(name = "email", length = 100)
    private String email;

    @ValidPhone
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Column(name = "address", length = 500)
    private String address;

    @ValidUrl
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
