package com.neobrutalism.crm.common.event;

import com.neobrutalism.crm.common.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for event-sourcing audit logs
 */
@Repository
public interface EventAuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            String entityType, String entityId, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            String entityType, String entityId);
}
