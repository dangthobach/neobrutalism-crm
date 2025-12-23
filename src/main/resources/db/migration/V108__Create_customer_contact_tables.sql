-- =====================================================
-- Migration V108: Create Customer and Contact Tables
-- Description: Add customers and contacts management
-- Author: System
-- Date: 2025-11-01
-- =====================================================

-- Create customers table (idempotent - V2 may have already created this)
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    customer_type VARCHAR(20) NOT NULL DEFAULT 'B2B',
    status VARCHAR(20) NOT NULL DEFAULT 'LEAD',
    industry VARCHAR(50),
    tax_id VARCHAR(50),
    email VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(255),
    billing_address VARCHAR(500),
    shipping_address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    owner_id UUID,
    branch_id UUID,
    organization_id UUID NOT NULL,
    annual_revenue DECIMAL(19, 2),
    employee_count INTEGER,
    acquisition_date DATE,
    last_contact_date DATE,
    next_followup_date DATE,
    lead_source VARCHAR(100),
    credit_limit DECIMAL(19, 2),
    payment_terms_days INTEGER,
    tags VARCHAR(500),
    notes TEXT,
    rating INTEGER,
    is_vip BOOLEAN DEFAULT FALSE,

    -- Tenant awareness
    tenant_id VARCHAR(50) NOT NULL,

    -- Auditing fields
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),

    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),

    -- Status tracking
    status_changed_at TIMESTAMP,
    status_changed_by VARCHAR(100),
    status_reason VARCHAR(500),

    CONSTRAINT customers_code_org_unique UNIQUE (code, organization_id)
);

-- Create contacts table (idempotent - V2 may have already created this)
CREATE TABLE IF NOT EXISTS contacts (
    id UUID PRIMARY KEY,
    customer_id UUID,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    full_name VARCHAR(255),
    title VARCHAR(100),
    department VARCHAR(100),
    contact_role VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email VARCHAR(255) NOT NULL,
    secondary_email VARCHAR(255),
    work_phone VARCHAR(50),
    mobile_phone VARCHAR(50),
    home_phone VARCHAR(50),
    fax VARCHAR(50),
    linkedin_url VARCHAR(255),
    twitter_handle VARCHAR(100),
    mailing_address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    owner_id UUID,
    organization_id UUID NOT NULL,
    birth_date DATE,
    preferred_contact_method VARCHAR(20),
    preferred_contact_time VARCHAR(100),
    assistant_name VARCHAR(100),
    assistant_phone VARCHAR(50),
    reports_to_id UUID,
    is_primary BOOLEAN DEFAULT FALSE,
    email_opt_out BOOLEAN DEFAULT FALSE,
    last_contact_date DATE,
    notes TEXT,
    tags VARCHAR(500),

    -- Tenant awareness
    tenant_id VARCHAR(50) NOT NULL,

    -- Auditing fields
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),

    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),

    -- Status tracking
    status_changed_at TIMESTAMP,
    status_changed_by VARCHAR(100),
    status_reason VARCHAR(500),

    CONSTRAINT contacts_email_unique UNIQUE (email),
    CONSTRAINT contacts_customer_fk FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL
);

-- Fix customers table if it already exists from V2 (add missing columns)
ALTER TABLE customers ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';
ALTER TABLE customers ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS status_changed_by VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS status_reason VARCHAR(500);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS acquisition_date DATE;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS next_followup_date DATE;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS lead_source VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS payment_terms_days INTEGER;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS rating INTEGER;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS is_vip BOOLEAN DEFAULT FALSE;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS state VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS country VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);

-- Fix contacts table if it already exists from V2 (add missing columns)
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS status_changed_by VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS status_reason VARCHAR(500);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS middle_name VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS contact_role VARCHAR(30);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS secondary_email VARCHAR(255);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS work_phone VARCHAR(50);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS mobile_phone VARCHAR(50);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS home_phone VARCHAR(50);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS fax VARCHAR(50);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(255);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS twitter_handle VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS mailing_address VARCHAR(500);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS state VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS country VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS birth_date DATE;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS preferred_contact_method VARCHAR(20);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS preferred_contact_time VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS assistant_name VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS assistant_phone VARCHAR(50);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS reports_to_id UUID;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS is_primary BOOLEAN DEFAULT FALSE;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS email_opt_out BOOLEAN DEFAULT FALSE;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS last_contact_date DATE;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS tags VARCHAR(500);

-- Create indexes for customers table
CREATE INDEX IF NOT EXISTS idx_customers_code ON customers(code);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_company_name ON customers(company_name);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(status);
CREATE INDEX IF NOT EXISTS idx_customers_type ON customers(customer_type);
CREATE INDEX IF NOT EXISTS idx_customers_tenant_id ON customers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_customers_owner_id ON customers(owner_id);
CREATE INDEX IF NOT EXISTS idx_customers_branch_id ON customers(branch_id);
CREATE INDEX IF NOT EXISTS idx_customers_organization_id ON customers(organization_id);
CREATE INDEX IF NOT EXISTS idx_customers_deleted_id ON customers(deleted, id);
CREATE INDEX IF NOT EXISTS idx_customers_acquisition_date ON customers(acquisition_date);
CREATE INDEX IF NOT EXISTS idx_customers_next_followup_date ON customers(next_followup_date);
CREATE INDEX IF NOT EXISTS idx_customers_last_contact_date ON customers(last_contact_date);
CREATE INDEX IF NOT EXISTS idx_customers_lead_source ON customers(lead_source);
CREATE INDEX IF NOT EXISTS idx_customers_industry ON customers(industry);
CREATE INDEX IF NOT EXISTS idx_customers_is_vip ON customers(is_vip);

-- Create indexes for contacts table
CREATE INDEX IF NOT EXISTS idx_contacts_email ON contacts(email);
CREATE INDEX IF NOT EXISTS idx_contacts_customer_id ON contacts(customer_id);
CREATE INDEX IF NOT EXISTS idx_contacts_status ON contacts(status);
CREATE INDEX IF NOT EXISTS idx_contacts_tenant_id ON contacts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contacts_owner_id ON contacts(owner_id);
CREATE INDEX IF NOT EXISTS idx_contacts_organization_id ON contacts(organization_id);
CREATE INDEX IF NOT EXISTS idx_contacts_deleted_id ON contacts(deleted, id);
CREATE INDEX IF NOT EXISTS idx_contacts_first_name ON contacts(first_name);
CREATE INDEX IF NOT EXISTS idx_contacts_last_name ON contacts(last_name);
CREATE INDEX IF NOT EXISTS idx_contacts_full_name ON contacts(full_name);
CREATE INDEX IF NOT EXISTS idx_contacts_contact_role ON contacts(contact_role);
CREATE INDEX IF NOT EXISTS idx_contacts_is_primary ON contacts(is_primary);
CREATE INDEX IF NOT EXISTS idx_contacts_reports_to_id ON contacts(reports_to_id);
CREATE INDEX IF NOT EXISTS idx_contacts_last_contact_date ON contacts(last_contact_date);
CREATE INDEX IF NOT EXISTS idx_contacts_email_opt_out ON contacts(email_opt_out);

-- Add comments for documentation
COMMENT ON TABLE customers IS 'Customer/Company records for CRM';
COMMENT ON TABLE contacts IS 'Individual contact persons associated with customers';

COMMENT ON COLUMN customers.code IS 'Unique customer code within organization';
COMMENT ON COLUMN customers.customer_type IS 'Type: B2B, B2C, PARTNER, RESELLER, VENDOR, PROSPECT';
COMMENT ON COLUMN customers.status IS 'Status: LEAD, PROSPECT, ACTIVE, INACTIVE, CHURNED, BLACKLISTED';
COMMENT ON COLUMN customers.is_vip IS 'Flag for VIP customers';
COMMENT ON COLUMN customers.credit_limit IS 'Maximum credit allowed for customer';
COMMENT ON COLUMN customers.payment_terms_days IS 'Payment terms in days (e.g., NET 30)';

COMMENT ON COLUMN contacts.contact_role IS 'Role: DECISION_MAKER, INFLUENCER, GATEKEEPER, END_USER, TECHNICAL_BUYER, FINANCIAL_BUYER, CHAMPION, OTHER';
COMMENT ON COLUMN contacts.status IS 'Status: ACTIVE, INACTIVE, LEFT_COMPANY, DO_NOT_CONTACT';
COMMENT ON COLUMN contacts.is_primary IS 'Is this the primary contact for the customer';
COMMENT ON COLUMN contacts.email_opt_out IS 'Has the contact opted out of email communications';
COMMENT ON COLUMN contacts.reports_to_id IS 'Contact ID of the person this contact reports to';
