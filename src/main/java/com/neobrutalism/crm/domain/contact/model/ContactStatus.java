package com.neobrutalism.crm.domain.contact.model;

/**
 * Contact Status
 */
public enum ContactStatus {
    /**
     * ACTIVE - Active contact
     */
    ACTIVE,

    /**
     * INACTIVE - Inactive contact
     */
    INACTIVE,

    /**
     * LEFT_COMPANY - No longer with the company
     */
    LEFT_COMPANY,

    /**
     * DO_NOT_CONTACT - Requested not to be contacted
     */
    DO_NOT_CONTACT
}
