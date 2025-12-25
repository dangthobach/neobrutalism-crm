package com.neobrutalism.crm.common.service;

import com.neobrutalism.crm.common.entity.AuditableEntity;
import com.neobrutalism.crm.common.audit.AuditLog;
import com.neobrutalism.crm.common.audit.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service with automatic audit log creation
 * TODO: Migrate to new AuditLog API
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
     * TODO: Fix API mismatch - temporarily disabled
     */
    protected void createAuditLog(T entity, String action, String fieldName,
                                  String oldValue, String newValue) {
        log.debug("Audit logging temporarily disabled - need to migrate to new AuditLog API");
        // Temporarily disabled due to API mismatch
    }

    /**
     * Get audit history for entity
     * TODO: Fix method name mismatch
     */
    public Page<AuditLog> getAuditHistory(UUID id, Pageable pageable) {
        throw new UnsupportedOperationException("Audit history temporarily disabled - need to migrate to new AuditLog API");
    }

    /**
     * Get audit history for entity without pagination
     * TODO: Fix method name mismatch
     */
    public List<AuditLog> getAuditHistory(UUID id) {
        throw new UnsupportedOperationException("Audit history temporarily disabled - need to migrate to new AuditLog API");
    }
}
