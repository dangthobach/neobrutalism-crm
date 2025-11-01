package com.neobrutalism.crm.domain.contact.dto;

import com.neobrutalism.crm.domain.contact.model.Contact;
import com.neobrutalism.crm.domain.contact.model.ContactRole;
import com.neobrutalism.crm.domain.contact.model.ContactStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Contact entity
 */
@Data
@Builder
@Schema(description = "Contact response data")
public class ContactResponse {

    @Schema(description = "Contact ID")
    private UUID id;

    @Schema(description = "Customer ID")
    private UUID customerId;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Middle name")
    private String middleName;

    @Schema(description = "Full name")
    private String fullName;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Department")
    private String department;

    @Schema(description = "Contact role")
    private ContactRole contactRole;

    @Schema(description = "Status")
    private ContactStatus status;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Secondary email")
    private String secondaryEmail;

    @Schema(description = "Work phone")
    private String workPhone;

    @Schema(description = "Mobile phone")
    private String mobilePhone;

    @Schema(description = "Home phone")
    private String homePhone;

    @Schema(description = "Fax")
    private String fax;

    @Schema(description = "LinkedIn URL")
    private String linkedinUrl;

    @Schema(description = "Twitter handle")
    private String twitterHandle;

    @Schema(description = "Mailing address")
    private String mailingAddress;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State")
    private String state;

    @Schema(description = "Country")
    private String country;

    @Schema(description = "Postal code")
    private String postalCode;

    @Schema(description = "Owner ID")
    private UUID ownerId;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Birth date")
    private LocalDate birthDate;

    @Schema(description = "Preferred contact method")
    private String preferredContactMethod;

    @Schema(description = "Preferred contact time")
    private String preferredContactTime;

    @Schema(description = "Assistant name")
    private String assistantName;

    @Schema(description = "Assistant phone")
    private String assistantPhone;

    @Schema(description = "Reports to ID")
    private UUID reportsToId;

    @Schema(description = "Is primary")
    private Boolean isPrimary;

    @Schema(description = "Email opt-out")
    private Boolean emailOptOut;

    @Schema(description = "Last contact date")
    private LocalDate lastContactDate;

    @Schema(description = "Notes")
    private String notes;

    @Schema(description = "Tags")
    private String tags;

    @Schema(description = "Tenant ID")
    private String tenantId;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Created at")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Updated at")
    private Instant updatedAt;

    @Schema(description = "Updated by")
    private String updatedBy;

    /**
     * Convert Contact entity to response DTO
     */
    public static ContactResponse from(Contact contact) {
        if (contact == null) {
            return null;
        }

        return ContactResponse.builder()
                .id(contact.getId())
                .customerId(contact.getCustomerId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .middleName(contact.getMiddleName())
                .fullName(contact.getFullName())
                .title(contact.getTitle())
                .department(contact.getDepartment())
                .contactRole(contact.getContactRole())
                .status(contact.getStatus())
                .email(contact.getEmail())
                .secondaryEmail(contact.getSecondaryEmail())
                .workPhone(contact.getWorkPhone())
                .mobilePhone(contact.getMobilePhone())
                .homePhone(contact.getHomePhone())
                .fax(contact.getFax())
                .linkedinUrl(contact.getLinkedinUrl())
                .twitterHandle(contact.getTwitterHandle())
                .mailingAddress(contact.getMailingAddress())
                .city(contact.getCity())
                .state(contact.getState())
                .country(contact.getCountry())
                .postalCode(contact.getPostalCode())
                .ownerId(contact.getOwnerId())
                .organizationId(contact.getOrganizationId())
                .birthDate(contact.getBirthDate())
                .preferredContactMethod(contact.getPreferredContactMethod())
                .preferredContactTime(contact.getPreferredContactTime())
                .assistantName(contact.getAssistantName())
                .assistantPhone(contact.getAssistantPhone())
                .reportsToId(contact.getReportsToId())
                .isPrimary(contact.getIsPrimary())
                .emailOptOut(contact.getEmailOptOut())
                .lastContactDate(contact.getLastContactDate())
                .notes(contact.getNotes())
                .tags(contact.getTags())
                .tenantId(contact.getTenantId())
                .version(contact.getVersion())
                .createdAt(contact.getCreatedAt())
                .createdBy(contact.getCreatedBy())
                .updatedAt(contact.getUpdatedAt())
                .updatedBy(contact.getUpdatedBy())
                .build();
    }
}
