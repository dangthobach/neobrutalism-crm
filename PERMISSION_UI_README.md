# Permission System UI - Implementation Guide

## 📋 Overview

Hệ thống quản trị phân quyền đã được triển khai đầy đủ với UI theo thiết kế trong `role_permission_ui_design.md`.

## 🎯 Features Implemented

### 1. **User Management Screen (3-Pane Layout)**
- **Location**: `/admin/permissions/users`
- **Features**:
  - Left pane: Danh sách users với search & filter (team, role, status)
  - Middle pane: User details, teams/groups, và role assignments
  - Right pane: Effective access (menus & permissions từ tất cả roles)
  - Virtual scroll support cho danh sách lớn
  - Real-time filter và search

### 2. **Role Detail Screen (3 Tabs)**
- **Location**: `/admin/permissions/roles/[roleCode]`
- **Tab A - General Info**:
  - Role information (code, name, description, priority)
  - Data scope management
  - Status management (Active/Inactive)
  - Users in role với table view
  - System role protection

- **Tab B - Menu Access**:
  - Hierarchical menu tree với checkbox 3 trạng thái
  - Expand/collapse all functionality
  - Menu filter/search
  - Access preview panel
  - Warnings khi có permission mismatch với menu
  - Auto-enable menu khi có permission

- **Tab C - Permission Matrix**:
  - Permission matrix grouped by feature/screen
  - Clone permissions từ role khác
  - Export to Excel functionality
  - Risk level badges (LOW/MEDIUM/HIGH/CRITICAL)
  - Permission dependencies checking
  - Auto-enable VIEW khi có quyền cao hơn
  - Summary statistics

### 3. **Permission Audit Screen**
- **Location**: `/admin/permissions/audit`
- **Features**:
  - Search permissions by code, module, description
  - Filter by permission code và module/feature
  - Audit results table showing:
    - Permission details
    - Roles có permission đó
    - User count per role
    - Risk level
  - Slide-over details panel showing:
    - Full permission info
    - All roles với permission
    - All users per role
  - Export to CSV functionality

### 4. **Permission System Overview**
- **Location**: `/admin/permissions/overview`
- **Features**:
  - Statistics cards (Users, Roles, Groups, Permissions)
  - Security alerts section
  - Quick actions navigation cards
  - Recent roles activity list
  - Risk metrics và warnings

## 📁 File Structure

```
src/
├── types/
│   └── permission.ts              # TypeScript interfaces for all domain models
├── data/
│   └── mock-permissions.ts        # Mock data & helper functions
├── app/
│   └── admin/
│       └── permissions/
│           ├── overview/
│           │   └── page.tsx       # Overview dashboard
│           ├── users/
│           │   └── page.tsx       # User Management (3-pane)
│           ├── roles/
│           │   └── [roleCode]/
│           │       ├── page.tsx   # Role Detail main
│           │       └── tabs/
│           │           ├── general-tab.tsx
│           │           ├── menu-access-tab.tsx
│           │           └── permission-matrix-tab.tsx
│           └── audit/
│               └── page.tsx       # Permission Audit
```

## 🔧 Data Models

### Backend Entities (Java)
- `User` - User entity với tenant awareness
- `Role` - Role entity với code, name, priority
- `Group` - Group/Team với hierarchy support
- `Menu` - Top-level menu items
- `MenuTab` - Second-level tabs under menus
- `MenuScreen` - Leaf-level screens
- `ApiEndpoint` - REST API endpoints
- Junction tables: `UserRole`, `UserGroup`, `GroupRole`, `RoleMenu`, `ScreenApi`

### Frontend Types (TypeScript)
Tất cả types được định nghĩa trong `src/types/permission.ts`:
- `User`, `Role`, `Group`, `Menu`, `MenuTab`, `MenuScreen`, `ApiEndpoint`
- Extended types: `UserWithRoles`, `MenuTreeNode`, `PermissionMatrix`
- Enums: `UserStatus`, `RoleStatus`, `GroupStatus`, `HttpMethod`

## 📊 Mock Data

Mock data được tạo trong `src/data/mock-permissions.ts`:
- **4 users**: nva01, ttb02, lvc03, ptd04
- **4 roles**: ROLE_ADMIN, ROLE_DEBT_SALE_MGR, ROLE_WH_ADMIN, ROLE_CASE_VIEWER
- **4 groups**: TEAM_DV, TEAM_ORC, TEAM_WH, TEAM_WH_NORTH (hierarchical)
- **4 menus**: Dashboard, Debt Sale, Warehouse, Administration
- **4 tabs**: Debt Dashboard, Debt List, WH Inventory, WH Transfer
- **4 screens**: Debt Sale Dashboard, Debt Detail, Box Inventory, Transfer Request
- **9 permissions**: VIEW_DASHBOARD, APPROVE_DEBT_SALE, REJECT_DEBT_SALE, VIEW_INVENTORY, DELETE_BOX, CREATE_REQUEST, EDIT_REQUEST, CANCEL_REQUEST, APPROVE_TRANSFER

### Helper Functions
- `getUserWithRoles(userId)` - Get user với roles, groups, effective menus & permissions
- `buildMenuTree()` - Build hierarchical menu tree
- `getPermissionMatrixForRole(roleCode)` - Get permission matrix grouped by feature

## 🎨 UI Components Used

- **shadcn/ui components**:
  - Card, Button, Badge, Input, Select
  - Table, Checkbox, RadioGroup
  - Tabs, Sheet, ScrollArea
  - Alert, Separator, Label

## 🚀 How to Use

### 1. Navigate to Permission System
```
http://localhost:3000/admin/permissions/overview
```

### 2. User Management
```
http://localhost:3000/admin/permissions/users
```
- Search/filter users
- Click user để xem details
- View effective access từ all roles

### 3. Role Management
```
http://localhost:3000/admin/permissions/roles/ROLE_ADMIN
```
- View/edit role general info
- Configure menu access
- Set permission matrix
- Clone permissions from another role

### 4. Permission Audit
```
http://localhost:3000/admin/permissions/audit
```
- Search permissions
- Filter by module
- View role & user assignments
- Export audit report

## 🔐 Business Logic Implemented

### 1. Permission Implies Visibility
- Nếu role có permission trên screen X → tự động enable menu X
- Warning hiển thị khi có permission nhưng menu bị tắt

### 2. Action Hierarchy
- APPROVE/EDIT/DELETE → tự động enable VIEW
- VIEW bị lock nếu có permission cao hơn

### 3. Permission Dependencies
- Permission có thể require permission khác
- Warning hiển thị khi dependency không được satisfy
- Example: APPROVE_DEBT_SALE requires VIEW_DASHBOARD

### 4. Risk Level Management
- Permission có risk level: LOW, MEDIUM, HIGH, CRITICAL
- High risk permissions được highlight
- Risk statistics trong overview

### 5. Multi-Role Support
- User có thể có multiple roles
- Effective permissions = union of all role permissions
- Effective menus = union of all role menus

## 📝 Next Steps (Integration với Backend)

### 1. Replace Mock Data với API Calls
```typescript
// Example: Fetch users
const response = await fetch('/api/users?search=&team=&role=&status=');
const users = await response.json();
```

### 2. Implement Save Functions
```typescript
// Example: Save role permissions
const response = await fetch(`/api/roles/${roleCode}/permissions`, {
  method: 'PUT',
  body: JSON.stringify(permissions)
});
```

### 3. Add Real-time Updates
- WebSocket hoặc polling cho live updates
- Optimistic updates với rollback on error

### 4. Add Audit Trail
- Log tất cả permission changes
- Track who changed what when
- Display audit trail in UI

### 5. Add Bulk Operations
- Bulk assign roles to users
- Bulk grant permissions
- Import/export via CSV/Excel

## 🎯 Key Features Highlights

✅ **3-Pane User Management** - Intuitive layout với real-time effective access
✅ **Hierarchical Menu Tree** - Checkbox với 3 states (checked, unchecked, indeterminate)
✅ **Permission Matrix** - Grouped by feature với risk indicators
✅ **Clone Permissions** - Copy permissions từ role khác
✅ **Permission Dependencies** - Auto-enable required permissions
✅ **Audit Trail Ready** - Track all permission assignments
✅ **Export Functionality** - Export to CSV/Excel
✅ **Risk Management** - Risk level tracking và warnings
✅ **Multi-Role Support** - Users có multiple roles với union permissions
✅ **Mock Data** - Complete mock data sẵn sàng để test

## 🐛 Known Limitations (Mock Data)

1. Pagination chỉ client-side (cần server-side pagination cho production)
2. Search chỉ filter dữ liệu đã load (cần server-side search)
3. Export functions chưa implement thực tế (cần integrate với library)
4. No real-time updates (cần WebSocket/polling)
5. No audit trail persistence (cần backend support)

## 📚 References

- Design Document: `role_permission_ui_design.md`
- Backend Entities: `src/main/java/com/neobrutalism/crm/domain/*/model/*.java`
- API Endpoints: Cần implement theo RESTful best practices

---

**Developed with ❤️ following the design specifications**
