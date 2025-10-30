# Menu System Improvements - Implementation Guide

## Overview

This document describes the improvements made to the Menu System in Neobrutalism CRM, including auto-linking screens to API endpoints, permission synchronization tools, and frontend permission guards.

## Table of Contents

1. [Auto-Link Screen to ApiEndpoint](#1-auto-link-screen-to-apiendpoint)
2. [Permission Sync Tool](#2-permission-sync-tool)
3. [Frontend Permission Guard](#3-frontend-permission-guard)
4. [Bulk Operations](#4-bulk-operations)
5. [API Reference](#5-api-reference)
6. [Usage Examples](#6-usage-examples)

---

## 1. Auto-Link Screen to ApiEndpoint

### Overview

Automatically links MenuScreens to ApiEndpoints based on route pattern matching. This eliminates the need to manually link every screen to its required API endpoints.

### How It Works

**Pattern Matching Algorithm:**
```
Frontend Route: /admin/users/list
  ↓ Extract base resource
Base Resource: "users"
  ↓ Match API endpoints
Matched APIs: /api/users, /api/users/{id}, /api/users/search, etc.
  ↓ Determine permissions based on HTTP method
GET → READ, POST → WRITE, DELETE → DELETE
  ↓ Create ScreenApiEndpoint links
```

### API Endpoints

#### Auto-link Single Screen
```http
POST /api/screen-api/auto-link/screen/{screenId}
```

**Response:**
```json
{
  "success": true,
  "message": "Auto-link completed",
  "data": {
    "screenCode": "USERS_VIEW",
    "screenRoute": "/users/list",
    "baseResource": "users",
    "linkedCount": 5,
    "skippedCount": 0,
    "linkedPaths": [
      "GET /api/users",
      "GET /api/users/{id}",
      "POST /api/users",
      "PUT /api/users/{id}",
      "DELETE /api/users/{id}"
    ]
  }
}
```

#### Auto-link All Screens
```http
POST /api/screen-api/auto-link/all
```

**Response:**
```json
{
  "success": true,
  "message": "Bulk auto-link completed",
  "data": {
    "totalScreens": 25,
    "totalLinked": 120,
    "totalSkipped": 10,
    "errors": []
  }
}
```

### Implementation Details

**Service:** `ScreenApiAutoLinkService.java`

**Route Pattern Extraction:**
- `/admin/users/list` → `users`
- `/users/{id}/edit` → `users`
- `/settings/profile` → `settings`

**API Pattern Matching:**
- Base resource `users` matches `/api/users/**`
- Uses regex: `^/api/{baseResource}(/.*)?$`

**Permission Mapping:**
| HTTP Method | Permission Type |
|-------------|-----------------|
| GET, HEAD, OPTIONS | READ |
| POST, PUT, PATCH | WRITE |
| DELETE | DELETE |

---

## 2. Permission Sync Tool

### Overview

Checks for inconsistencies between Role-Menu permissions and Screen-API permissions. Helps identify permission mismatches and suggests fixes.

### API Endpoints

#### Check Permission Consistency
```http
GET /api/permissions/sync/check
```

**Response:**
```json
{
  "success": true,
  "message": "Found 3 permission inconsistencies",
  "data": {
    "totalRoleMenusChecked": 15,
    "issuesFound": 3,
    "status": "INCONSISTENT",
    "inconsistencies": [
      {
        "type": "MISSING_API_LINKS",
        "severity": "WARNING",
        "screenCode": "REPORTS_DASHBOARD",
        "screenRoute": "/reports/dashboard",
        "message": "Screen has no API endpoints linked"
      },
      {
        "type": "PERMISSION_MISMATCH",
        "severity": "ERROR",
        "screenCode": "USERS_VIEW",
        "rolePermissions": {
          "canView": false,
          "canCreate": true,
          "canEdit": true,
          "canDelete": false
        },
        "requiredPermissions": {
          "needsRead": true,
          "needsWrite": false,
          "needsDelete": false
        },
        "issues": [
          "Screen requires READ permission but role has canView=false"
        ]
      }
    ]
  }
}
```

#### Get Suggestions
```http
GET /api/permissions/sync/suggestions
```

**Response:**
```json
{
  "success": true,
  "message": "Suggestions generated",
  "data": {
    "totalIssues": 3,
    "suggestions": [
      {
        "issue": { /* issue details */ },
        "action": "AUTO_LINK",
        "description": "Use POST /api/screen-api/auto-link/screen/{screenId} to automatically link APIs"
      },
      {
        "issue": { /* issue details */ },
        "action": "UPDATE_ROLE_MENU",
        "description": "Update RoleMenu permissions to match required API permissions",
        "suggestedPermissions": {
          "canView": true,
          "canCreate": false,
          "canEdit": false,
          "canDelete": false
        }
      }
    ]
  }
}
```

### Issue Types

| Type | Severity | Description | Solution |
|------|----------|-------------|----------|
| MISSING_API_LINKS | WARNING | Screen has no API endpoints | Use auto-link |
| PERMISSION_MISMATCH | ERROR | Role permissions don't match API requirements | Update RoleMenu |

### Implementation Details

**Service:** `PermissionSyncService.java`

**Check Algorithm:**
1. Get all RoleMenus
2. For each RoleMenu, get all screens under that menu
3. For each screen, get linked API endpoints
4. Check if RoleMenu permissions match required API permissions
5. Generate report with inconsistencies and suggestions

---

## 3. Frontend Permission Guard

### Overview

React hooks and components for checking user permissions on the frontend. Automatically checks both menu-level and API-level permissions.

### Installation

The hooks are available at:
- `src/hooks/usePermission.ts`
- `src/components/PermissionGuard.tsx`

### Usage Examples

#### Basic Hook Usage

```typescript
import { usePermission } from '@/hooks/usePermission'

function UserManagement() {
  const { canView, canCreate, canEdit, canDelete } = usePermission()

  return (
    <div>
      {canView('/users') && <UserList />}
      {canCreate('/users') && <Button>Create User</Button>}
      {canEdit('/users') && <Button>Edit</Button>}
      {canDelete('/users') && <Button>Delete</Button>}
    </div>
  )
}
```

#### Check Specific Permission

```typescript
const { hasPermission } = usePermission()

if (hasPermission('/users', 'canCreate')) {
  // Show create button
}

if (hasPermission('/users', 'WRITE')) {
  // WRITE permission (maps to canCreate or canEdit)
}
```

#### Check Multiple Permissions

```typescript
const { checkPermissions } = usePermission()

const permissions = checkPermissions('/users', ['canView', 'canCreate', 'canEdit'])
// Returns: { canView: true, canCreate: true, canEdit: false }
```

#### Permission Guard Component

```tsx
import { PermissionGuard } from '@/components/PermissionGuard'

<PermissionGuard route="/users" permission="canCreate">
  <Button>Create User</Button>
</PermissionGuard>
```

#### Guard with Fallback

```tsx
<PermissionGuard
  route="/users"
  permission="canCreate"
  fallback={<div>You don't have permission to create users</div>}
>
  <Button>Create User</Button>
</PermissionGuard>
```

#### Multiple Permission Guards

```tsx
import { PermissionGuardAll, PermissionGuardAny } from '@/components/PermissionGuard'

// Requires ALL permissions
<PermissionGuardAll route="/users" permissions={['canView', 'canCreate']}>
  <UserManagement />
</PermissionGuardAll>

// Requires ANY permission
<PermissionGuardAny route="/users" permissions={['canCreate', 'canEdit']}>
  <Button>Modify</Button>
</PermissionGuardAny>
```

#### HOC Pattern

```tsx
import { withPermission } from '@/hooks/usePermission'

const ProtectedButton = withPermission(Button, '/users', 'canCreate')

// Usage
<ProtectedButton>Create User</ProtectedButton>
```

### Permission Types

| Type | Maps To Menu Permission | Description |
|------|------------------------|-------------|
| READ | canView | View/Read data |
| WRITE | canCreate OR canEdit | Create or update data |
| DELETE | canDelete | Delete data |
| EXECUTE | canCreate OR canEdit | Execute actions |
| canView | canView | Direct menu permission |
| canCreate | canCreate | Direct menu permission |
| canEdit | canEdit | Direct menu permission |
| canDelete | canDelete | Direct menu permission |
| canExport | canExport | Direct menu permission |
| canImport | canImport | Direct menu permission |

### Implementation Details

**Hook:** `usePermission.ts`

**Features:**
- Caches user menu tree from `/api/users/me/menus`
- Supports nested menu structures
- Matches by route or code
- Maps API permission types to menu permissions
- Provides convenience methods for common checks

---

## 4. Bulk Operations

### Overview

Efficiently assign or remove multiple API endpoints to/from a screen at once.

### API Endpoints

#### Bulk Assign Endpoints
```http
POST /api/screen-api/bulk-assign/{screenId}
Content-Type: application/json

[
  {
    "endpointId": "018e0004-0000-0000-0000-000000000001",
    "requiredPermission": "READ"
  },
  {
    "endpointId": "018e0004-0000-0000-0000-000000000002",
    "requiredPermission": "WRITE"
  },
  {
    "endpointId": "018e0004-0000-0000-0000-000000000003",
    "requiredPermission": "DELETE"
  }
]
```

**Response:**
```json
{
  "success": true,
  "message": "Bulk assignment completed",
  "data": {
    "screenId": "018e0003-0000-0000-0000-000000000001",
    "assigned": 3,
    "total": 3,
    "skipped": 0
  }
}
```

#### Bulk Remove Endpoints
```http
DELETE /api/screen-api/bulk-remove/{screenId}
Content-Type: application/json

[
  "018e0004-0000-0000-0000-000000000001",
  "018e0004-0000-0000-0000-000000000002"
]
```

**Response:**
```json
{
  "success": true,
  "message": "Bulk removal completed",
  "data": {
    "screenId": "018e0003-0000-0000-0000-000000000001",
    "removed": 2,
    "total": 2,
    "notFound": 0
  }
}
```

### Implementation Details

**Service:** `ScreenApiEndpointService.java`

**Methods:**
- `bulkAssignEndpoints(UUID screenId, List<BulkAssignment> assignments)`
- `bulkRemoveEndpoints(UUID screenId, List<UUID> endpointIds)`

**Features:**
- Skips existing links (no duplicates)
- Transactional (all or nothing)
- Returns statistics (assigned, skipped, removed)

---

## 5. API Reference

### Complete API List

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/screen-api/auto-link/screen/{screenId}` | Auto-link single screen |
| POST | `/api/screen-api/auto-link/all` | Auto-link all screens |
| POST | `/api/screen-api/bulk-assign/{screenId}` | Bulk assign endpoints |
| DELETE | `/api/screen-api/bulk-remove/{screenId}` | Bulk remove endpoints |
| GET | `/api/permissions/sync/check` | Check permission consistency |
| GET | `/api/permissions/sync/suggestions` | Get fix suggestions |

---

## 6. Usage Examples

### Example 1: Initial Setup

```bash
# 1. Auto-discover all API endpoints
POST /api/api-endpoints/scan

# 2. Auto-link all screens to their API endpoints
POST /api/screen-api/auto-link/all

# 3. Check for permission issues
GET /api/permissions/sync/check

# 4. Get suggestions to fix issues
GET /api/permissions/sync/suggestions
```

### Example 2: Add New Screen

```bash
# 1. Create new screen
POST /api/menu-screens
{
  "code": "PRODUCTS_LIST",
  "name": "Product List",
  "route": "/products/list",
  "menuId": "...",
  "tabId": "..."
}

# 2. Auto-link the screen to API endpoints
POST /api/screen-api/auto-link/screen/{screenId}

# 3. Grant role permissions
POST /api/role-menus
{
  "roleId": "...",
  "menuId": "...",
  "canView": true,
  "canCreate": true
}

# 4. Verify consistency
GET /api/permissions/sync/check
```

### Example 3: Frontend Permission Usage

```tsx
import { usePermission } from '@/hooks/usePermission'
import { PermissionGuard } from '@/components/PermissionGuard'
import { Button } from '@/components/ui/button'

export default function ProductsPage() {
  const { canView, canCreate, canEdit, canDelete, canExport } = usePermission()

  if (!canView('/products')) {
    return <div>Access Denied</div>
  }

  return (
    <div>
      <h1>Products</h1>

      <PermissionGuard route="/products" permission="canCreate">
        <Button>Create Product</Button>
      </PermissionGuard>

      <PermissionGuard route="/products" permission="canExport">
        <Button>Export to Excel</Button>
      </PermissionGuard>

      <ProductTable
        canEdit={canEdit('/products')}
        canDelete={canDelete('/products')}
      />
    </div>
  )
}
```

---

## Benefits

### 1. Reduced Manual Work
- No need to manually link every screen to every API endpoint
- Auto-discovery handles the linking automatically

### 2. Improved Consistency
- Permission sync tool catches mismatches
- Ensures role permissions match API requirements

### 3. Better Security
- Frontend permission guards prevent unauthorized UI access
- Server-side permissions remain enforced

### 4. Easier Maintenance
- Bulk operations for managing multiple endpoints
- Clear suggestions for fixing permission issues

### 5. Developer Experience
- Simple React hooks for permission checks
- Reusable permission guard components
- Type-safe TypeScript interfaces

---

## Migration Guide

### For Existing Screens

1. **Run Auto-Link for All Screens:**
   ```bash
   POST /api/screen-api/auto-link/all
   ```

2. **Check for Issues:**
   ```bash
   GET /api/permissions/sync/check
   ```

3. **Fix Issues Following Suggestions:**
   ```bash
   GET /api/permissions/sync/suggestions
   ```

### For New Screens

1. Create the screen via API or admin UI
2. Auto-link: `POST /api/screen-api/auto-link/screen/{screenId}`
3. Verify: `GET /api/permissions/sync/check`

---

## Troubleshooting

### Screen Not Auto-Linking

**Problem:** Auto-link returns 0 linked endpoints

**Solutions:**
- Check if screen has a valid route
- Ensure API endpoints exist (run `/api/api-endpoints/scan`)
- Verify route pattern matches API pattern (e.g., `/users` should match `/api/users`)

### Permission Mismatch Warnings

**Problem:** Permission sync shows mismatches

**Solutions:**
- Follow suggestions from `/api/permissions/sync/suggestions`
- Update RoleMenu permissions to match required API permissions
- Or remove unnecessary API endpoint links

### Frontend Permission Not Working

**Problem:** `usePermission` hook returns false for all checks

**Solutions:**
- Ensure user menu tree is loaded (check `/api/users/me/menus`)
- Verify user has roles assigned
- Check that role has menu permissions via RoleMenu

---

## Configuration

### Auto-Link Route Patterns

To customize route pattern extraction, modify:
```java
ScreenApiAutoLinkService.extractBaseResource(String route)
```

### Permission Mapping

To customize HTTP method to permission mapping, modify:
```java
ScreenApiAutoLinkService.determinePermissionType(HttpMethod method)
```

---

## Future Enhancements

1. **Visual Admin UI:**
   - Drag-and-drop menu builder
   - Visual permission matrix
   - One-click auto-link from UI

2. **Advanced Pattern Matching:**
   - Support for more complex route patterns
   - Custom mapping rules per screen

3. **Permission Testing:**
   - Simulate user permissions
   - Test permission flows

4. **Audit Logging:**
   - Track permission changes
   - Log auto-link operations

---

## Support

For questions or issues, please refer to:
- Main documentation: `/docs/MENU_SYSTEM_ARCHITECTURE.md`
- API documentation: Swagger UI at `/swagger-ui.html`
- Source code: `/src/main/java/com/neobrutalism/crm/domain/`

---

**Last Updated:** 2025-10-30
**Version:** 1.0.0
