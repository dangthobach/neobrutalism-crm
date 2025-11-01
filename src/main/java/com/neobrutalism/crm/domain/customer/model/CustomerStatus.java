package com.neobrutalism.crm.domain.customer.model;

/**
 * Customer Status Enum
 */
public enum CustomerStatus {
    /**
     * LEAD - Initial contact, not yet qualified
     */
    LEAD,

    /**
     * PROSPECT - Qualified lead, potential customer
     */
    PROSPECT,

    /**
     * ACTIVE - Active paying customer
     */
    ACTIVE,

    /**
     * INACTIVE - Customer not actively engaging
     */
    INACTIVE,

    /**
     * CHURNED - Lost customer
     */
    CHURNED,

    /**
     * BLACKLISTED - Blocked from doing business
     */
    BLACKLISTED
}
