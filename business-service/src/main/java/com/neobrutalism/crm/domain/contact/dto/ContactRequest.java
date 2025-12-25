package com.neobrutalism.crm.domain.contact.dto;

import com.neobrutalism.crm.domain.contact.model.ContactRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for Contact operations
 */
@Data
@Schema(description = "Contact request data")
public class ContactRequest {

    @Schema(description = "Customer ID")
    private UUID customerId;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    @Schema(description = "First name", example = "John")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Size(max = 100, message = "Middle name must be at most 100 characters")
    @Schema(description = "Middle name")
    private String middleName;

    @Size(max = 100, message = "Title must be at most 100 characters")
    @Schema(description = "Job title", example = "CEO")
    private String title;

    @Size(max = 100, message = "Department must be at most 100 characters")
    @Schema(description = "Department", example = "Executive")
    private String department;

    @Schema(description = "Contact role", example = "DECISION_MAKER")
    private ContactRole contactRole;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    @Schema(description = "Primary email", example = "john.doe@acme.com")
    private String email;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Secondary email must be at most 255 characters")
    @Schema(description = "Secondary email")
    private String secondaryEmail;

    @Size(max = 50, message = "Work phone must be at most 50 characters")
    @Schema(description = "Work phone", example = "+1-555-0123")
    private String workPhone;

    @Size(max = 50, message = "Mobile phone must be at most 50 characters")
    @Schema(description = "Mobile phone", example = "+1-555-0124")
    private String mobilePhone;

    @Size(max = 50, message = "Home phone must be at most 50 characters")
    @Schema(description = "Home phone")
    private String homePhone;

    @Size(max = 50, message = "Fax must be at most 50 characters")
    @Schema(description = "Fax")
    private String fax;

    @Size(max = 255, message = "LinkedIn URL must be at most 255 characters")
    @Schema(description = "LinkedIn profile URL")
    private String linkedinUrl;

    @Size(max = 100, message = "Twitter handle must be at most 100 characters")
    @Schema(description = "Twitter handle", example = "@johndoe")
    private String twitterHandle;

    @Size(max = 500, message = "Mailing address must be at most 500 characters")
    @Schema(description = "Mailing address")
    private String mailingAddress;

    @Size(max = 100, message = "City must be at most 100 characters")
    @Schema(description = "City")
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    @Schema(description = "State/Province")
    private String state;

    @Size(max = 100, message = "Country must be at most 100 characters")
    @Schema(description = "Country")
    private String country;

    @Size(max = 20, message = "Postal code must be at most 20 characters")
    @Schema(description = "Postal code")
    private String postalCode;

    @Schema(description = "Owner user ID")
    private UUID ownerId;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Birth date")
    private LocalDate birthDate;

    @Size(max = 20, message = "Preferred contact method must be at most 20 characters")
    @Schema(description = "Preferred contact method", example = "EMAIL")
    private String preferredContactMethod;

    @Size(max = 100, message = "Preferred contact time must be at most 100 characters")
    @Schema(description = "Preferred contact time", example = "Morning")
    private String preferredContactTime;

    @Size(max = 100, message = "Assistant name must be at most 100 characters")
    @Schema(description = "Assistant name")
    private String assistantName;

    @Size(max = 50, message = "Assistant phone must be at most 50 characters")
    @Schema(description = "Assistant phone")
    private String assistantPhone;

    @Schema(description = "Reports to contact ID")
    private UUID reportsToId;

    @Schema(description = "Is primary contact", example = "false")
    private Boolean isPrimary;

    @Schema(description = "Email opt-out", example = "false")
    private Boolean emailOptOut;

    @Schema(description = "Last contact date")
    private LocalDate lastContactDate;

    @Schema(description = "Notes")
    private String notes;

    @Size(max = 500, message = "Tags must be at most 500 characters")
    @Schema(description = "Tags (comma-separated)")
    private String tags;
}
