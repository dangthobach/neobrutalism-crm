# User & Permission Management System - Implementation Roadmap

## 📋 CURRENT STATUS

### ✅ Completed (Phase 0 - Foundation)
- [x] 12 Core Entities (User, Group, Role, Menu, etc.)
- [x] 12 Repositories with custom queries
- [x] 12 Services with business logic
- [x] 24 DTOs (Request & Response)
- [x] 12 Domain Events
- [x] Database Migration (V4)
- [x] Build & Run verification

**Total Files Created:** 80+ files
**Build Status:** ✅ SUCCESS
**Runtime Status:** ✅ RUNNING on port 8080

---

## 🎯 IMPLEMENTATION ROADMAP

### 📌 Phase 1: Authentication & Security (Week 1-2) - **HIGH PRIORITY**

#### 1.1 JWT Authentication
**Objectives:**
- Implement JWT token generation and validation
- User login/logout endpoints
- Token refresh mechanism
- Password encryption

**Tasks:**
- [ ] Create `JwtTokenProvider` utility class
  - Generate access token (15 min expiry)
  - Generate refresh token (7 days expiry)
  - Validate and parse tokens
  - Extract user details from token

- [ ] Create `AuthenticationService`
  - Login with username/password
  - Validate credentials
  - Generate JWT tokens
  - Refresh token mechanism
  - Logout (invalidate tokens)

- [ ] Create `AuthenticationController`
  ```
  POST /api/auth/login
  POST /api/auth/logout
  POST /api/auth/refresh
  POST /api/auth/register
  GET  /api/auth/me
  ```

- [ ] Update `SecurityConfig`
  - JWT authentication filter
  - Stateless session management
  - Public endpoints configuration
  - CORS configuration

**Files to Create:**
```
common/security/
  ├── JwtTokenProvider.java
  ├── JwtAuthenticationFilter.java
  ├── JwtAuthenticationEntryPoint.java
  └── CustomUserDetailsService.java
domain/auth/
  ├── dto/LoginRequest.java
  ├── dto/LoginResponse.java
  ├── dto/RegisterRequest.java
  ├── dto/TokenRefreshRequest.java
  ├── service/AuthenticationService.java
  └── controller/AuthenticationController.java
```

**Deliverables:**
- Working login/logout flow
- JWT token generation and validation
- Secure password storage (BCrypt)
- User registration endpoint

---

#### 1.2 Authorization & Permission Checking
**Objectives:**
- Role-based access control (RBAC)
- Method-level security
- Custom permission annotations
- Permission evaluation service

**Tasks:**
- [ ] Create `PermissionEvaluator` service
  - Check user has role
  - Check user has menu access
  - Check user has API endpoint access
  - Check screen-level permissions

- [ ] Create custom annotations
  ```java
  @RequiresPermission("USER_CREATE")
  @RequiresRole("ADMIN")
  @RequiresMenu("USER_MANAGEMENT")
  ```

- [ ] Create `PermissionAspect` (AOP)
  - Intercept method calls
  - Validate permissions before execution
  - Throw unauthorized exceptions

- [ ] Create `AuthorizationService`
  - Load user permissions from database
  - Cache user permissions (Redis/Memory)
  - Check permission hierarchies
  - Evaluate API endpoint access

**Files to Create:**
```
common/security/
  ├── annotations/
  │   ├── RequiresPermission.java
  │   ├── RequiresRole.java
  │   └── RequiresMenu.java
  ├── aspect/PermissionAspect.java
  ├── evaluator/PermissionEvaluator.java
  └── service/AuthorizationService.java
domain/permission/
  └── service/PermissionService.java
```

**Deliverables:**
- Working permission checking
- Method-level security annotations
- API endpoint authorization
- Permission caching mechanism

---

### 📌 Phase 2: User Management APIs (Week 2-3) - **HIGH PRIORITY**

#### 2.1 User CRUD Operations
**Tasks:**
- [ ] Create `UserController`
  ```
  GET    /api/users              (List all users - paginated)
  GET    /api/users/{id}         (Get user by ID)
  POST   /api/users              (Create user)
  PUT    /api/users/{id}         (Update user)
  DELETE /api/users/{id}         (Soft delete user)
  POST   /api/users/{id}/restore (Restore deleted user)
  ```

- [ ] Create `UserQueryController` (CQRS Read Model)
  ```
  GET /api/users/search          (Advanced search)
  GET /api/users/by-organization/{orgId}
  GET /api/users/by-group/{groupId}
  GET /api/users/by-role/{roleId}
  ```

- [ ] Add User validation
  - Email uniqueness
  - Username uniqueness
  - Password strength validation
  - Phone number format

- [ ] Add User business logic
  - Auto-generate username if not provided
  - Send welcome email on creation
  - Auto-assign default role
  - Password change validation

**Files to Create:**
```
domain/user/
  ├── controller/
  │   ├── UserController.java
  │   └── UserQueryController.java
  ├── dto/
  │   ├── UserUpdateRequest.java
  │   ├── UserSearchRequest.java
  │   ├── ChangePasswordRequest.java
  │   └── UserDetailResponse.java
  └── validation/
      ├── UniqueUsernameValidator.java
      ├── UniqueEmailValidator.java
      └── PasswordStrengthValidator.java
```

**Deliverables:**
- Complete user CRUD APIs
- User search and filtering
- Email/Username uniqueness validation
- Soft delete and restore

---

#### 2.2 User Profile & Password Management
**Tasks:**
- [ ] Create profile endpoints
  ```
  GET  /api/users/me/profile
  PUT  /api/users/me/profile
  POST /api/users/me/change-password
  POST /api/users/me/upload-avatar
  GET  /api/users/me/activity-log
  ```

- [ ] Password reset flow
  ```
  POST /api/auth/forgot-password
  POST /api/auth/reset-password
  POST /api/auth/verify-reset-token
  ```

- [ ] Account security
  - Failed login tracking
  - Account lock after 5 failed attempts
  - Password expiry (90 days)
  - Force password change on first login

**Files to Create:**
```
domain/user/
  ├── controller/UserProfileController.java
  ├── dto/
  │   ├── ProfileUpdateRequest.java
  │   ├── ChangePasswordRequest.java
  │   └── ForgotPasswordRequest.java
  └── service/
      ├── UserProfileService.java
      └── PasswordResetService.java
```

**Deliverables:**
- User profile management
- Password change functionality
- Password reset flow
- Avatar upload support

---

### 📌 Phase 3: Group & Role Management (Week 3-4) - **MEDIUM PRIORITY**

#### 3.1 Group Management
**Tasks:**
- [ ] Create `GroupController`
  ```
  GET    /api/groups
  GET    /api/groups/{id}
  POST   /api/groups
  PUT    /api/groups/{id}
  DELETE /api/groups/{id}
  GET    /api/groups/{id}/children      (Get child groups)
  GET    /api/groups/{id}/ancestors     (Get parent hierarchy)
  GET    /api/groups/tree               (Full group tree)
  ```

- [ ] Group hierarchy management
  - Build materialized path
  - Prevent circular references
  - Cascade operations (optional)
  - Move group to different parent

- [ ] User-Group assignment
  ```
  POST   /api/groups/{groupId}/users/{userId}
  DELETE /api/groups/{groupId}/users/{userId}
  GET    /api/groups/{groupId}/users
  POST   /api/groups/{groupId}/users/{userId}/set-primary
  ```

**Files to Create:**
```
domain/group/
  ├── controller/GroupController.java
  ├── dto/
  │   ├── GroupTreeResponse.java
  │   └── GroupHierarchyResponse.java
  └── service/GroupHierarchyService.java
domain/usergroup/
  └── controller/UserGroupController.java
```

**Deliverables:**
- Group CRUD operations
- Hierarchical group tree
- User-Group assignments
- Primary group management

---

#### 3.2 Role Management
**Tasks:**
- [ ] Create `RoleController`
  ```
  GET    /api/roles
  GET    /api/roles/{id}
  POST   /api/roles
  PUT    /api/roles/{id}
  DELETE /api/roles/{id}
  GET    /api/roles/system          (System roles)
  ```

- [ ] User-Role assignment
  ```
  POST   /api/roles/{roleId}/users/{userId}
  DELETE /api/roles/{roleId}/users/{userId}
  GET    /api/roles/{roleId}/users
  POST   /api/roles/{roleId}/users/{userId}/grant?expiresAt=...
  ```

- [ ] Group-Role assignment
  ```
  POST   /api/roles/{roleId}/groups/{groupId}
  DELETE /api/roles/{roleId}/groups/{groupId}
  GET    /api/roles/{roleId}/groups
  ```

- [ ] Role permission management
  ```
  GET /api/roles/{roleId}/permissions
  PUT /api/roles/{roleId}/permissions
  ```

**Files to Create:**
```
domain/role/
  └── controller/RoleController.java
domain/userrole/
  ├── controller/UserRoleController.java
  └── dto/GrantRoleRequest.java
domain/grouprole/
  └── controller/GroupRoleController.java
```

**Deliverables:**
- Role CRUD operations
- User-Role assignments with expiration
- Group-Role assignments
- Role permission overview

---

### 📌 Phase 4: Menu & Permission Management (Week 4-5) - **MEDIUM PRIORITY**

#### 4.1 Menu Management
**Tasks:**
- [ ] Create `MenuController`
  ```
  GET    /api/menus
  GET    /api/menus/{id}
  POST   /api/menus
  PUT    /api/menus/{id}
  DELETE /api/menus/{id}
  GET    /api/menus/tree
  GET    /api/menus/{id}/tabs
  GET    /api/menus/{id}/screens
  ```

- [ ] Create `MenuTabController`
  ```
  GET    /api/menu-tabs
  POST   /api/menu-tabs
  PUT    /api/menu-tabs/{id}
  DELETE /api/menu-tabs/{id}
  ```

- [ ] Create `MenuScreenController`
  ```
  GET    /api/menu-screens
  POST   /api/menu-screens
  PUT    /api/menu-screens/{id}
  DELETE /api/menu-screens/{id}
  GET    /api/menu-screens/{id}/apis
  ```

- [ ] User menu rendering
  ```
  GET /api/users/me/menus         (Get menus for current user)
  GET /api/users/{id}/menus       (Get menus for specific user)
  ```

**Files to Create:**
```
domain/menu/
  ├── controller/MenuController.java
  └── service/MenuRenderingService.java
domain/menutab/
  └── controller/MenuTabController.java
domain/menuscreen/
  └── controller/MenuScreenController.java
```

**Deliverables:**
- Menu CRUD operations
- Menu tree structure
- Tab and screen management
- User-specific menu rendering

---

#### 4.2 API Endpoint & Permission Mapping
**Tasks:**
- [ ] Create `ApiEndpointController`
  ```
  GET    /api/endpoints
  POST   /api/endpoints
  PUT    /api/endpoints/{id}
  DELETE /api/endpoints/{id}
  POST   /api/endpoints/scan         (Auto-scan controllers)
  ```

- [ ] Auto-discover API endpoints
  - Scan all @RestController classes
  - Extract @RequestMapping paths
  - Auto-create ApiEndpoint records
  - Tag by controller name

- [ ] Screen-API mapping
  ```
  POST   /api/screens/{screenId}/apis/{apiId}
  DELETE /api/screens/{screenId}/apis/{apiId}
  GET    /api/screens/{screenId}/apis
  ```

- [ ] Role-Menu permission
  ```
  POST /api/roles/{roleId}/menus/{menuId}/permissions
  PUT  /api/roles/{roleId}/menus/{menuId}/permissions
  GET  /api/roles/{roleId}/menus
  ```

**Files to Create:**
```
domain/apiendpoint/
  ├── controller/ApiEndpointController.java
  └── service/EndpointDiscoveryService.java
domain/screenapi/
  └── controller/ScreenApiController.java
domain/rolemenu/
  ├── controller/RoleMenuController.java
  └── dto/MenuPermissionRequest.java
```

**Deliverables:**
- API endpoint registry
- Auto-discovery of endpoints
- Screen-API mappings
- Role-Menu permissions

---

### 📌 Phase 5: Advanced Features (Week 5-6) - **MEDIUM PRIORITY**

#### 5.1 Permission Caching
**Tasks:**
- [ ] Implement Redis caching
  - Cache user permissions
  - Cache role permissions
  - Cache menu tree
  - TTL configuration (30 minutes)

- [ ] Create cache invalidation
  - Invalidate on role change
  - Invalidate on permission update
  - Invalidate on user role assignment
  - Manual cache clear endpoint

**Files to Create:**
```
common/cache/
  ├── PermissionCacheService.java
  ├── CacheConfig.java
  └── CacheInvalidationListener.java
```

**Deliverables:**
- Redis cache integration
- Permission caching
- Cache invalidation strategy

---

#### 5.2 Audit & Activity Logging
**Tasks:**
- [ ] User activity tracking
  - Login/Logout logs
  - Permission changes
  - Role assignments
  - Failed login attempts

- [ ] Create activity log endpoints
  ```
  GET /api/users/{id}/activities
  GET /api/audit/user-activities
  GET /api/audit/permission-changes
  GET /api/audit/role-assignments
  ```

**Files to Create:**
```
domain/audit/
  ├── controller/AuditController.java
  ├── dto/UserActivityResponse.java
  └── service/AuditQueryService.java
```

**Deliverables:**
- User activity logs
- Audit trail reports
- Permission change history

---

#### 5.3 Bulk Operations
**Tasks:**
- [ ] Bulk user import
  ```
  POST /api/users/import          (CSV/Excel upload)
  POST /api/users/export          (Export to CSV/Excel)
  ```

- [ ] Bulk role assignment
  ```
  POST /api/roles/bulk-assign     (Assign role to multiple users)
  POST /api/groups/bulk-assign    (Assign group to multiple users)
  ```

- [ ] Bulk permission update
  ```
  PUT /api/roles/{roleId}/permissions/bulk
  ```

**Files to Create:**
```
domain/user/
  ├── controller/UserBulkController.java
  └── service/
      ├── UserImportService.java
      └── UserExportService.java
```

**Deliverables:**
- CSV import/export
- Bulk operations
- Progress tracking

---

### 📌 Phase 6: Data Seeding & Testing (Week 6-7) - **HIGH PRIORITY**

#### 6.1 Initial Data Seeding
**Tasks:**
- [ ] Create seed data scripts
  - Default roles (SUPER_ADMIN, ADMIN, USER, GUEST)
  - Default menus (Dashboard, Users, Roles, Settings)
  - System user (admin@system.com)
  - Default organization

- [ ] Create database seeders
  ```
  resources/db/seed/
    ├── V100__Seed_default_roles.sql
    ├── V101__Seed_default_menus.sql
    ├── V102__Seed_system_user.sql
    └── V103__Seed_permissions.sql
  ```

- [ ] Create Java seeders (for complex logic)
  ```java
  @Component
  public class DefaultDataSeeder implements ApplicationRunner
  ```

**Files to Create:**
```
config/seed/
  ├── DefaultDataSeeder.java
  ├── RoleSeeder.java
  ├── MenuSeeder.java
  └── UserSeeder.java
resources/db/seed/
  └── *.sql (seed files)
```

**Deliverables:**
- Default system data
- Seeder utilities
- Sample users and roles

---

#### 6.2 Testing
**Tasks:**
- [ ] Unit tests for services
  - UserService tests
  - RoleService tests
  - PermissionService tests
  - AuthenticationService tests

- [ ] Integration tests for APIs
  - User CRUD tests
  - Authentication tests
  - Authorization tests
  - Permission checking tests

- [ ] Security tests
  - JWT validation tests
  - Permission evaluation tests
  - Unauthorized access tests
  - SQL injection prevention

**Files to Create:**
```
src/test/java/
  ├── domain/user/
  │   ├── UserServiceTest.java
  │   └── UserControllerTest.java
  ├── domain/role/
  │   ├── RoleServiceTest.java
  │   └── RoleControllerTest.java
  └── security/
      ├── JwtTokenProviderTest.java
      └── PermissionEvaluatorTest.java
```

**Deliverables:**
- 80%+ code coverage
- Integration tests
- Security test suite

---

### 📌 Phase 7: Documentation & DevOps (Week 7-8) - **MEDIUM PRIORITY**

#### 7.1 API Documentation
**Tasks:**
- [ ] Complete Swagger/OpenAPI specs
  - All endpoints documented
  - Request/Response examples
  - Security scheme configuration
  - Error code documentation

- [ ] Create Postman collection
  - All API endpoints
  - Environment variables
  - Sample requests

- [ ] Write API usage guide
  - Authentication flow
  - Permission checking
  - Common use cases

**Files to Create:**
```
docs/
  ├── API_DOCUMENTATION.md
  ├── AUTHENTICATION_GUIDE.md
  ├── PERMISSION_GUIDE.md
  └── postman/
      └── Permission_System.postman_collection.json
```

**Deliverables:**
- Complete API documentation
- Postman collection
- Usage guides

---

#### 7.2 Deployment & Monitoring
**Tasks:**
- [ ] Environment configuration
  - Development
  - Staging
  - Production

- [ ] Security hardening
  - HTTPS enforcement
  - Rate limiting
  - CORS configuration
  - Security headers

- [ ] Monitoring setup
  - Login attempts tracking
  - Failed authentication monitoring
  - Permission denial alerts
  - Performance metrics

**Files to Create:**
```
src/main/resources/
  ├── application-dev.yml
  ├── application-staging.yml
  └── application-prod.yml
config/
  ├── SecurityHardeningConfig.java
  └── MonitoringConfig.java
```

**Deliverables:**
- Environment configurations
- Security hardening
- Monitoring dashboards

---

## 📊 SUMMARY & PRIORITIES

### Must-Have (Phase 1-2: Weeks 1-3)
1. ✅ JWT Authentication & Authorization
2. ✅ User CRUD Operations
3. ✅ Password Management
4. ✅ Basic Permission Checking

### Should-Have (Phase 3-4: Weeks 3-5)
5. ✅ Group & Role Management
6. ✅ Menu Management
7. ✅ API Permission Mapping

### Nice-to-Have (Phase 5-7: Weeks 5-8)
8. ✅ Caching & Performance
9. ✅ Audit Logging
10. ✅ Bulk Operations
11. ✅ Testing & Documentation

---

## 🔧 TECHNICAL REQUIREMENTS

### Dependencies to Add
```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- Redis Cache (Optional) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Email (for password reset) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Excel Import/Export -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

### Environment Variables
```yaml
# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-here}
  access-token-expiry: 900000      # 15 minutes
  refresh-token-expiry: 604800000  # 7 days

# Redis Cache
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

# Email
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
```

---

## 📈 SUCCESS METRICS

### Phase 1-2 (Authentication & User Management)
- [ ] Login success rate > 99%
- [ ] JWT validation < 10ms
- [ ] User API response time < 100ms
- [ ] Zero authentication bypass vulnerabilities

### Phase 3-4 (Group & Role Management)
- [ ] Permission check < 50ms (without cache)
- [ ] Permission check < 5ms (with cache)
- [ ] Role assignment success rate > 99%
- [ ] Menu rendering < 200ms

### Phase 5-7 (Advanced Features)
- [ ] Cache hit rate > 90%
- [ ] Test coverage > 80%
- [ ] API documentation completeness 100%
- [ ] Zero security vulnerabilities (OWASP Top 10)

---

## 🚀 QUICK START CHECKLIST

### Week 1: Foundation
- [ ] Setup JWT dependencies
- [ ] Create JwtTokenProvider
- [ ] Implement login/logout endpoints
- [ ] Update SecurityConfig

### Week 2: Core Features
- [ ] User CRUD APIs
- [ ] Password management
- [ ] Basic permission checking
- [ ] Role assignment

### Week 3: Extended Features
- [ ] Group management
- [ ] Menu management
- [ ] Permission caching

### Week 4: Polish & Testing
- [ ] Write tests
- [ ] Create seed data
- [ ] API documentation
- [ ] Security audit

---

## 📚 REFERENCE ARCHITECTURE

```
┌─────────────────────────────────────────────────┐
│              Frontend Application                │
└───────────────────┬─────────────────────────────┘
                    │ JWT Token
                    ↓
┌─────────────────────────────────────────────────┐
│          API Gateway / Load Balancer            │
└───────────────────┬─────────────────────────────┘
                    │
    ┌───────────────┴───────────────┐
    ↓                               ↓
┌─────────────────┐         ┌──────────────────┐
│ Authentication  │         │   Authorization   │
│    Service      │         │     Service       │
│  (JWT Validate) │         │ (Permission Check)│
└────────┬────────┘         └─────────┬─────────┘
         │                            │
         ↓                            ↓
┌──────────────────────────────────────────────────┐
│              Business Services                    │
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌────────┐       │
│  │ User │  │ Role │  │ Menu │  │ Perms  │       │
│  │Service  │Service  │Service  │ Service│       │
│  └──────┘  └──────┘  └──────┘  └────────┘       │
└───────────────────┬──────────────────────────────┘
                    │
         ┌──────────┴──────────┐
         ↓                     ↓
  ┌────────────┐        ┌──────────────┐
  │  Database  │        │  Redis Cache │
  │ PostgreSQL │        │  (Optional)  │
  └────────────┘        └──────────────┘
```

---

**Generated:** 2025-10-26
**Version:** 1.0
**Status:** Ready for Implementation 🚀
