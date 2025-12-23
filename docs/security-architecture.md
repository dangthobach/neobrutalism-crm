# Security Architecture - Neobrutalism CRM

**Generated:** 2025-12-07
**Project:** Neobrutalism CRM
**Security Maturity:** HIGH

---

## Table of Contents

1. [Security Overview](#security-overview)
2. [Authentication Flow](#authentication-flow)
3. [Authorization Model](#authorization-model)
4. [Multi-Tenancy Security](#multi-tenancy-security)
5. [Token Management](#token-management)
6. [Security Headers & CORS](#security-headers--cors)
7. [Frontend Security](#frontend-security)
8. [Vulnerabilities & Mitigations](#vulnerabilities--mitigations)
9. [Security Checklist](#security-checklist)

---

## Security Overview

The Neobrutalism CRM implements a **multi-layered security architecture** combining:

- **JWT Token-Based Authentication** with automatic refresh
- **Casbin RBAC Authorization** for fine-grained access control
- **Multi-Tenancy Isolation** via domain-based policies
- **Token Blacklisting** for immediate invalidation
- **Account Lockout** mechanisms
- **Security Headers** (CSP, HSTS, X-Frame-Options)
- **BCrypt Password Hashing** (cost factor 12)

### Security Architecture Layers

```
┌─────────────────────────────────────────┐
│     Client (Next.js Frontend)           │
│  - Token Storage (localStorage)         │
│  - Permission Guards                    │
│  - Route Protection                     │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│     Security Filter Chain               │
│  1. RateLimitFilter (optional)          │
│  2. JwtAuthenticationFilter             │
│  3. CasbinAuthorizationFilter           │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│     Backend Services                    │
│  - UserSessionService (caching)         │
│  - PermissionService (Casbin)           │
│  - TokenBlacklistService                │
│  - RefreshTokenService                  │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│     Database (PostgreSQL)               │
│  - users (credentials)                  │
│  - refresh_tokens                       │
│  - token_blacklist                      │
│  - casbin_rule (policies)               │
└─────────────────────────────────────────┘
```

---

## Authentication Flow

### 1. JWT Token Generation & Validation

**Location:** `src/main/java/com/neobrutalism/crm/common/security/JwtTokenProvider.java`

**Token Structure:**

```
Access Token (1 hour default):
- Claims: userId, username, email, tenantId, roles, type="access"
- Signature: HS256 with SHA-256 HMAC
- Validity: 3600000ms (configurable)

Refresh Token (7 days default):
- Claims: userId, username, tenantId, type="refresh"
- Validity: 604800000ms (configurable)
- Storage: SHA-256 hashed in database
```

**Key Security Features:**

1. **SecureRandom Token Generation** - 64 bytes = 512 bits
2. **SHA-256 Token Hashing** - Tokens stored as hashes, never plaintext
3. **Configurable Token Lifespans** via application.yml
4. **HS256 Signature** - HMAC-SHA256 with 256+ bit keys

**⚠️ CRITICAL:** Default JWT secret must be overridden in production:
```yaml
jwt.secret: ${JWT_SECRET:neobrutalism-crm-secret-key-change-this-in-production-min-256-bits}
```

### 2. Authentication Filter Pipeline

**Location:** `src/main/java/com/neobrutalism/crm/common/security/JwtAuthenticationFilter.java`

**Request Processing Flow:**

```
HTTP Request
    ↓
[JwtAuthenticationFilter] - Extract JWT from Authorization header
    ↓
[Token Validation] - Verify signature, expiration, format
    ↓
[Blacklist Check] - Verify token not blacklisted
    ↓
[User Token Blacklist Check] - Verify all user tokens not blacklisted
    ↓
[Access Token Verification] - Ensure token type = "access" (not refresh)
    ↓
[Tenant Context Setup] - Set TenantContext for multi-tenancy
    ↓
[UserPrincipal Loading] - Build complete user with cached roles/permissions
    ↓
[SecurityContext Population] - Create UsernamePasswordAuthenticationToken
    ↓
[Downstream Filter] - CasbinAuthorizationFilter
```

**Security Controls:**

1. **Bearer Token Extraction:** `Authorization: Bearer <token>`
2. **Token Validation:** Signature, expiration, format verification
3. **Double Blacklist Checks:**
   - Individual token blacklist (logout/refresh)
   - User-level token blacklist (password change/security breach)
4. **Tenant Context Isolation:** Multi-tenant data separation
5. **Automatic Cache Loading:** User roles/permissions cached with proper eviction

### 3. Login Process

**Location:** `src/main/java/com/neobrutalism/crm/common/security/AuthenticationService.java`

**Login Steps:**

```java
1. Find user by username or email (case-sensitive)
2. Verify account not locked (failed login tracking)
3. Verify account status = ACTIVE
4. Verify password using BCrypt(12 rounds)
5. Record successful login (IP address, timestamp)
6. Load user roles from database (cached)
7. Generate access token with roles
8. Create refresh token with rotation
9. Return LoginResponse with both tokens
```

**Security Features:**

- **Account Lockout:** Tracks failed login attempts, locks after 5 failures (30 min)
- **IP Tracking:** Records login IP for audit trail
- **User Agent Tracking:** Browser/device information stored
- **BCrypt Validation:** Cost factor 12 (~2.5 seconds per check)
- **Role Loading:** Filters by expiration date, active status

---

## Authorization Model

### Casbin RBAC Implementation

**Location:** `src/main/java/com/neobrutalism/crm/config/CasbinConfig.java`

**Setup:**
```java
Enforcer enforcer = new Enforcer(modelPath, adapter)
- Model: src/main/resources/casbin/model.conf (RBAC with domain)
- Adapter: JDBCAdapter (policies stored in database)
- Auto-save: Enabled (automatic persistence)
```

**Policy Structure:**
```
p: role, domain/tenant, resource/path, action, effect
g: user, role, domain        (user-role assignment)
g2: role, parentRole, domain (role hierarchy/inheritance)

Example Policies:
- p, ROLE_ADMIN, default, /api/users/*, (read|create|update|delete), allow
- p, ROLE_MANAGER, default, /api/customers/*, (read|create|update), allow
- p, ROLE_USER, default, /api/dashboard, read, allow
- g, user123, ROLE_ADMIN, default
- g2, ROLE_MANAGER, ROLE_USER, default
```

### Authorization Filter

**Location:** `src/main/java/com/neobrutalism/crm/config/security/CasbinAuthorizationFilter.java`

**Authorization Check Flow:**

```
Request hits CasbinAuthorizationFilter
    ↓
Is path in SKIP_PATHS? (auth, public, swagger, websocket, actuator)
    ├─ Yes → Allow through
    └─ No → Continue
    ↓
Is user authenticated?
    ├─ No → Return 401 Unauthorized
    └─ Yes → Continue
    ↓
Extract: username, domain (TenantContext), action (HTTP method), resource (path)
    ↓
Try enforcement with ROLES (preferred):
  For each role in user.roles:
    ├─ enforcer.enforce("ROLE_" + role, domain, resource, action)?
    ├─ Yes → Grant access, log, continue
    └─ No → Try next role
    ↓
If no role match, try username-based enforcement:
  ├─ enforcer.enforce(username, domain, resource, action)?
  └─ Yes → Grant access, log, continue
    ↓
If dev mode and no policies exist:
  ├─ Check if any policies loaded
  ├─ If empty, allow authenticated users (dev convenience)
  └─ If user has roles, allow (policy setup in progress)
    ↓
Otherwise: Return 403 Forbidden
```

**Multi-Tenant Support:**
- **Domain Parameter:** Uses `TenantContext.getCurrentTenantOrDefault()`
- **Isolation:** Each tenant has separate policies
- **Fallback:** Uses "default" tenant if context not set

### Permission Service

**Location:** `src/main/java/com/neobrutalism/crm/common/security/PermissionService.java`

**Core Methods:**
```java
// Permission Checking
hasPermission(userId, tenantId, resource, action) → boolean
hasPermissionWithScope(userId, tenantId, resource, action, scope) → boolean

// Role Management
assignRoleToUser(userId, roleCode, tenantId) → boolean
removeRoleFromUser(userId, roleCode, tenantId) → boolean
getRolesForUser(userId, tenantId) → List<String>

// Permission Management
addPermissionForRole(roleCode, tenantId, resource, action) → boolean
removePermissionFromRole(roleCode, tenantId, resource, action) → boolean
getPermissionsForRole(roleCode, tenantId) → List<List<String>>

// Policy Management
reloadPolicy() → void
clearAllPolicies() → void
```

### Policy Synchronization

**Location:** `src/main/java/com/neobrutalism/crm/config/security/CasbinPolicyManager.java`

**Synchronization Flow:**

```
Application Startup
    ↓
[CasbinPolicyManager.loadPoliciesOnStartup()]
    ↓
Clear existing Casbin policies
    ↓
For each Role in database:
  - Get RoleMenu mappings
  - For each RoleMenu:
    - Extract Menu.path (resource)
    - Extract permissions (canView → "read", canCreate → "create", etc)
    - Add to Casbin: p, role, organizationId, resource, action, allow
    ↓
Save all policies to database
```

**Action Mapping:**
```
HTTP Method → Casbin Action:
- GET → read
- POST → create
- PUT/PATCH → update
- DELETE → delete
- (Additional): export, import
```

---

## Multi-Tenancy Security

### Tenant Context Management

**Dual Implementation:**

1. **Legacy:** `src/main/java/com/neobrutalism/crm/common/security/TenantContext.java`
2. **Current:** `src/main/java/com/neobrutalism/crm/common/multitenancy/TenantContext.java`

**API:**
```java
ThreadLocal<String> currentTenant
String DEFAULT_TENANT = "default"

setCurrentTenant(tenantId)      // Set current thread's tenant
getCurrentTenant()               // Null if not set
getCurrentTenantOrDefault()      // Returns default if null
clear()                          // Clear from thread-local
isSet()                          // Check if set
```

**Lifecycle Management:**
```
HTTP Request arrives
    ↓
JwtAuthenticationFilter extracts tenantId from JWT
    ↓
TenantContext.setCurrentTenant(tenantId)
    ↓
Request processed with tenant context available
    ↓
Finally block: TenantContext.clear()
    ↓
Thread returned to pool with clean context
```

### Tenant Isolation in Authorization

**Enforcement:**
```
CasbinAuthorizationFilter checks permissions with domain parameter:

enforcer.enforce(
    subject: "ROLE_ADMIN",
    domain: TenantContext.getCurrentTenantOrDefault(),  // Tenant isolation key
    resource: "/api/customers",
    action: "read"
)

Only policies for this specific tenant are evaluated.
Tenant A cannot access Tenant B's policies.
```

### Data Scope (Row-Level Security)

**User has DataScope:**
- **ALL:** All data across all branches
- **ORGANIZATION:** Only own organization data
- **BRANCH:** Only own branch data
- **DEPARTMENT:** Only own department
- **SELF:** Only own data

**Accessible Branch IDs:** `Set<UUID>` in UserPrincipal used for filtering query results

---

## Token Management

### Refresh Token with Rotation

**Location:** `src/main/java/com/neobrutalism/crm/common/security/service/RefreshTokenService.java`

**Database Table:**
```sql
refresh_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) UNIQUE NOT NULL,  -- SHA-256 hash
    jti VARCHAR(36) UNIQUE,                  -- JWT ID
    user_id UUID NOT NULL,
    username VARCHAR(50),
    issued_at TIMESTAMP,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,

    -- Revocation
    revoked BOOLEAN DEFAULT false,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(200),

    -- Rotation
    replaced_by_token UUID,  -- Linked to new token on rotation
    rotation_count INTEGER,

    -- Device info
    device_id VARCHAR(100),
    device_name VARCHAR(200),
    device_type VARCHAR(50),
    user_agent TEXT,
    ip_address VARCHAR(45),
    location VARCHAR(200),

    -- Security
    is_suspicious BOOLEAN DEFAULT false,
    suspicious_reason TEXT
)
```

**Rotation Flow:**

```
Client uses old refresh token to get new access token
    ↓
[RefreshTokenService.rotateRefreshToken()]
    ↓
Hash old token and lookup in DB
    ↓
Check if old token is revoked (indicates previous reuse)
    ├─ Yes: TOKEN_REUSE_DETECTED
    │       ↓ Revoke ALL user tokens (security breach detected)
    │       ↓ Throw exception
    └─ No: Continue
    ↓
Check if expired
    ├─ Yes: Throw REFRESH_TOKEN_EXPIRED
    └─ No: Continue
    ↓
Detect suspicious activity:
    ├─ IP address mismatch
    ├─ High rotation count (>100)
    ├─ Rapid successive refreshes (<1 minute)
    └─ Flag token if detected
    ↓
Generate NEW refresh token (SecureRandom 64 bytes)
    ↓
Store NEW token hash in DB with rotation_count++
    ↓
Link OLD token to NEW: old.replaceWith(newId)
    ↓
Return new token plaintext to client (transient field)
```

**Session Management:**
- **Max sessions per user:** 5 (configurable)
- When creating new token:
  - Count active sessions for user
  - If count >= max:
    - Find oldest active token
    - Revoke with reason "MAX_SESSIONS_EXCEEDED"

**Automatic Cleanup:**
```java
@Scheduled(cron = "0 0 3 * * *")  // Daily at 3 AM
cleanupExpiredTokens()
  Delete tokens where:
    - (revoked=true AND revokedAt < now - 30 days)
    OR (expired=true AND expiresAt < now - 30 days)
```

### Token Blacklist Service

**Location:** `src/main/java/com/neobrutalism/crm/common/security/service/TokenBlacklistService.java`

**Database Table:**
```sql
token_blacklist (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) UNIQUE NOT NULL,  -- SHA-256 hash
    jti VARCHAR(36),
    user_id UUID NOT NULL,
    username VARCHAR(50),
    reason VARCHAR(100),  -- LOGOUT, REFRESH, SECURITY_BREACH, PASSWORD_CHANGE, etc
    blacklisted_at TIMESTAMP,
    expires_at TIMESTAMP,
    user_agent TEXT,
    ip_address VARCHAR(45),
    created_by UUID
)
```

**Blacklist Reasons:**
- LOGOUT - User logged out
- REFRESH - Token was refreshed
- SECURITY_BREACH - Security incident
- PASSWORD_CHANGE - User changed password
- ACCOUNT_LOCKED - Account was locked
- FORCED_LOGOUT - Admin forced logout
- TOKEN_EXPIRED - Token expired
- INVALID_SIGNATURE - Signature verification failed

**Lookup Flow:**
```
JwtAuthenticationFilter receives token
    ↓
Extract token from Authorization header
    ↓
Validate signature/format
    ↓
Hash token using SHA-256
    ↓
Query: blacklistRepository.existsByTokenHash(hash)?
    ├─ Yes: REJECT (token blacklisted)
    └─ Log: "Blocked blacklisted token"

Check user-level blacklist:
  Query: blacklistRepository.existsByTokenHash("USER_ALL_TOKENS_" + userId)?
    ├─ Yes: REJECT (all user tokens blacklisted)
    └─ Log: "Blocked token for user with blacklisted tokens"
```

**User-Level Blacklist:**
```
When password changed:
  Create special marker in blacklist table:
    tokenHash = "USER_ALL_TOKENS_" + userId
    expiresAt = now + 24 hours

When token arrives:
  areUserTokensBlacklisted(userId)?
    ├─ Yes: Reject all tokens for this user
    └─ Forces re-login with new password
```

---

## Security Headers & CORS

### HTTP Security Configuration

**Location:** `src/main/java/com/neobrutalism/crm/config/SecurityConfig.java`

**CORS Setup:**
```yaml
Allowed Origins:
  - http://localhost:3000 (frontend dev)
  - http://localhost:5173 (vite dev)

Allowed Methods:
  - GET, POST, PUT, PATCH, DELETE, OPTIONS

Allowed Headers:
  - Authorization, Content-Type, Accept
  - X-Tenant-ID, X-Request-ID, X-Organization-ID

Exposed Headers:
  - Authorization, X-Total-Count, X-Page-Number, X-Page-Size

Max Age: 600s (10 minutes)
Credentials: true
```

**Session Management:**
```
sessionCreationPolicy: STATELESS
// JWT-based stateless sessions
// No HttpSession objects created
```

**CSRF Protection:**
```
csrf: disabled
// Reason: Stateless API with token-based auth
// Tokens in Authorization header (not cookie)
```

### Security Headers

**Implemented via SecurityHeadersConfig:**

```
Content-Security-Policy:
  default-src 'self'
  script-src 'self' 'unsafe-inline' 'unsafe-eval'  // ⚠️ Unsafe for production
  style-src 'self' 'unsafe-inline'
  img-src 'self' data: blob:
  connect-src 'self' https://eu.i.posthog.com
  frame-ancestors 'self'

X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains  // Production only
Referrer-Policy: no-referrer
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

---

## Frontend Security

### Auth Context

**Location:** `src/contexts/auth-context.tsx`

**Token Storage:**
```typescript
Storage Strategy:
- Access Token: In-memory only (vulnerable to XSS but not CSRF)
- Refresh Token: localStorage (persistent across page reloads)
  └─ Risk: Vulnerable to XSS attacks
```

**Token Expiration Handling:**
```typescript
Login:
  expiresIn = response.expiresIn (seconds)
  expiresAt = now + expiresIn * 1000
  Store: localStorage['access_token_expires_at'] = expiresAt
  Schedule: Refresh 60s before expiry

Token Refresh:
  Scheduled refresh fires
  Call refreshToken() endpoint
  Get new access + refresh tokens
  Update localStorage
  Reschedule next refresh

Auto-Logout:
  refreshToken() fails → logout() → clear all tokens → redirect to login
```

### Next.js Middleware

**Location:** `src/middleware.ts`

**Route Protection:**
```typescript
Public Routes (no auth):
  /login, /register, /forgot-password

Protected Routes (require auth):
  /admin/**  (all admin pages)

Auth Routes (redirect if authenticated):
  /login, /register  → Redirect to /admin

Logic:
  Has access_token?
    ├─ Yes + /login → Redirect to /admin
    ├─ Yes + /admin → Allow
    ├─ No + /admin → Redirect to /login?returnUrl=...
    └─ No + public → Allow
```

**⚠️ Limitation:** Middleware checks for access_token cookie only. Current implementation uses localStorage.

**Recommendation:** Move tokens to HttpOnly cookies (prevents XSS access).

### Permission Guards

**Location:** `src/components/PermissionGuard.tsx`

**Components:**
```typescript
<PermissionGuard route="/users" permission="canCreate">
  <CreateUserButton />  // Only shows if user has permission
</PermissionGuard>

<PermissionGuardAll route="/users" permissions={['canView', 'canCreate']}>
  <UserManagement />    // All permissions required
</PermissionGuardAll>

<PermissionGuardAny route="/users" permissions={['canCreate', 'canEdit']}>
  <ModifyButton />      // Any permission required
</PermissionGuardAny>
```

---

## Vulnerabilities & Mitigations

### Identified Issues

#### 1. CSP `unsafe-inline` and `unsafe-eval` ⚠️ MEDIUM

**Location:** `application.yml` and `SecurityHeadersConfig.java`

```yaml
script-src 'self' 'unsafe-inline' 'unsafe-eval'
// Allows inline scripts and eval()
// Defeats XSS protection
```

**Impact:** XSS attacks can execute arbitrary scripts

**Mitigation:**
- Remove `unsafe-inline` and `unsafe-eval`
- Use nonce-based CSP for inline scripts
- Move scripts to external files with proper CSP directives

---

#### 2. Frontend Token Storage ⚠️ HIGH

**Location:** `src/contexts/auth-context.tsx`

```typescript
localStorage.setItem('refresh_token', response.refreshToken)
// Stored in plaintext, accessible to XSS attacks
```

**Impact:**
- XSS attacker can steal refresh token
- Can perform token refresh attacks
- Can impersonate user

**Mitigation:**
- Move to HttpOnly, Secure, SameSite cookies
- Backend sets cookies via Set-Cookie header
- JavaScript cannot access HttpOnly cookies
- Still vulnerable to CSRF (mitigate with SameSite=Strict)

---

#### 3. Default JWT Secret ⚠️ CRITICAL

**Location:** `application.yml`

```yaml
jwt:
  secret: ${JWT_SECRET:neobrutalism-crm-secret-key-change-this-in-production-min-256-bits}
```

**Impact:**
- Default secret is weak (predictable)
- Anyone knowing the default can forge tokens
- Production deployments MUST override

**Mitigation:**
```bash
export JWT_SECRET="$(openssl rand -base64 32)"
```
Use cryptographically strong random value

---

#### 4. Password Storage ✅ LOW (Good Practice)

**Implementation:** `BCryptPasswordEncoder(12)`

```java
// Good: 12 rounds (cost factor)
// Stronger than default 10
// ~2.5 seconds per password check (acceptable)
```

---

## Security Checklist

### Critical Deployment Items

- [ ] Change JWT secret to strong random value
- [ ] Enable HTTPS/HSTS in production
- [ ] Fix CSP unsafe directives
- [ ] Implement HttpOnly cookie storage for refresh tokens
- [ ] Configure rate limiting appropriately
- [ ] Set up monitoring for failed logins
- [ ] Configure audit log retention
- [ ] Test token refresh workflow
- [ ] Test permission/authorization matrix
- [ ] Document incident response procedures

### Medium-Term Improvements

1. **Token Rotation Frequency**
   - Current: 7 days
   - Consider: 3-5 days for higher security

2. **Implement Device Fingerprinting**
   - Track device characteristics
   - Flag suspicious device changes
   - Require re-authentication for new devices

3. **Add 2FA/MFA**
   - TOTP (Time-based One-Time Password)
   - SMS verification
   - Backup codes

4. **IP Geolocation Checks**
   - Flag impossible travel
   - Unusual location access
   - Request verification

5. **Audit Logging**
   - Log all authentication events
   - Log all permission changes
   - Log suspicious activities
   - Retention: 1+ years

### Operational Security

1. **Key Rotation**
   - Rotate JWT secret periodically
   - Plan migration path for active tokens

2. **Monitoring & Alerting**
   - Monitor failed login attempts
   - Alert on token reuse detection
   - Alert on suspicious activity flags
   - Track revoked token counts

3. **Incident Response**
   - Document token revocation procedure
   - Test emergency token blacklisting
   - Prepare account lock procedures

4. **Security Testing**
   - Regular penetration testing
   - JWT token validation tests
   - CORS bypass attempts
   - CSRF tests with cookies

---

## Summary

The Neobrutalism CRM security architecture implements **enterprise-grade security practices** including:

### Key Strengths

1. **JWT Token-Based Auth** with proper signature validation
2. **Refresh Token Rotation** with reuse detection
3. **Casbin RBAC** for dynamic, role-based authorization
4. **Multi-Tenancy Isolation** via domain model
5. **Token Blacklisting** for immediate invalidation
6. **Caching Strategy** for performance (with proper eviction)
7. **Security Headers** (CSP, X-Frame-Options, HSTS)
8. **Password Security** (BCrypt with cost factor 12)
9. **Account Lockout** mechanism (failed login tracking)
10. **Audit Trail** (device info, IP tracking, revocation reasons)

### Critical Actions Required

1. Override JWT secret in production
2. Enable HTTPS and HSTS
3. Fix CSP unsafe directives
4. Implement HttpOnly cookies for tokens
5. Configure monitoring and alerting

---

For related documentation:
- [Architecture Backend](./architecture-backend.md)
- [Architecture Frontend](./architecture-frontend.md)
- [API Contracts](./api-contracts-backend.md)
