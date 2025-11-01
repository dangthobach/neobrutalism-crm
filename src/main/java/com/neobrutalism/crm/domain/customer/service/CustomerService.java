package com.neobrutalism.crm.domain.customer.service;

import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.exception.ValidationException;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.customer.model.Customer;
import com.neobrutalism.crm.domain.customer.model.CustomerStatus;
import com.neobrutalism.crm.domain.customer.model.CustomerType;
import com.neobrutalism.crm.domain.customer.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Customer management
 */
@Slf4j
@Service
public class CustomerService extends BaseService<Customer> {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    protected CustomerRepository getRepository() {
        return customerRepository;
    }

    @Override
    protected String getEntityName() {
        return "Customer";
    }

    /**
     * Create a new customer
     */
    @Transactional
    public Customer create(Customer customer) {
        log.info("Creating new customer: {}", customer.getCode());

        // Set tenant ID if not set
        if (customer.getTenantId() == null) {
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr != null) {
                customer.setTenantId(tenantIdStr);
            }
        }

        // Validate customer code uniqueness
        if (customerRepository.existsByCodeAndOrganizationId(customer.getCode(), customer.getOrganizationId())) {
            throw new ValidationException("Customer code already exists in this organization: " + customer.getCode());
        }

        // Set initial status if not set
        if (customer.getStatus() == null) {
            customer.setStatus(CustomerStatus.LEAD);
        }

        // Set acquisition date if becoming active
        if (customer.getStatus() == CustomerStatus.ACTIVE && customer.getAcquisitionDate() == null) {
            customer.setAcquisitionDate(LocalDate.now());
        }

        return customerRepository.save(customer);
    }

    /**
     * Update existing customer
     */
    @Transactional
    public Customer update(UUID id, Customer updatedCustomer) {
        log.info("Updating customer: {}", id);

        Customer existingCustomer = findById(id);

        // Validate code uniqueness (excluding current customer)
        if (!existingCustomer.getCode().equals(updatedCustomer.getCode())) {
            if (customerRepository.existsByCodeAndOrganizationIdExcluding(
                    updatedCustomer.getCode(),
                    existingCustomer.getOrganizationId(),
                    id)) {
                throw new ValidationException("Customer code already exists: " + updatedCustomer.getCode());
            }
        }

        // Update fields
        existingCustomer.setCode(updatedCustomer.getCode());
        existingCustomer.setCompanyName(updatedCustomer.getCompanyName());
        existingCustomer.setLegalName(updatedCustomer.getLegalName());
        existingCustomer.setCustomerType(updatedCustomer.getCustomerType());
        existingCustomer.setIndustry(updatedCustomer.getIndustry());
        existingCustomer.setTaxId(updatedCustomer.getTaxId());
        existingCustomer.setEmail(updatedCustomer.getEmail());
        existingCustomer.setPhone(updatedCustomer.getPhone());
        existingCustomer.setWebsite(updatedCustomer.getWebsite());
        existingCustomer.setBillingAddress(updatedCustomer.getBillingAddress());
        existingCustomer.setShippingAddress(updatedCustomer.getShippingAddress());
        existingCustomer.setCity(updatedCustomer.getCity());
        existingCustomer.setState(updatedCustomer.getState());
        existingCustomer.setCountry(updatedCustomer.getCountry());
        existingCustomer.setPostalCode(updatedCustomer.getPostalCode());
        existingCustomer.setOwnerId(updatedCustomer.getOwnerId());
        existingCustomer.setBranchId(updatedCustomer.getBranchId());
        existingCustomer.setAnnualRevenue(updatedCustomer.getAnnualRevenue());
        existingCustomer.setEmployeeCount(updatedCustomer.getEmployeeCount());
        existingCustomer.setAcquisitionDate(updatedCustomer.getAcquisitionDate());
        existingCustomer.setLastContactDate(updatedCustomer.getLastContactDate());
        existingCustomer.setNextFollowupDate(updatedCustomer.getNextFollowupDate());
        existingCustomer.setLeadSource(updatedCustomer.getLeadSource());
        existingCustomer.setCreditLimit(updatedCustomer.getCreditLimit());
        existingCustomer.setPaymentTermsDays(updatedCustomer.getPaymentTermsDays());
        existingCustomer.setTags(updatedCustomer.getTags());
        existingCustomer.setNotes(updatedCustomer.getNotes());
        existingCustomer.setRating(updatedCustomer.getRating());
        existingCustomer.setIsVip(updatedCustomer.getIsVip());

        return customerRepository.save(existingCustomer);
    }

    /**
     * Find customer by code
     */
    public Optional<Customer> findByCode(String code) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        return customerRepository.findByCodeAndTenantId(code, tenantIdStr);
    }

    /**
     * Find customer by email
     */
    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    /**
     * Find all customers by organization
     */
    public List<Customer> findByOrganizationId(UUID organizationId) {
        return customerRepository.findByOrganizationId(organizationId);
    }

    /**
     * Find all customers by owner (account manager)
     */
    public List<Customer> findByOwnerId(UUID ownerId) {
        return customerRepository.findByOwnerId(ownerId);
    }

    /**
     * Find all customers by branch
     */
    public List<Customer> findByBranchId(UUID branchId) {
        return customerRepository.findByBranchId(branchId);
    }

    /**
     * Find customers by type
     */
    public List<Customer> findByCustomerType(CustomerType type) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        return customerRepository.findByCustomerType(type, tenantIdStr);
    }

    /**
     * Find customers by status
     */
    public List<Customer> findByStatus(CustomerStatus status) {
        return customerRepository.findByStatus(status);
    }

    /**
     * Find customers by status with pagination
     */
    public Page<Customer> findByStatus(CustomerStatus status, Pageable pageable) {
        return customerRepository.findByStatus(status, pageable);
    }

    /**
     * Find VIP customers
     */
    public List<Customer> findVipCustomers() {
        String tenantIdStr = TenantContext.getCurrentTenant();
        return customerRepository.findVipCustomers(tenantIdStr);
    }

    /**
     * Search customers by company name
     */
    public List<Customer> searchByCompanyName(String keyword) {
        return customerRepository.searchByCompanyName(keyword);
    }

    /**
     * Find customers acquired within date range
     */
    public List<Customer> findByAcquisitionDateBetween(LocalDate startDate, LocalDate endDate) {
        return customerRepository.findByAcquisitionDateBetween(startDate, endDate);
    }

    /**
     * Find customers requiring follow-up
     */
    public List<Customer> findRequiringFollowup(LocalDate date, CustomerStatus status) {
        return customerRepository.findRequiringFollowup(date, status);
    }

    /**
     * Find customers by tag
     */
    public List<Customer> findByTag(String tag) {
        return customerRepository.findByTag(tag);
    }

    /**
     * Find customers by lead source
     */
    public List<Customer> findByLeadSource(String leadSource) {
        return customerRepository.findByLeadSource(leadSource);
    }

    /**
     * Get customers with no recent contact
     */
    public List<Customer> findWithNoRecentContact(LocalDate sinceDate, List<CustomerStatus> statuses) {
        return customerRepository.findWithNoRecentContact(sinceDate, statuses);
    }

    /**
     * Convert lead to prospect
     */
    @Transactional
    public Customer convertToProspect(UUID customerId, String reason) {
        log.info("Converting customer {} to PROSPECT", customerId);
        Customer customer = findById(customerId);
        String currentUser = TenantContext.getCurrentTenant();
        customer.transitionTo(CustomerStatus.PROSPECT, currentUser, reason);
        return customerRepository.save(customer);
    }

    /**
     * Convert prospect to active customer
     */
    @Transactional
    public Customer convertToActive(UUID customerId, String reason) {
        log.info("Converting customer {} to ACTIVE", customerId);
        Customer customer = findById(customerId);

        // Set acquisition date if not already set
        if (customer.getAcquisitionDate() == null) {
            customer.setAcquisitionDate(LocalDate.now());
        }

        String currentUser = TenantContext.getCurrentTenant();
        customer.transitionTo(CustomerStatus.ACTIVE, currentUser, reason);
        return customerRepository.save(customer);
    }

    /**
     * Mark customer as inactive
     */
    @Transactional
    public Customer markInactive(UUID customerId, String reason) {
        log.info("Marking customer {} as INACTIVE", customerId);
        Customer customer = findById(customerId);
        String currentUser = TenantContext.getCurrentTenant();
        customer.transitionTo(CustomerStatus.INACTIVE, currentUser, reason);
        return customerRepository.save(customer);
    }

    /**
     * Mark customer as churned
     */
    @Transactional
    public Customer markChurned(UUID customerId, String reason) {
        log.info("Marking customer {} as CHURNED", customerId);
        Customer customer = findById(customerId);
        String currentUser = TenantContext.getCurrentTenant();
        customer.transitionTo(CustomerStatus.CHURNED, currentUser, reason);
        return customerRepository.save(customer);
    }

    /**
     * Blacklist customer
     */
    @Transactional
    public Customer blacklist(UUID customerId, String reason) {
        log.info("Blacklisting customer {}", customerId);
        Customer customer = findById(customerId);
        String currentUser = TenantContext.getCurrentTenant();
        customer.transitionTo(CustomerStatus.BLACKLISTED, currentUser, reason);
        return customerRepository.save(customer);
    }

    /**
     * Reactivate customer
     */
    @Transactional
    public Customer reactivate(UUID customerId, String reason) {
        log.info("Reactivating customer {}", customerId);
        Customer customer = findById(customerId);
        String currentUser = TenantContext.getCurrentTenant();
        customer.transitionTo(CustomerStatus.ACTIVE, currentUser, reason);
        return customerRepository.save(customer);
    }

    /**
     * Update last contact date
     */
    @Transactional
    public Customer updateLastContactDate(UUID customerId) {
        Customer customer = findById(customerId);
        customer.setLastContactDate(LocalDate.now());
        return customerRepository.save(customer);
    }

    /**
     * Count customers by status
     */
    public long countByStatus(CustomerStatus status) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        return customerRepository.countByStatusAndTenantId(status, tenantIdStr);
    }

    /**
     * Count customers by type
     */
    public long countByType(CustomerType type) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        return customerRepository.countByTypeAndTenantId(type, tenantIdStr);
    }

    /**
     * Get all active customers
     */
    public List<Customer> findAllActive() {
        return customerRepository.findByStatus(CustomerStatus.ACTIVE);
    }

    /**
     * Get all active customers with pagination
     */
    public Page<Customer> findAllActive(Pageable pageable) {
        return customerRepository.findByStatus(CustomerStatus.ACTIVE, pageable);
    }

    @Transactional
    public void deleteById(UUID id) {
        log.info("Soft deleting customer: {}", id);
        Customer customer = findById(id);
        customer.setDeleted(true);
        customerRepository.save(customer);
    }
}
