package com.neobrutalism.crm.domain.customer.dto;

import com.neobrutalism.crm.domain.customer.model.CustomerType;
import com.neobrutalism.crm.domain.customer.model.Industry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for Customer operations
 */
@Data
@Schema(description = "Customer request data")
public class CustomerRequest {

    @NotBlank(message = "Customer code is required")
    @Size(max = 50, message = "Customer code must be at most 50 characters")
    @Schema(description = "Unique customer code", example = "CUST-001")
    private String code;

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must be at most 255 characters")
    @Schema(description = "Company name", example = "Acme Corporation")
    private String companyName;

    @Size(max = 255, message = "Legal name must be at most 255 characters")
    @Schema(description = "Legal company name", example = "Acme Corporation Ltd.")
    private String legalName;

    @Schema(description = "Customer type", example = "B2B")
    private CustomerType customerType;

    @Schema(description = "Industry", example = "TECHNOLOGY")
    private Industry industry;

    @Size(max = 50, message = "Tax ID must be at most 50 characters")
    @Schema(description = "Tax ID / VAT number", example = "123456789")
    private String taxId;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    @Schema(description = "Primary email", example = "contact@acme.com")
    private String email;

    @Size(max = 50, message = "Phone must be at most 50 characters")
    @Schema(description = "Primary phone", example = "+1-555-0123")
    private String phone;

    @Size(max = 255, message = "Website must be at most 255 characters")
    @Schema(description = "Company website", example = "https://acme.com")
    private String website;

    @Size(max = 500, message = "Billing address must be at most 500 characters")
    @Schema(description = "Billing address")
    private String billingAddress;

    @Size(max = 500, message = "Shipping address must be at most 500 characters")
    @Schema(description = "Shipping address")
    private String shippingAddress;

    @Size(max = 100, message = "City must be at most 100 characters")
    @Schema(description = "City", example = "New York")
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    @Schema(description = "State/Province", example = "NY")
    private String state;

    @Size(max = 100, message = "Country must be at most 100 characters")
    @Schema(description = "Country", example = "USA")
    private String country;

    @Size(max = 20, message = "Postal code must be at most 20 characters")
    @Schema(description = "Postal code", example = "10001")
    private String postalCode;

    @Schema(description = "Owner user ID (account manager)")
    private UUID ownerId;

    @Schema(description = "Branch ID")
    private UUID branchId;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Annual revenue", example = "1000000.00")
    private BigDecimal annualRevenue;

    @Schema(description = "Number of employees", example = "50")
    private Integer employeeCount;

    @Schema(description = "Date customer was acquired")
    private LocalDate acquisitionDate;

    @Schema(description = "Last contact date")
    private LocalDate lastContactDate;

    @Schema(description = "Next follow-up date")
    private LocalDate nextFollowupDate;

    @Size(max = 100, message = "Lead source must be at most 100 characters")
    @Schema(description = "Lead source", example = "Website")
    private String leadSource;

    @Schema(description = "Credit limit", example = "50000.00")
    private BigDecimal creditLimit;

    @Schema(description = "Payment terms in days", example = "30")
    private Integer paymentTermsDays;

    @Size(max = 500, message = "Tags must be at most 500 characters")
    @Schema(description = "Tags (comma-separated)", example = "vip,enterprise")
    private String tags;

    @Schema(description = "Notes")
    private String notes;

    @Schema(description = "Rating (1-5)", example = "5")
    private Integer rating;

    @Schema(description = "Is VIP customer", example = "false")
    private Boolean isVip;
}
