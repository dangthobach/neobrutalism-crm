package com.neobrutalism.crm.common.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Event object for audit logging
 * Captured by AuditAspect and published to AuditService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    
    private UUID tenantId;
    private String entityType;
    private UUID entityId;
    private AuditAction action;
    private UUID userId;
    private String username;
    private String description;
    
    // Change tracking
    private Map<String, Object> changes;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    
    // Request metadata
    private Map<String, Object> requestParams;
    private String ipAddress;
    private String userAgent;
    private String methodName;
    
    // Execution metadata
    private Long executionTimeMs;
    private Boolean success;
    private String errorMessage;
}
