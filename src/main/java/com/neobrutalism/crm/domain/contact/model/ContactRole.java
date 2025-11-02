package com.neobrutalism.crm.domain.contact.model;

/**
 * Contact Role in organization
 */
public enum ContactRole {
    /**
     * DECISION_MAKER - Final decision authority
     */
    DECISION_MAKER,

    /**
     * INFLUENCER - Influences buying decisions
     */
    INFLUENCER,

    /**
     * GATEKEEPER - Controls access to decision makers
     */
    GATEKEEPER,

    /**
     * END_USER - Will use the product/service
     */
    END_USER,

    /**
     * TECHNICAL_BUYER - Evaluates technical aspects
     */
    TECHNICAL_BUYER,

    /**
     * FINANCIAL_BUYER - Controls budget
     */
    FINANCIAL_BUYER,

    /**
     * CHAMPION - Internal advocate for your solution
     */
    CHAMPION,

    /**
     * OTHER - Other role
     */
    OTHER
}
