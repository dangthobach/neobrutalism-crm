package com.neobrutalism.crm.domain.contact.model;

import com.neobrutalism.crm.common.entity.TenantAwareAggregateRoot;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Contact Entity - Individual contact person
 */
@Getter
@Setter
@Entity
@Table(
    name = "contacts",
    indexes = {
        @Index(name = "idx_contact_email", columnList = "email"),
        @Index(name = "idx_contact_customer_id", columnList = "customer_id"),
        @Index(name = "idx_contact_status", columnList = "status"),
        @Index(name = "idx_contact_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_contact_owner_id", columnList = "owner_id"),
        @Index(name = "idx_contact_deleted_id", columnList = "deleted, id")
    }
)
public class Contact extends TenantAwareAggregateRoot<ContactStatus> {

    /**
     * Customer ID (optional - can be independent contact)
     */
    @Column(name = "customer_id")
    private UUID customerId;

    /**
     * First name
     */
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Last name
     */
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Middle name
     */
    @Size(max = 100, message = "Middle name must be at most 100 characters")
    @Column(name = "middle_name", length = 100)
    private String middleName;

    /**
     * Full name (computed)
     */
    @Size(max = 255, message = "Full name must be at most 255 characters")
    @Column(name = "full_name", length = 255)
    private String fullName;

    /**
     * Title/Position
     */
    @Size(max = 100, message = "Title must be at most 100 characters")
    @Column(name = "title", length = 100)
    private String title;

    /**
     * Department
     */
    @Size(max = 100, message = "Department must be at most 100 characters")
    @Column(name = "department", length = 100)
    private String department;

    /**
     * Contact role
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_role", length = 30)
    private ContactRole contactRole;

    /**
     * Status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContactStatus status = ContactStatus.ACTIVE;

    /**
     * Primary email
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Secondary email
     */
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Secondary email must be at most 255 characters")
    @Column(name = "secondary_email", length = 255)
    private String secondaryEmail;

    /**
     * Work phone
     */
    @Size(max = 50, message = "Work phone must be at most 50 characters")
    @Column(name = "work_phone", length = 50)
    private String workPhone;

    /**
     * Mobile phone
     */
    @Size(max = 50, message = "Mobile phone must be at most 50 characters")
    @Column(name = "mobile_phone", length = 50)
    private String mobilePhone;

    /**
     * Home phone
     */
    @Size(max = 50, message = "Home phone must be at most 50 characters")
    @Column(name = "home_phone", length = 50)
    private String homePhone;

    /**
     * Fax
     */
    @Size(max = 50, message = "Fax must be at most 50 characters")
    @Column(name = "fax", length = 50)
    private String fax;

    /**
     * LinkedIn profile
     */
    @Size(max = 255, message = "LinkedIn URL must be at most 255 characters")
    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    /**
     * Twitter handle
     */
    @Size(max = 100, message = "Twitter handle must be at most 100 characters")
    @Column(name = "twitter_handle", length = 100)
    private String twitterHandle;

    /**
     * Mailing address
     */
    @Size(max = 500, message = "Mailing address must be at most 500 characters")
    @Column(name = "mailing_address", length = 500)
    private String mailingAddress;

    /**
     * City
     */
    @Size(max = 100, message = "City must be at most 100 characters")
    @Column(name = "city", length = 100)
    private String city;

    /**
     * State
     */
    @Size(max = 100, message = "State must be at most 100 characters")
    @Column(name = "state", length = 100)
    private String state;

    /**
     * Country
     */
    @Size(max = 100, message = "Country must be at most 100 characters")
    @Column(name = "country", length = 100)
    private String country;

    /**
     * Postal code
     */
    @Size(max = 20, message = "Postal code must be at most 20 characters")
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * Owner user ID - Account manager
     */
    @Column(name = "owner_id")
    private UUID ownerId;

    /**
     * Organization ID
     */
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /**
     * Birth date
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * Preferred contact method
     */
    @Size(max = 20, message = "Preferred contact method must be at most 20 characters")
    @Column(name = "preferred_contact_method", length = 20)
    private String preferredContactMethod; // EMAIL, PHONE, SMS, etc.

    /**
     * Preferred contact time
     */
    @Size(max = 100, message = "Preferred contact time must be at most 100 characters")
    @Column(name = "preferred_contact_time", length = 100)
    private String preferredContactTime; // Morning, Afternoon, Evening

    /**
     * Assistant name
     */
    @Size(max = 100, message = "Assistant name must be at most 100 characters")
    @Column(name = "assistant_name", length = 100)
    private String assistantName;

    /**
     * Assistant phone
     */
    @Size(max = 50, message = "Assistant phone must be at most 50 characters")
    @Column(name = "assistant_phone", length = 50)
    private String assistantPhone;

    /**
     * Reports to (another contact)
     */
    @Column(name = "reports_to_id")
    private UUID reportsToId;

    /**
     * Is primary contact for customer
     */
    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    /**
     * Email opt-out
     */
    @Column(name = "email_opt_out")
    private Boolean emailOptOut = false;

    /**
     * Last contact date
     */
    @Column(name = "last_contact_date")
    private LocalDate lastContactDate;

    /**
     * Notes
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Tags (comma-separated)
     */
    @Size(max = 500, message = "Tags must be at most 500 characters")
    @Column(name = "tags", length = 500)
    private String tags;

    @Override
    protected Set<ContactStatus> getAllowedTransitions(ContactStatus currentStatus) {
        return switch (currentStatus) {
            case ACTIVE -> Set.of(ContactStatus.INACTIVE, ContactStatus.LEFT_COMPANY, ContactStatus.DO_NOT_CONTACT);
            case INACTIVE -> Set.of(ContactStatus.ACTIVE, ContactStatus.LEFT_COMPANY, ContactStatus.DO_NOT_CONTACT);
            case LEFT_COMPANY -> Set.of(ContactStatus.ACTIVE);
            case DO_NOT_CONTACT -> Set.of(ContactStatus.ACTIVE);
        };
    }

    @Override
    protected ContactStatus getInitialStatus() {
        return ContactStatus.ACTIVE;
    }

    @PrePersist
    @PreUpdate
    public void validateContact() {
        // Compute full name
        if (middleName != null && !middleName.isBlank()) {
            fullName = firstName + " " + middleName + " " + lastName;
        } else {
            fullName = firstName + " " + lastName;
        }

        // Normalize email
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (secondaryEmail != null) {
            secondaryEmail = secondaryEmail.trim().toLowerCase();
        }
    }
}
