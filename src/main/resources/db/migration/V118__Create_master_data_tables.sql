-- V118__Create_master_data_tables.sql
-- Create master data tables for migration transformation

-- Contracts table (from staging_hsbg_hop_dong)
CREATE TABLE IF NOT EXISTS contracts (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    tenant_id UUID NOT NULL,
    
    -- Core contract identity
    contract_number VARCHAR(100) NOT NULL,
    customer_cif VARCHAR(100) NOT NULL,
    customer_name VARCHAR(500),
    customer_segment VARCHAR(100),
    
    -- Organization context
    unit_code VARCHAR(100),
    warehouse_vpbank VARCHAR(100),
    delivery_responsibility VARCHAR(200),
    
    -- Document information
    document_type VARCHAR(100),
    document_flow VARCHAR(100),
    volume_name VARCHAR(200),
    volume_quantity INTEGER,
    
    -- Loan/Credit information
    product VARCHAR(200),
    credit_term_category VARCHAR(100),
    credit_term_months INTEGER,
    
    -- Important dates
    required_delivery_date DATE,
    delivery_date DATE,
    disbursement_date DATE,
    due_date DATE,
    expected_destruction_date DATE,
    
    -- Status and case management
    pdm_case_status VARCHAR(100),
    
    -- Physical storage
    box_code VARCHAR(100),
    vpbank_warehouse_entry_date DATE,
    crown_warehouse_transfer_date DATE,
    area VARCHAR(50),
    "row" VARCHAR(50),
    "column" VARCHAR(50),
    box_condition VARCHAR(100),
    box_status VARCHAR(100),
    
    -- Additional codes
    dao_code VARCHAR(100),
    ts_code VARCHAR(100),
    rrt_id VARCHAR(100),
    nq_code VARCHAR(100),
    
    -- Metadata
    notes TEXT,
    source_system VARCHAR(50) DEFAULT 'MIGRATION',
    migration_job_id UUID,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL
);

-- Indexes for contracts (H2 compatible - no WHERE clauses)
CREATE INDEX idx_contract_number ON contracts(contract_number);
CREATE UNIQUE INDEX idx_contract_number_tenant ON contracts(contract_number, tenant_id);
CREATE INDEX idx_contract_tenant ON contracts(tenant_id);
CREATE INDEX idx_contract_customer_cif ON contracts(customer_cif);
CREATE INDEX idx_contract_due_date ON contracts(due_date);
CREATE INDEX idx_contract_status ON contracts(pdm_case_status);
CREATE INDEX idx_contract_box_code ON contracts(box_code);

-- Document Volumes table (from staging_hsbg_tap)
CREATE TABLE IF NOT EXISTS document_volumes (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    tenant_id UUID NOT NULL,
    
    -- Core volume identity
    volume_name VARCHAR(200) NOT NULL,
    volume_quantity INTEGER,
    
    -- Customer reference
    customer_cif VARCHAR(100),
    customer_name VARCHAR(500),
    customer_segment VARCHAR(100),
    
    -- Organization context
    unit_code VARCHAR(100),
    warehouse_vpbank VARCHAR(100),
    delivery_responsibility VARCHAR(200),
    
    -- Document information
    document_type VARCHAR(100),
    document_flow VARCHAR(100),
    credit_term_category VARCHAR(100),
    
    -- Important dates
    required_delivery_date DATE,
    delivery_date DATE,
    disbursement_date DATE,
    
    -- Status
    product VARCHAR(200),
    pdm_case_status VARCHAR(100),
    
    -- Physical storage
    box_code VARCHAR(100),
    vpbank_warehouse_entry_date DATE,
    crown_warehouse_transfer_date DATE,
    area VARCHAR(50),
    "row" VARCHAR(50),
    "column" VARCHAR(50),
    box_condition VARCHAR(100),
    box_status VARCHAR(100),
    
    -- Additional codes
    nq_code VARCHAR(100),
    
    -- Metadata
    notes TEXT,
    source_system VARCHAR(50) DEFAULT 'MIGRATION',
    migration_job_id UUID,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL
);

-- Indexes for document_volumes (H2 compatible - no WHERE clauses)
CREATE INDEX idx_volume_name ON document_volumes(volume_name);
CREATE INDEX idx_volume_tenant ON document_volumes(tenant_id);
CREATE INDEX idx_volume_box_code ON document_volumes(box_code);
CREATE INDEX idx_volume_customer_cif ON document_volumes(customer_cif);
CREATE INDEX idx_volume_unique_check ON document_volumes(volume_name, box_code, tenant_id);

-- Comments: H2 doesn't support COMMENT ON statements
-- See code documentation for table descriptions

-- Note: customers table already exists from V2__customer_contact_schema.sql
-- CIF records will be transformed to existing customers table
