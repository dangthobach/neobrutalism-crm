-- ===================================
-- Content Management System Tables
-- Version: V112
-- Description: CMS tables for blog, articles, and content management
-- ===================================

-- Content Categories
CREATE TABLE content_categories (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    description TEXT,
    parent_id UUID REFERENCES content_categories(id) ON DELETE SET NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uq_content_category_slug_tenant UNIQUE (tenant_id, slug, deleted)
);

-- Content Tags
CREATE TABLE content_tags (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uq_content_tag_slug_tenant UNIQUE (tenant_id, slug, deleted)
);

-- Content Series
CREATE TABLE content_series (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    description TEXT,
    thumbnail_id UUID REFERENCES attachments(id) ON DELETE SET NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uq_content_series_slug_tenant UNIQUE (tenant_id, slug, deleted)
);

-- Main Content Table
CREATE TABLE contents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    slug VARCHAR(500) NOT NULL,
    summary TEXT,
    body TEXT NOT NULL,
    featured_image_id UUID REFERENCES attachments(id) ON DELETE SET NULL,
    content_type VARCHAR(50) NOT NULL DEFAULT 'BLOG',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP WITHOUT TIME ZONE,
    view_count INTEGER DEFAULT 0,
    tier_required VARCHAR(50) DEFAULT 'FREE',
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    series_id UUID REFERENCES content_series(id) ON DELETE SET NULL,
    series_order INTEGER DEFAULT 0,

    -- SEO fields
    seo_title VARCHAR(255),
    seo_description TEXT,
    seo_keywords TEXT,

    -- Audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Optimistic locking
    version INTEGER DEFAULT 0,

    CONSTRAINT uq_content_slug_tenant UNIQUE (tenant_id, slug, deleted),
    CONSTRAINT chk_content_type CHECK (content_type IN ('BLOG', 'ARTICLE', 'PAGE', 'NEWS', 'GUIDE', 'VIDEO')),
    CONSTRAINT chk_content_status CHECK (status IN ('DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED', 'DELETED')),
    CONSTRAINT chk_tier_required CHECK (tier_required IN ('FREE', 'SILVER', 'GOLD', 'VIP'))
);

-- Content to Category Mapping (Many-to-Many)
CREATE TABLE content_category_mappings (
    content_id UUID NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES content_categories(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (content_id, category_id)
);

-- Content to Tag Mapping (Many-to-Many)
CREATE TABLE content_tag_mappings (
    content_id UUID NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES content_tags(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (content_id, tag_id)
);

-- Content Views Tracking
CREATE TABLE content_views (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    session_id VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent TEXT,
    referrer VARCHAR(500),
    viewed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    time_spent_seconds INTEGER DEFAULT 0,
    scroll_percentage INTEGER DEFAULT 0,

    CONSTRAINT chk_scroll_percentage CHECK (scroll_percentage >= 0 AND scroll_percentage <= 100)
);

-- Content Read Model (CQRS)
CREATE TABLE content_read_models (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    slug VARCHAR(500) NOT NULL,
    summary TEXT,
    featured_image_url VARCHAR(1000),
    content_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    published_at TIMESTAMP WITHOUT TIME ZONE,
    view_count INTEGER DEFAULT 0,
    tier_required VARCHAR(50) DEFAULT 'FREE',

    -- Denormalized author info
    author_id UUID NOT NULL,
    author_name VARCHAR(200),
    author_avatar_url VARCHAR(1000),

    -- Denormalized category/tag names (for search)
    category_names TEXT, -- Comma-separated
    tag_names TEXT, -- Comma-separated

    -- SEO
    seo_title VARCHAR(255),
    seo_description TEXT,

    -- Audit
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT uq_content_read_model_slug UNIQUE (tenant_id, slug)
);

-- ===================================
-- Indexes for Performance
-- ===================================

-- Content Categories
CREATE INDEX idx_content_categories_tenant ON content_categories(tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_content_categories_parent ON content_categories(parent_id) WHERE deleted = FALSE;
CREATE INDEX idx_content_categories_slug ON content_categories(slug) WHERE deleted = FALSE;

-- Content Tags
CREATE INDEX idx_content_tags_tenant ON content_tags(tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_content_tags_slug ON content_tags(slug) WHERE deleted = FALSE;

-- Content Series
CREATE INDEX idx_content_series_tenant ON content_series(tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_content_series_slug ON content_series(slug) WHERE deleted = FALSE;

-- Contents
CREATE INDEX idx_contents_tenant ON contents(tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_contents_slug ON contents(slug) WHERE deleted = FALSE;
CREATE INDEX idx_contents_status ON contents(status) WHERE deleted = FALSE;
CREATE INDEX idx_contents_author ON contents(author_id) WHERE deleted = FALSE;
CREATE INDEX idx_contents_series ON contents(series_id) WHERE deleted = FALSE;
CREATE INDEX idx_contents_published_at ON contents(published_at DESC) WHERE status = 'PUBLISHED' AND deleted = FALSE;
CREATE INDEX idx_contents_tier ON contents(tier_required) WHERE deleted = FALSE;
CREATE INDEX idx_contents_type_status ON contents(content_type, status) WHERE deleted = FALSE;

-- Content Category Mappings
CREATE INDEX idx_content_categories_content ON content_category_mappings(content_id);
CREATE INDEX idx_content_categories_category ON content_category_mappings(category_id);

-- Content Tag Mappings
CREATE INDEX idx_content_tags_content ON content_tag_mappings(content_id);
CREATE INDEX idx_content_tags_tag ON content_tag_mappings(tag_id);

-- Content Views
CREATE INDEX idx_content_views_content ON content_views(content_id);
CREATE INDEX idx_content_views_user ON content_views(user_id);
CREATE INDEX idx_content_views_session ON content_views(session_id);
CREATE INDEX idx_content_views_viewed_at ON content_views(viewed_at DESC);

-- Content Read Models
CREATE INDEX idx_content_read_models_tenant ON content_read_models(tenant_id);
CREATE INDEX idx_content_read_models_status ON content_read_models(status);
CREATE INDEX idx_content_read_models_published_at ON content_read_models(published_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_content_read_models_author ON content_read_models(author_id);
CREATE INDEX idx_content_read_models_tier ON content_read_models(tier_required);

-- Full-text search on content
CREATE INDEX idx_contents_title_search ON contents USING gin(to_tsvector('english', title)) WHERE deleted = FALSE;
CREATE INDEX idx_contents_body_search ON contents USING gin(to_tsvector('english', body)) WHERE deleted = FALSE;

-- ===================================
-- Comments
-- ===================================
COMMENT ON TABLE contents IS 'Main content table for blog posts, articles, pages, and other content types';
COMMENT ON TABLE content_categories IS 'Hierarchical categories for organizing content';
COMMENT ON TABLE content_tags IS 'Tags for flexible content classification';
COMMENT ON TABLE content_series IS 'Content series for grouping related content';
COMMENT ON TABLE content_views IS 'Detailed tracking of content views for analytics and engagement scoring';
COMMENT ON TABLE content_read_models IS 'CQRS read model for optimized content queries';
COMMENT ON COLUMN contents.tier_required IS 'Minimum member tier required to access this content (FREE, SILVER, GOLD, VIP)';
COMMENT ON COLUMN contents.view_count IS 'Denormalized view count for quick access';
COMMENT ON COLUMN content_views.time_spent_seconds IS 'Time user spent reading the content';
COMMENT ON COLUMN content_views.scroll_percentage IS 'How far user scrolled (0-100%)';
