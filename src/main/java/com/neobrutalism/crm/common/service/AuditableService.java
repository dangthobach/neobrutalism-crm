package com.neobrutalism.crm.common.service;

import com.neobrutalism.crm.common.entity.AuditableEntity;
import com.neobrutalism.crm.common.event.AuditLog;
import com.neobrutalism.crm.common.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service with automatic audit log creation
 */
@Slf4j
public abstract class AuditableService<T extends AuditableEntity>
        extends BaseService<T> {

    @Autowired(required = false)
    private AuditLogRepository auditLogRepository;

    /**
     * Get current user for audit logging
     * Override in subclasses to get from security context
     */
    protected String getCurrentUser() {
        return "system";
    }

    @Override
    protected void afterCreate(T entity) {
        super.afterCreate(entity);
        createAuditLog(entity, "CREATE", null, null, null);
    }

    @Override
    protected void afterUpdate(T entity) {
        super.afterUpdate(entity);
        createAuditLog(entity, "UPDATE", null, null, null);
    }

    @Override
    protected void afterDelete(T entity) {
        super.afterDelete(entity);
        createAuditLog(entity, "DELETE", null, null, null);
    }

    /**
     * Create audit log entry
     */
    protected void createAuditLog(T entity, String action, String fieldName,
                                  String oldValue, String newValue) {
        if (auditLogRepository == null) {
            log.warn("AuditLogRepository not available, skipping audit log");
            return;
        }

        try {
            AuditLog auditLog = AuditLog.create(
                    getEntityName(),
                    entity.getId().toString(),
                    action,
                    fieldName,
                    oldValue,
                    newValue,
                    getCurrentUser(),
                    null
            );
            auditLogRepository.save(auditLog);
            log.debug("Created audit log for {} {} with action {}", getEntityName(), entity.getId(), action);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    /**
     * Get audit history for entity
     */
    public Page<AuditLog> getAuditHistory(UUID id, Pageable pageable) {
        if (auditLogRepository == null) {
            throw new IllegalStateException("AuditLogRepository not available");
        }
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(
                getEntityName(), id.toString(), pageable);
    }

    /**
     * Get audit history for entity without pagination
     */
    public List<AuditLog> getAuditHistory(UUID id) {
        if (auditLogRepository == null) {
            throw new IllegalStateException("AuditLogRepository not available");
        }
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(
                getEntityName(), id.toString());
    }
}
