package com.neobrutalism.crm.domain.customer.dto;

import com.neobrutalism.crm.domain.customer.model.Customer;
import com.neobrutalism.crm.domain.customer.model.CustomerStatus;
import com.neobrutalism.crm.domain.customer.model.CustomerType;
import com.neobrutalism.crm.domain.customer.model.Industry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Customer entity
 */
@Data
@Builder
@Schema(description = "Customer response data")
public class CustomerResponse {

    @Schema(description = "Customer ID")
    private UUID id;

    @Schema(description = "Customer code")
    private String code;

    @Schema(description = "Company name")
    private String companyName;

    @Schema(description = "Legal name")
    private String legalName;

    @Schema(description = "Customer type")
    private CustomerType customerType;

    @Schema(description = "Customer status")
    private CustomerStatus status;

    @Schema(description = "Industry")
    private Industry industry;

    @Schema(description = "Tax ID")
    private String taxId;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Phone")
    private String phone;

    @Schema(description = "Website")
    private String website;

    @Schema(description = "Billing address")
    private String billingAddress;

    @Schema(description = "Shipping address")
    private String shippingAddress;

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

    @Schema(description = "Branch ID")
    private UUID branchId;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Annual revenue")
    private BigDecimal annualRevenue;

    @Schema(description = "Employee count")
    private Integer employeeCount;

    @Schema(description = "Acquisition date")
    private LocalDate acquisitionDate;

    @Schema(description = "Last contact date")
    private LocalDate lastContactDate;

    @Schema(description = "Next follow-up date")
    private LocalDate nextFollowupDate;

    @Schema(description = "Lead source")
    private String leadSource;

    @Schema(description = "Credit limit")
    private BigDecimal creditLimit;

    @Schema(description = "Payment terms days")
    private Integer paymentTermsDays;

    @Schema(description = "Tags")
    private String tags;

    @Schema(description = "Notes")
    private String notes;

    @Schema(description = "Rating")
    private Integer rating;

    @Schema(description = "Is VIP")
    private Boolean isVip;

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
     * Convert Customer entity to response DTO
     */
    public static CustomerResponse from(Customer customer) {
        if (customer == null) {
            return null;
        }

        return CustomerResponse.builder()
                .id(customer.getId())
                .code(customer.getCode())
                .companyName(customer.getCompanyName())
                .legalName(customer.getLegalName())
                .customerType(customer.getCustomerType())
                .status(customer.getStatus())
                .industry(customer.getIndustry())
                .taxId(customer.getTaxId())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .website(customer.getWebsite())
                .billingAddress(customer.getBillingAddress())
                .shippingAddress(customer.getShippingAddress())
                .city(customer.getCity())
                .state(customer.getState())
                .country(customer.getCountry())
                .postalCode(customer.getPostalCode())
                .ownerId(customer.getOwnerId())
                .branchId(customer.getBranchId())
                .organizationId(customer.getOrganizationId())
                .annualRevenue(customer.getAnnualRevenue())
                .employeeCount(customer.getEmployeeCount())
                .acquisitionDate(customer.getAcquisitionDate())
                .lastContactDate(customer.getLastContactDate())
                .nextFollowupDate(customer.getNextFollowupDate())
                .leadSource(customer.getLeadSource())
                .creditLimit(customer.getCreditLimit())
                .paymentTermsDays(customer.getPaymentTermsDays())
                .tags(customer.getTags())
                .notes(customer.getNotes())
                .rating(customer.getRating())
                .isVip(customer.getIsVip())
                .tenantId(customer.getTenantId())
                .version(customer.getVersion())
                .createdAt(customer.getCreatedAt())
                .createdBy(customer.getCreatedBy())
                .updatedAt(customer.getUpdatedAt())
                .updatedBy(customer.getUpdatedBy())
                .build();
    }
}
