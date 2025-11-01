-- Create attachments table
CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL UNIQUE,
    file_path VARCHAR(1000) NOT NULL,
    minio_bucket VARCHAR(100) NOT NULL,
    minio_object_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(200),
    file_extension VARCHAR(20),
    attachment_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    uploaded_by UUID NOT NULL,
    description VARCHAR(1000),
    tags VARCHAR(500),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    download_count INTEGER NOT NULL DEFAULT 0,
    checksum VARCHAR(100),

    -- Audit fields
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT,
    tenant_id VARCHAR(255)
);

-- Create indexes
CREATE INDEX idx_attachment_entity ON attachments(entity_type, entity_id);
CREATE INDEX idx_attachment_type ON attachments(attachment_type);
CREATE INDEX idx_attachment_uploader ON attachments(uploaded_by);
CREATE INDEX idx_attachment_filename ON attachments(original_filename);
CREATE INDEX idx_attachment_deleted ON attachments(deleted);
CREATE INDEX idx_attachment_tenant ON attachments(tenant_id);
CREATE INDEX idx_attachment_created_at ON attachments(created_at);
CREATE INDEX idx_attachment_public ON attachments(is_public);
CREATE INDEX idx_attachment_active_entity ON attachments(entity_type, entity_id, attachment_type);
