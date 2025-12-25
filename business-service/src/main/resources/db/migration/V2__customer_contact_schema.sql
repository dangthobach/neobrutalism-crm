-- =====================================================
-- ✅ PHASE 1.2: FLYWAY MIGRATION V2
-- Customer & Contact Schema with Relationships
-- =====================================================

-- =====================================================
-- BRANCHES
-- =====================================================
CREATE TABLE branches (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    branch_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    -- Organization
    organization_id UUID NOT NULL,
    
    -- Hierarchy
    parent_id UUID,
    level INTEGER DEFAULT 0,
    path VARCHAR(500),
    
    -- Manager
    manager_id UUID,
    
    -- Contact Information
    email VARCHAR(255),
    phone VARCHAR(20),
    fax VARCHAR(20),
    
    -- Address
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    timezone VARCHAR(50),
    
    -- Business Details
    tax_id VARCHAR(50),
    opening_hours VARCHAR(200),
    
    -- Metadata
    is_main BOOLEAN DEFAULT false,
    notes TEXT,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_branch_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_branch_parent FOREIGN KEY (parent_id) REFERENCES branches(id),
    CONSTRAINT fk_branch_manager FOREIGN KEY (manager_id) REFERENCES users(id),
    CONSTRAINT uk_branch_code_org UNIQUE (code, organization_id)
);

-- =====================================================
-- CUSTOMERS
-- =====================================================
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    customer_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    -- Organization & Branch
    organization_id UUID NOT NULL,
    branch_id UUID,
    owner_id UUID,
    
    -- Contact Information
    email VARCHAR(255),
    phone VARCHAR(20),
    mobile VARCHAR(20),
    fax VARCHAR(20),
    website VARCHAR(500),
    
    -- Billing Address
    billing_address VARCHAR(500),
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_country VARCHAR(100),
    billing_postal_code VARCHAR(20),
    
    -- Shipping Address
    shipping_address VARCHAR(500),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_country VARCHAR(100),
    shipping_postal_code VARCHAR(20),
    
    -- Business Details
    tax_id VARCHAR(50),
    business_registration_number VARCHAR(50),
    industry VARCHAR(100),
    employee_count INTEGER,
    annual_revenue DECIMAL(15, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Customer Relationship
    customer_since DATE,
    last_contact_date DATE,
    next_follow_up_date DATE,
    
    -- Credit Information
    credit_limit DECIMAL(15, 2),
    current_balance DECIMAL(15, 2) DEFAULT 0,
    payment_terms VARCHAR(100),
    
    -- Preferences
    preferred_contact_method VARCHAR(50),
    preferred_language VARCHAR(10),
    timezone VARCHAR(50),
    
    -- Social Media (stored as JSON)
    social_media TEXT,
    
    -- Tags (stored as JSON array)
    tags TEXT,
    
    -- Ratings & Scores
    rating INTEGER CHECK (rating BETWEEN 1 AND 5),
    credit_score INTEGER,
    
    -- Flags
    is_vip BOOLEAN DEFAULT false,
    allow_marketing BOOLEAN DEFAULT true,
    
    -- Notes
    notes TEXT,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_customer_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_customer_branch FOREIGN KEY (branch_id) REFERENCES branches(id),
    CONSTRAINT fk_customer_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT uk_customer_code_org UNIQUE (code, organization_id)
);

-- =====================================================
-- CONTACTS
-- =====================================================
CREATE TABLE contacts (
    id UUID PRIMARY KEY,
    
    -- Personal Information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    full_name VARCHAR(300),
    
    -- Job Information
    job_title VARCHAR(100),
    department VARCHAR(100),
    
    -- Contact Type & Status
    contact_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    -- Customer Relationship
    customer_id UUID NOT NULL,
    is_primary BOOLEAN DEFAULT false,
    
    -- Organization & Owner
    organization_id UUID NOT NULL,
    owner_id UUID,
    
    -- Contact Information
    email VARCHAR(255),
    phone VARCHAR(20),
    mobile VARCHAR(20),
    fax VARCHAR(20),
    
    -- Address
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    
    -- Personal Details
    date_of_birth DATE,
    gender VARCHAR(20),
    nationality VARCHAR(50),
    
    -- Preferences
    preferred_contact_method VARCHAR(50),
    preferred_language VARCHAR(10),
    timezone VARCHAR(50),
    
    -- Social Media (stored as JSON)
    social_media TEXT,
    
    -- Communication Preferences
    allow_email BOOLEAN DEFAULT true,
    allow_phone BOOLEAN DEFAULT true,
    allow_sms BOOLEAN DEFAULT true,
    
    -- Relationship Management
    last_contact_date DATE,
    next_follow_up_date DATE,
    
    -- Assistant Information
    assistant_name VARCHAR(200),
    assistant_phone VARCHAR(20),
    assistant_email VARCHAR(255),
    
    -- Notes
    notes TEXT,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_contact_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_contact_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_contact_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Branches
CREATE INDEX idx_branch_code ON branches(code);
CREATE INDEX idx_branch_org ON branches(organization_id);
CREATE INDEX idx_branch_type ON branches(branch_type);
CREATE INDEX idx_branch_status ON branches(status);
CREATE INDEX idx_branch_parent ON branches(parent_id);
CREATE INDEX idx_branch_manager ON branches(manager_id);

-- Customers
CREATE INDEX idx_customer_code ON customers(code);
CREATE INDEX idx_customer_org ON customers(organization_id);
CREATE INDEX idx_customer_branch ON customers(branch_id);
CREATE INDEX idx_customer_owner ON customers(owner_id);
CREATE INDEX idx_customer_type ON customers(customer_type);
CREATE INDEX idx_customer_status ON customers(status);
CREATE INDEX idx_customer_email ON customers(email);
CREATE INDEX idx_customer_created_at ON customers(created_at);
CREATE INDEX idx_customer_rating ON customers(rating);
CREATE INDEX idx_customer_is_vip ON customers(is_vip);

-- Full-text search on customer name (PostgreSQL-specific, disabled for H2)
-- CREATE INDEX idx_customer_company_name_gin ON customers USING gin(to_tsvector('english', company_name));

-- Contacts
CREATE INDEX idx_contact_customer ON contacts(customer_id);
CREATE INDEX idx_contact_org ON contacts(organization_id);
CREATE INDEX idx_contact_owner ON contacts(owner_id);
CREATE INDEX idx_contact_type ON contacts(contact_type);
CREATE INDEX idx_contact_status ON contacts(status);
CREATE INDEX idx_contact_email ON contacts(email);
CREATE INDEX idx_contact_is_primary ON contacts(is_primary);
CREATE INDEX idx_contact_created_at ON contacts(created_at);

-- Full-text search on contact name (PostgreSQL-specific, disabled for H2)
-- CREATE INDEX idx_contact_full_name_gin ON contacts USING gin(to_tsvector('english', full_name));

-- Composite indexes for common queries
CREATE INDEX idx_customer_org_status ON customers(organization_id, status);
CREATE INDEX idx_customer_org_type ON customers(organization_id, customer_type);
CREATE INDEX idx_customer_owner_status ON customers(owner_id, status);
CREATE INDEX idx_contact_customer_primary ON contacts(customer_id, is_primary);
CREATE INDEX idx_contact_org_status ON contacts(organization_id, status);

-- =====================================================
-- TRIGGER: Auto-generate full_name for contacts (PostgreSQL-specific, disabled for H2)
-- =====================================================
-- CREATE OR REPLACE FUNCTION generate_contact_full_name()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     NEW.full_name := TRIM(
--         COALESCE(NEW.first_name, '') || ' ' ||
--         COALESCE(NEW.middle_name, '') || ' ' ||
--         COALESCE(NEW.last_name, '')
--     );
--     RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;
--
-- CREATE TRIGGER trg_contact_full_name
--     BEFORE INSERT OR UPDATE ON contacts
--     FOR EACH ROW
--     EXECUTE FUNCTION generate_contact_full_name();

-- =====================================================
-- TRIGGER: Update customer member count (PostgreSQL-specific, disabled for H2)
-- =====================================================
-- CREATE OR REPLACE FUNCTION update_customer_contact_count()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     -- This would be implemented if we track contact count in customers table
--     -- Currently not in schema, but useful for future
--     RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;

-- =====================================================
-- END OF V2 MIGRATION
-- =====================================================
