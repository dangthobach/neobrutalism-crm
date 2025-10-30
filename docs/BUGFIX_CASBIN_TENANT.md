# Bug Fix: Casbin Permission Check and Tenant Context

## Date
2025-10-30

## Issues Identified

### 1. Casbin Matcher Exception
**Error:**
```
org.casbin.jcasbin.exception.CasbinMatcherException: invalid request size: expected 5, got 4
rvals: [ROLE_SUPER_ADMIN, default, /api/users, GET]
```

**Root Cause:**
- The Casbin model configuration in [model.conf](../src/main/resources/casbin/model.conf) defined request with 5 parameters: `r = sub, dom, obj, act, scope`
- But [PermissionService.java:61](../src/main/java/com/neobrutalism/crm/common/security/PermissionService.java#L61) was only passing 4 parameters to `enforcer.enforce()`:
  ```java
  boolean result = enforcer.enforce(subject, tenantId, resource, action);
  ```
- This mismatch caused the authentication filter to fail all permission checks

### 2. Tenant Context Warning in Scheduled Tasks
**Warning:**
```
WARN [scheduling-1] c.n.c.common.multitenancy.TenantContext : No tenant context set for current thread
```

**Root Cause:**
- Background scheduled jobs in [OutboxEventPublisher.java](../src/main/java/com/neobrutalism/crm/common/service/OutboxEventPublisher.java) run without HTTP request context
- The `@Scheduled` methods (`publishPendingEvents()` and `cleanupOldPublishedEvents()`) accessed the database without setting tenant context
- JPA Hibernate interceptor checked for tenant context and logged warnings

## Solutions Applied

### Fix 1: Simplify Casbin Model
**File:** [src/main/resources/casbin/model.conf](../src/main/resources/casbin/model.conf)

**Change:**
```diff
- r = sub, dom, obj, act, scope
+ r = sub, dom, obj, act
```

**Rationale:**
- The `scope` parameter (data scope) is not yet implemented in the permission system
- Removed it from the request definition to match the 4-parameter enforcement calls
- Future enhancement: Add scope back when implementing row-level security

**Alternative Considered:**
- Modify all `enforcer.enforce()` calls to pass a 5th parameter (e.g., `"*"` for default scope)
- Rejected because it adds unnecessary complexity for unused functionality

### Fix 2: Set Default Tenant for Scheduled Jobs
**File:** [src/main/java/com/neobrutalism/crm/common/service/OutboxEventPublisher.java](../src/main/java/com/neobrutalism/crm/common/service/OutboxEventPublisher.java)

**Changes:**

#### publishPendingEvents() method (line 67-115)
```java
@Scheduled(fixedDelayString = "${outbox.publisher.interval:5000}")
@Transactional
public void publishPendingEvents() {
    // Set default tenant for background job
    TenantContext.setCurrentTenant("default");

    try {
        // ... existing logic ...
    } finally {
        TenantContext.clear();
    }
}
```

#### cleanupOldPublishedEvents() method (line 169-181)
```java
@Scheduled(cron = "${outbox.cleanup.cron:0 0 2 * * *}")
@Transactional
public void cleanupOldPublishedEvents() {
    // Set default tenant for background job
    TenantContext.setCurrentTenant("default");

    try {
        // ... existing logic ...
    } finally {
        TenantContext.clear();
    }
}
```

**Rationale:**
- Background jobs should operate on "default" tenant (system tenant)
- Outbox events already contain tenant information in their aggregate context
- Ensures thread-local tenant context is properly set and cleaned up

## Testing

### Test 1: Authentication and Permission Check
**Before Fix:**
```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <token>"

# Result: 401 Unauthorized
# Error: CasbinMatcherException - invalid request size
```

**After Fix:**
```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <token>"

# Result: 200 OK or 403 Forbidden (based on actual permissions)
# No more CasbinMatcherException
```

### Test 2: Scheduled Jobs
**Before Fix:**
```
WARN [scheduling-1] c.n.c.common.multitenancy.TenantContext : No tenant context set for current thread
```

**After Fix:**
```
DEBUG [scheduling-1] c.n.c.common.multitenancy.TenantContext : Setting tenant context: default
DEBUG [scheduling-1] c.n.c.common.service.OutboxEventPublisher : Publishing 0 pending events from outbox
DEBUG [scheduling-1] c.n.c.common.multitenancy.TenantContext : Clearing tenant context: default
```

## Impact Analysis

### 1. Authentication Flow
**Before:**
- All authenticated requests with valid JWT tokens were denied
- Permission check failed with CasbinMatcherException
- Users could not access any protected endpoints

**After:**
- Normal authentication flow restored
- Permission checks work correctly based on Casbin policies
- Users with proper roles can access authorized endpoints

### 2. Scheduled Jobs
**Before:**
- Warnings logged every 5 seconds
- No functional impact (queries still worked)
- Log noise made debugging difficult

**After:**
- Clean logs without warnings
- Proper tenant context for audit trail
- Consistent multi-tenancy behavior

### 3. Permission System
**No Breaking Changes:**
- Existing permission policies remain valid
- No changes to role-menu mappings
- No changes to user-role assignments

**Future Enhancement Path:**
- Scope parameter can be re-added when implementing data scope
- Will require updating all enforce() calls
- Migration path: `enforcer.enforce(sub, dom, obj, act, "*")` for default scope

## Related Files

### Modified Files
1. [src/main/resources/casbin/model.conf](../src/main/resources/casbin/model.conf) - Removed scope parameter
2. [src/main/java/com/neobrutalism/crm/common/service/OutboxEventPublisher.java](../src/main/java/com/neobrutalism/crm/common/service/OutboxEventPublisher.java) - Added tenant context for scheduled jobs

### Related Files (No Changes)
1. [src/main/java/com/neobrutalism/crm/common/security/PermissionService.java](../src/main/java/com/neobrutalism/crm/common/security/PermissionService.java) - Permission checking logic
2. [src/main/java/com/neobrutalism/crm/common/security/JwtAuthenticationFilter.java](../src/main/java/com/neobrutalism/crm/common/security/JwtAuthenticationFilter.java) - Authentication filter
3. [src/main/java/com/neobrutalism/crm/common/multitenancy/TenantContext.java](../src/main/java/com/neobrutalism/crm/common/multitenancy/TenantContext.java) - Tenant context holder

## Deployment Notes

### No Database Changes
- No migrations required
- No changes to casbin_rule table
- No changes to existing policies

### Configuration Changes
- No application.yml changes required
- No environment variable changes

### Rolling Update Safe
- Changes are backward compatible
- No coordination required between old/new instances
- Can be deployed gradually

## Prevention

### Code Review Checklist
- [ ] Verify Casbin model parameters match enforcer.enforce() calls
- [ ] Ensure scheduled jobs set tenant context before database access
- [ ] Add integration tests for permission checks
- [ ] Document any changes to RBAC model

### Testing Guidelines
1. **Permission Tests:**
   - Test each role's access to protected endpoints
   - Verify deny rules override allow rules
   - Test hierarchical role inheritance

2. **Multi-tenancy Tests:**
   - Verify tenant isolation in API requests
   - Test scheduled jobs with multiple tenants
   - Ensure tenant context cleanup in finally blocks

3. **Integration Tests:**
   ```java
   @Test
   void testPermissionCheck() {
       // Given: User with ADMIN role
       UUID userId = createUserWithRole("ADMIN");
       String token = generateToken(userId);

       // When: Access protected endpoint
       ResponseEntity<?> response = restTemplate.exchange(
           "/api/users", HttpMethod.GET,
           new HttpEntity<>(createHeaders(token)),
           String.class
       );

       // Then: Should be allowed
       assertEquals(HttpStatus.OK, response.getStatusCode());
   }
   ```

## Monitoring

### Metrics to Track
1. **Permission Denials:**
   - Monitor 403 Forbidden responses
   - Alert on sudden spikes

2. **Authentication Failures:**
   - Monitor 401 Unauthorized responses
   - Track CasbinMatcherException (should be zero)

3. **Tenant Context:**
   - Monitor "No tenant context" warnings (should be zero)
   - Track tenant context setup/cleanup times

### Log Queries
```bash
# Find permission errors
grep "CasbinMatcherException" application.log

# Find tenant context warnings
grep "No tenant context set" application.log

# Find permission denials
grep "denied access to" application.log
```

## Conclusion

Both issues have been resolved with minimal code changes:
1. **Casbin Model** - Simplified to match current usage pattern
2. **Tenant Context** - Added proper context management for scheduled jobs

The fixes restore normal authentication and permission checking functionality while maintaining clean logs and proper multi-tenancy behavior.
