package com.neobrutalism.crm.domain.organization.dto;

import com.neobrutalism.crm.domain.organization.model.Organization;
import com.neobrutalism.crm.domain.organization.model.OrganizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Organization response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization response")
public class OrganizationResponse {

    @Schema(description = "Organization ID (UUID v7)", example = "018d3f5c-7b44-7a90-a123-456789abcdef")
    private UUID id;

    @Schema(description = "Organization name", example = "Acme Corporation")
    private String name;

    @Schema(description = "Unique organization code", example = "ACME")
    private String code;

    @Schema(description = "Organization description")
    private String description;

    @Schema(description = "Contact email")
    private String email;

    @Schema(description = "Contact phone")
    private String phone;

    @Schema(description = "Physical address")
    private String address;

    @Schema(description = "Website URL")
    private String website;

    @Schema(description = "Current status")
    private OrganizationStatus status;

    @Schema(description = "Is deleted")
    private Boolean deleted;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Last updated timestamp")
    private Instant updatedAt;

    @Schema(description = "Last updated by")
    private String updatedBy;

    public static OrganizationResponse from(Organization organization) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .code(organization.getCode())
                .description(organization.getDescription())
                .email(organization.getEmail())
                .phone(organization.getPhone())
                .address(organization.getAddress())
                .website(organization.getWebsite())
                .status(organization.getStatus())
                .deleted(organization.getDeleted())
                .createdAt(organization.getCreatedAt())
                .createdBy(organization.getCreatedBy())
                .updatedAt(organization.getUpdatedAt())
                .updatedBy(organization.getUpdatedBy())
                .build();
    }
}
