-- ================================================
-- V125: Create Role Hierarchy Table
-- ================================================
-- Purpose: Support role inheritance and hierarchical permissions
-- Created: 2025-12-09
-- Author: Phase 3 Implementation
-- ================================================

CREATE TABLE role_hierarchy (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT DEFAULT 0,

    -- Hierarchy Relationship
    parent_role_id UUID NOT NULL,
    child_role_id UUID NOT NULL,

    -- Hierarchy Metadata
    hierarchy_level INTEGER NOT NULL DEFAULT 1,
    inheritance_type VARCHAR(50) NOT NULL DEFAULT 'FULL',

    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Multi-Tenancy
    organization_id UUID,
    tenant_id VARCHAR(50) NOT NULL,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Foreign Keys
    CONSTRAINT fk_role_hierarchy_parent FOREIGN KEY (parent_role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_hierarchy_child FOREIGN KEY (child_role_id) REFERENCES roles(id),

    -- Constraints
    CONSTRAINT chk_role_hierarchy_no_self CHECK (parent_role_id != child_role_id),
    CONSTRAINT chk_role_hierarchy_level CHECK (hierarchy_level > 0 AND hierarchy_level <= 10),
    CONSTRAINT uq_role_hierarchy_parent_child UNIQUE (parent_role_id, child_role_id, tenant_id)
);

-- ================================================
-- INDEXES
-- ================================================

-- Query indexes
CREATE INDEX idx_role_hierarchy_parent ON role_hierarchy(parent_role_id) WHERE deleted = false;
CREATE INDEX idx_role_hierarchy_child ON role_hierarchy(child_role_id) WHERE deleted = false;
CREATE INDEX idx_role_hierarchy_tenant ON role_hierarchy(tenant_id) WHERE deleted = false;
CREATE INDEX idx_role_hierarchy_active ON role_hierarchy(active) WHERE deleted = false AND active = true;

-- Composite indexes
CREATE INDEX idx_role_hierarchy_parent_tenant ON role_hierarchy(parent_role_id, tenant_id) WHERE deleted = false;
CREATE INDEX idx_role_hierarchy_child_tenant ON role_hierarchy(child_role_id, tenant_id) WHERE deleted = false;

-- ================================================
-- COMMENTS
-- ================================================

COMMENT ON TABLE role_hierarchy IS 'Role inheritance hierarchy - defines parent-child relationships between roles';

COMMENT ON COLUMN role_hierarchy.parent_role_id IS 'Parent role (inherits to child)';
COMMENT ON COLUMN role_hierarchy.child_role_id IS 'Child role (inherits from parent)';
COMMENT ON COLUMN role_hierarchy.hierarchy_level IS 'Depth in hierarchy (1 = direct child, 2 = grandchild, etc.)';
COMMENT ON COLUMN role_hierarchy.inheritance_type IS 'Type of inheritance: FULL (all permissions), PARTIAL (subset), OVERRIDE (can override parent)';
COMMENT ON COLUMN role_hierarchy.active IS 'Whether this hierarchy relationship is currently active';

-- ================================================
-- EXAMPLE DATA
-- ================================================

-- Example: SUPER_ADMIN > ADMIN > MANAGER > USER
-- COMMENT:
-- INSERT INTO role_hierarchy (parent_role_id, child_role_id, hierarchy_level, tenant_id)
-- SELECT
--     (SELECT id FROM roles WHERE code = 'SUPER_ADMIN'),
--     (SELECT id FROM roles WHERE code = 'ADMIN'),
--     1,
--     'default';

-- ================================================
-- HELPER FUNCTION: Get All Inherited Roles
-- ================================================

CREATE OR REPLACE FUNCTION get_inherited_roles(
    p_role_id UUID,
    p_tenant_id VARCHAR(50)
) RETURNS TABLE (
    role_id UUID,
    role_code VARCHAR(100),
    hierarchy_level INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE role_tree AS (
        -- Base case: direct role
        SELECT
            r.id,
            r.code,
            0 as level
        FROM roles r
        WHERE r.id = p_role_id

        UNION ALL

        -- Recursive case: parent roles
        SELECT
            r.id,
            r.code,
            rt.level + 1
        FROM roles r
        JOIN role_hierarchy rh ON rh.parent_role_id = r.id
        JOIN role_tree rt ON rt.role_id = rh.child_role_id
        WHERE rh.tenant_id = p_tenant_id
          AND rh.deleted = false
          AND rh.active = true
          AND rt.level < 10  -- Prevent infinite loops
    )
    SELECT
        rt.id,
        rt.code,
        rt.level
    FROM role_tree rt
    ORDER BY rt.level;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_inherited_roles IS 'Recursively get all roles inherited by a given role (including parents and grandparents)';

-- ================================================
-- HELPER FUNCTION: Check Circular Dependency
-- ================================================

CREATE OR REPLACE FUNCTION check_circular_role_hierarchy(
    p_parent_role_id UUID,
    p_child_role_id UUID,
    p_tenant_id VARCHAR(50)
) RETURNS BOOLEAN AS $$
DECLARE
    v_has_circular BOOLEAN;
BEGIN
    -- Check if adding this relationship would create a circular dependency
    WITH RECURSIVE hierarchy_check AS (
        -- Start from the proposed child
        SELECT
            rh.child_role_id,
            rh.parent_role_id,
            1 as depth
        FROM role_hierarchy rh
        WHERE rh.child_role_id = p_parent_role_id
          AND rh.tenant_id = p_tenant_id
          AND rh.deleted = false

        UNION ALL

        -- Follow the chain
        SELECT
            rh.child_role_id,
            rh.parent_role_id,
            hc.depth + 1
        FROM role_hierarchy rh
        JOIN hierarchy_check hc ON hc.parent_role_id = rh.child_role_id
        WHERE rh.tenant_id = p_tenant_id
          AND rh.deleted = false
          AND hc.depth < 20  -- Limit recursion
    )
    SELECT EXISTS (
        SELECT 1
        FROM hierarchy_check
        WHERE parent_role_id = p_child_role_id
    ) INTO v_has_circular;

    RETURN v_has_circular;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION check_circular_role_hierarchy IS 'Check if adding a hierarchy relationship would create a circular dependency';

-- ================================================
-- TRIGGER: Prevent Circular Dependencies
-- ================================================

CREATE OR REPLACE FUNCTION prevent_circular_role_hierarchy()
RETURNS TRIGGER AS $$
BEGIN
    IF check_circular_role_hierarchy(NEW.parent_role_id, NEW.child_role_id, NEW.tenant_id) THEN
        RAISE EXCEPTION 'Cannot create role hierarchy: would create circular dependency';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_circular_role_hierarchy
    BEFORE INSERT OR UPDATE ON role_hierarchy
    FOR EACH ROW
    EXECUTE FUNCTION prevent_circular_role_hierarchy();

COMMENT ON TRIGGER trg_prevent_circular_role_hierarchy ON role_hierarchy IS 'Prevents creation of circular role hierarchies';

-- ================================================
-- PERFORMANCE NOTES
-- ================================================
-- 1. Recursive queries are indexed by parent_role_id and child_role_id
-- 2. Depth limit (10 levels) prevents infinite loops
-- 3. Circular dependency check happens at INSERT/UPDATE time
-- 4. Consider materializing hierarchy paths for very deep hierarchies (>5 levels)

-- ================================================
-- SECURITY NOTES
-- ================================================
-- 1. Role hierarchy changes are security-sensitive operations
-- 2. All hierarchy changes should be audited via permission_audit_logs
-- 3. Circular dependencies are prevented by database trigger
-- 4. Maximum hierarchy depth is 10 levels (configurable)
