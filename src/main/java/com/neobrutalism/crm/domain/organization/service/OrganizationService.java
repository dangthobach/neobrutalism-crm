package com.neobrutalism.crm.domain.organization.service;

import com.neobrutalism.crm.common.exception.ValidationException;
import com.neobrutalism.crm.common.service.EventPublisher;
import com.neobrutalism.crm.common.service.StatefulService;
import com.neobrutalism.crm.domain.organization.event.OrganizationDeletedEvent;
import com.neobrutalism.crm.domain.organization.event.OrganizationUpdatedEvent;
import com.neobrutalism.crm.domain.organization.model.Organization;
import com.neobrutalism.crm.domain.organization.model.OrganizationStatus;
import com.neobrutalism.crm.domain.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Organization entity
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService extends StatefulService<Organization, OrganizationStatus> {

    private final OrganizationRepository organizationRepository;
    private final EventPublisher eventPublisher;

    @Override
    protected OrganizationRepository getRepository() {
        return organizationRepository;
    }

    @Override
    protected String getEntityName() {
        return "Organization";
    }

    @Override
    protected void beforeCreate(Organization entity) {
        super.beforeCreate(entity);
        validateUniqueCode(entity.getCode(), null);
    }

    @Override
    protected void beforeUpdate(Organization entity) {
        super.beforeUpdate(entity);
        validateUniqueCode(entity.getCode(), entity.getId());
    }

    @Override
    protected void afterCreate(Organization entity) {
        super.afterCreate(entity);
        publishDomainEvents(entity);
    }

    @Override
    protected void afterUpdate(Organization entity) {
        super.afterUpdate(entity);
        publishDomainEvents(entity);
        // Publish update event for read model synchronization
        eventPublisher.publish(new OrganizationUpdatedEvent(
                entity.getId().toString(),
                entity.getName(),
                entity.getUpdatedBy()
        ));
    }

    @Override
    protected void afterDelete(Organization entity) {
        super.afterDelete(entity);
        // Publish delete event for read model synchronization
        eventPublisher.publish(new OrganizationDeletedEvent(
                entity.getId().toString(),
                entity.getName(),
                entity.getUpdatedBy()
        ));
    }

    /**
     * Validate unique organization code
     */
    private void validateUniqueCode(String code, UUID excludeId) {
        Optional<Organization> existing = organizationRepository.findByCode(code);
        if (existing.isPresent() && !existing.get().getId().equals(excludeId)) {
            throw new ValidationException("Organization code already exists: " + code);
        }
    }

    /**
     * Publish domain events
     */
    private void publishDomainEvents(Organization entity) {
        if (!entity.getDomainEvents().isEmpty()) {
            eventPublisher.publishAll(entity.getDomainEvents());
            entity.clearDomainEvents();
        }
    }

    /**
     * Find organization by code
     */
    public Optional<Organization> findByCode(String code) {
        return organizationRepository.findByCode(code);
    }

    /**
     * Find active organization by code
     */
    public Optional<Organization> findActiveByCode(String code) {
        return organizationRepository.findActiveByCode(code);
    }

    /**
     * Activate organization
     */
    @Transactional
    public Organization activate(UUID id, String reason) {
        return transitionTo(id, OrganizationStatus.ACTIVE, reason);
    }

    /**
     * Suspend organization
     */
    @Transactional
    public Organization suspend(UUID id, String reason) {
        return transitionTo(id, OrganizationStatus.SUSPENDED, reason);
    }

    /**
     * Archive organization
     */
    @Transactional
    public Organization archive(UUID id, String reason) {
        return transitionTo(id, OrganizationStatus.ARCHIVED, reason);
    }

    /**
     * Find organizations by status
     */
    public List<Organization> findByStatus(OrganizationStatus status) {
        return organizationRepository.findByStatus(status);
    }
}
