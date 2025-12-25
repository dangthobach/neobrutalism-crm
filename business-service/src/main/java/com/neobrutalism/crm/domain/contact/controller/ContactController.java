package com.neobrutalism.crm.domain.contact.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.domain.contact.dto.ContactRequest;
import com.neobrutalism.crm.domain.contact.dto.ContactResponse;
import com.neobrutalism.crm.domain.contact.model.Contact;
import com.neobrutalism.crm.domain.contact.model.ContactRole;
import com.neobrutalism.crm.domain.contact.model.ContactStatus;
import com.neobrutalism.crm.domain.contact.service.ContactService;
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
 * REST controller for Contact management
 */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Contact management APIs")
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @Operation(summary = "Get all contacts", description = "Retrieve all contacts with pagination")
    public ApiResponse<PageResponse<ContactResponse>> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Contact> contactPage = contactService.findAllActive(pageable);
        Page<ContactResponse> responsePage = contactPage.map(ContactResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID", description = "Retrieve a specific contact by its ID")
    public ApiResponse<ContactResponse> getContactById(@PathVariable UUID id) {
        Contact contact = contactService.findById(id);
        return ApiResponse.success(ContactResponse.from(contact));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get contact by email", description = "Retrieve a specific contact by email")
    public ApiResponse<ContactResponse> getContactByEmail(@PathVariable String email) {
        Contact contact = contactService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Contact not found with email: " + email));
        return ApiResponse.success(ContactResponse.from(contact));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get contacts by customer", description = "Retrieve all contacts for a specific customer")
    public ApiResponse<List<ContactResponse>> getContactsByCustomer(@PathVariable UUID customerId) {
        List<Contact> contacts = contactService.findByCustomerId(customerId);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/customer/{customerId}/primary")
    @Operation(summary = "Get primary contact for customer", description = "Retrieve the primary contact for a customer")
    public ApiResponse<ContactResponse> getPrimaryContactByCustomer(@PathVariable UUID customerId) {
        Contact contact = contactService.findPrimaryContactByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("No primary contact found for customer: " + customerId));
        return ApiResponse.success(ContactResponse.from(contact));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get contacts by owner", description = "Retrieve all contacts managed by a specific user")
    public ApiResponse<List<ContactResponse>> getContactsByOwner(@PathVariable UUID ownerId) {
        List<Contact> contacts = contactService.findByOwnerId(ownerId);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get contacts by organization", description = "Retrieve all contacts for a specific organization")
    public ApiResponse<List<ContactResponse>> getContactsByOrganization(@PathVariable UUID organizationId) {
        List<Contact> contacts = contactService.findByOrganizationId(organizationId);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get contacts by role", description = "Retrieve contacts by contact role")
    public ApiResponse<List<ContactResponse>> getContactsByRole(@PathVariable ContactRole role) {
        List<Contact> contacts = contactService.findByContactRole(role);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get contacts by status", description = "Retrieve all contacts with a specific status")
    public ApiResponse<List<ContactResponse>> getContactsByStatus(@PathVariable ContactStatus status) {
        List<Contact> contacts = contactService.findByStatus(status);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/search")
    @Operation(summary = "Search contacts by name", description = "Search contacts by name keyword")
    public ApiResponse<List<ContactResponse>> searchContacts(@RequestParam String keyword) {
        List<Contact> contacts = contactService.searchByName(keyword);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/domain/{domain}")
    @Operation(summary = "Get contacts by email domain", description = "Retrieve contacts by email domain")
    public ApiResponse<List<ContactResponse>> getContactsByEmailDomain(@PathVariable String domain) {
        List<Contact> contacts = contactService.findByEmailDomain(domain);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/tag/{tag}")
    @Operation(summary = "Get contacts by tag", description = "Retrieve contacts with a specific tag")
    public ApiResponse<List<ContactResponse>> getContactsByTag(@PathVariable String tag) {
        List<Contact> contacts = contactService.findByTag(tag);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/followup")
    @Operation(summary = "Get contacts requiring follow-up", description = "Retrieve contacts that require follow-up since a specific date")
    public ApiResponse<List<ContactResponse>> getContactsRequiringFollowup(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sinceDate) {
        List<Contact> contacts = contactService.findRequiringFollowup(sinceDate);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/email-optouts")
    @Operation(summary = "Get email opt-outs", description = "Retrieve contacts who opted out of email")
    public ApiResponse<List<ContactResponse>> getEmailOptOuts() {
        List<Contact> contacts = contactService.findEmailOptOuts();
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/reports-to/{reportsToId}")
    @Operation(summary = "Get contacts reporting to", description = "Retrieve contacts reporting to another contact")
    public ApiResponse<List<ContactResponse>> getContactsReportingTo(@PathVariable UUID reportsToId) {
        List<Contact> contacts = contactService.findByReportsToId(reportsToId);
        List<ContactResponse> responses = contacts.stream()
                .map(ContactResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create contact", description = "Create a new contact")
    public ApiResponse<ContactResponse> createContact(@Valid @RequestBody ContactRequest request) {
        Contact contact = mapToEntity(request);
        Contact created = contactService.create(contact);
        return ApiResponse.success("Contact created successfully", ContactResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update contact", description = "Update an existing contact")
    public ApiResponse<ContactResponse> updateContact(
            @PathVariable UUID id,
            @Valid @RequestBody ContactRequest request) {
        Contact contact = mapToEntity(request);
        Contact updated = contactService.update(id, contact);
        return ApiResponse.success("Contact updated successfully", ContactResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete contact", description = "Soft delete a contact")
    public ApiResponse<Void> deleteContact(@PathVariable UUID id) {
        contactService.deleteById(id);
        return ApiResponse.success("Contact deleted successfully");
    }

    @PostMapping("/{id}/mark-inactive")
    @Operation(summary = "Mark as inactive", description = "Mark contact as inactive")
    public ApiResponse<ContactResponse> markInactive(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Contact contact = contactService.markInactive(id, reason);
        return ApiResponse.success("Contact marked as inactive", ContactResponse.from(contact));
    }

    @PostMapping("/{id}/mark-left-company")
    @Operation(summary = "Mark as left company", description = "Mark contact as having left the company")
    public ApiResponse<ContactResponse> markLeftCompany(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Contact contact = contactService.markLeftCompany(id, reason);
        return ApiResponse.success("Contact marked as left company", ContactResponse.from(contact));
    }

    @PostMapping("/{id}/mark-do-not-contact")
    @Operation(summary = "Mark as do not contact", description = "Mark contact as do not contact")
    public ApiResponse<ContactResponse> markDoNotContact(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Contact contact = contactService.markDoNotContact(id, reason);
        return ApiResponse.success("Contact marked as do not contact", ContactResponse.from(contact));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate contact", description = "Reactivate an inactive contact")
    public ApiResponse<ContactResponse> reactivate(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Contact contact = contactService.reactivate(id, reason);
        return ApiResponse.success("Contact reactivated", ContactResponse.from(contact));
    }

    @PostMapping("/{id}/update-contact-date")
    @Operation(summary = "Update last contact date", description = "Update the last contact date to now")
    public ApiResponse<ContactResponse> updateLastContactDate(@PathVariable UUID id) {
        Contact contact = contactService.updateLastContactDate(id);
        return ApiResponse.success("Last contact date updated", ContactResponse.from(contact));
    }

    @PostMapping("/{id}/set-primary")
    @Operation(summary = "Set as primary contact", description = "Set this contact as the primary contact for the customer")
    public ApiResponse<ContactResponse> setAsPrimary(@PathVariable UUID id) {
        Contact contact = contactService.setAsPrimary(id);
        return ApiResponse.success("Contact set as primary", ContactResponse.from(contact));
    }

    @GetMapping("/stats/by-customer/{customerId}")
    @Operation(summary = "Count contacts by customer", description = "Get the count of contacts for a customer")
    public ApiResponse<Long> countByCustomer(@PathVariable UUID customerId) {
        long count = contactService.countByCustomerId(customerId);
        return ApiResponse.success(count);
    }

    @GetMapping("/stats/by-status")
    @Operation(summary = "Count contacts by status", description = "Get the count of contacts for a status")
    public ApiResponse<Long> countByStatus(@RequestParam ContactStatus status) {
        long count = contactService.countByStatus(status);
        return ApiResponse.success(count);
    }

    /**
     * Map request DTO to entity
     */
    private Contact mapToEntity(ContactRequest request) {
        Contact contact = new Contact();
        contact.setCustomerId(request.getCustomerId());
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setMiddleName(request.getMiddleName());
        contact.setTitle(request.getTitle());
        contact.setDepartment(request.getDepartment());
        contact.setContactRole(request.getContactRole());
        contact.setEmail(request.getEmail());
        contact.setSecondaryEmail(request.getSecondaryEmail());
        contact.setWorkPhone(request.getWorkPhone());
        contact.setMobilePhone(request.getMobilePhone());
        contact.setHomePhone(request.getHomePhone());
        contact.setFax(request.getFax());
        contact.setLinkedinUrl(request.getLinkedinUrl());
        contact.setTwitterHandle(request.getTwitterHandle());
        contact.setMailingAddress(request.getMailingAddress());
        contact.setCity(request.getCity());
        contact.setState(request.getState());
        contact.setCountry(request.getCountry());
        contact.setPostalCode(request.getPostalCode());
        contact.setOwnerId(request.getOwnerId());
        contact.setOrganizationId(request.getOrganizationId());
        contact.setBirthDate(request.getBirthDate());
        contact.setPreferredContactMethod(request.getPreferredContactMethod());
        contact.setPreferredContactTime(request.getPreferredContactTime());
        contact.setAssistantName(request.getAssistantName());
        contact.setAssistantPhone(request.getAssistantPhone());
        contact.setReportsToId(request.getReportsToId());
        contact.setIsPrimary(request.getIsPrimary());
        contact.setEmailOptOut(request.getEmailOptOut());
        contact.setLastContactDate(request.getLastContactDate());
        contact.setNotes(request.getNotes());
        contact.setTags(request.getTags());
        return contact;
    }
}
