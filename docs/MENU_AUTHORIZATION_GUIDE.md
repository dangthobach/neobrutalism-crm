# Menu Authorization & Role-Based Access Control Guide

## Tổng quan

Hệ thống menu sử dụng **Casbin** để quản lý quyền truy cập dựa trên Role (RBAC - Role-Based Access Control). Mỗi menu có thể được gán quyền cho các role khác nhau.

## Kiến trúc phân quyền

### 1. Cấu trúc dữ liệu

#### Menu (menus table)
```sql
- id: UUID
- code: String (unique) -- Dùng để identify menu
- name: String
- route: String -- URL path
- parent_id: UUID (nullable)
- is_visible: Boolean
- requires_auth: Boolean -- Menu yêu cầu đăng nhập
```

#### Role Menu (role_menus table)
```sql
- role_id: UUID
- menu_id: UUID
- can_view: Boolean
- can_create: Boolean
- can_edit: Boolean
- can_delete: Boolean
- can_export: Boolean
- can_import: Boolean
```

### 2. Quy trình phân quyền

```
User → Role → RoleMenu → Menu → Screen → API Endpoints
```

1. **User** được gán một hoặc nhiều **Role**
2. **Role** có quyền truy cập các **Menu** thông qua bảng **RoleMenu**
3. **Menu** chứa các **Screen** (màn hình cụ thể)
4. **Screen** mapping với **API Endpoints**
5. Khi user truy cập, hệ thống kiểm tra:
   - User có role nào?
   - Role đó có quyền access menu này không?
   - Loại quyền: view, create, edit, delete, export, import

## Cách kiểm tra Menu đã sync vào Database

### Bước 1: Truy cập Menu Management
```
URL: http://localhost:3000/admin/menus
```

### Bước 2: Xem Audit Report
Tại đầu trang, bạn sẽ thấy card **"Menu Synchronization Status"**

Click **"Run Audit"** để kiểm tra:
- ✅ **Matched**: Menus đã sync thành công
- ❌ **Missing in Database**: Menus chưa sync (cần sync)
- ⚠️ **Extra in Database**: Menus thừa trong DB (custom menus)

### Bước 3: Sync Menu nếu thiếu
Nếu có menu **Missing in Database**, click:
1. **"Sync from UI"** button ở header
2. Hoặc **"Preview UI Menus"** → **"Sync to Database"**

## Cách cấp quyền Menu cho Role

### Phương pháp 1: Qua UI (Khuyến nghị)

#### Bước 1: Truy cập Role Permissions
```
URL: http://localhost:3000/admin/permissions/roles
```

#### Bước 2: Chọn Role cần cấp quyền
- Click vào role card (ví dụ: SUPER_ADMIN, ADMIN, USER)

#### Bước 3: Tab "Menu Access"
- Chọn tab **"Menu Access"**
- Xem danh sách tất cả menus
- Tick checkbox để cấp quyền:
  - **View**: Xem menu
  - **Create**: Tạo mới
  - **Edit**: Chỉnh sửa
  - **Delete**: Xóa
  - **Export**: Xuất dữ liệu
  - **Import**: Nhập dữ liệu

#### Bước 4: Save
- Click **"Save Changes"**
- Permissions được lưu vào database

### Phương pháp 2: Qua API (Programmatic)

#### API Endpoint: Assign Menu to Role
```http
POST /api/roles/{roleId}/menus
Content-Type: application/json

{
  "menuId": "018e0010-0000-0000-0000-000000000001",
  "permissions": {
    "canView": true,
    "canCreate": true,
    "canEdit": true,
    "canDelete": false,
    "canExport": true,
    "canImport": false
  }
}
```

#### API Endpoint: Get Role's Menus
```http
GET /api/roles/{roleId}/menus
```

Response:
```json
{
  "success": true,
  "data": [
    {
      "id": "018e0010-0000-0000-0000-000000000001",
      "code": "USERS",
      "name": "Users",
      "route": "/admin/users",
      "permissions": {
        "canView": true,
        "canCreate": true,
        "canEdit": true,
        "canDelete": false,
        "canExport": true,
        "canImport": false
      }
    }
  ]
}
```

### Phương pháp 3: Qua Database (Direct SQL)

```sql
-- Cấp full quyền menu USERS cho role ADMIN
INSERT INTO role_menus (
  role_id,
  menu_id,
  can_view, can_create, can_edit, can_delete, can_export, can_import
) VALUES (
  (SELECT id FROM roles WHERE code = 'ADMIN'),
  (SELECT id FROM menus WHERE code = 'USERS'),
  true, true, true, true, true, true
);

-- Cấp quyền view-only menu REPORTS cho role USER
INSERT INTO role_menus (
  role_id,
  menu_id,
  can_view, can_create, can_edit, can_delete, can_export, can_import
) VALUES (
  (SELECT id FROM roles WHERE code = 'USER'),
  (SELECT id FROM menus WHERE code = 'REPORTS'),
  true, false, false, false, false, false
);
```

## Ví dụ: Setup quyền cho 3 roles cơ bản

### 1. SUPER_ADMIN - Full Access
```sql
-- Cấp toàn bộ quyền cho tất cả menus
INSERT INTO role_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import)
SELECT
  (SELECT id FROM roles WHERE code = 'SUPER_ADMIN'),
  id,
  true, true, true, true, true, true
FROM menus;
```

### 2. ADMIN - Management Access
```sql
-- Admin có quyền quản lý users, roles, groups nhưng không có quyền quản lý permissions
INSERT INTO role_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import)
SELECT
  (SELECT id FROM roles WHERE code = 'ADMIN'),
  id,
  true, true, true, true, true, false
FROM menus
WHERE code IN ('USERS', 'ROLES', 'GROUPS', 'ORGANIZATIONS');
```

### 3. USER - View Only
```sql
-- User chỉ có quyền xem
INSERT INTO role_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import)
SELECT
  (SELECT id FROM roles WHERE code = 'USER'),
  id,
  true, false, false, false, false, false
FROM menus
WHERE code IN ('USERS', 'TASKS', 'NOTIFICATIONS');
```

## Kiểm tra quyền trong Code

### Frontend - React Component

```typescript
import { PermissionGuard } from "@/components/auth/permission-guard"

// Check quyền theo route
<PermissionGuard routeOrCode="/admin/users" permission="canEdit">
  <Button>Edit User</Button>
</PermissionGuard>

// Check quyền theo menu code
<PermissionGuard routeOrCode="USERS" permission="canDelete">
  <Button variant="destructive">Delete</Button>
</PermissionGuard>

// Multiple permissions
<PermissionGuard
  routeOrCode="/admin/users"
  permission="canCreate"
  fallback={<div>You don't have permission</div>}
>
  <CreateUserForm />
</PermissionGuard>
```

### Backend - Java Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // Kiểm tra quyền qua Casbin
    @GetMapping
    @PreAuthorize("hasPermission('/users', 'canView')")
    public ResponseEntity<List<User>> getUsers() {
        // ...
    }

    @PostMapping
    @PreAuthorize("hasPermission('/users', 'canCreate')")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // ...
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('/users', 'canEdit')")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User user) {
        // ...
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('/users', 'canDelete')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        // ...
    }
}
```

### Casbin Policy Format

```csv
# p, sub, obj, act
# subject (role), object (resource), action (permission)

p, SUPER_ADMIN, /admin/users, canView
p, SUPER_ADMIN, /admin/users, canCreate
p, SUPER_ADMIN, /admin/users, canEdit
p, SUPER_ADMIN, /admin/users, canDelete

p, ADMIN, /admin/users, canView
p, ADMIN, /admin/users, canCreate
p, ADMIN, /admin/users, canEdit

p, USER, /admin/users, canView
```

## Troubleshooting

### Lỗi: "Access Denied" hoặc 403 Forbidden

**Nguyên nhân:**
- User không có role được assign menu
- Role không có permission cho action đó
- Menu chưa được sync vào database

**Giải pháp:**
1. Kiểm tra role của user:
```sql
SELECT r.code, r.name
FROM roles r
JOIN user_roles ur ON ur.role_id = r.id
JOIN users u ON u.id = ur.user_id
WHERE u.username = 'admin';
```

2. Kiểm tra permissions của role:
```sql
SELECT m.code, m.name, rm.*
FROM role_menus rm
JOIN menus m ON m.id = rm.menu_id
JOIN roles r ON r.id = rm.role_id
WHERE r.code = 'ADMIN';
```

3. Cấp quyền cho role:
```sql
-- Via UI: /admin/permissions/roles/{roleCode}
-- Hoặc via SQL như ở trên
```

### Menu không hiển thị trên sidebar

**Nguyên nhân:**
- Menu chưa sync vào database
- User không có quyền `canView`
- Menu có `is_visible = false`

**Giải pháp:**
1. Run audit: `/admin/menus` → Click "Run Audit"
2. Sync nếu thiếu: Click "Sync from UI"
3. Kiểm tra permissions: `/admin/permissions/roles`
4. Kiểm tra visibility:
```sql
UPDATE menus SET is_visible = true WHERE code = 'USERS';
```

### Quyền bị cache, không update ngay

**Nguyên nhân:**
- Redis/Caffeine cache chưa được clear

**Giải pháp:**
```bash
# Clear cache trong application.yml
# Hoặc logout và login lại
# Hoặc restart backend
```

## Best Practices

### 1. Principle of Least Privilege
- Chỉ cấp quyền tối thiểu cần thiết
- SUPER_ADMIN: Full access
- ADMIN: Management access
- USER: Read-only hoặc limited write

### 2. Menu Hierarchy
- Menu cha: Dùng để nhóm, không cần route
- Menu con: Có route cụ thể, check permissions

### 3. Regular Audit
- Định kỳ kiểm tra menu sync status
- Review role permissions quarterly
- Remove unused menus

### 4. Testing
```typescript
// Test permissions
describe('Menu Permissions', () => {
  it('ADMIN can view users', async () => {
    const hasPermission = await checkPermission('ADMIN', '/admin/users', 'canView')
    expect(hasPermission).toBe(true)
  })

  it('USER cannot delete users', async () => {
    const hasPermission = await checkPermission('USER', '/admin/users', 'canDelete')
    expect(hasPermission).toBe(false)
  })
})
```

## Quick Start Checklist

- [ ] 1. Sync menus vào database (`/admin/menus` → "Sync from UI")
- [ ] 2. Verify menus đã sync ("Run Audit")
- [ ] 3. Tạo roles nếu chưa có (`/admin/roles`)
- [ ] 4. Cấp quyền menus cho roles (`/admin/permissions/roles/{roleCode}`)
- [ ] 5. Assign roles cho users (`/admin/users/{userId}/roles`)
- [ ] 6. Test login với user có role
- [ ] 7. Verify menus hiển thị đúng trên sidebar
- [ ] 8. Test CRUD operations với các quyền khác nhau

## Support

Nếu gặp vấn đề:
1. Check backend logs
2. Check browser console
3. Verify database data
4. Clear cache và refresh
5. Contact dev team
