package com.neobrutalism.crm.domain.organization.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.domain.organization.model.OrganizationStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain event fired when organization status changes
 */
@Getter
@Setter
public class OrganizationStatusChangedEvent extends DomainEvent {

    private OrganizationStatus oldStatus;
    private OrganizationStatus newStatus;

    public OrganizationStatusChangedEvent() {
        super();
    }

    public OrganizationStatusChangedEvent(String organizationId, OrganizationStatus oldStatus,
                                         OrganizationStatus newStatus, String changedBy) {
        super("OrganizationStatusChanged", organizationId, "Organization", changedBy);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("organizationId", getAggregateId());
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        payload.put("changedBy", getOccurredBy());
        return payload;
    }
}
