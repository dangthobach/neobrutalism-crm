# Architecture Diagram - Neobrutalism CRM Permission System

## Overview

Hệ thống phân quyền sử dụng jCasbin với 3 layers: Frontend, Backend API, và Database.

---

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           FRONTEND (React)                           │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │  Menu        │  │  Button      │  │  Data Table  │              │
│  │  Component   │  │  Component   │  │  Component   │              │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤              │
│  │ usePermission│  │ usePermission│  │ useQuery     │              │
│  │ hook         │  │ hook         │  │ (auto filter)│              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
│         │                 │                 │                       │
│         └─────────────────┴─────────────────┘                       │
│                           │                                          │
│                    HTTP Requests                                     │
│                    (with JWT Token)                                  │
└───────────────────────────┼──────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      BACKEND (Spring Boot)                           │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │               Security Filter Chain                           │  │
│  │                                                               │  │
│  │  ┌─────────────────────────────────────────────────────────┐ │  │
│  │  │      1. JwtAuthenticationFilter                         │ │  │
│  │  │   ┌─────────────────────────────────────────────────┐   │ │  │
│  │  │   │ • Extract JWT from request header              │   │ │  │
│  │  │   │ • Validate token signature                     │   │ │  │
│  │  │   │ • Extract user info (id, roles, branch, etc)   │   │ │  │
│  │  │   │ • Load user from database                      │   │ │  │
│  │  │   └─────────────────────────────────────────────────┘   │ │  │
│  │  │                        ▼                                 │ │  │
│  │  │   ┌─────────────────────────────────────────────────┐   │ │  │
│  │  │   │ • Calculate accessible branches                │   │ │  │
│  │  │   │   - ALL_BRANCHES: return Set.of()              │   │ │  │
│  │  │   │   - CURRENT_BRANCH: get all child branches     │   │ │  │
│  │  │   │   - SELF_ONLY: return Set.of()                 │   │ │  │
│  │  │   └─────────────────────────────────────────────────┘   │ │  │
│  │  │                        ▼                                 │ │  │
│  │  │   ┌─────────────────────────────────────────────────┐   │ │  │
│  │  │   │ • Set DataScopeContext                         │   │ │  │
│  │  │   │   - userId                                     │   │ │  │
│  │  │   │   - tenantId                                   │   │ │  │
│  │  │   │   - dataScope                                  │   │ │  │
│  │  │   │   - branchId                                   │   │ │  │
│  │  │   │   - accessibleBranchIds                        │   │ │  │
│  │  │   └─────────────────────────────────────────────────┘   │ │  │
│  │  │                        ▼                                 │ │  │
│  │  │   ┌─────────────────────────────────────────────────┐   │ │  │
│  │  │   │ • Set TenantContext                            │   │ │  │
│  │  │   │   - tenantId                                   │   │ │  │
│  │  │   └─────────────────────────────────────────────────┘   │ │  │
│  │  │                        ▼                                 │ │  │
│  │  │   ┌─────────────────────────────────────────────────┐   │ │  │
│  │  │   │ • Set SecurityContext                          │   │ │  │
│  │  │   │   - Authentication object                      │   │ │  │
│  │  │   └─────────────────────────────────────────────────┘   │ │  │
│  │  └─────────────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                   Controller Layer                            │  │
│  │                                                               │  │
│  │  @RestController                                              │  │
│  │  @RequestMapping("/api/users")                               │  │
│  │  public class UserController {                               │  │
│  │                                                               │  │
│  │    @GetMapping                                               │  │
│  │    public ResponseEntity<Page<User>> getUsers() {            │  │
│  │                                                               │  │
│  │      ┌─────────────────────────────────────────────────┐     │  │
│  │      │ 1. Get current user ID from SecurityContext    │     │  │
│  │      └─────────────────────────────────────────────────┘     │  │
│  │                        ▼                                      │  │
│  │      ┌─────────────────────────────────────────────────┐     │  │
│  │      │ 2. Check permission via PermissionService      │     │  │
│  │      │    permissionService.hasPermission(            │     │  │
│  │      │      userId, "default", "/api/users", "GET")   │     │  │
│  │      └─────────────────────────────────────────────────┘     │  │
│  │                        ▼                                      │  │
│  │      ┌─────────────────────────────────────────────────┐     │  │
│  │      │ 3. If denied → throw AccessDeniedException     │     │  │
│  │      │    return 403 Forbidden                        │     │  │
│  │      └─────────────────────────────────────────────────┘     │  │
│  │                        ▼                                      │  │
│  │      ┌─────────────────────────────────────────────────┐     │  │
│  │      │ 4. If allowed → call service layer             │     │  │
│  │      └─────────────────────────────────────────────────┘     │  │
│  │    }                                                          │  │
│  │  }                                                            │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    Service Layer                              │  │
│  │                                                               │  │
│  │  @Service                                                     │  │
│  │  public class UserService {                                  │  │
│  │                                                               │  │
│  │    public Page<User> getAllUsers(Pageable pageable) {        │  │
│  │                                                               │  │
│  │      ┌─────────────────────────────────────────────────┐     │  │
│  │      │ 1. Create DataScopeSpecification                │     │  │
│  │      │    Specification<User> spec =                   │     │  │
│  │      │      DataScopeSpecification.<User>create();     │     │  │
│  │      └─────────────────────────────────────────────────┘     │  │
│  │                        ▼                                      │  │
│  │      ┌─────────────────────────────────────────────────┐     │  │
│  │      │ 2. Combine with other specifications           │     │  │
│  │      │    spec = spec.and(                            │     │  │
│  │      │      UserSpec.hasStatus(UserStatus.ACTIVE)     │     │  │
│  │      │    );                                           │     │  │
│  │      └─────────────────────────────────────────────────┘     │  │
│  │                        ▼                                      │  │
│  │      ┌─────────────────────────────────────────────────┐     │  │
│  │      │ 3. Query repository with specification         │     │  │
│  │      │    return userRepository.findAll(spec, page);  │     │  │
│  │      └─────────────────────────────────────────────────┘     │  │
│  │    }                                                          │  │
│  │  }                                                            │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │         Repository + JPA Specification (Data Access)          │  │
│  │                                                               │  │
│  │  DataScopeSpecification.create() generates:                  │  │
│  │                                                               │  │
│  │  ┌────────────────────────────────────────────────────────┐  │  │
│  │  │ DataScope.ALL_BRANCHES (Management Role)              │  │  │
│  │  │   ↓                                                    │  │  │
│  │  │ WHERE 1=1  (No filter)                                │  │  │
│  │  │                                                        │  │  │
│  │  │ SELECT * FROM users                                   │  │  │
│  │  │ WHERE deleted = false                                 │  │  │
│  │  │   AND status = 'ACTIVE'                               │  │  │
│  │  └────────────────────────────────────────────────────────┘  │  │
│  │                                                               │  │
│  │  ┌────────────────────────────────────────────────────────┐  │  │
│  │  │ DataScope.CURRENT_BRANCH (ORC Role)                   │  │  │
│  │  │   ↓                                                    │  │  │
│  │  │ WHERE branch_id IN (accessibleBranchIds)              │  │  │
│  │  │                                                        │  │  │
│  │  │ SELECT * FROM users                                   │  │  │
│  │  │ WHERE deleted = false                                 │  │  │
│  │  │   AND status = 'ACTIVE'                               │  │  │
│  │  │   AND branch_id IN (                                  │  │  │
│  │  │     'HN-001',         -- Current branch               │  │  │
│  │  │     'HN-001-001',     -- Child branch 1               │  │  │
│  │  │     'HN-001-002'      -- Child branch 2               │  │  │
│  │  │   )                                                    │  │  │
│  │  └────────────────────────────────────────────────────────┘  │  │
│  │                                                               │  │
│  │  ┌────────────────────────────────────────────────────────┐  │  │
│  │  │ DataScope.SELF_ONLY (Maker/Checker Role)              │  │  │
│  │  │   ↓                                                    │  │  │
│  │  │ WHERE created_by = currentUserId                      │  │  │
│  │  │                                                        │  │  │
│  │  │ SELECT * FROM users                                   │  │  │
│  │  │ WHERE deleted = false                                 │  │  │
│  │  │   AND status = 'ACTIVE'                               │  │  │
│  │  │   AND created_by = 'user-123-uuid'                    │  │  │
│  │  └────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │              PermissionService (Casbin)                       │  │
│  │                                                               │  │
│  │  public boolean hasPermission(UUID userId,                   │  │
│  │                               String tenantId,               │  │
│  │                               String resource,               │  │
│  │                               String action) {               │  │
│  │                                                               │  │
│  │    ┌──────────────────────────────────────────────────┐      │  │
│  │    │ 1. Load policies from casbin_rule table         │      │  │
│  │    │    - User-Role mappings (g)                      │      │  │
│  │    │    - Role permissions (p)                        │      │  │
│  │    └──────────────────────────────────────────────────┘      │  │
│  │                      ▼                                        │  │
│  │    ┌──────────────────────────────────────────────────┐      │  │
│  │    │ 2. Match with Casbin model                       │      │  │
│  │    │    - g(userId, roleCode, tenantId)               │      │  │
│  │    │    - p(roleCode, tenantId, resource, action)     │      │  │
│  │    │    - regexMatch(resource, pattern)               │      │  │
│  │    └──────────────────────────────────────────────────┘      │  │
│  │                      ▼                                        │  │
│  │    ┌──────────────────────────────────────────────────┐      │  │
│  │    │ 3. Return allow/deny                             │      │  │
│  │    │    - true: User has permission                   │      │  │
│  │    │    - false: Access denied                        │      │  │
│  │    └──────────────────────────────────────────────────┘      │  │
│  │  }                                                            │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                 Finally Block (Cleanup)                       │  │
│  │                                                               │  │
│  │  finally {                                                    │  │
│  │    DataScopeContext.clear();  // Clear thread-local          │  │
│  │    TenantContext.clear();     // Clear thread-local          │  │
│  │  }                                                            │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        DATABASE (PostgreSQL/H2)                      │
│                                                                      │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │  users           │  │  branches        │  │  casbin_rule     │  │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤  │
│  │ id               │  │ id               │  │ id               │  │
│  │ username         │  │ code             │  │ ptype            │  │
│  │ email            │  │ name             │  │ v0 (subject)     │  │
│  │ branch_id     ◄──┼──┤ organization_id  │  │ v1 (domain)      │  │
│  │ data_scope       │  │ parent_id    ◄───┤  │ v2 (object)      │  │
│  │ organization_id  │  │ level            │  │ v3 (action)      │  │
│  │ created_by       │  │ path             │  │ v4 (effect)      │  │
│  │ ...              │  │ branch_type      │  │ v5 (scope)       │  │
│  └──────────────────┘  │ status           │  └──────────────────┘  │
│                        │ ...              │                         │
│  ┌──────────────────┐  └──────────────────┘  ┌──────────────────┐  │
│  │  user_roles      │                         │  roles           │  │
│  ├──────────────────┤                         ├──────────────────┤  │
│  │ user_id          │                         │ id               │  │
│  │ role_id          │                         │ code             │  │
│  │ is_active        │                         │ name             │  │
│  │ granted_at       │                         │ is_system        │  │
│  │ expires_at       │                         │ ...              │  │
│  └──────────────────┘                         └──────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Data Flow - Permission Check

### Step-by-Step Flow

```
┌──────────────────────────────────────────────────────────────────┐
│ Step 1: User Login & JWT Generation                             │
└──────────────────────────────────────────────────────────────────┘
                            │
                            ▼
POST /api/auth/login
{
  "username": "john.doe",
  "password": "secret"
}
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│ AuthController validates credentials                             │
│   ↓                                                              │
│ Load User from database:                                         │
│   - id: 123e4567-e89b-12d3-a456-426614174000                     │
│   - username: john.doe                                           │
│   - branchId: HN-001                                             │
│   - dataScope: CURRENT_BRANCH                                    │
│   - organizationId: org-001                                      │
│   ↓                                                              │
│ Generate JWT Token with claims:                                  │
│   {                                                              │
│     "sub": "123e4567-e89b-12d3-a456-426614174000",              │
│     "username": "john.doe",                                      │
│     "branchId": "HN-001",                                        │
│     "dataScope": "CURRENT_BRANCH",                               │
│     "organizationId": "org-001",                                 │
│     "roles": ["ROLE_ORC"],                                       │
│     "exp": 1640000000                                            │
│   }                                                              │
└──────────────────────────────────────────────────────────────────┘
                            │
                            ▼
Return JWT Token to Frontend
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│ Step 2: Frontend stores token & makes API call                  │
└──────────────────────────────────────────────────────────────────┘
                            │
                            ▼
GET /api/users
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│ Step 3: JwtAuthenticationFilter processes request               │
└──────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────┐
│ Extract & Validate JWT                         │
│   ↓                                            │
│ Decode claims:                                 │
│   - userId = 123e4567-e89b-12d3-a456-426614174000 │
│   - branchId = HN-001                          │
│   - dataScope = CURRENT_BRANCH                 │
│   - roles = [ROLE_ORC]                         │
│   ↓                                            │
│ Calculate accessible branches:                 │
│   branchService.getAllChildBranches(HN-001)    │
│   → [HN-001, HN-001-001, HN-001-002]          │
│   ↓                                            │
│ Set contexts:                                  │
│   DataScopeContext.set(...)                    │
│   TenantContext.setTenantId("org-001")         │
└────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│ Step 4: Controller checks permission                            │
└──────────────────────────────────────────────────────────────────┘
                            │
                            ▼
UserController.getUsers()
    │
    ▼
permissionService.hasPermission(
    userId: 123e4567-e89b-12d3-a456-426614174000,
    tenantId: "default",
    resource: "/api/users",
    action: "GET"
)
    │
    ▼
┌────────────────────────────────────────────────┐
│ Casbin Enforcer checks:                        │
│                                                │
│ 1. Get user roles in domain:                  │
│    g, 123e4567-..., ROLE_ORC, default          │
│    → User has ROLE_ORC                         │
│                                                │
│ 2. Check role permissions:                     │
│    p, ROLE_ORC, default, /api/.*, GET, allow   │
│    regexMatch("/api/users", "/api/.*") → true  │
│    → Permission granted                        │
│                                                │
│ 3. Return: true                                │
└────────────────────────────────────────────────┘
    │
    ▼
Permission OK → Continue to Service Layer
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│ Step 5: Service applies data scope filter                       │
└──────────────────────────────────────────────────────────────────┘
                            │
                            ▼
UserService.getAllUsers()
    │
    ▼
Specification<User> spec = DataScopeSpecification.<User>create()
    │
    ▼
┌────────────────────────────────────────────────┐
│ DataScopeSpecification builds predicate:       │
│                                                │
│ DataScope = CURRENT_BRANCH                     │
│ AccessibleBranchIds = [HN-001, HN-001-001, HN-001-002] │
│   ↓                                            │
│ Generate WHERE clause:                         │
│   WHERE branch_id IN ('HN-001', 'HN-001-001', 'HN-001-002') │
└────────────────────────────────────────────────┘
    │
    ▼
userRepository.findAll(spec)
    │
    ▼
┌────────────────────────────────────────────────┐
│ Execute SQL:                                   │
│                                                │
│ SELECT * FROM users                            │
│ WHERE deleted = false                          │
│   AND branch_id IN (                           │
│     'HN-001',                                  │
│     'HN-001-001',                              │
│     'HN-001-002'                               │
│   )                                            │
│ ORDER BY created_at DESC                       │
│ LIMIT 10                                       │
└────────────────────────────────────────────────┘
    │
    ▼
Return filtered users (only from accessible branches)
    │
    ▼
┌──────────────────────────────────────────────────────────────────┐
│ Step 6: Return response to frontend                             │
└──────────────────────────────────────────────────────────────────┘
    │
    ▼
HTTP 200 OK
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "...",
        "username": "user1",
        "branchId": "HN-001",
        ...
      },
      {
        "id": "...",
        "username": "user2",
        "branchId": "HN-001-001",
        ...
      }
    ],
    "totalElements": 25,
    "totalPages": 3
  }
}
    │
    ▼
┌──────────────────────────────────────────────────────────────────┐
│ Step 7: Finally block cleans up                                 │
└──────────────────────────────────────────────────────────────────┘
    │
    ▼
DataScopeContext.clear()
TenantContext.clear()
```

---

## 3. Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      Component Relationships                     │
└─────────────────────────────────────────────────────────────────┘

                    JwtAuthenticationFilter
                            │
                ┌───────────┼───────────┐
                │           │           │
                ▼           ▼           ▼
        DataScopeContext  TenantContext  SecurityContext
                │           │
                │           └──────────────┐
                │                          │
                ▼                          ▼
        ┌──────────────────┐      ┌──────────────────┐
        │  Controller      │      │ PermissionService │
        │                  │◄─────│                  │
        │ • Check perm     │      │ • hasPermission  │
        │ • If denied →    │      │ • Casbin enforcer│
        │   throw 403      │      └──────────────────┘
        └────────┬─────────┘              │
                 │                        │
                 ▼                        ▼
        ┌──────────────────┐      ┌──────────────────┐
        │  Service         │      │ casbin_rule table│
        │                  │      │                  │
        │ • Apply spec     │      │ ptype, v0-v5     │
        │ • Query repo     │      └──────────────────┘
        └────────┬─────────┘
                 │
                 ▼
        ┌──────────────────┐
        │ DataScopeSpec    │
        │                  │
        │ • Read context   │
        │ • Build predicate│
        └────────┬─────────┘
                 │
                 ▼
        ┌──────────────────┐      ┌──────────────────┐
        │  Repository      │      │  branches table  │
        │                  │◄─────│                  │
        │ • JPA Spec       │      │  parent_id, level│
        │ • Execute SQL    │      └──────────────────┘
        └────────┬─────────┘
                 │
                 ▼
        ┌──────────────────┐
        │  Database        │
        │                  │
        │ • users          │
        │ • branches       │
        │ • casbin_rule    │
        └──────────────────┘
```

---

## 4. Data Scope Decision Tree

```
                    User makes API call
                            │
                            ▼
            ┌───────────────────────────────┐
            │ What is user's data_scope?    │
            └───────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ALL_BRANCHES  │    │CURRENT_BRANCH│    │ SELF_ONLY    │
│(Management)  │    │(ORC)         │    │(Maker/Check) │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ No Filter    │    │Get branch    │    │Filter by     │
│              │    │hierarchy     │    │created_by    │
│WHERE 1=1     │    │              │    │              │
│              │    │branchService │    │WHERE         │
│See ALL data  │    │.getChildren()│    │created_by =  │
│              │    │              │    │currentUserId │
└──────────────┘    └──────┬───────┘    └──────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │Return:       │
                    │[HN-001,      │
                    │ HN-001-001,  │
                    │ HN-001-002]  │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │WHERE         │
                    │branch_id IN  │
                    │(...)         │
                    └──────────────┘
```

---

## 5. Security Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      Security Layers                             │
└─────────────────────────────────────────────────────────────────┘

Request → │ Layer 1: JWT Authentication
          │   • Validate token signature
          │   • Check token expiration
          │   • Extract user claims
          │   ✓ Pass → Continue
          │   ✗ Fail → 401 Unauthorized
          │
          ▼
          │ Layer 2: Authorization (Casbin)
          │   • Check user has role
          │   • Check role has permission
          │   • Match resource pattern
          │   • Match action pattern
          │   ✓ Pass → Continue
          │   ✗ Fail → 403 Forbidden
          │
          ▼
          │ Layer 3: Data Scope Filter
          │   • Read data_scope from context
          │   • Apply branch filter (if CURRENT_BRANCH)
          │   • Apply user filter (if SELF_ONLY)
          │   • Execute query with filters
          │   ✓ Return filtered data
          │
          ▼
Response ← │ Return data to frontend
```

---

## 6. Branch Hierarchy Example

```
Organization: ABC Company (org-001)
│
├─ HQ (branch-hq)                    [Level 0, Path: /HQ]
│  │
│  ├─ IT Department (branch-it)      [Level 1, Path: /HQ/IT]
│  │  │
│  │  ├─ Dev Team (branch-dev)       [Level 2, Path: /HQ/IT/DEV]
│  │  └─ QA Team (branch-qa)         [Level 2, Path: /HQ/IT/QA]
│  │
│  └─ HR Department (branch-hr)      [Level 1, Path: /HQ/HR]
│
├─ Hanoi Branch (HN-001)             [Level 0, Path: /HN-001]
│  │
│  ├─ HN Sales (HN-001-001)          [Level 1, Path: /HN-001/HN-001-001]
│  └─ HN Support (HN-001-002)        [Level 1, Path: /HN-001/HN-001-002]
│
└─ HCMC Branch (HCM-001)             [Level 0, Path: /HCM-001]
   │
   ├─ HCM Sales (HCM-001-001)        [Level 1, Path: /HCM-001/HCM-001-001]
   └─ HCM Support (HCM-001-002)      [Level 1, Path: /HCM-001/HCM-001-002]


User A (ROLE_ADMIN, dataScope=ALL_BRANCHES)
→ Can see: ALL branches
→ Query: SELECT * FROM users WHERE deleted=false

User B (ROLE_ORC, branchId=HN-001, dataScope=CURRENT_BRANCH)
→ Can see: HN-001, HN-001-001, HN-001-002
→ Query: SELECT * FROM users WHERE branch_id IN ('HN-001', 'HN-001-001', 'HN-001-002')

User C (ROLE_MAKER, branchId=HN-001-001, dataScope=SELF_ONLY)
→ Can see: Only records created by User C
→ Query: SELECT * FROM users WHERE created_by = 'user-c-id'
```

---

## 7. Casbin Policy Examples

### Policy Table (casbin_rule)

```sql
-- User-Role mappings (grouping policy)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES
('g', 'user-123-uuid', 'ROLE_ADMIN', 'default'),
('g', 'user-456-uuid', 'ROLE_ORC', 'default'),
('g', 'user-789-uuid', 'ROLE_MAKER', 'default');

-- Role permissions (policy)
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3, v4) VALUES
('p', 'ROLE_ADMIN', 'default', '/api/.*', '(GET)|(POST)|(PUT)|(DELETE)', 'allow'),
('p', 'ROLE_ORC', 'default', '/api/.*', 'GET', 'allow'),
('p', 'ROLE_MAKER', 'default', '/api/.*/create', 'POST', 'allow'),
('p', 'ROLE_CHECKER', 'default', '/api/.*/approve', 'POST', 'allow');

-- Role hierarchy (g2)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES
('g2', 'ROLE_ADMIN', 'ROLE_MANAGER', 'default'),
('g2', 'ROLE_MANAGER', 'ROLE_ORC', 'default');
```

### Permission Check Examples

```
Check 1: Can user-123 access GET /api/users?
  → g(user-123, ROLE_ADMIN, default) = true
  → p(ROLE_ADMIN, default, /api/.*, (GET)|..., allow) = true
  → regexMatch(/api/users, /api/.*) = true
  → Result: ALLOW

Check 2: Can user-456 access POST /api/users?
  → g(user-456, ROLE_ORC, default) = true
  → p(ROLE_ORC, default, /api/.*, GET, allow) = false (action mismatch)
  → Result: DENY

Check 3: Can user-789 access POST /api/users/create?
  → g(user-789, ROLE_MAKER, default) = true
  → p(ROLE_MAKER, default, /api/.*/create, POST, allow) = true
  → regexMatch(/api/users/create, /api/.*/create) = true
  → Result: ALLOW
```

---

## 8. Thread-Local Context Flow

```
Request Thread Lifecycle:

Start Request
     │
     ▼
┌─────────────────────────────────────┐
│ JwtAuthenticationFilter.doFilter()  │
│                                     │
│ try {                               │
│   1. Extract user from JWT          │
│   2. Calculate accessible branches  │
│   3. Set DataScopeContext           │
│   4. Set TenantContext              │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ ThreadLocal Storage:        │   │
│   │ • userId                    │   │
│   │ • tenantId                  │   │
│   │ • dataScope                 │   │
│   │ • branchId                  │   │
│   │ • accessibleBranchIds       │   │
│   └─────────────────────────────┘   │
│                                     │
│   5. filterChain.doFilter()         │
│      ↓                               │
│      Controller → Service → Repo    │
│      (All can access context)       │
│                                     │
│ } finally {                         │
│   6. DataScopeContext.clear()       │
│   7. TenantContext.clear()          │
│   (Prevent memory leak!)            │
│ }                                   │
└─────────────────────────────────────┘
     │
     ▼
End Request (Thread returned to pool)
```

---

## Notes

- **Security First**: Authentication → Authorization → Data Filtering
- **Performance**: Casbin policies cached, branch hierarchy cached
- **Thread-Safety**: All contexts use ThreadLocal storage
- **Extensibility**: Easy to add new roles, permissions, data scopes
- **Traceability**: All permission checks logged for audit

---

**Last Updated:** 2025-01-XX
**Version:** 1.0.0
