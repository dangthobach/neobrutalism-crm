# Thiết Kế Giao Diện Hệ Thống Quản Trị Phân Quyền

## 0. Mục Tiêu Hệ Thống

Hệ thống quản trị phân quyền cho phép:
- User tạo **Group/Team**, mỗi User có thể có **1 hoặc nhiều Role**.
- Mỗi Role được gán **cây Menu phân cấp** (Module → Menu → Submenu → Tab/Screen).
- Mỗi Screen/Feature có thể có **các Permission/API riêng**.
- Giao diện quản trị cần đảm bảo **logic phân quyền đúng đắn, trực quan, dễ sử dụng**, và **chịu được dữ liệu lớn**.

---

## 1. Tổng Quan Kiến Trúc Giao Diện

Gồm 3 màn hình chính:
1. **User Management (3-pane)** – Quản lý người dùng, role, team và quyền thực tế.
2. **Role Detail** – Quản lý chi tiết từng role với 3 tab con:
   - Tab A: General Info
   - Tab B: Menu Access
   - Tab C: Permission Matrix
3. **Permission Audit** – Tra cứu role và user nào có quyền cụ thể.

---

## 2. User Management Screen (3-Pane Layout)

### Wireframe
```txt
+------------------------------------------------------------------------------------------------------+
| Users (Filter/Search)           | User Detail / Membership                | Effective Access         |
|---------------------------------+-----------------------------------------+--------------------------|
| [ Search: ________________ ]    | [ Full Name        ]   [ Active ⬤ ]     |  Menus visible           |
| Filter by: Team [▼]  Role [▼]   | [ Username         ]   [ Locked ☐ ]     |  --------------------    |
|                                 | [ Email            ]                    |  ▾ Warehouse Module      |
|  ------------------------------------------------------                 |    - Box Inventory       |
|  > Nguyễn Văn A  (nva01)        | Teams / Groups                          |  Permissions / Actions   |
|  > Trần Thị B   (ttb02)         | Role(s): ROLE_CASE, ROLE_DEBT           |  ACTION_CODE | ROLE_SRC |
|  > ... (virtual scroll)         | [+ Assign Role]                         |  VIEW_CASE   | ROLE_CASE |
+------------------------------------------------------------------------------------------------------+
```

### Chức năng chính
- **Pane trái**: Danh sách user, filter theo team, role, status.
- **Pane giữa**: Thông tin user, danh sách team/role.
- **Pane phải**: Hiển thị quyền thực tế (union từ tất cả role).

### API Gợi ý
- `GET /users?search=&team=&role=&status=`
- `GET /users/{id}` → trả profile, teams, roles, effectiveMenus[], effectivePermissions[]
- `POST /users/{id}/roles`, `DELETE /users/{id}/roles/{code}`
- `GET /users/{id}/effective-access`

---

## 3. Role Detail Screen

### Header chung
```txt
+-----------------------------------------------------------------------------------------+
| < Back  ROLE_DEBT_SALE_MGR  [Active ⬤]   [ Save Changes ] [ Cancel ]                   |
| Tabs:  [ General Info ] [ Menu Access ] [ Permission Matrix ]                           |
+-----------------------------------------------------------------------------------------+
```

### 3.1. Tab A – General Info
```txt
+--------------------------------------------------------------------------------------+
| Role Info                                                                            |
| ------------------------------------------------------------------------------------ |
| Role Code:     [ ROLE_DEBT_SALE_MGR ] (read-only)                                   |
| Role Name:     [ Debt Sale Manager  ]                                               |
| Description:   [ Có quyền duyệt bán nợ ... ]                                        |
| Data Scope:    [WH_NORTH] [WH_SOUTH] [+]                                            |
| Status:        (● Active) (○ Deprecated)                                            |
| ------------------------------------------------------------------------------------ |
| Users in this Role:                                                                 |
| | User | Email | Team | Remove |                                                    |
| |------|-------|------|--------|                                                    |
| | A.Nguyễn | a.nguyen@... | TEAM_DV | x |                                          |
| [+ Add user]                                                                        |
+--------------------------------------------------------------------------------------+
```

### 3.2. Tab B – Menu Access (Role ↔ Menu)
```txt
+------------------------------------------------------------------------------------------------------+
| LEFT: Menu Tree (Editable)                 | RIGHT: Access Preview / Validation                      |
|--------------------------------------------+----------------------------------------------------------|
| [ Filter menu: ___ ] [Expand all]          | Screen / Route                   | Notes                |
| ▸ [x] DEBT SALE                            | Debt Sale Detail (/debt/:id)     | OK                   |
|    ▸ [x] Dashboard                         | WARNINGS                         |
|       [x] Debt Sale Detail Screen          | - APPROVE_DEBT_SALE granted but  |
|                                            |   Menu hidden                    |
+------------------------------------------------------------------------------------------------------+
```

**Logic:**
- Checkbox 3 trạng thái: [ ] unchecked, [x] checked, [-] indeterminate.
- Tick cha → tick toàn bộ con; bỏ hết con → cha unchecked.
- Warnings hiển thị khi có permission không tương ứng menu.

**API:**
- `GET /roles/{roleCode}/menus-tree`
- `PUT /roles/{roleCode}/menus-tree`

### 3.3. Tab C – Permission Matrix (Role ↔ Permission/API)

#### Vấn đề: mỗi Screen có các permission khác nhau → hiển thị động theo Feature.

#### Hướng 1: Ma trận dọc (đề xuất chính)
```txt
Debt Sale Dashboard (/debt-sale/dashboard)
-------------------------------------------------------------
| Action Code          | Description                  | ✔? |
|-----------------------|------------------------------|----|
| VIEW_DASHBOARD        | Xem dashboard bán nợ         | [x]|
| APPROVE_DEBT_SALE     | Phê duyệt bán nợ             | [x]|
| REJECT_DEBT_SALE      | Từ chối hồ sơ bán nợ         | [ ]|

Warehouse Transfer Request (/warehouse/transfer-request)
-------------------------------------------------------------
| Action Code          | Description                  | ✔? |
|-----------------------|------------------------------|----|
| CREATE_REQUEST        | Tạo phiếu điều chuyển        | [x]|
| EDIT_REQUEST          | Sửa phiếu điều chuyển        | [x]|
| CANCEL_REQUEST        | Hủy phiếu điều chuyển        | [ ]|
| APPROVE_TRANSFER      | Duyệt phiếu điều chuyển      | [ ]|
-------------------------------------------------------------
[ Save Changes ] | [ Clone from Role ▼ ] | [ Export Excel ]
-------------------------------------------------------------
! APPROVE_DEBT_SALE ⇒ VIEW_DASHBOARD auto-enabled.
! EDIT_REQUEST ⇒ requires CREATE_REQUEST.
```

**API:**
- `GET /roles/{roleCode}/permission-matrix` → trả danh sách feature + actions.
- `PUT /roles/{roleCode}/permission-matrix` → cập nhật quyền.

#### Mô hình dữ liệu permission
```sql
permission (
  id uuid,
  feature_id varchar,
  action_code varchar,
  action_label varchar,
  requires jsonb[],
  risk_level varchar
)

role_permission (
  role_code varchar,
  permission_id uuid,
  granted boolean
)
```

### 3.4. Modal Summary of Changes
```txt
+-----------------------------------------------------+
| Confirm Changes to ROLE_DEBT_SALE_MGR               |
|-----------------------------------------------------|
| MENU ACCESS                                         |
| + Enabled 'Debt Sale Dashboard'                     |
| - Disabled 'Warehouse Transfer Request'             |
| PERMISSIONS                                         |
| + Granted APPROVE on 'Debt Sale Dashboard'          |
| - Revoked DELETE_BOX on 'Box Inventory'             |
|-----------------------------------------------------|
| These changes affect 12 users                       |
| [ Confirm & Save ] [ Cancel ]                       |
+-----------------------------------------------------+
```

---

## 4. Permission Audit Screen

### Wireframe
```txt
+------------------------------------------------------------------------------------------------------+
| Permission Audit                                                                                    |
+------------------------------------------------------------------------------------------------------+
| Permission Code [ APPROVE_DEBT_SALE ▼ ]   Module [ Debt Sale ▼ ]    [ Search ]                      |
| Result                                                                                               |
| ---------------------------------------------------------------------------------------------------- |
| Permission | Role(s)             | #Users | Risk | Details                                            |
|-------------|--------------------|--------|------|---------------------------------------------------|
| APPROVE_DEBT_SALE | ROLE_DEBT_MGR | 12 | High | [ > ]                                               |
| BOX_DELETE        | ROLE_WH_ADMIN | 2  | Critical | [ > ]                                           |
+------------------------------------------------------------------------------------------------------+
```

**Slide-over Details:**
```txt
+-------------------------------- Details: APPROVE_DEBT_SALE --------------------------------+
| Role: ROLE_DEBT_SALE_MGR                                                                   |
| Users (12):                                                                                |
| 1. Nguyễn Văn A (TEAM_DV, ORC)                                                             |
| 2. Trần Thị B (TEAM_ORC)                                                                   |
| ...                                                                                        |
| [ Export CSV ]                                                                             |
+-------------------------------------------------------------------------------------------+
```

**API:**
- `GET /permissions/audit?permissionCode=...`
- `GET /permissions/audit/{permissionCode}/roles/{roleCode}`

---

## 5. Nguyên Tắc UX & Logic Tự Động

1. **Permission implies visibility**  
   - Nếu role có quyền API trên screen X → tự động bật menu X.
   - Nếu admin tắt menu X → cảnh báo “Quyền API mồ côi”.

2. **Action hierarchy**  
   - APPROVE, EDIT, DELETE ⇒ tự bật VIEW.
   - VIEW bị khóa nếu có quyền cao hơn cùng feature.

3. **Scope awareness**  
   - Role có scope (warehouse, branch, product...) → hiển thị badge “Scope: WH_NORTH”.

4. **Bulk operations**  
   - Tick header để cấp toàn bộ action.
   - "Clone Role" để sao chép toàn bộ ma trận.

5. **Change review before save**  
   - Modal “Summary of changes” trước khi ghi DB.
   - Log audit trail khi commit.

---

## 6. Entity & Mối Quan Hệ Cốt Lõi

| Entity | Mục đích | Ghi chú |
|---------|-----------|--------|
| user | Người dùng hệ thống | - |
| team | Nhóm / bộ phận | - |
| user_team | Quan hệ N-N | - |
| role | Vai trò | Có data scope |
| user_role | Quan hệ user-role | - |
| menu_item | Cấu trúc cây menu | parent_id, type = MODULE/MENU/SCREEN/TAB |
| role_menu_item | Phân quyền menu | role ↔ menu_item |
| permission | Quyền hành động | feature_id, action_code, label, requires |
| role_permission | Phân quyền chi tiết | role ↔ permission |

---

## 7. Tổng Kết

- **User Management:** 3-pane, xem quyền thực tế.
- **Role Detail:** 3 tab (General / Menu / Permission), có cảnh báo logic và modal review.
- **Permission Matrix:** hiển thị động theo Feature, hỗ trợ clone, export.
- **Permission Audit:** truy ngược từ quyền → role → user.
- **Logic bảo toàn:** auto-enable VIEW, cảnh báo khi mismatch, audit trail trước khi lưu.

> 👉 Kết hợp cấu trúc này, hệ thống phân quyền sẽ vừa **logic, audit-friendly, và trực quan**, sẵn sàng cho dữ liệu lớn và môi trường kiểm soát nội bộ (bank/enterprise).

