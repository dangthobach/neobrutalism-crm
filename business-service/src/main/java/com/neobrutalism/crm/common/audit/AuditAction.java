package com.neobrutalism.crm.common.audit;

/**
 * Enum representing audit actions
 */
public enum AuditAction {
    /**
     * Entity creation
     */
    CREATE,
    
    /**
     * Entity update/modification
     */
    UPDATE,
    
    /**
     * Entity deletion (soft or hard)
     */
    DELETE,
    
    /**
     * Entity read/view (optional, for sensitive data)
     */
    READ,
    
    /**
     * Entity export
     */
    EXPORT,
    
    /**
     * Entity import
     */
    IMPORT,
    
    /**
     * Status change
     */
    STATUS_CHANGE,
    
    /**
     * Permission/access change
     */
    PERMISSION_CHANGE,
    
    /**
     * Login/logout
     */
    LOGIN,
    LOGOUT,
    
    /**
     * Other custom actions
     */
    OTHER
}
