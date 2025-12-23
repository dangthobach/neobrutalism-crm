package com.neobrutalism.crm.common.factory;

import com.neobrutalism.crm.common.entity.AuditableEntity;
import com.neobrutalism.crm.common.entity.BaseEntity;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Factory for creating entities with common setup
 * Reduces boilerplate in service classes
 */
@Component
public class EntityFactory {

    /**
     * Setup common fields for a new entity
     */
    public <T extends AuditableEntity> T setupNewEntity(T entity, String createdBy) {
        entity.setCreatedAt(Instant.now());
        entity.setCreatedBy(createdBy);
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(createdBy);
        return entity;
    }

    /**
     * Setup common fields for entity update
     */
    public <T extends AuditableEntity> T setupUpdatedEntity(T entity, String updatedBy) {
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(updatedBy);
        return entity;
    }

    /**
     * Setup tenant context for entity
     */
    public <T extends BaseEntity> T setupTenantContext(T entity) {
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null && entity instanceof com.neobrutalism.crm.common.entity.TenantAwareEntity) {
            ((com.neobrutalism.crm.common.entity.TenantAwareEntity) entity).setTenantId(currentTenant);
        }
        return entity;
    }

    /**
     * Setup audit fields for entity
     */
    public <T extends AuditableEntity> T setupAuditFields(T entity, String userId) {
        if (entity.isNew()) {
            setupNewEntity(entity, userId);
        } else {
            setupUpdatedEntity(entity, userId);
        }
        return entity;
    }
}
