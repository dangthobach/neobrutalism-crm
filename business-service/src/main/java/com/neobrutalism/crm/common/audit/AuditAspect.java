package com.neobrutalism.crm.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AOP Aspect for intercepting @Audited methods
 * Captures method execution, parameters, results, and publishes audit events
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Intercept all methods annotated with @Audited
     */
    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // Extract method information
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        
        // Extract request metadata
        HttpServletRequest request = getHttpServletRequest();
        String ipAddress = extractIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        
        // Extract user information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = extractUserId(authentication);
        String username = extractUsername(authentication);
        UUID tenantId = extractTenantId(authentication);
        
        // Capture method parameters if enabled
        Map<String, Object> requestParams = null;
        if (audited.captureParameters()) {
            requestParams = captureParameters(joinPoint, signature);
        }
        
        // Build base audit event
        AuditEvent.AuditEventBuilder eventBuilder = AuditEvent.builder()
            .tenantId(tenantId)
            .entityType(audited.entity())
            .action(audited.action())
            .userId(userId)
            .username(username)
            .description(audited.description().isEmpty() ? null : audited.description())
            .methodName(methodName)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .requestParams(requestParams);
        
        Object result = null;
        boolean success = true;
        String errorMessage = null;
        
        try {
            // Capture old state for UPDATE/DELETE actions
            Map<String, Object> oldValues = null;
            if (audited.captureState() && (audited.action() == AuditAction.UPDATE || audited.action() == AuditAction.DELETE)) {
                oldValues = captureOldState(joinPoint);
            }
            
            // Execute the actual method
            result = joinPoint.proceed();
            
            // Capture new state and entity ID from result
            UUID entityId = null;
            Map<String, Object> newValues = null;
            
            if (result != null) {
                entityId = extractEntityId(result);
                
                if (audited.captureState()) {
                    newValues = captureNewState(result);
                }
            }
            
            // Calculate changes
            Map<String, Object> changes = null;
            if (oldValues != null && newValues != null) {
                changes = calculateChanges(oldValues, newValues);
            }
            
            // Complete audit event
            eventBuilder
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .changes(changes)
                .success(true);
            
        } catch (Throwable e) {
            success = false;
            errorMessage = e.getMessage();
            
            eventBuilder
                .success(false)
                .errorMessage(errorMessage);
            
            // Re-throw exception
            throw e;
            
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            eventBuilder.executionTimeMs(executionTime);
            
            // Publish audit event asynchronously
            try {
                auditService.logAuditEvent(eventBuilder.build());
            } catch (Exception e) {
                log.error("Failed to log audit event for method {}: {}", methodName, e.getMessage(), e);
                // Don't fail the actual operation if audit logging fails
            }
        }
        
        return result;
    }

    /**
     * Get HTTP request from request context
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract IP address from request
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) return null;
        
        // Check for proxy headers
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // Handle multiple IPs (take first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * Extract user ID from authentication
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                // Try to parse as UUID
                try {
                    return UUID.fromString(username);
                } catch (IllegalArgumentException e) {
                    // Username is not a UUID, return null
                    return null;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract user ID: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Extract username from authentication
     */
    private String extractUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        
        return authentication.getName();
    }

    /**
     * Extract tenant ID from authentication
     */
    private UUID extractTenantId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // Try to extract tenant ID from authentication details
        // This depends on your security implementation
        try {
            Object details = authentication.getDetails();
            if (details instanceof Map) {
                Object tenantId = ((Map<?, ?>) details).get("tenantId");
                if (tenantId instanceof String) {
                    return UUID.fromString((String) tenantId);
                } else if (tenantId instanceof UUID) {
                    return (UUID) tenantId;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract tenant ID: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Capture method parameters
     */
    private Map<String, Object> captureParameters(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        Map<String, Object> params = new HashMap<>();
        
        try {
            String[] paramNames = signature.getParameterNames();
            Object[] paramValues = joinPoint.getArgs();
            
            for (int i = 0; i < paramNames.length; i++) {
                if (paramValues[i] != null) {
                    // Convert to map representation (avoid circular references)
                    params.put(paramNames[i], sanitizeParameter(paramValues[i]));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to capture parameters: {}", e.getMessage());
        }
        
        return params.isEmpty() ? null : params;
    }

    /**
     * Sanitize parameter value (remove sensitive data, handle complex objects)
     */
    private Object sanitizeParameter(Object param) {
        try {
            // Convert to JSON and back to ensure it's serializable
            String json = objectMapper.writeValueAsString(param);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            // If conversion fails, use toString()
            return param.toString();
        }
    }

    /**
     * Capture old state (before operation)
     */
    private Map<String, Object> captureOldState(ProceedingJoinPoint joinPoint) {
        // For UPDATE/DELETE, first parameter is usually the entity ID
        // You would need to fetch the current state from database
        // This is a simplified implementation
        return null; // Implement based on your needs
    }

    /**
     * Capture new state (after operation)
     */
    private Map<String, Object> captureNewState(Object result) {
        try {
            // Convert entity to map
            String json = objectMapper.writeValueAsString(result);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.debug("Failed to capture new state: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract entity ID from result object
     */
    private UUID extractEntityId(Object result) {
        try {
            // Try to get "id" field via reflection
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);
            
            if (id instanceof UUID) {
                return (UUID) id;
            } else if (id instanceof String) {
                return UUID.fromString((String) id);
            }
        } catch (Exception e) {
            log.debug("Failed to extract entity ID: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Calculate changes between old and new values
     */
    private Map<String, Object> calculateChanges(Map<String, Object> oldValues, Map<String, Object> newValues) {
        Map<String, Object> changes = new HashMap<>();
        
        for (String key : newValues.keySet()) {
            Object oldValue = oldValues.get(key);
            Object newValue = newValues.get(key);
            
            // Only record actual changes
            if (oldValue == null && newValue != null) {
                changes.put(key, Map.of("old", "null", "new", newValue));
            } else if (oldValue != null && !oldValue.equals(newValue)) {
                changes.put(key, Map.of("old", oldValue, "new", newValue));
            }
        }
        
        return changes.isEmpty() ? null : changes;
    }
}
