package com.neobrutalism.crm.domain.customer.model;

import com.neobrutalism.crm.common.entity.TenantAwareAggregateRoot;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Customer Entity - Company or organization customer
 */
@Getter
@Setter
@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(name = "idx_customer_code", columnList = "code"),
        @Index(name = "idx_customer_email", columnList = "email"),
        @Index(name = "idx_customer_status", columnList = "status"),
        @Index(name = "idx_customer_type", columnList = "customer_type"),
        @Index(name = "idx_customer_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_customer_owner_id", columnList = "owner_id"),
        @Index(name = "idx_customer_deleted_id", columnList = "deleted, id")
    }
)
public class Customer extends TenantAwareAggregateRoot<CustomerStatus> {

    /**
     * Customer code (unique within organization)
     */
    @NotBlank(message = "Customer code is required")
    @Size(max = 50, message = "Customer code must be at most 50 characters")
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /**
     * Company name
     */
    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must be at most 255 characters")
    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    /**
     * Legal name (if different from company name)
     */
    @Size(max = 255, message = "Legal name must be at most 255 characters")
    @Column(name = "legal_name", length = 255)
    private String legalName;

    /**
     * Customer type (B2B, B2C, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    private CustomerType customerType = CustomerType.B2B;

    /**
     * Customer status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.LEAD;

    /**
     * Industry
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "industry", length = 50)
    private Industry industry;

    /**
     * Tax ID / VAT Number
     */
    @Size(max = 50, message = "Tax ID must be at most 50 characters")
    @Column(name = "tax_id", length = 50)
    private String taxId;

    /**
     * Primary email
     */
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Primary phone
     */
    @Size(max = 50, message = "Phone must be at most 50 characters")
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Website
     */
    @Size(max = 255, message = "Website must be at most 255 characters")
    @Column(name = "website", length = 255)
    private String website;

    /**
     * Billing address
     */
    @Size(max = 500, message = "Billing address must be at most 500 characters")
    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    /**
     * Shipping address
     */
    @Size(max = 500, message = "Shipping address must be at most 500 characters")
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    /**
     * City
     */
    @Size(max = 100, message = "City must be at most 100 characters")
    @Column(name = "city", length = 100)
    private String city;

    /**
     * State/Province
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
     * Owner user ID - Account manager/sales rep
     */
    @Column(name = "owner_id")
    private UUID ownerId;

    /**
     * Branch ID
     */
    @Column(name = "branch_id")
    private UUID branchId;

    /**
     * Organization ID
     */
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /**
     * Annual revenue
     */
    @Column(name = "annual_revenue", precision = 19, scale = 2)
    private BigDecimal annualRevenue;

    /**
     * Number of employees
     */
    @Column(name = "employee_count")
    private Integer employeeCount;

    /**
     * Date customer was acquired
     */
    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    /**
     * Last contact date
     */
    @Column(name = "last_contact_date")
    private LocalDate lastContactDate;

    /**
     * Next follow-up date
     */
    @Column(name = "next_followup_date")
    private LocalDate nextFollowupDate;

    /**
     * Lead source
     */
    @Size(max = 100, message = "Lead source must be at most 100 characters")
    @Column(name = "lead_source", length = 100)
    private String leadSource;

    /**
     * Credit limit
     */
    @Column(name = "credit_limit", precision = 19, scale = 2)
    private BigDecimal creditLimit;

    /**
     * Payment terms in days
     */
    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    /**
     * Tags (comma-separated)
     */
    @Size(max = 500, message = "Tags must be at most 500 characters")
    @Column(name = "tags", length = 500)
    private String tags;

    /**
     * Notes
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Rating (1-5 stars)
     */
    @Column(name = "rating")
    private Integer rating;

    /**
     * Is VIP customer
     */
    @Column(name = "is_vip")
    private Boolean isVip = false;

    @Override
    protected Set<CustomerStatus> getAllowedTransitions(CustomerStatus currentStatus) {
        return switch (currentStatus) {
            case LEAD -> Set.of(CustomerStatus.PROSPECT, CustomerStatus.INACTIVE, CustomerStatus.BLACKLISTED);
            case PROSPECT -> Set.of(CustomerStatus.ACTIVE, CustomerStatus.INACTIVE, CustomerStatus.BLACKLISTED);
            case ACTIVE -> Set.of(CustomerStatus.INACTIVE, CustomerStatus.CHURNED, CustomerStatus.BLACKLISTED);
            case INACTIVE -> Set.of(CustomerStatus.ACTIVE, CustomerStatus.CHURNED, CustomerStatus.BLACKLISTED);
            case CHURNED -> Set.of(CustomerStatus.PROSPECT, CustomerStatus.ACTIVE);
            case BLACKLISTED -> Set.of(); // Cannot transition from blacklisted
        };
    }

    @Override
    protected CustomerStatus getInitialStatus() {
        return CustomerStatus.LEAD;
    }

    @PrePersist
    @PreUpdate
    public void validateCustomer() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (companyName != null) {
            companyName = companyName.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
