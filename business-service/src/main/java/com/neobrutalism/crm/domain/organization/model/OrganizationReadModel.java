package com.neobrutalism.crm.domain.organization.model;

import com.neobrutalism.crm.common.cqrs.ReadModel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Read Model for Organization queries
 * Denormalized, immutable, optimized for reads
 * Updated only via event handlers
 */
@Entity
@Table(name = "organization_read_model", indexes = {
        @Index(name = "idx_org_rm_name", columnList = "name"),
        @Index(name = "idx_org_rm_code", columnList = "code"),
        @Index(name = "idx_org_rm_status", columnList = "status"),
        @Index(name = "idx_org_rm_active", columnList = "is_active"),
        @Index(name = "idx_org_rm_created", columnList = "created_at"),
        @Index(name = "idx_org_rm_search", columnList = "search_text")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ReadModel(aggregate = "Organization", description = "Optimized read model for organization queries")
public class OrganizationReadModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OrganizationStatus status;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    // Denormalized fields for optimized queries
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Full-text search field (denormalized)
    @Column(name = "search_text", length = 2000)
    private String searchText;

    // Computed fields
    @Column(name = "has_contact_info", nullable = false)
    private Boolean hasContactInfo;

    @Column(name = "days_since_created", nullable = false)
    private Integer daysSinceCreated;

    /**
     * Create read model from Organization aggregate
     */
    public static OrganizationReadModel from(com.neobrutalism.crm.domain.organization.model.Organization org) {
        String searchText = buildSearchText(org);
        boolean hasContactInfo = (org.getEmail() != null && !org.getEmail().isEmpty()) ||
                (org.getPhone() != null && !org.getPhone().isEmpty());

        int daysSinceCreated = (int) java.time.Duration.between(
                org.getCreatedAt(), Instant.now()
        ).toDays();

        return OrganizationReadModel.builder()
                .id(org.getId())
                .name(org.getName())
                .code(org.getCode())
                .description(org.getDescription())
                .email(org.getEmail())
                .phone(org.getPhone())
                .website(org.getWebsite())
                .status(org.getStatus())
                .isActive(org.getStatus() == OrganizationStatus.ACTIVE)
                .isDeleted(org.isDeleted())
                .createdAt(org.getCreatedAt())
                .createdBy(org.getCreatedBy())
                .updatedAt(org.getUpdatedAt())
                .updatedBy(org.getUpdatedBy())
                .searchText(searchText)
                .hasContactInfo(hasContactInfo)
                .daysSinceCreated(daysSinceCreated)
                .build();
    }

    /**
     * Build full-text search field
     */
    private static String buildSearchText(com.neobrutalism.crm.domain.organization.model.Organization org) {
        StringBuilder sb = new StringBuilder();
        if (org.getName() != null) sb.append(org.getName().toLowerCase()).append(" ");
        if (org.getCode() != null) sb.append(org.getCode().toLowerCase()).append(" ");
        if (org.getDescription() != null) sb.append(org.getDescription().toLowerCase()).append(" ");
        if (org.getEmail() != null) sb.append(org.getEmail().toLowerCase()).append(" ");
        return sb.toString().trim();
    }
}
