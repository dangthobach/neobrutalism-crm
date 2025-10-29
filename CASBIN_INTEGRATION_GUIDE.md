# Casbin Integration Guide - Neobrutalism CRM

## Tổng quan Hệ thống Phân quyền

Hệ thống sử dụng **jCasbin** để quản lý phân quyền với các tính năng:

1. **RBAC (Role-Based Access Control)** - Phân quyền theo vai trò
2. **Multi-tenancy** - Hỗ trợ nhiều tenant (tổ chức)
3. **Data Scope Control** - Kiểm soát phạm vi dữ liệu theo 3 cấp độ
4. **Hierarchical Resources** - Cấu trúc phân cấp Menu → Tab → Screen → API
5. **Branch-based Filtering** - Lọc dữ liệu theo chi nhánh

---

## Kiến trúc Tổng thể

```
User → Roles → Permissions → Resources (APIs/Menus)
  ↓
Branch → Data Scope (ALL_BRANCHES / CURRENT_BRANCH / SELF_ONLY)
  ↓
Casbin Enforcer → Check Permission → Allow/Deny
```

---

## 1. Cấu trúc Database

### Bảng `branches`
```sql
- id: UUID
- code: VARCHAR(50) - Mã chi nhánh (HQ, HN-001, HCM-002)
- name: VARCHAR(200)
- organization_id: UUID
- parent_id: UUID - Chi nhánh cha (hỗ trợ hierarchy)
- level: INTEGER - Cấp độ (0=root, 1=level1)
- path: VARCHAR(500) - Đường dẫn /HQ/HN/HN-001
- branch_type: ENUM(HQ, REGIONAL, LOCAL)
- status: ENUM(ACTIVE, INACTIVE, CLOSED)
```

### Bảng `users` - Cập nhật
```sql
- branch_id: UUID
- data_scope: ENUM(ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)
```

### Bảng `casbin_rule`
```sql
- ptype: VARCHAR(100) - p, g, g2
- v0: VARCHAR(100) - subject (user/role)
- v1: VARCHAR(100) - domain (tenant)
- v2: VARCHAR(100) - object (resource)
- v3: VARCHAR(100) - action
- v4: VARCHAR(100) - effect (allow/deny)
- v5: VARCHAR(100) - scope (optional)
```

---

## 2. Casbin Model

File: `src/main/resources/casbin/model.conf`

```ini
[request_definition]
r = sub, dom, obj, act, scope

[policy_definition]
p = sub, dom, obj, act, eft

[role_definition]
g = _, _, _
g2 = _, _, _

[policy_effect]
e = some(where (p.eft == allow)) && !some(where (p.eft == deny))

[matchers]
m = g(r.sub, p.sub, r.dom) && r.dom == p.dom && regexMatch(r.obj, p.obj) && regexMatch(r.act, p.act)
```

**Giải thích:**
- `r = sub, dom, obj, act, scope`: Request gồm subject (user), domain (tenant), object (resource), action, scope
- `g = _, _, _`: User-Role mapping với tenant
- `g2 = _, _, _`: Role hierarchy
- **regexMatch**: Cho phép dùng regex patterns (VD: `/api/users.*`, `(GET)|(POST)`)

---

## 3. Data Scope - Phạm vi Dữ liệu

### 3 Cấp độ Data Scope

#### **ALL_BRANCHES** (Management Role)
- Xem tất cả dữ liệu của tất cả branches
- Không filter theo branch
- Dành cho: ROLE_ADMIN, ROLE_MANAGER

```java
// Không có filter
List<User> users = userRepository.findAll();
```

#### **CURRENT_BRANCH** (ORC Role)
- Xem dữ liệu của branch hiện tại + các branch con
- Filter theo `branchId IN (accessible_branch_ids)`
- Dành cho: ROLE_ORC (Operation Risk Control)

```java
// Auto filter theo branch
Specification<User> spec = DataScopeSpecification.<User>create();
List<User> users = userRepository.findAll(spec);
```

#### **SELF_ONLY** (Maker/Checker Role)
- Chỉ xem bản ghi do chính mình tạo
- Filter theo `createdBy = current_user_id`
- Dành cho: ROLE_MAKER, ROLE_CHECKER, ROLE_USER

```java
// Auto filter theo createdBy
Specification<User> spec = DataScopeSpecification.<User>create();
List<User> users = userRepository.findAll(spec);
```

---

## 4. Sử dụng PermissionService

### 4.1. Kiểm tra Permission

```java
@Autowired
private PermissionService permissionService;

// Kiểm tra user có quyền call API không
boolean canAccess = permissionService.hasPermission(
    userId,
    "default",  // tenant ID
    "/api/users",
    "POST"
);

if (canAccess) {
    // Cho phép thực hiện
} else {
    throw new AccessDeniedException("No permission");
}
```

### 4.2. Gán Role cho User

```java
// Gán ROLE_ADMIN cho user
permissionService.assignRoleToUser(
    userId,
    "ROLE_ADMIN",
    "default"
);

// Xóa role
permissionService.removeRoleFromUser(
    userId,
    "ROLE_ADMIN",
    "default"
);
```

### 4.3. Gán Permission cho Role

```java
// Thêm permission cho ROLE_MANAGER
permissionService.addPermissionForRole(
    "ROLE_MANAGER",
    "default",
    "/api/organizations.*",  // Regex pattern
    "(GET)|(POST)"           // GET hoặc POST
);

// Xóa permission
permissionService.removePermissionFromRole(
    "ROLE_MANAGER",
    "default",
    "/api/organizations.*",
    "(GET)|(POST)"
);
```

### 4.4. Batch Add Permissions

```java
List<PermissionRequest> permissions = List.of(
    new PermissionRequest("/api/users", "GET"),
    new PermissionRequest("/api/users", "POST"),
    new PermissionRequest("/api/organizations", "GET")
);

permissionService.addPermissionsForRole(
    "ROLE_MANAGER",
    "default",
    permissions
);
```

---

## 5. Sử dụng Data Scope Specification

### 5.1. Auto Filter theo Data Scope

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        // Tự động filter theo data scope của user hiện tại
        Specification<User> spec = DataScopeSpecification.<User>create();
        return userRepository.findAll(spec);
    }

    public List<User> getActiveUsers() {
        // Kết hợp với các specification khác
        Specification<User> spec = DataScopeSpecification.<User>create()
            .and((root, query, cb) -> cb.equal(root.get("status"), UserStatus.ACTIVE));

        return userRepository.findAll(spec);
    }
}
```

### 5.2. Custom Branch Field

Nếu entity không dùng `branchId` mà dùng tên field khác:

```java
Specification<Transaction> spec = DataScopeSpecification
    .createWithBranchField("merchantBranchId");

List<Transaction> transactions = transactionRepository.findAll(spec);
```

### 5.3. Custom Creator Field

Nếu entity không dùng `createdBy` mà dùng tên field khác:

```java
Specification<Order> spec = DataScopeSpecification
    .createWithCreatorField("ownerId");

List<Order> orders = orderRepository.findAll(spec);
```

---

## 6. Setup Data Scope Context

### 6.1. Trong Authentication Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && validateToken(token)) {
            User user = getUserFromToken(token);

            // Set DataScopeContext
            Set<UUID> accessibleBranchIds = calculateAccessibleBranches(user);

            DataScopeContext context = DataScopeContext.builder()
                .userId(user.getId())
                .tenantId(user.getOrganizationId().toString())
                .dataScope(user.getDataScope())
                .branchId(user.getBranchId())
                .accessibleBranchIds(accessibleBranchIds)
                .build();

            DataScopeContext.set(context);

            // Set TenantContext
            TenantContext.setTenantId(user.getOrganizationId().toString());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            DataScopeContext.clear();
            TenantContext.clear();
        }
    }

    private Set<UUID> calculateAccessibleBranches(User user) {
        if (user.getDataScope() == DataScope.ALL_BRANCHES) {
            return Set.of(); // Empty = all branches
        }

        if (user.getDataScope() == DataScope.CURRENT_BRANCH) {
            // Lấy branch hiện tại + các branch con
            return branchService.getAllChildBranches(user.getBranchId());
        }

        return Set.of(); // SELF_ONLY không cần branch IDs
    }
}
```

---

## 7. Ví dụ Thực tế

### Ví dụ 1: User với ROLE_ORC xem danh sách users

```java
// User context
User currentUser = getCurrentUser();
// - branchId = "HN-001"
// - dataScope = CURRENT_BRANCH
// - accessibleBranchIds = ["HN-001", "HN-001-001", "HN-001-002"]

// Service method
public List<User> getAllUsers() {
    Specification<User> spec = DataScopeSpecification.<User>create();
    return userRepository.findAll(spec);
}

// SQL được sinh ra:
// SELECT * FROM users
// WHERE branch_id IN ('HN-001', 'HN-001-001', 'HN-001-002')
// AND deleted = false
```

### Ví dụ 2: User với ROLE_MAKER tạo order

```java
// 1. Check permission
boolean canCreate = permissionService.hasPermission(
    userId,
    "default",
    "/api/orders",
    "POST"
);

if (!canCreate) {
    throw new AccessDeniedException("No permission to create order");
}

// 2. Create order
Order order = new Order();
order.setCreatedBy(currentUser.getId().toString());
order.setBranchId(currentUser.getBranchId());
orderRepository.save(order);

// 3. Chỉ xem orders của mình
public List<Order> getMyOrders() {
    Specification<Order> spec = DataScopeSpecification.<Order>create();
    return orderRepository.findAll(spec);
    // WHERE created_by = currentUser.getId()
}
```

### Ví dụ 3: Hierarchical Menu Access

```java
// User có ROLE_USER
// Policy: p, ROLE_USER, default, /menu/dashboard.*, GET, allow

// Check có quyền xem menu không
boolean canView = permissionService.hasPermission(
    userId,
    "default",
    "/menu/dashboard",
    "GET"
); // true

boolean canViewReports = permissionService.hasPermission(
    userId,
    "default",
    "/menu/dashboard/reports",
    "GET"
); // true (match với /menu/dashboard.*)

boolean canViewAdmin = permissionService.hasPermission(
    userId,
    "default",
    "/menu/admin/users",
    "GET"
); // false (không match)
```

---

## 8. Best Practices

### 8.1. Naming Conventions

**Roles:**
- `ROLE_ADMIN` - Quản trị viên hệ thống
- `ROLE_MANAGER` - Quản lý chi nhánh/tổ chức
- `ROLE_ORC` - Operation Risk Control
- `ROLE_MAKER` - Người tạo giao dịch
- `ROLE_CHECKER` - Người duyệt giao dịch
- `ROLE_USER` - Người dùng thường

**Resources:**
- API: `/api/users`, `/api/organizations.*`
- Menu: `/menu/dashboard`, `/menu/admin.*`
- Tab: `/menu/dashboard/overview`, `/menu/dashboard/analytics`
- Screen: `/screen/user-list`, `/screen/user-create`

**Actions:**
- HTTP methods: `GET`, `POST`, `PUT`, `DELETE`
- Patterns: `(GET)|(POST)`, `(PUT)|(DELETE)`, `.*` (all)

### 8.2. Testing Permissions

```java
@Test
public void testUserPermissions() {
    // Setup
    UUID userId = UUID.randomUUID();
    permissionService.assignRoleToUser(userId, "ROLE_USER", "default");
    permissionService.addPermissionForRole("ROLE_USER", "default", "/api/users/me", "GET");

    // Test
    boolean canAccess = permissionService.hasPermission(userId, "default", "/api/users/me", "GET");
    assertTrue(canAccess);

    boolean canDelete = permissionService.hasPermission(userId, "default", "/api/users/me", "DELETE");
    assertFalse(canDelete);
}
```

### 8.3. Reload Policies

Sau khi thêm/xóa policies qua SQL trực tiếp:

```java
permissionService.reloadPolicy();
```

---

## 9. Troubleshooting

### Issue 1: TenantId null

**Problem:** Log hiển thị "Tenant ID is null"

**Solution:**
```properties
# application.properties
app.tenant.default-tenant-id=default
app.tenant.enabled=true
app.tenant.strict-mode=false
```

### Issue 2: Data không filter

**Problem:** Data Scope không hoạt động

**Solution:**
- Kiểm tra `DataScopeContext.hasContext()` = true
- Kiểm tra entity có field `branchId` và `createdBy`
- Verify user có `dataScope` được set

```java
// Debug
DataScopeContext context = DataScopeContext.get();
System.out.println("Context: " + context);
System.out.println("DataScope: " + DataScopeContext.getCurrentDataScope());
```

### Issue 3: Permission denied

**Problem:** User có role nhưng vẫn bị deny

**Solution:**
```java
// Check policies
List<String> roles = permissionService.getRolesForUser(userId, "default");
System.out.println("User roles: " + roles);

List<List<String>> permissions = permissionService.getPermissionsForRole("ROLE_USER", "default");
System.out.println("Role permissions: " + permissions);

// Reload policies
permissionService.reloadPolicy();
```

---

## 10. API Examples

### 10.1. Thêm Role mới

```bash
# POST /api/roles
{
  "code": "ROLE_AUDITOR",
  "name": "Auditor",
  "description": "Can view audit logs",
  "isSystem": false
}

# Thêm permissions
curl -X POST http://localhost:8080/api/permissions/roles/ROLE_AUDITOR \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "default",
    "permissions": [
      {"resource": "/api/audit-logs", "action": "GET"},
      {"resource": "/api/audit-logs/.*", "action": "GET"}
    ]
  }'
```

### 10.2. Gán Role cho User

```bash
# POST /api/users/{userId}/roles
curl -X POST http://localhost:8080/api/users/123e4567-e89b-12d3-a456-426614174000/roles \
  -H "Content-Type: application/json" \
  -d '{
    "roleCode": "ROLE_AUDITOR",
    "tenantId": "default"
  }'
```

### 10.3. Check Permission

```bash
# GET /api/permissions/check
curl -X GET "http://localhost:8080/api/permissions/check?resource=/api/users&action=POST" \
  -H "Authorization: Bearer <token>"

# Response
{
  "success": true,
  "data": {
    "hasPermission": true
  }
}
```

---

## 11. Mở rộng

### 11.1. Thêm Custom Matcher

Nếu cần logic phức tạp hơn, có thể viết custom matcher:

```java
enforcer.addFunction("myCustomFunction", new CustomFunction() {
    @Override
    public AviatorObject call(Map<String, Object> env,
                              AviatorObject arg1, AviatorObject arg2) {
        // Custom logic
        return AviatorBoolean.valueOf(result);
    }
});
```

### 11.2. Caching Policies

jCasbin tự động cache policies. Nếu cần clear cache:

```java
enforcer.clearCache();
```

### 11.3. Audit Logging

Log tất cả permission checks:

```java
@Aspect
@Component
public class PermissionAuditAspect {

    @Around("execution(* PermissionService.hasPermission(..))")
    public Object auditPermissionCheck(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        boolean result = (boolean) pjp.proceed();

        // Log to audit table
        auditService.log(
            "PERMISSION_CHECK",
            "userId=" + args[0] + ", resource=" + args[2] + ", result=" + result
        );

        return result;
    }
}
```

---

## Tổng kết

Hệ thống Casbin integration này cung cấp:

✅ **RBAC hoàn chỉnh** với user-role-permission
✅ **Multi-tenancy** với tenant isolation
✅ **Data scope control** 3 cấp độ
✅ **Branch hierarchy** với parent-child relationships
✅ **Regex-based permissions** linh hoạt
✅ **JPA Specification** tự động filter data
✅ **Thread-safe context** cho concurrent requests

**Contact:** Xem thêm tại [Casbin Documentation](https://casbin.org/docs/overview)
