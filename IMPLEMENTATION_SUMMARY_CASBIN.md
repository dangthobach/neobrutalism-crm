# Implementation Summary - Casbin Integration & Branch-based Permissions

## Tổng quan

Đã hoàn thành việc tích hợp **jCasbin** vào hệ thống CRM với đầy đủ tính năng phân quyền phân cấp, data scope control, và branch-based filtering.

---

## 📋 Files Đã Tạo/Cập nhật

### 1. Domain Entities

#### ✅ **Branch.java** - Entity chi nhánh
**Path:** `src/main/java/com/neobrutalism/crm/domain/branch/Branch.java`

**Features:**
- Cấu trúc phân cấp với `parentId`, `level`, `path`
- 3 loại branch: `HQ`, `REGIONAL`, `LOCAL`
- Hỗ trợ manager, contact info
- Soft delete enabled

**Fields chính:**
```java
- code: String (unique per organization)
- name: String
- organizationId: UUID
- parentId: UUID (nullable)
- level: Integer (0 = root)
- path: String (/HQ/HN/HN-001)
- branchType: ENUM(HQ, REGIONAL, LOCAL)
- status: ENUM(ACTIVE, INACTIVE, CLOSED)
- managerId: UUID
```

#### ✅ **User.java** - Cập nhật với Branch và Data Scope
**Path:** `src/main/java/com/neobrutalism/crm/domain/user/model/User.java`

**Thêm fields:**
```java
- branchId: UUID
- dataScope: DataScope (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)
```

#### ✅ **DataScope.java** - Enum cho phạm vi dữ liệu
**Path:** `src/main/java/com/neobrutalism/crm/domain/user/model/DataScope.java`

```java
public enum DataScope {
    ALL_BRANCHES,      // Management role - Xem tất cả
    CURRENT_BRANCH,    // ORC role - Xem branch hiện tại + branch con
    SELF_ONLY          // Maker/Checker - Chỉ xem bản ghi của mình
}
```

---

### 2. Configuration Classes

#### ✅ **TenantConfig.java** - Default Tenant Configuration
**Path:** `src/main/java/com/neobrutalism/crm/config/TenantConfig.java`

```yaml
# application.yml
app:
  tenant:
    default-tenant-id: default
    enabled: true
    header-name: X-Tenant-ID
    strict-mode: false
```

#### ✅ **CasbinConfig.java** - Casbin Enforcer Bean
**Path:** `src/main/java/com/neobrutalism/crm/config/CasbinConfig.java`

- Tạo Casbin `Enforcer` với JDBC Adapter
- Load model từ `casbin/model.conf`
- Auto-save enabled

#### ✅ **model.conf** - Casbin RBAC Model
**Path:** `src/main/resources/casbin/model.conf`

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

---

### 3. Security Components

#### ✅ **TenantContext.java** - Thread-local Tenant Storage
**Path:** `src/main/java/com/neobrutalism/crm/common/security/TenantContext.java`

```java
TenantContext.setTenantId("default");
String tenantId = TenantContext.getTenantId();
TenantContext.clear();
```

#### ✅ **DataScopeContext.java** - Thread-local Data Scope Storage
**Path:** `src/main/java/com/neobrutalism/crm/common/security/DataScopeContext.java`

```java
DataScopeContext context = DataScopeContext.builder()
    .userId(user.getId())
    .tenantId("default")
    .dataScope(DataScope.CURRENT_BRANCH)
    .branchId(user.getBranchId())
    .accessibleBranchIds(Set.of(branch1, branch2))
    .build();

DataScopeContext.set(context);
```

#### ✅ **PermissionService.java** - Casbin Permission Management
**Path:** `src/main/java/com/neobrutalism/crm/common/security/PermissionService.java`

**Key Methods:**
```java
// Check permission
boolean hasPermission(UUID userId, String tenantId, String resource, String action)

// Assign role
boolean assignRoleToUser(UUID userId, String roleCode, String tenantId)

// Add permission to role
boolean addPermissionForRole(String roleCode, String tenantId, String resource, String action)

// Get user roles
List<String> getRolesForUser(UUID userId, String tenantId)

// Get role permissions
List<List<String>> getPermissionsForRole(String roleCode, String tenantId)
```

---

### 4. JPA Specifications

#### ✅ **DataScopeSpecification.java** - Auto Data Filtering
**Path:** `src/main/java/com/neobrutalism/crm/common/specification/DataScopeSpecification.java`

**Usage:**
```java
// Auto filter theo data scope
Specification<User> spec = DataScopeSpecification.<User>create();
List<User> users = userRepository.findAll(spec);

// Combine với các specs khác
Specification<User> spec = DataScopeSpecification.<User>create()
    .and((root, query, cb) -> cb.equal(root.get("status"), UserStatus.ACTIVE));
```

**Logic:**
- `ALL_BRANCHES`: Không filter
- `CURRENT_BRANCH`: Filter `branchId IN (accessibleBranchIds)`
- `SELF_ONLY`: Filter `createdBy = currentUserId`

---

### 5. Database Migration

#### ✅ **V5__Create_branch_and_casbin_tables.sql**
**Path:** `src/main/resources/db/migration/V5__Create_branch_and_casbin_tables.sql`

**Tables Created:**
1. **branches** - Chi nhánh table
2. **casbin_rule** - Casbin policy storage
3. **ALTER users** - Thêm `branch_id` và `data_scope`

**Default Data:**
- Default HQ branch cho mỗi organization
- Default Casbin policies cho các roles:
  - `ROLE_ADMIN` - Full access
  - `ROLE_MANAGER` - GET, POST
  - `ROLE_ORC` - GET only
  - `ROLE_MAKER` - Create own records
  - `ROLE_CHECKER` - Approve pending
  - `ROLE_USER` - Basic read

---

### 6. Dependencies

#### ✅ **pom.xml** - Thêm jCasbin
```xml
<!-- jCasbin for RBAC -->
<dependency>
    <groupId>org.casbin</groupId>
    <artifactId>jcasbin</artifactId>
    <version>1.55.0</version>
</dependency>

<!-- jCasbin JDBC Adapter -->
<dependency>
    <groupId>org.casbin</groupId>
    <artifactId>jdbc-adapter</artifactId>
    <version>2.6.1</version>
</dependency>
```

---

### 7. Configuration

#### ✅ **application.yml** - Tenant Config
```yaml
app:
  tenant:
    default-tenant-id: default
    enabled: true
    header-name: X-Tenant-ID
    strict-mode: false
```

---

## 🎯 Cách Sử dụng

### 1. Kiểm tra Permission

```java
@Autowired
private PermissionService permissionService;

@GetMapping("/api/users")
public ResponseEntity<List<User>> getUsers() {
    UUID userId = getCurrentUserId();

    // Check permission
    boolean canAccess = permissionService.hasPermission(
        userId,
        "default",
        "/api/users",
        "GET"
    );

    if (!canAccess) {
        throw new AccessDeniedException("No permission");
    }

    // Filter data theo data scope
    Specification<User> spec = DataScopeSpecification.<User>create();
    List<User> users = userRepository.findAll(spec);

    return ResponseEntity.ok(users);
}
```

### 2. Setup Data Scope trong Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        try {
            User user = getUserFromToken(extractToken(request));

            // Setup Data Scope Context
            Set<UUID> accessibleBranches = calculateAccessibleBranches(user);

            DataScopeContext.set(DataScopeContext.builder()
                .userId(user.getId())
                .tenantId(user.getOrganizationId().toString())
                .dataScope(user.getDataScope())
                .branchId(user.getBranchId())
                .accessibleBranchIds(accessibleBranches)
                .build());

            TenantContext.setTenantId(user.getOrganizationId().toString());

            filterChain.doFilter(request, response);
        } finally {
            DataScopeContext.clear();
            TenantContext.clear();
        }
    }

    private Set<UUID> calculateAccessibleBranches(User user) {
        if (user.getDataScope() == DataScope.ALL_BRANCHES) {
            return Set.of();
        }
        if (user.getDataScope() == DataScope.CURRENT_BRANCH) {
            return branchService.getAllChildBranches(user.getBranchId());
        }
        return Set.of();
    }
}
```

### 3. Gán Role và Permission

```java
// Gán role cho user
permissionService.assignRoleToUser(userId, "ROLE_MANAGER", "default");

// Thêm permission cho role
permissionService.addPermissionForRole(
    "ROLE_MANAGER",
    "default",
    "/api/organizations.*",
    "(GET)|(POST)"
);

// Batch add permissions
List<PermissionRequest> permissions = List.of(
    new PermissionRequest("/api/users", "GET"),
    new PermissionRequest("/api/users", "POST")
);
permissionService.addPermissionsForRole("ROLE_MANAGER", "default", permissions);
```

---

## 🔑 Key Concepts

### Data Scope Levels

| Level | Role | Description | Filter Logic |
|-------|------|-------------|--------------|
| **ALL_BRANCHES** | ROLE_ADMIN, ROLE_MANAGER | Xem tất cả branches | Không filter |
| **CURRENT_BRANCH** | ROLE_ORC | Xem branch hiện tại + con | `WHERE branchId IN (...)` |
| **SELF_ONLY** | ROLE_MAKER, ROLE_CHECKER | Chỉ xem của mình | `WHERE createdBy = userId` |

### Casbin Policy Structure

**Format:** `p, subject, domain, object, action, effect`

**Examples:**
```
p, ROLE_ADMIN, default, /api/.*, (GET)|(POST)|(PUT)|(DELETE), allow
p, ROLE_USER, default, /api/users/me, GET, allow
p, ROLE_MANAGER, default, /api/organizations.*, (GET)|(POST), allow
```

**Grouping:** `g, user, role, domain`

**Examples:**
```
g, user-123-uuid, ROLE_ADMIN, default
g, user-456-uuid, ROLE_MANAGER, default
```

---

## ✅ Testing Checklist

### 1. Tenant Context
- [ ] TenantContext trả về "default" khi không set
- [ ] TenantContext.clear() hoạt động đúng
- [ ] Thread-safe với concurrent requests

### 2. Permission Checks
- [ ] ROLE_ADMIN có quyền truy cập tất cả APIs
- [ ] ROLE_USER chỉ có quyền đọc
- [ ] Regex patterns hoạt động (`/api/users.*`)
- [ ] Action patterns hoạt động (`(GET)|(POST)`)

### 3. Data Scope Filtering
- [ ] ALL_BRANCHES: Không filter, xem tất cả
- [ ] CURRENT_BRANCH: Filter theo branchId
- [ ] SELF_ONLY: Filter theo createdBy

### 4. Branch Hierarchy
- [ ] Parent-child relationships đúng
- [ ] Level và path được tính tự động
- [ ] Query child branches hoạt động

---

## 📚 Documentation

**Chi tiết documentation:** Xem `CASBIN_INTEGRATION_GUIDE.md`

**Nội dung:**
- Setup guide
- API examples
- Troubleshooting
- Best practices
- Extension points

---

## 🚀 Next Steps

### Immediate (Cần làm ngay)

1. **Tạo BranchRepository và BranchService**
   ```java
   public interface BranchRepository extends JpaRepository<Branch, UUID> {
       List<Branch> findByOrganizationId(UUID organizationId);
       List<Branch> findByParentId(UUID parentId);
       Optional<Branch> findByCodeAndOrganizationId(String code, UUID orgId);
   }
   ```

2. **Tạo BranchController**
   - GET /api/branches
   - POST /api/branches
   - GET /api/branches/{id}/children
   - GET /api/branches/tree

3. **Implement calculateAccessibleBranches()**
   ```java
   public Set<UUID> getAllChildBranches(UUID branchId) {
       Set<UUID> result = new HashSet<>();
       result.add(branchId);

       List<Branch> children = branchRepository.findByParentId(branchId);
       for (Branch child : children) {
           result.addAll(getAllChildBranches(child.getId()));
       }

       return result;
   }
   ```

4. **Update UserController**
   - Thêm field `branchId` và `dataScope` vào UserRequest/Response
   - Validate branchId tồn tại

5. **Create Permission Management Endpoints**
   ```java
   POST /api/permissions/roles/{roleCode}/permissions
   DELETE /api/permissions/roles/{roleCode}/permissions
   GET /api/permissions/users/{userId}/roles
   POST /api/permissions/users/{userId}/roles
   GET /api/permissions/check
   ```

### Short-term (1-2 tuần)

6. **Implement Menu-Tab-Screen Permissions**
   - Link MenuScreen với ApiEndpoint
   - Check permission theo screen khi render UI
   - Frontend: `usePermission()` hook

7. **Add Audit Logging**
   - Log tất cả permission checks
   - Log role assignments
   - Dashboard xem access logs

8. **Performance Optimization**
   - Cache Casbin policies
   - Cache accessible branches
   - Batch permission checks

### Long-term (1 tháng+)

9. **Advanced Features**
   - Time-based permissions (expires_at)
   - IP-based restrictions
   - Device-based restrictions
   - Permission delegation

10. **UI Components**
    - Permission matrix UI
    - Role management UI
    - Branch hierarchy tree UI
    - User permission viewer

---

## 🐛 Known Issues & Limitations

1. **Casbin Performance**
   - Large policy sets (>10k rules) có thể chậm
   - **Solution:** Enable caching, use filtered queries

2. **Circular Branch Dependencies**
   - Migration không kiểm tra circular references
   - **Solution:** Validate trong service layer

3. **Thread-local Context**
   - Phải clear context sau mỗi request
   - **Solution:** Dùng try-finally hoặc Filter

4. **Regex Performance**
   - Nhiều regex patterns có thể chậm
   - **Solution:** Dùng specific patterns hơn

---

## 📞 Support

**Questions?**
- Xem `CASBIN_INTEGRATION_GUIDE.md` cho chi tiết
- Check Casbin docs: https://casbin.org/docs/overview
- Review test cases trong source code

**Architecture Questions:**
- Data Scope: Làm sao để user xem nhiều branches?
  → Gán thêm `CURRENT_BRANCH` scope và set đúng branch hierarchy

- Permission denied: User có role nhưng vẫn bị deny?
  → Check policies trong `casbin_rule` table, reload policies

- Filter không hoạt động: Data không được filter?
  → Verify `DataScopeContext` đã được set trong filter

---

## ✨ Summary

**Đã hoàn thành:**
✅ Branch entity với hierarchy
✅ User data scope (3 levels)
✅ jCasbin integration với JDBC adapter
✅ PermissionService với full CRUD
✅ DataScopeSpecification cho auto filtering
✅ TenantContext và DataScopeContext
✅ Migration scripts với default data
✅ Full documentation

**Chưa làm:**
❌ BranchRepository & BranchService
❌ BranchController
❌ Permission management endpoints
❌ Frontend integration
❌ Menu-Screen-API linking

**Estimated Time to Production-Ready:**
- Core features: ✅ Done
- Repository & Service: 2-3 hours
- Controllers: 3-4 hours
- Testing: 4-5 hours
- Frontend integration: 8-10 hours

**Total:** ~20-25 hours

---

**Last Updated:** 2025-01-XX
**Version:** 1.0.0
