package com.neobrutalism.crm.common.service;

import com.neobrutalism.crm.common.entity.StatefulEntity;
import com.neobrutalism.crm.common.event.StateTransition;
import com.neobrutalism.crm.common.repository.StateTransitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service with state transition management
 */
@Slf4j
public abstract class StatefulService<T extends StatefulEntity<S>, S extends Enum<S>>
        extends SoftDeleteService<T> {

    @Autowired(required = false)
    private StateTransitionRepository stateTransitionRepository;

    /**
     * Transition entity to new status
     */
    @Transactional
    public T transitionTo(UUID id, S newStatus, String reason) {
        T entity = findById(id);
        S oldStatus = entity.getStatus();

        entity.transitionTo(newStatus, getCurrentUser(), reason);
        T updated = getRepository().save(entity);

        recordStateTransition(entity, oldStatus, newStatus, reason);
        return updated;
    }

    /**
     * Record state transition
     */
    protected void recordStateTransition(T entity, S oldStatus, S newStatus, String reason) {
        if (stateTransitionRepository == null) {
            log.warn("StateTransitionRepository not available, skipping state transition record");
            return;
        }

        try {
            StateTransition transition = StateTransition.create(
                    getEntityName(),
                    entity.getId().toString(),
                    oldStatus != null ? oldStatus.name() : null,
                    newStatus.name(),
                    reason,
                    getCurrentUser()
            );
            stateTransitionRepository.save(transition);
            log.debug("Recorded state transition for {} {} from {} to {}",
                    getEntityName(), entity.getId(), oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to record state transition", e);
        }
    }

    /**
     * Get state transition history
     */
    public Page<StateTransition> getStateTransitionHistory(UUID id, Pageable pageable) {
        if (stateTransitionRepository == null) {
            throw new IllegalStateException("StateTransitionRepository not available");
        }
        return stateTransitionRepository.findByEntityTypeAndEntityIdOrderByTransitionedAtDesc(
                getEntityName(), id.toString(), pageable);
    }

    /**
     * Get state transition history without pagination
     */
    public List<StateTransition> getStateTransitionHistory(UUID id) {
        if (stateTransitionRepository == null) {
            throw new IllegalStateException("StateTransitionRepository not available");
        }
        return stateTransitionRepository.findByEntityTypeAndEntityIdOrderByTransitionedAtDesc(
                getEntityName(), id.toString());
    }

    /**
     * Get latest state transition
     */
    public StateTransition getLatestStateTransition(UUID id) {
        if (stateTransitionRepository == null) {
            throw new IllegalStateException("StateTransitionRepository not available");
        }
        return stateTransitionRepository.findLatestByEntity(getEntityName(), id.toString());
    }
}
