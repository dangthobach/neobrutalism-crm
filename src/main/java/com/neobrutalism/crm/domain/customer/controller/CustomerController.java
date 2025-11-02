package com.neobrutalism.crm.domain.customer.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.domain.customer.dto.CustomerRequest;
import com.neobrutalism.crm.domain.customer.dto.CustomerResponse;
import com.neobrutalism.crm.domain.customer.model.Customer;
import com.neobrutalism.crm.domain.customer.model.CustomerStatus;
import com.neobrutalism.crm.domain.customer.model.CustomerType;
import com.neobrutalism.crm.domain.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Customer management
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management APIs")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Get all customers", description = "Retrieve all customers with pagination")
    public ApiResponse<PageResponse<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "companyName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Customer> customerPage = customerService.findAllActive(pageable);
        Page<CustomerResponse> responsePage = customerPage.map(CustomerResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieve a specific customer by its ID")
    public ApiResponse<CustomerResponse> getCustomerById(@PathVariable UUID id) {
        Customer customer = customerService.findById(id);
        return ApiResponse.success(CustomerResponse.from(customer));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get customer by code", description = "Retrieve a specific customer by its unique code")
    public ApiResponse<CustomerResponse> getCustomerByCode(@PathVariable String code) {
        Customer customer = customerService.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Customer not found with code: " + code));
        return ApiResponse.success(CustomerResponse.from(customer));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get customer by email", description = "Retrieve a specific customer by email")
    public ApiResponse<CustomerResponse> getCustomerByEmail(@PathVariable String email) {
        Customer customer = customerService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found with email: " + email));
        return ApiResponse.success(CustomerResponse.from(customer));
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get customers by organization", description = "Retrieve all customers for a specific organization")
    public ApiResponse<List<CustomerResponse>> getCustomersByOrganization(@PathVariable UUID organizationId) {
        List<Customer> customers = customerService.findByOrganizationId(organizationId);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get customers by owner", description = "Retrieve all customers managed by a specific user")
    public ApiResponse<List<CustomerResponse>> getCustomersByOwner(@PathVariable UUID ownerId) {
        List<Customer> customers = customerService.findByOwnerId(ownerId);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get customers by branch", description = "Retrieve all customers for a specific branch")
    public ApiResponse<List<CustomerResponse>> getCustomersByBranch(@PathVariable UUID branchId) {
        List<Customer> customers = customerService.findByBranchId(branchId);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get customers by type", description = "Retrieve customers by customer type")
    public ApiResponse<List<CustomerResponse>> getCustomersByType(@PathVariable CustomerType type) {
        List<Customer> customers = customerService.findByCustomerType(type);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get customers by status", description = "Retrieve all customers with a specific status")
    public ApiResponse<List<CustomerResponse>> getCustomersByStatus(@PathVariable CustomerStatus status) {
        List<Customer> customers = customerService.findByStatus(status);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/vip")
    @Operation(summary = "Get VIP customers", description = "Retrieve all VIP customers")
    public ApiResponse<List<CustomerResponse>> getVipCustomers() {
        List<Customer> customers = customerService.findVipCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers by company name", description = "Search customers by company name keyword")
    public ApiResponse<List<CustomerResponse>> searchCustomers(@RequestParam String keyword) {
        List<Customer> customers = customerService.searchByCompanyName(keyword);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/acquisition")
    @Operation(summary = "Get customers by acquisition date range", description = "Retrieve customers acquired within a date range")
    public ApiResponse<List<CustomerResponse>> getCustomersByAcquisitionDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Customer> customers = customerService.findByAcquisitionDateBetween(startDate, endDate);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/followup")
    @Operation(summary = "Get customers requiring follow-up", description = "Retrieve customers that require follow-up by a specific date")
    public ApiResponse<List<CustomerResponse>> getCustomersRequiringFollowup(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam CustomerStatus status) {
        List<Customer> customers = customerService.findRequiringFollowup(date, status);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/tag/{tag}")
    @Operation(summary = "Get customers by tag", description = "Retrieve customers with a specific tag")
    public ApiResponse<List<CustomerResponse>> getCustomersByTag(@PathVariable String tag) {
        List<Customer> customers = customerService.findByTag(tag);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/lead-source/{leadSource}")
    @Operation(summary = "Get customers by lead source", description = "Retrieve customers from a specific lead source")
    public ApiResponse<List<CustomerResponse>> getCustomersByLeadSource(@PathVariable String leadSource) {
        List<Customer> customers = customerService.findByLeadSource(leadSource);
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create customer", description = "Create a new customer")
    public ApiResponse<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        Customer customer = mapToEntity(request);
        Customer created = customerService.create(customer);
        return ApiResponse.success("Customer created successfully", CustomerResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Update an existing customer")
    public ApiResponse<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        Customer customer = mapToEntity(request);
        Customer updated = customerService.update(id, customer);
        return ApiResponse.success("Customer updated successfully", CustomerResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer", description = "Soft delete a customer")
    public ApiResponse<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteById(id);
        return ApiResponse.success("Customer deleted successfully");
    }

    @PostMapping("/{id}/convert-to-prospect")
    @Operation(summary = "Convert to prospect", description = "Convert lead to prospect")
    public ApiResponse<CustomerResponse> convertToProspect(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Customer customer = customerService.convertToProspect(id, reason);
        return ApiResponse.success("Customer converted to prospect", CustomerResponse.from(customer));
    }

    @PostMapping("/{id}/convert-to-active")
    @Operation(summary = "Convert to active", description = "Convert prospect to active customer")
    public ApiResponse<CustomerResponse> convertToActive(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Customer customer = customerService.convertToActive(id, reason);
        return ApiResponse.success("Customer converted to active", CustomerResponse.from(customer));
    }

    @PostMapping("/{id}/mark-inactive")
    @Operation(summary = "Mark as inactive", description = "Mark customer as inactive")
    public ApiResponse<CustomerResponse> markInactive(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Customer customer = customerService.markInactive(id, reason);
        return ApiResponse.success("Customer marked as inactive", CustomerResponse.from(customer));
    }

    @PostMapping("/{id}/mark-churned")
    @Operation(summary = "Mark as churned", description = "Mark customer as churned")
    public ApiResponse<CustomerResponse> markChurned(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Customer customer = customerService.markChurned(id, reason);
        return ApiResponse.success("Customer marked as churned", CustomerResponse.from(customer));
    }

    @PostMapping("/{id}/blacklist")
    @Operation(summary = "Blacklist customer", description = "Blacklist a customer")
    public ApiResponse<CustomerResponse> blacklist(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Customer customer = customerService.blacklist(id, reason);
        return ApiResponse.success("Customer blacklisted", CustomerResponse.from(customer));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate customer", description = "Reactivate an inactive or churned customer")
    public ApiResponse<CustomerResponse> reactivate(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Customer customer = customerService.reactivate(id, reason);
        return ApiResponse.success("Customer reactivated", CustomerResponse.from(customer));
    }

    @PostMapping("/{id}/update-contact-date")
    @Operation(summary = "Update last contact date", description = "Update the last contact date to now")
    public ApiResponse<CustomerResponse> updateLastContactDate(@PathVariable UUID id) {
        Customer customer = customerService.updateLastContactDate(id);
        return ApiResponse.success("Last contact date updated", CustomerResponse.from(customer));
    }

    @GetMapping("/stats/by-status")
    @Operation(summary = "Get customer count by status", description = "Get the count of customers for each status")
    public ApiResponse<Long> countByStatus(@RequestParam CustomerStatus status) {
        long count = customerService.countByStatus(status);
        return ApiResponse.success(count);
    }

    @GetMapping("/stats/by-type")
    @Operation(summary = "Get customer count by type", description = "Get the count of customers for each type")
    public ApiResponse<Long> countByType(@RequestParam CustomerType type) {
        long count = customerService.countByType(type);
        return ApiResponse.success(count);
    }

    /**
     * Map request DTO to entity
     */
    private Customer mapToEntity(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setCode(request.getCode());
        customer.setCompanyName(request.getCompanyName());
        customer.setLegalName(request.getLegalName());
        customer.setCustomerType(request.getCustomerType());
        customer.setIndustry(request.getIndustry());
        customer.setTaxId(request.getTaxId());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setWebsite(request.getWebsite());
        customer.setBillingAddress(request.getBillingAddress());
        customer.setShippingAddress(request.getShippingAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setCountry(request.getCountry());
        customer.setPostalCode(request.getPostalCode());
        customer.setOwnerId(request.getOwnerId());
        customer.setBranchId(request.getBranchId());
        customer.setOrganizationId(request.getOrganizationId());
        customer.setAnnualRevenue(request.getAnnualRevenue());
        customer.setEmployeeCount(request.getEmployeeCount());
        customer.setAcquisitionDate(request.getAcquisitionDate());
        customer.setLastContactDate(request.getLastContactDate());
        customer.setNextFollowupDate(request.getNextFollowupDate());
        customer.setLeadSource(request.getLeadSource());
        customer.setCreditLimit(request.getCreditLimit());
        customer.setPaymentTermsDays(request.getPaymentTermsDays());
        customer.setTags(request.getTags());
        customer.setNotes(request.getNotes());
        customer.setRating(request.getRating());
        customer.setIsVip(request.getIsVip());
        return customer;
    }
}
