# Phase 2: Granular Authorization - Optimal Implementation Plan

**Date:** December 9, 2025
**Status:** Planning
**Duration:** 6 weeks (Weeks 7-12 of 30-week roadmap)

---

## 📊 CURRENT STATE ANALYSIS

### ✅ **What's Already Implemented (Good Foundation)**

1. **Casbin RBAC Framework** ✅
   - Multi-tenant support (domain-scoped policies)
   - Regex-based resource matching
   - Role-based access control
   - Database-backed policies (casbin_rule table)
   - Dynamic policy reloading

2. **Permission Infrastructure** ✅
   - PermissionService with comprehensive methods
   - Role, UserRole, GroupRole, RoleMenu entities
   - Menu-based permission management
   - JWT authentication with UserPrincipal
   - Multi-tenancy enforcement (TenantContext)

3. **Data Scope Foundation** ✅
   - DataScope enum (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)
   - DataScopeContext (ThreadLocal)
   - DataScopeSpecification (JPA filtering)
   - User.dataScope and User.accessibleBranchIds fields

### ❌ **Critical Gaps (Must Fix)**

1. **Data Scope NOT Enforced** ❌
   - DataScopeContext never populated from UserPrincipal
   - DataScopeSpecification exists but not applied to queries
   - No automatic enforcement mechanism

2. **Policy Audit Trail Missing** ❌
   - Permission changes not logged
   - No audit for authorization attempts
   - AuditService available but unused for permissions

3. **@RequirePermission Annotation Unused** ❌
   - Annotation defined but no AOP interceptor
   - Service-layer authorization not enforced
   - Only filter-level checks (HTTP layer)

4. **Incomplete Casbin Features** ❌
   - Role hierarchy (g2) defined but unpopulated
   - Scope parameter (v5) unused
   - Effect (deny) not utilized

5. **Caching Issues** ❌
   - UserSessionService cache never invalidated
   - Permission changes require manual cache eviction

---

## 🎯 PHASE 2 OBJECTIVES

### **Primary Goals:**
1. ✅ **Complete Data Scope Enforcement** - Auto-apply data filtering
2. ✅ **Implement Policy Audit Trail** - Track all permission changes
3. ✅ **Enable Service-Layer Authorization** - @RequirePermission AOP
4. ✅ **Role Hierarchy Support** - Parent role inheritance
5. ✅ **Permission Matrix UI** - Visual permission management
6. ✅ **Cache Invalidation** - Auto-refresh on permission changes

### **Success Criteria:**
- [ ] All queries automatically filtered by data scope
- [ ] Every permission change logged to audit table
- [ ] Service methods protected with @RequirePermission
- [ ] Admin can view permission matrix with data scopes
- [ ] Tests verify scope enforcement works correctly

---

## 🏗️ OPTIMAL IMPLEMENTATION STRATEGY

### **Strategy: Fix & Enhance (Not Rebuild)**
We will leverage the existing Casbin infrastructure and complete the missing pieces rather than rebuilding from scratch.

**Why This Approach?**
- ✅ 70% of the system is already built and working
- ✅ Casbin is production-tested and performant
- ✅ Avoids breaking existing role/menu authorization
- ✅ Faster to market (6 weeks vs 12+ weeks for rebuild)
- ✅ Incremental enhancement = lower risk

---

## 📋 IMPLEMENTATION PHASES

### **Week 7-8: Data Scope Enforcement** 🎯 PRIORITY 1

#### **Task 1.1: Auto-Populate DataScopeContext**
**File:** `JwtAuthenticationFilter.java`

**What to Add:**
```java
// After line: setAuthentication(authentication)
// Add:
DataScopeContext.clear();
DataScopeContext.setUserId(userPrincipal.getId());
DataScopeContext.setTenantId(userPrincipal.getTenantId());
DataScopeContext.setDataScope(userPrincipal.getDataScope());
DataScopeContext.setBranchId(userPrincipal.getBranchId());
DataScopeContext.setAccessibleBranchIds(userPrincipal.getAccessibleBranchIds());

log.debug("DataScopeContext populated: userId={}, dataScope={}, branchId={}",
    userPrincipal.getId(), userPrincipal.getDataScope(), userPrincipal.getBranchId());
```

**Don't Forget:**
- Clear DataScopeContext in `finally` block (after TenantContext.clear())

#### **Task 1.2: Create @DataScopeFiltered AOP**
**New File:** `DataScopeFilterAspect.java`

**Purpose:** Auto-apply DataScopeSpecification to repository queries

```java
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeFilterAspect {

    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..)) " +
            "&& target(repository)")
    public Object applyDataScopeFilter(ProceedingJoinPoint joinPoint, Object repository) {
        // If DataScopeContext is set:
        // - Wrap query with DataScopeSpecification
        // - Filter results by user's data scope
        // - Return filtered list/page
    }
}
```

**Alternative (Simpler):** Extend `JpaSpecificationExecutor` in all repositories and create helper utility:
```java
public class DataScopeHelper {
    public static <T> Specification<T> applyDataScope() {
        return (root, query, cb) -> DataScopeSpecification.apply(root, query, cb);
    }
}

// Usage in service:
customerRepository.findAll(DataScopeHelper.applyDataScope(), pageable);
```

**Decision:** Use helper utility approach (simpler, more explicit, easier to debug)

#### **Task 1.3: Update Repository Methods**
**Files:** All `*Repository.java` interfaces

**Pattern:**
```java
// OLD:
List<Customer> findByOrganizationId(UUID organizationId);

// NEW:
default List<Customer> findByOrganizationIdWithScope(UUID organizationId) {
    return findAll(DataScopeHelper.applyDataScope()
        .and((root, query, cb) -> cb.equal(root.get("organizationId"), organizationId)));
}
```

**Impact:**
- ~15 repositories to update
- Add `WithScope` suffix to methods that need filtering
- Keep original methods for admin/system operations

#### **Task 1.4: Test Data Scope Enforcement**
**New File:** `DataScopeEnforcementTest.java`

**Test Scenarios:**
1. `ALL_BRANCHES` user sees all records
2. `CURRENT_BRANCH` user sees only their branch + children
3. `SELF_ONLY` user sees only their created records
4. Cross-tenant isolation still works
5. Admin bypass works (system operations)

**Effort:** Week 7-8 (10-15 hours)

---

### **Week 9: Policy Audit Trail** 🎯 PRIORITY 2

#### **Task 2.1: Create Permission Audit Entities**
**New Files:**
- `PermissionAuditLog.java` (entity)
- `PermissionAuditLogRepository.java`
- `PermissionAuditService.java`

**Schema:**
```sql
CREATE TABLE permission_audit_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,

    -- Event info
    event_type VARCHAR(50) NOT NULL,  -- PERMISSION_GRANTED, PERMISSION_REVOKED, ROLE_ASSIGNED, etc.
    event_time TIMESTAMP NOT NULL,

    -- Subject (who made the change)
    actor_id UUID,
    actor_username VARCHAR(100),

    -- Target (what was changed)
    target_type VARCHAR(50),          -- ROLE, USER, POLICY
    target_id VARCHAR(255),

    -- Policy details
    policy_type VARCHAR(20),          -- 'p' (policy), 'g' (grouping)
    subject VARCHAR(100),             -- role/user
    domain VARCHAR(100),              -- tenant
    resource VARCHAR(255),            -- API path
    action VARCHAR(50),               -- read/create/update/delete
    effect VARCHAR(20),               -- allow/deny
    scope VARCHAR(50),                -- ALL/DEPARTMENT/SELF_ONLY

    -- Context
    reason TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_perm_audit_org_time ON permission_audit_logs(organization_id, event_time);
CREATE INDEX idx_perm_audit_actor ON permission_audit_logs(actor_id);
CREATE INDEX idx_perm_audit_target ON permission_audit_logs(target_type, target_id);
```

#### **Task 2.2: Integrate Audit Logging**
**Files to Modify:**
- `PermissionService.java` - Add audit calls to all methods
- `CasbinPolicyManager.java` - Log policy sync operations
- `CasbinAuthorizationFilter.java` - Log authorization attempts

**Pattern:**
```java
// In PermissionService.addPermissionForRole():
public void addPermissionForRole(String roleCode, String tenantId, String resource, String action) {
    enforcer.addPolicy(roleCode, tenantId, resource, action, "allow");

    // NEW: Audit log
    permissionAuditService.logPermissionGranted(
        getCurrentUser(),
        "ROLE",
        roleCode,
        tenantId,
        resource,
        action,
        "allow",
        null, // scope
        "Granted via API"
    );
}
```

#### **Task 2.3: Create Audit Query API**
**New File:** `PermissionAuditController.java`

**Endpoints:**
```
GET /api/audit/permissions                    # List all permission changes
GET /api/audit/permissions/user/{userId}      # Changes for specific user
GET /api/audit/permissions/role/{roleCode}    # Changes for specific role
GET /api/audit/authorization-attempts         # Failed authorization attempts
```

**Effort:** Week 9 (8-10 hours)

---

### **Week 10: Service-Layer Authorization** 🎯 PRIORITY 3

#### **Task 3.1: Implement @RequirePermission Aspect**
**New File:** `PermissionCheckAspect.java`

```java
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // After @Transactional
@RequiredArgsConstructor
public class PermissionCheckAspect {

    private final PermissionService permissionService;

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        UserPrincipal user = SecurityUtils.getCurrentUser();

        String resource = requirePermission.resource();
        String action = requirePermission.action();

        // Check if user has permission
        boolean hasPermission = permissionService.hasPermission(
            user.getId(),
            user.getTenantId(),
            resource,
            action
        );

        if (!hasPermission) {
            throw new PermissionDeniedException(
                String.format("User %s lacks permission: %s:%s",
                    user.getUsername(), resource, action)
            );
        }
    }
}
```

#### **Task 3.2: Apply @RequirePermission to Services**
**Pattern:**
```java
@Service
@RequiredArgsConstructor
public class CustomerService {

    @RequirePermission(resource = "/api/customers", permission = PermissionType.READ)
    public List<Customer> findAll() {
        return customerRepository.findAllWithScope();
    }

    @RequirePermission(resource = "/api/customers", permission = PermissionType.CREATE)
    public Customer create(CustomerRequest request) {
        // Business logic
    }

    @RequirePermission(resource = "/api/customers", permission = PermissionType.UPDATE)
    public Customer update(UUID id, CustomerRequest request) {
        // Business logic
    }
}
```

**Impact:**
- ~20 service classes to annotate
- ~100+ methods to protect

**Effort:** Week 10 (10-12 hours)

---

### **Week 11: Role Hierarchy & Scope Integration** 🎯 PRIORITY 4

#### **Task 4.1: Populate Role Hierarchy (g2)**
**File:** `CasbinPolicyManager.java`

**Modify:** `syncRolePolicies()` method

```java
public void syncRolePolicies() {
    // Existing: Load roles and sync 'p' policies

    // NEW: Sync role hierarchy
    List<Role> allRoles = roleRepository.findAll();

    // Build hierarchy based on priority
    for (Role childRole : allRoles) {
        List<Role> parentRoles = allRoles.stream()
            .filter(r -> r.getPriority() != null && childRole.getPriority() != null)
            .filter(r -> r.getPriority() < childRole.getPriority()) // Lower priority = higher authority
            .collect(Collectors.toList());

        for (Role parentRole : parentRoles) {
            // g2: child_role, parent_role, tenant
            enforcer.addRoleForUser(childRole.getCode(), parentRole.getCode(), childRole.getOrganizationId().toString());
        }
    }

    log.info("Synced role hierarchy (g2 groupings)");
}
```

#### **Task 4.2: Add Scope to Policies**
**File:** `CasbinPolicyManager.java`

**Modify:** Policy creation to include scope

```java
// When creating policy from RoleMenu:
String scope = determineScope(roleMenu);
enforcer.addPolicy(
    roleCode,               // v0: subject
    tenantId,               // v1: domain
    apiPath,                // v2: object
    action,                 // v3: action
    "allow",                // v4: effect
    scope                   // v5: scope (NEW!)
);

private String determineScope(RoleMenu roleMenu) {
    // Logic to determine scope based on role priority or explicit setting
    // For now, default to "ALL" for management roles, "SELF_ONLY" for others
    return isManagementRole(roleMenu.getRoleId()) ? "ALL" : "SELF_ONLY";
}
```

#### **Task 4.3: Update Casbin Model**
**File:** `casbin/model.conf`

**Modify matcher to use scope:**
```
[matchers]
m = g(r.sub, p.sub, r.dom) && r.dom == p.dom && regexMatch(r.obj, p.obj) && regexMatch(r.act, p.act) && (p.eft == allow || !p.eft) && matchScope(r.scope, p.scope)
```

**Create custom function:** `matchScope(requestScope, policyScope)`
- Implement in Java as Casbin custom function
- Logic: `ALL` > `CURRENT_BRANCH` > `SELF_ONLY`

**Effort:** Week 11 (8-10 hours)

---

### **Week 12: Permission Matrix UI & Cache Invalidation** 🎯 PRIORITY 5

#### **Task 5.1: Create Permission Matrix API**
**New File:** `PermissionMatrixController.java`

**Endpoints:**
```
GET /api/permissions/matrix                          # Full permission matrix
GET /api/permissions/matrix/role/{roleCode}          # Permissions for role
GET /api/permissions/matrix/user/{userId}            # Effective permissions for user
POST /api/permissions/matrix/bulk-update             # Bulk update permissions
```

**Response Format:**
```json
{
  "role": "ROLE_MANAGER",
  "permissions": [
    {
      "resource": "/api/customers",
      "actions": {
        "read": true,
        "create": true,
        "update": true,
        "delete": false
      },
      "scope": "CURRENT_BRANCH"
    },
    {
      "resource": "/api/users",
      "actions": {
        "read": true,
        "create": false,
        "update": false,
        "delete": false
      },
      "scope": "SELF_ONLY"
    }
  ]
}
```

#### **Task 5.2: Create Frontend Permission Matrix Component**
**New File:** `src/app/admin/permissions/matrix/page.tsx`

**Features:**
- Table view: Roles × Resources
- Checkboxes for actions (read/create/update/delete)
- Dropdown for scope (ALL/DEPARTMENT/SELF_ONLY)
- Bulk edit mode
- Color coding (green = allowed, red = denied, yellow = scoped)
- Export to CSV
- Audit log viewer (show recent changes)

#### **Task 5.3: Implement Cache Invalidation**
**Files:**
- `UserSessionService.java`
- `PermissionService.java`

**Add Event Listeners:**
```java
@EventListener
public void onPermissionChanged(PermissionChangedEvent event) {
    // Invalidate cache for affected users
    if (event.getTargetType() == TargetType.ROLE) {
        // Invalidate all users with this role
        List<UUID> affectedUsers = userRoleRepository.findUserIdsByRoleCode(event.getRoleCode());
        affectedUsers.forEach(userId -> userSessionService.evictUserCache(userId));
    } else if (event.getTargetType() == TargetType.USER) {
        // Invalidate specific user
        userSessionService.evictUserCache(event.getUserId());
    }

    // Reload Casbin policies
    permissionService.reloadPolicy();
}
```

**Trigger Events:**
```java
// In PermissionService after policy change:
applicationEventPublisher.publishEvent(
    new PermissionChangedEvent(this, TargetType.ROLE, roleCode, tenantId)
);
```

**Effort:** Week 12 (12-15 hours)

---

## 📁 FILES TO CREATE

### **New Java Files (Backend)**
| File | Purpose | Lines (Est.) |
|------|---------|--------------|
| `DataScopeFilterAspect.java` | AOP for data scope enforcement | 150 |
| `DataScopeHelper.java` | Utility for applying data scope specifications | 80 |
| `PermissionAuditLog.java` | Entity for permission audit trail | 120 |
| `PermissionAuditLogRepository.java` | Repository interface | 30 |
| `PermissionAuditService.java` | Service for audit logging | 200 |
| `PermissionAuditController.java` | REST API for audit queries | 150 |
| `PermissionCheckAspect.java` | AOP for @RequirePermission | 120 |
| `PermissionDeniedException.java` | Exception for permission denied | 30 |
| `PermissionMatrixController.java` | REST API for permission matrix | 250 |
| `PermissionMatrixService.java` | Service for matrix operations | 300 |
| `PermissionChangedEvent.java` | Event for cache invalidation | 50 |
| `PermissionChangedEventListener.java` | Event listener | 100 |

**Total:** ~1,580 lines (backend)

### **Modified Java Files**
| File | Changes |
|------|---------|
| `JwtAuthenticationFilter.java` | Populate DataScopeContext |
| `PermissionService.java` | Add audit logging to all methods |
| `CasbinPolicyManager.java` | Populate g2 hierarchy, add scope |
| `UserSessionService.java` | Add cache eviction methods |
| All `*Repository.java` (15 files) | Add `WithScope` query methods |
| All `*Service.java` (20 files) | Add @RequirePermission annotations |

### **New Frontend Files**
| File | Purpose | Lines (Est.) |
|------|---------|--------------|
| `src/app/admin/permissions/matrix/page.tsx` | Permission matrix page | 400 |
| `src/components/permissions/permission-matrix-table.tsx` | Matrix table component | 350 |
| `src/components/permissions/permission-cell.tsx` | Individual permission cell | 150 |
| `src/components/permissions/scope-selector.tsx` | Scope dropdown | 100 |
| `src/hooks/usePermissionMatrix.ts` | React Query hooks | 200 |
| `src/lib/api/permissions.ts` | API client | 150 |

**Total:** ~1,350 lines (frontend)

### **Database Migrations**
| Migration | Purpose |
|-----------|---------|
| `V124__Create_permission_audit_logs_table.sql` | Audit logging table |
| `V125__Add_scope_to_casbin_rules.sql` | Add scope column (optional - v5 already exists) |
| `V126__Populate_role_hierarchy.sql` | Initial g2 groupings |

---

## 🧪 TESTING STRATEGY

### **Unit Tests**
- `DataScopeHelperTest.java` - Test scope specification generation
- `PermissionCheckAspectTest.java` - Test @RequirePermission AOP
- `PermissionAuditServiceTest.java` - Test audit logging

### **Integration Tests**
- `DataScopeEnforcementIT.java` - Test end-to-end data filtering
- `PermissionMatrixIT.java` - Test matrix CRUD operations
- `CacheInvalidationIT.java` - Test cache eviction on permission change

### **E2E Tests (Frontend)**
- `permission-matrix.spec.ts` - Test matrix UI interactions
- `data-scope-filtering.spec.ts` - Test frontend data filtering

**Coverage Target:** 80%+ for new code

---

## 📊 EFFORT ESTIMATION

| Week | Tasks | Hours | Status |
|------|-------|-------|--------|
| Week 7 | Data Scope - Context Population | 5h | ⏳ Pending |
| Week 7-8 | Data Scope - AOP/Helper + Repository Updates | 10h | ⏳ Pending |
| Week 9 | Policy Audit Trail - Entity + Service + API | 10h | ⏳ Pending |
| Week 10 | Service-Layer Authorization - Aspect + Annotations | 12h | ⏳ Pending |
| Week 11 | Role Hierarchy + Scope Integration | 10h | ⏳ Pending |
| Week 12 | Permission Matrix UI + Cache Invalidation | 15h | ⏳ Pending |
| **Total** | | **62 hours** | **6 weeks** |

**Resource:** 1 senior developer @ 10-12 hours/week

---

## 🎯 SUCCESS METRICS

### **Functional Metrics:**
- [ ] 100% of queries automatically filtered by data scope
- [ ] 100% of permission changes logged to audit table
- [ ] 100% of service methods protected with @RequirePermission
- [ ] Permission matrix UI allows visual permission management
- [ ] Cache automatically invalidates on permission changes

### **Performance Metrics:**
- [ ] Data scope filtering adds < 5ms overhead per query
- [ ] Permission check latency < 10ms (p95)
- [ ] Audit logging doesn't block main transaction
- [ ] Cache hit rate > 90% for permission checks

### **Security Metrics:**
- [ ] No data leaks across tenants (verified by tests)
- [ ] No unauthorized access (verified by E2E tests)
- [ ] All permission changes attributable to user (audit log)

---

## 🚀 DEPLOYMENT PLAN

### **Phase 1: Data Scope (Week 7-8)**
1. Deploy backend changes (DataScopeContext population)
2. Deploy repository updates (WithScope methods)
3. Run smoke tests
4. Monitor logs for scope enforcement
5. Fix any edge cases

### **Phase 2: Audit Trail (Week 9)**
1. Apply migration V124 (permission_audit_logs table)
2. Deploy audit service
3. Deploy audit API
4. Verify audit logs being created
5. Create Grafana dashboard for audit metrics

### **Phase 3: Service Authorization (Week 10)**
1. Deploy @RequirePermission aspect
2. Deploy annotated services
3. Test permission denials return proper errors
4. Monitor error rates

### **Phase 4: Role Hierarchy (Week 11)**
1. Apply migration V126 (populate g2)
2. Deploy updated Casbin policy manager
3. Verify role inheritance works
4. Test scope enforcement

### **Phase 5: Permission Matrix UI (Week 12)**
1. Deploy frontend permission matrix
2. Deploy matrix API
3. Train admins on new UI
4. Deploy cache invalidation
5. Monitor cache hit rates

---

## 🔒 SECURITY CONSIDERATIONS

1. **Bypass Prevention:**
   - Disable dev mode bypass in production
   - Remove hardcoded skip paths where possible
   - Ensure all endpoints have permission checks

2. **Audit Integrity:**
   - Audit logs stored in separate schema/database
   - Audit writes use REQUIRES_NEW propagation (survive rollbacks)
   - No delete/update on audit records (append-only)

3. **Cache Poisoning:**
   - Cache keys include tenant ID
   - Cache TTL: 5 minutes (short to prevent stale permissions)
   - Eviction on permission change

4. **Data Scope Bypass:**
   - Admin operations must explicitly skip scope (require special flag)
   - System operations use service account with ALL scope
   - Monitor for scope bypasses in audit logs

---

## 📝 DOCUMENTATION DELIVERABLES

1. **Architecture Decision Record (ADR):**
   - Why we chose to enhance Casbin vs rebuild
   - Data scope enforcement strategy
   - Audit trail design decisions

2. **Admin Guide:**
   - How to use permission matrix UI
   - Understanding data scopes
   - Reading audit logs

3. **Developer Guide:**
   - How to use @RequirePermission annotation
   - How to write scope-aware queries
   - Best practices for permission checks

4. **API Documentation:**
   - Swagger docs for permission matrix API
   - Swagger docs for audit API

---

## ✅ REVIEW & APPROVAL

**Please review this plan and confirm:**
1. ✅ Agree with "Fix & Enhance" strategy (vs rebuild)?
2. ✅ Agree with 6-week timeline?
3. ✅ Agree with prioritization (Data Scope → Audit → Service Auth → Hierarchy → Matrix)?
4. ✅ Any additional requirements or concerns?

**Once approved, I will:**
1. Create detailed task breakdown (Jira/GitHub issues)
2. Start implementation Week 7: Data Scope Enforcement
3. Provide daily progress updates

---

**Prepared by:** Claude Sonnet 4.5
**Date:** December 9, 2025
**Next Step:** Awaiting stakeholder approval

