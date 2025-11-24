package com.neobrutalism.crm.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for audit logging
 * Captures entity changes, user actions, and metadata
 * 
 * Usage:
 * <pre>
 * {@code
 * @Audited(entity = "Customer", action = AuditAction.CREATE)
 * public Customer createCustomer(CustomerRequest request) {
 *     // Method implementation
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    
    /**
     * Entity type being audited (e.g., "Customer", "Contract", "User")
     */
    String entity();
    
    /**
     * Action being performed
     */
    AuditAction action();
    
    /**
     * Optional description for the audit log
     */
    String description() default "";
    
    /**
     * Whether to capture the full entity state (before and after)
     * Set to false for large entities to reduce storage
     */
    boolean captureState() default true;
    
    /**
     * Whether to capture request parameters
     */
    boolean captureParameters() default true;
}
