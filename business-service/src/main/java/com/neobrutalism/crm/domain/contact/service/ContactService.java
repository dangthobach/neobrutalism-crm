package com.neobrutalism.crm.domain.contact.service;

import com.neobrutalism.crm.common.exception.ValidationException;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.contact.model.Contact;
import com.neobrutalism.crm.domain.contact.model.ContactRole;
import com.neobrutalism.crm.domain.contact.model.ContactStatus;
import com.neobrutalism.crm.domain.contact.repository.ContactRepository;
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
 * Service for Contact management
 */
@Slf4j
@Service
public class ContactService extends BaseService<Contact> {

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Override
    protected ContactRepository getRepository() {
        return contactRepository;
    }

    @Override
    protected String getEntityName() {
        return "Contact";
    }

    /**
     * Create a new contact
     */
    @Transactional
    public Contact create(Contact contact) {
        log.info("Creating new contact: {} {}", contact.getFirstName(), contact.getLastName());

        // Set tenant ID if not set
        if (contact.getTenantId() == null) {
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr != null) {
                contact.setTenantId(tenantIdStr);
            }
        }

        // Validate email uniqueness
        if (contactRepository.existsByEmail(contact.getEmail())) {
            throw new ValidationException("Contact with this email already exists: " + contact.getEmail());
        }

        // Set initial status if not set
        if (contact.getStatus() == null) {
            contact.setStatus(ContactStatus.ACTIVE);
        }

        // If this is marked as primary contact, unset other primary contacts for the same customer
        if (Boolean.TRUE.equals(contact.getIsPrimary()) && contact.getCustomerId() != null) {
            unsetPrimaryContactForCustomer(contact.getCustomerId());
        }

        return contactRepository.save(contact);
    }

    /**
     * Update existing contact
     */
    @Transactional
    public Contact update(UUID id, Contact updatedContact) {
        log.info("Updating contact: {}", id);

        Contact existingContact = findById(id);

        // Validate email uniqueness (excluding current contact)
        if (!existingContact.getEmail().equals(updatedContact.getEmail())) {
            if (contactRepository.existsByEmailExcluding(updatedContact.getEmail(), id)) {
                throw new ValidationException("Contact with this email already exists: " + updatedContact.getEmail());
            }
        }

        // Update fields
        existingContact.setCustomerId(updatedContact.getCustomerId());
        existingContact.setFirstName(updatedContact.getFirstName());
        existingContact.setLastName(updatedContact.getLastName());
        existingContact.setMiddleName(updatedContact.getMiddleName());
        existingContact.setTitle(updatedContact.getTitle());
        existingContact.setDepartment(updatedContact.getDepartment());
        existingContact.setContactRole(updatedContact.getContactRole());
        existingContact.setEmail(updatedContact.getEmail());
        existingContact.setSecondaryEmail(updatedContact.getSecondaryEmail());
        existingContact.setWorkPhone(updatedContact.getWorkPhone());
        existingContact.setMobilePhone(updatedContact.getMobilePhone());
        existingContact.setHomePhone(updatedContact.getHomePhone());
        existingContact.setFax(updatedContact.getFax());
        existingContact.setLinkedinUrl(updatedContact.getLinkedinUrl());
        existingContact.setTwitterHandle(updatedContact.getTwitterHandle());
        existingContact.setMailingAddress(updatedContact.getMailingAddress());
        existingContact.setCity(updatedContact.getCity());
        existingContact.setState(updatedContact.getState());
        existingContact.setCountry(updatedContact.getCountry());
        existingContact.setPostalCode(updatedContact.getPostalCode());
        existingContact.setOwnerId(updatedContact.getOwnerId());
        existingContact.setBirthDate(updatedContact.getBirthDate());
        existingContact.setPreferredContactMethod(updatedContact.getPreferredContactMethod());
        existingContact.setPreferredContactTime(updatedContact.getPreferredContactTime());
        existingContact.setAssistantName(updatedContact.getAssistantName());
        existingContact.setAssistantPhone(updatedContact.getAssistantPhone());
        existingContact.setReportsToId(updatedContact.getReportsToId());
        existingContact.setEmailOptOut(updatedContact.getEmailOptOut());
        existingContact.setLastContactDate(updatedContact.getLastContactDate());
        existingContact.setNotes(updatedContact.getNotes());
        existingContact.setTags(updatedContact.getTags());

        // Handle primary contact flag
        if (Boolean.TRUE.equals(updatedContact.getIsPrimary()) &&
            !Boolean.TRUE.equals(existingContact.getIsPrimary()) &&
            updatedContact.getCustomerId() != null) {
            unsetPrimaryContactForCustomer(updatedContact.getCustomerId());
        }
        existingContact.setIsPrimary(updatedContact.getIsPrimary());

        return contactRepository.save(existingContact);
    }

    /**
     * Unset primary contact flag for all contacts of a customer
     */
    @Transactional
    protected void unsetPrimaryContactForCustomer(UUID customerId) {
        Optional<Contact> currentPrimary = contactRepository.findPrimaryContactByCustomerId(customerId);
        if (currentPrimary.isPresent()) {
            Contact contact = currentPrimary.get();
            contact.setIsPrimary(false);
            contactRepository.save(contact);
        }
    }

    /**
     * Find contact by email
     */
    public Optional<Contact> findByEmail(String email) {
        return contactRepository.findByEmail(email);
    }

    /**
     * Find all contacts by customer
     */
    public List<Contact> findByCustomerId(UUID customerId) {
        return contactRepository.findByCustomerId(customerId);
    }

    /**
     * Find primary contact for customer
     */
    public Optional<Contact> findPrimaryContactByCustomerId(UUID customerId) {
        return contactRepository.findPrimaryContactByCustomerId(customerId);
    }

    /**
     * Find all contacts by owner
     */
    public List<Contact> findByOwnerId(UUID ownerId) {
        return contactRepository.findByOwnerId(ownerId);
    }

    /**
     * Find all contacts by organization
     */
    public List<Contact> findByOrganizationId(UUID organizationId) {
        return contactRepository.findByOrganizationId(organizationId);
    }

    /**
     * Find contacts by role
     */
    public List<Contact> findByContactRole(ContactRole role) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        return contactRepository.findByContactRole(role, tenantIdStr);
    }

    /**
     * Find contacts by status
     */
    public List<Contact> findByStatus(ContactStatus status) {
        return contactRepository.findByStatus(status);
    }

    /**
     * Find contacts by status with pagination
     */
    public Page<Contact> findByStatus(ContactStatus status, Pageable pageable) {
        return contactRepository.findByStatus(status, pageable);
    }

    /**
     * Search contacts by name
     */
    public List<Contact> searchByName(String keyword) {
        return contactRepository.searchByName(keyword);
    }

    /**
     * Find contacts by email domain
     */
    public List<Contact> findByEmailDomain(String domain) {
        return contactRepository.findByEmailDomain(domain);
    }

    /**
     * Find contacts by tag
     */
    public List<Contact> findByTag(String tag) {
        return contactRepository.findByTag(tag);
    }

    /**
     * Find contacts requiring follow-up
     */
    public List<Contact> findRequiringFollowup(LocalDate sinceDate) {
        return contactRepository.findRequiringFollowup(sinceDate);
    }

    /**
     * Find contacts who opted out of email
     */
    public List<Contact> findEmailOptOuts() {
        return contactRepository.findEmailOptOuts();
    }

    /**
     * Find contacts by status and customer
     */
    public List<Contact> findByCustomerIdAndStatus(UUID customerId, ContactStatus status) {
        return contactRepository.findByCustomerIdAndStatus(customerId, status);
    }

    /**
     * Find contacts reporting to another contact
     */
    public List<Contact> findByReportsToId(UUID reportsToId) {
        return contactRepository.findByReportsToId(reportsToId);
    }

    /**
     * Mark contact as inactive
     */
    @Transactional
    public Contact markInactive(UUID contactId, String reason) {
        log.info("Marking contact {} as INACTIVE", contactId);
        Contact contact = findById(contactId);
        String currentUser = TenantContext.getCurrentTenant();
        contact.transitionTo(ContactStatus.INACTIVE, currentUser, reason);
        return contactRepository.save(contact);
    }

    /**
     * Mark contact as left company
     */
    @Transactional
    public Contact markLeftCompany(UUID contactId, String reason) {
        log.info("Marking contact {} as LEFT_COMPANY", contactId);
        Contact contact = findById(contactId);
        String currentUser = TenantContext.getCurrentTenant();
        contact.transitionTo(ContactStatus.LEFT_COMPANY, currentUser, reason);
        return contactRepository.save(contact);
    }

    /**
     * Mark contact as do not contact
     */
    @Transactional
    public Contact markDoNotContact(UUID contactId, String reason) {
        log.info("Marking contact {} as DO_NOT_CONTACT", contactId);
        Contact contact = findById(contactId);
        String currentUser = TenantContext.getCurrentTenant();
        contact.transitionTo(ContactStatus.DO_NOT_CONTACT, currentUser, reason);
        return contactRepository.save(contact);
    }

    /**
     * Reactivate contact
     */
    @Transactional
    public Contact reactivate(UUID contactId, String reason) {
        log.info("Reactivating contact {}", contactId);
        Contact contact = findById(contactId);
        String currentUser = TenantContext.getCurrentTenant();
        contact.transitionTo(ContactStatus.ACTIVE, currentUser, reason);
        return contactRepository.save(contact);
    }

    /**
     * Update last contact date
     */
    @Transactional
    public Contact updateLastContactDate(UUID contactId) {
        Contact contact = findById(contactId);
        contact.setLastContactDate(LocalDate.now());
        return contactRepository.save(contact);
    }

    /**
     * Set as primary contact
     */
    @Transactional
    public Contact setAsPrimary(UUID contactId) {
        Contact contact = findById(contactId);

        if (contact.getCustomerId() == null) {
            throw new ValidationException("Cannot set as primary: contact is not associated with a customer");
        }

        // Unset current primary
        unsetPrimaryContactForCustomer(contact.getCustomerId());

        // Set this as primary
        contact.setIsPrimary(true);
        return contactRepository.save(contact);
    }

    /**
     * Count contacts by customer
     */
    public long countByCustomerId(UUID customerId) {
        return contactRepository.countByCustomerId(customerId);
    }

    /**
     * Count contacts by status
     */
    public long countByStatus(ContactStatus status) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        return contactRepository.countByStatusAndTenantId(status, tenantIdStr);
    }

    /**
     * Get all active contacts
     */
    public List<Contact> findAllActive() {
        return contactRepository.findByStatus(ContactStatus.ACTIVE);
    }

    /**
     * Get all active contacts with pagination
     * ✅ Optimized: Uses optimized query to prevent N+1
     */
    public Page<Contact> findAllActive(Pageable pageable) {
        return contactRepository.findAllActiveOptimized(pageable);
    }

    @Transactional
    public void deleteById(UUID id) {
        log.info("Soft deleting contact: {}", id);
        Contact contact = findById(id);
        contact.setDeleted(true);
        contactRepository.save(contact);
    }
}
