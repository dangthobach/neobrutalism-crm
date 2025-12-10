-- =====================================================
-- Migration V305: Seed Initial Commands
--
-- Inserts default commands for command palette.
--
-- Categories:
-- - Customer Management
-- - Task Management
-- - Navigation
-- - Search
--
-- @author Admin
-- @since Phase 1
-- =====================================================

-- Customer Commands
INSERT INTO commands (
    tenant_id, command_id, label, description, category, icon,
    shortcut_key, action_type, action_payload, required_permission, is_active
) VALUES
    ('default', 'customer.create', 'Create New Customer', 'Open dialog to create a new customer',
     'CUSTOMER', 'UserPlus', 'Ctrl+Shift+C', 'MODAL',
     '{"modal": "CustomerCreate"}', 'customer:create', true),

    ('default', 'customer.list', 'View Customers', 'Navigate to customers list page',
     'CUSTOMER', 'Users', 'Ctrl+Shift+U', 'NAVIGATION',
     '{"route": "/admin/customers"}', 'customer:view', true),

    ('default', 'customer.search', 'Search Customers', 'Open customer search dialog',
     'SEARCH', 'Search', 'Ctrl+K C', 'MODAL',
     '{"modal": "CustomerSearch"}', 'customer:view', true);

-- Task Commands
INSERT INTO commands (
    tenant_id, command_id, label, description, category, icon,
    shortcut_key, action_type, action_payload, required_permission, is_active
) VALUES
    ('default', 'task.create', 'Create New Task', 'Open dialog to create a new task',
     'TASK', 'Plus', 'Ctrl+Shift+T', 'MODAL',
     '{"modal": "TaskCreate"}', 'task:create', true),

    ('default', 'task.list', 'View Tasks', 'Navigate to tasks board',
     'TASK', 'CheckSquare', 'Ctrl+Shift+B', 'NAVIGATION',
     '{"route": "/admin/tasks"}', 'task:view', true),

    ('default', 'task.assign', 'Assign Task', 'Quickly assign task to user',
     'TASK', 'UserCheck', NULL, 'MODAL',
     '{"modal": "TaskAssign"}', 'task:edit', true);

-- Navigation Commands
INSERT INTO commands (
    tenant_id, command_id, label, description, category, icon,
    shortcut_key, action_type, action_payload, required_permission, is_active
) VALUES
    ('default', 'nav.dashboard', 'Go to Dashboard', 'Navigate to dashboard',
     'NAVIGATION', 'Home', 'Ctrl+Shift+H', 'NAVIGATION',
     '{"route": "/admin"}', NULL, true),

    ('default', 'nav.settings', 'Go to Settings', 'Navigate to settings page',
     'SETTINGS', 'Settings', 'Ctrl+Comma', 'NAVIGATION',
     '{"route": "/admin/settings"}', NULL, true);

-- Search Commands
INSERT INTO commands (
    tenant_id, command_id, label, description, category, icon,
    shortcut_key, action_type, action_payload, required_permission, is_active
) VALUES
    ('default', 'search.global', 'Global Search', 'Search across all entities',
     'SEARCH', 'Search', 'Ctrl+K', 'MODAL',
     '{"modal": "GlobalSearch"}', NULL, true);

-- Add search keywords for better discoverability
UPDATE commands SET search_keywords = 'customer client company account new add'
WHERE command_id = 'customer.create';

UPDATE commands SET search_keywords = 'task todo checklist project work item'
WHERE command_id = 'task.create';

UPDATE commands SET search_keywords = 'home main landing start'
WHERE command_id = 'nav.dashboard';
