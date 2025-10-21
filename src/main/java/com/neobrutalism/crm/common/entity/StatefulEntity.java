package com.neobrutalism.crm.common.entity;

import com.neobrutalism.crm.common.exception.InvalidStateTransitionException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

/**
 * Stateful entity with state transition management
 */
@Getter
@Setter
@MappedSuperclass
public abstract class StatefulEntity<S extends Enum<S>> extends SoftDeletableEntity {

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private S status;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "status_changed_by", length = 100)
    private String statusChangedBy;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    /**
     * Define valid state transitions for the entity
     * Override in subclasses to specify allowed transitions
     */
    protected abstract Set<S> getAllowedTransitions(S currentStatus);

    /**
     * Get initial status for new entities
     */
    protected abstract S getInitialStatus();

    /**
     * Transition to a new status with validation
     */
    public void transitionTo(S newStatus, String changedBy, String reason) {
        if (this.status == null) {
            this.status = getInitialStatus();
        }

        if (this.status == newStatus) {
            return; // Already in target state
        }

        Set<S> allowedTransitions = getAllowedTransitions(this.status);
        if (!allowedTransitions.contains(newStatus)) {
            throw new InvalidStateTransitionException(
                String.format("Invalid state transition from %s to %s", this.status, newStatus)
            );
        }

        S oldStatus = this.status;
        this.status = newStatus;
        this.statusChangedAt = Instant.now();
        this.statusChangedBy = changedBy;
        this.statusReason = reason;

        onStatusChanged(oldStatus, newStatus);
    }

    /**
     * Hook method called after status change
     * Override in subclasses for custom behavior
     */
    protected void onStatusChanged(S oldStatus, S newStatus) {
        // Default implementation does nothing
        // Override in subclasses for custom behavior
    }

    /**
     * Initialize status for new entities
     */
    @PrePersist
    protected void initializeStatus() {
        if (this.status == null) {
            this.status = getInitialStatus();
            this.statusChangedAt = Instant.now();
        }
    }
}
