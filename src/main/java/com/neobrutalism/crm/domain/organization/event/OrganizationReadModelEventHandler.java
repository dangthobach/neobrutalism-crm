package com.neobrutalism.crm.domain.organization.event;

import com.neobrutalism.crm.domain.organization.model.Organization;
import com.neobrutalism.crm.domain.organization.model.OrganizationReadModel;
import com.neobrutalism.crm.domain.organization.repository.OrganizationReadModelRepository;
import com.neobrutalism.crm.domain.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Event handler for updating OrganizationReadModel
 * Listens to domain events and updates the read model accordingly
 *
 * CQRS Pattern: Keep read model in sync with write model via events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationReadModelEventHandler {

    private final OrganizationReadModelRepository readModelRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Handle organization created event
     * Create new read model entry
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrganizationCreated(OrganizationCreatedEvent event) {
        log.debug("Handling OrganizationCreatedEvent for organization: {}", event.getAggregateId());

        try {
            // Load the aggregate from write model
            Optional<Organization> orgOpt = organizationRepository.findById(UUID.fromString(event.getAggregateId()));
            if (orgOpt.isEmpty()) {
                log.warn("Organization not found for read model creation: {}", event.getAggregateId());
                return;
            }

            Organization org = orgOpt.get();

            // Create read model from aggregate
            OrganizationReadModel readModel = OrganizationReadModel.from(org);

            // Save to read model repository
            readModelRepository.save(readModel);

            log.info("Created read model for organization: {} ({})", org.getName(), org.getId());

        } catch (Exception e) {
            log.error("Failed to create read model for organization: {}", event.getAggregateId(), e);
            throw e; // Re-throw to trigger retry via outbox pattern
        }
    }

    /**
     * Handle organization status changed event
     * Update read model status and computed fields
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrganizationStatusChanged(OrganizationStatusChangedEvent event) {
        log.debug("Handling OrganizationStatusChangedEvent for organization: {}", event.getAggregateId());

        try {
            // Load the aggregate from write model
            Optional<Organization> orgOpt = organizationRepository.findById(UUID.fromString(event.getAggregateId()));
            if (orgOpt.isEmpty()) {
                log.warn("Organization not found for read model update: {}", event.getAggregateId());
                return;
            }

            Organization org = orgOpt.get();

            // Rebuild read model from current aggregate state
            OrganizationReadModel readModel = OrganizationReadModel.from(org);

            // Save updated read model (JPA will merge if ID exists)
            readModelRepository.save(readModel);

            log.info("Updated read model for organization: {} ({}) - Status: {} -> {}",
                    org.getName(), org.getId(), event.getOldStatus(), event.getNewStatus());

        } catch (Exception e) {
            log.error("Failed to update read model for organization: {}", event.getAggregateId(), e);
            throw e; // Re-throw to trigger retry via outbox pattern
        }
    }

    /**
     * Handle organization updated event (for other changes like contact info)
     * This is a catch-all for non-status changes
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrganizationUpdated(OrganizationUpdatedEvent event) {
        log.debug("Handling OrganizationUpdatedEvent for organization: {}", event.getAggregateId());

        try {
            // Load the aggregate from write model
            Optional<Organization> orgOpt = organizationRepository.findById(UUID.fromString(event.getAggregateId()));
            if (orgOpt.isEmpty()) {
                log.warn("Organization not found for read model update: {}", event.getAggregateId());
                return;
            }

            Organization org = orgOpt.get();

            // Rebuild read model from current aggregate state
            OrganizationReadModel readModel = OrganizationReadModel.from(org);

            // Save updated read model
            readModelRepository.save(readModel);

            log.info("Updated read model for organization: {} ({})", org.getName(), org.getId());

        } catch (Exception e) {
            log.error("Failed to update read model for organization: {}", event.getAggregateId(), e);
            throw e; // Re-throw to trigger retry via outbox pattern
        }
    }

    /**
     * Handle organization deleted event (soft delete)
     * Update isDeleted flag in read model
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrganizationDeleted(OrganizationDeletedEvent event) {
        log.debug("Handling OrganizationDeletedEvent for organization: {}", event.getAggregateId());

        try {
            // Load the aggregate from write model
            Optional<Organization> orgOpt = organizationRepository.findById(UUID.fromString(event.getAggregateId()));
            if (orgOpt.isEmpty()) {
                log.warn("Organization not found for read model deletion: {}", event.getAggregateId());
                return;
            }

            Organization org = orgOpt.get();

            // Rebuild read model with updated deletion status
            OrganizationReadModel readModel = OrganizationReadModel.from(org);

            // Save updated read model
            readModelRepository.save(readModel);

            log.info("Marked read model as deleted for organization: {} ({})", org.getName(), org.getId());

        } catch (Exception e) {
            log.error("Failed to delete read model for organization: {}", event.getAggregateId(), e);
            throw e; // Re-throw to trigger retry via outbox pattern
        }
    }
}
