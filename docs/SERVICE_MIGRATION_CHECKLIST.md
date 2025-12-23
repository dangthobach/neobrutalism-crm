# Service Migration Checklist - From Monolith to Microservices

**Project**: Neobrutalism CRM
**Version**: 1.0.0
**Date**: 2025-12-22

---

## 📊 Current Monolith Analysis

### Existing Domains (From codebase analysis)

```
src/main/java/com/neobrutalism/crm/domain/
├── activity/          # Activity tracking domain
├── apiendpoint/       # API endpoint management (IAM related)
├── attachment/        # File attachment management
├── branch/            # Branch/hierarchy management
├── command/           # Command pattern implementation
├── contact/           # Contact management
├── content/           # CMS content management
├── contract/          # Contract management
├── course/            # LMS course management
├── customer/          # Customer management
├── document/          # Document management
├── group/             # User group management
├── grouprole/         # Group-role mapping
├── idempotency/       # Idempotent operation tracking
├── menu/              # Menu management (IAM related)
├── menuscreen/        # Menu-screen mapping (IAM related)
├── menutab/           # Menu-tab mapping (IAM related)
├── notification/      # Notification management
├── organization/      # Organization/tenant management
├── permission/        # Permission management (IAM related)
├── role/              # Role management (IAM related)
├── rolemenu/          # Role-menu mapping (IAM related)
├── screenapi/         # Screen-API mapping (IAM related)
├── task/              # Task management
├── user/              # User management
├── usergroup/         # User-group mapping
└── userrole/          # User-role mapping
```

### Common Infrastructure (Shared components)

```
src/main/java/com/neobrutalism/crm/common/
├── cqrs/              # CQRS pattern base classes
├── entity/            # Base entity classes
├── event/             # Domain event infrastructure
├── exception/         # Exception handling
├── filter/            # Request filters
├── multitenancy/      # Multi-tenancy support
├── ratelimit/         # Rate limiting
├── repository/        # Base repository interfaces
├── security/          # Security infrastructure
├── service/           # Base service classes
├── specification/     # JPA Specification builders
├── storage/           # MinIO file storage
├── util/              # Utility classes
└── validation/        # Validation framework
```

---

## 🎯 Migration Strategy Overview

### Phase Approach

```
PHASE 1: Foundation Services (Critical Path)
  └─ IAM Service (Week 1-2) ──┐
                               ├─► Organization Service (Week 3-4)
                               └─► User Service (Week 5-6)

PHASE 2: Core Business Services
  └─ Customer Service (Week 7-8)
  └─ Contact Service (Week 9-10)
  └─ Task Service (Week 11-12)

PHASE 3: Supporting Services
  └─ Notification Service (Week 13-14)
  └─ Attachment Service (Week 15-16)

PHASE 4: Domain-Specific Services
  └─ Course Service - LMS (Week 17-18)
  └─ Content Service - CMS (Week 19-20)
  └─ Contract/Document Service (Week 21-22)

PHASE 5: Infrastructure & Optimization
  └─ Monitoring & Observability (Week 23-24)
  └─ Load Testing & Tuning (Week 25-26)
```

---

## 🔧 Phase 1: IAM Service (CRITICAL)

### Priority: P0 (Must Complete First)

**Timeline**: Week 1-2
**Effort**: 80-120 hours
**Risk**: HIGH (affects all other services)

### 1.1 Pre-Migration Analysis ✅

**Current Components** (already identified in codebase):
- [x] `domain/permission/*` - Permission management
- [x] `domain/role/*` - Role management
- [x] `domain/rolemenu/*` - Role-menu mapping
- [x] `domain/apiendpoint/*` - API endpoint registry
- [x] `domain/menu/*` - Menu management
- [x] `domain/menuscreen/*` - Menu-screen mapping
- [x] `domain/menutab/*` - Menu-tab mapping
- [x] `domain/screenapi/*` - Screen-API mapping
- [x] `common/security/*` - JWT, authentication filters
- [x] jCasbin integration (already implemented)
- [x] L1/L2 cache (Caffeine + Redis, already implemented)

**Dependencies Analysis**:
```
IAM Service depends on:
├── Keycloak (External)            # Authentication
├── PostgreSQL                     # Casbin policies storage
├── Redis Cluster                  # L2 cache
└── Kafka                          # Event publishing

IAM Service is depended by:
├── API Gateway                    # Permission check
├── All Business Services          # Authorization
└── Frontend                       # Permission UI
```

### 1.2 Create New Service Module

```bash
# Create service directory
mkdir -p microservices/iam-service

# Initialize Maven project
cd microservices/iam-service
mvn archetype:generate \
  -DgroupId=com.neobrutalism.crm \
  -DartifactId=iam-service \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false

# Copy and adapt pom.xml dependencies
```

**pom.xml** (Key dependencies):
```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Spring Cloud -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>

    <!-- jCasbin -->
    <dependency>
        <groupId>org.casbin</groupId>
        <artifactId>jcasbin</artifactId>
        <version>1.55.0</version>
    </dependency>
    <dependency>
        <groupId>org.casbin</groupId>
        <artifactId>jdbc-adapter</artifactId>
        <version>2.7.0</version>
    </dependency>

    <!-- Cache -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>

    <!-- Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
</dependencies>
```

### 1.3 Code Migration Checklist

#### Step 1: Move Base Infrastructure ✅
- [ ] Copy `common/security/JwtTokenProvider.java`
- [ ] Copy `common/security/JwtAuthenticationFilter.java`
- [ ] Copy `common/security/UserPrincipal.java`
- [ ] Copy `common/security/JwtAuthenticationEntryPoint.java`
- [ ] Copy `common/security/annotation/RequirePermission.java`
- [ ] Adapt package names from `common.security` to `iam.security`

#### Step 2: Move jCasbin Components ✅
- [ ] Copy `domain/permission/service/PermissionService.java`
- [ ] Copy `domain/permission/service/CasbinPolicyManager.java`
- [ ] Copy `domain/permission/service/CasbinCacheService.java`
- [ ] Copy `domain/permission/controller/PermissionManagementController.java`
- [ ] Copy `domain/permission/dto/*` (all DTOs)
- [ ] Copy `config/CasbinConfig.java`
- [ ] Copy `resources/casbin/model.conf`

#### Step 3: Move Domain Entities ✅
- [ ] Copy `domain/role/*` (Role entity + repository + service + controller)
- [ ] Copy `domain/rolemenu/*` (RoleMenu mapping)
- [ ] Copy `domain/apiendpoint/*` (ApiEndpoint registry)
- [ ] Copy `domain/menu/*` (Menu hierarchy)
- [ ] Copy `domain/menuscreen/*` (MenuScreen mapping)
- [ ] Copy `domain/menutab/*` (MenuTab mapping)
- [ ] Copy `domain/screenapi/*` (ScreenApi mapping)

#### Step 4: Move Database Migrations
- [ ] Copy Casbin-related Flyway migrations:
  - `V2__Create_casbin_tables.sql`
  - `V11__Add_casbin_composite_indexes.sql`
  - Any seed data for roles/permissions
- [ ] Update migration scripts to use `iam_db` database

#### Step 5: Create New REST APIs
- [ ] **POST** `/api/iam/auth/validate` - Validate JWT token
  ```java
  @PostMapping("/auth/validate")
  public ResponseEntity<TokenValidationResponse> validateToken(
      @RequestBody TokenValidationRequest request
  ) {
      boolean valid = jwtTokenProvider.validateToken(request.getToken());
      Claims claims = jwtTokenProvider.getClaims(request.getToken());

      return ResponseEntity.ok(new TokenValidationResponse(
          valid,
          claims.get("sub", String.class),
          claims.get("tenantId", String.class),
          claims.get("roles", List.class),
          claims.get("dataScope", String.class),
          claims.get("branchId", String.class)
      ));
  }
  ```

- [ ] **POST** `/api/iam/permissions/check` - Check permission
  ```java
  @PostMapping("/permissions/check")
  public ResponseEntity<PermissionCheckResponse> checkPermission(
      @RequestBody PermissionCheckRequest request
  ) {
      boolean allowed = permissionService.hasPermission(
          request.getUserId(),
          request.getTenantId(),
          request.getResource(),
          request.getAction()
      );

      return ResponseEntity.ok(new PermissionCheckResponse(allowed));
  }
  ```

- [ ] **GET** `/api/iam/permissions/user/{userId}` - Get user permissions
- [ ] **POST** `/api/iam/roles/{roleId}/permissions` - Assign permissions to role
- [ ] **DELETE** `/api/iam/roles/{roleId}/permissions/{permissionId}` - Remove permission
- [ ] **POST** `/api/iam/cache/invalidate` - Invalidate cache
- [ ] **GET** `/api/iam/cache/stats` - Get cache statistics

#### Step 6: Configuration Files
- [ ] Create `application.yml`:
  ```yaml
  spring:
    application:
      name: iam-service
    datasource:
      url: jdbc:postgresql://postgres-primary:5000/iam_db
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      hikari:
        maximum-pool-size: 50
        minimum-idle: 10
        connection-timeout: 30000

    redis:
      cluster:
        nodes:
          - redis-node-1:6379
          - redis-node-2:6379
          - redis-node-3:6379
      password: ${REDIS_PASSWORD}

    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
      producer:
        key-serializer: org.apache.kafka.common.serialization.StringSerializer
        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

    jpa:
      hibernate:
        ddl-auto: validate
      show-sql: false

    flyway:
      enabled: true
      locations: classpath:db/migration

  # Casbin configuration
  casbin:
    model: classpath:casbin/model.conf
    cache:
      enabled: true
      max-size: 10000
      l1:
        enabled: true
        max-size: 10000
        ttl-minutes: 10

  # Service configuration
  server:
    port: 8081

  # Eureka discovery
  eureka:
    client:
      service-url:
        defaultZone: http://eureka-server:8761/eureka/
    instance:
      prefer-ip-address: true

  # Actuator endpoints
  management:
    endpoints:
      web:
        exposure:
          include: health,info,metrics,prometheus
    metrics:
      export:
        prometheus:
          enabled: true
  ```

- [ ] Create `application-dev.yml`, `application-staging.yml`, `application-prod.yml`

#### Step 7: Docker & Kubernetes
- [ ] Create `Dockerfile`:
  ```dockerfile
  FROM eclipse-temurin:21-jre-alpine

  WORKDIR /app

  COPY target/iam-service-1.0.0.jar app.jar

  EXPOSE 8081

  ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx2g", "app.jar"]
  ```

- [ ] Create `k8s/deployment.yaml`, `k8s/service.yaml`, `k8s/hpa.yaml`

#### Step 8: Testing
- [ ] Unit tests for `PermissionService`
- [ ] Integration tests with embedded H2 + Casbin
- [ ] Performance tests (target: 50k ops/sec)
- [ ] Load test cache hit rate (target: >95%)

#### Step 9: API Gateway Integration
- [ ] Update Gateway to call IAM service instead of local auth
- [ ] Implement `JwtAuthenticationFilter` in Gateway (calls IAM)
- [ ] Implement `AuthorizationFilter` in Gateway (calls IAM)
- [ ] Add circuit breaker for IAM calls
- [ ] Add fallback strategy (deny all if IAM unavailable)

### 1.4 Rollback Plan
- [ ] Keep monolith code intact (feature flag to switch)
- [ ] Create database backup before migration
- [ ] Blue-green deployment strategy
- [ ] Automated rollback script

### 1.5 Success Criteria
- [ ] IAM service handles 50k+ permission checks/sec
- [ ] L1 cache hit rate >95%
- [ ] API Gateway latency <5ms for auth check
- [ ] Zero downtime during migration
- [ ] All existing tests pass

---

## 🏢 Phase 1: Organization Service

### Priority: P1 (After IAM)

**Timeline**: Week 3-4
**Effort**: 60-80 hours
**Risk**: MEDIUM

### 2.1 Components to Migrate

**Current Components**:
- [x] `domain/organization/*`
- [x] `domain/branch/*`

**Dependencies**:
```
Organization Service depends on:
├── PostgreSQL                     # Organization data
├── Kafka                          # Event publishing
└── IAM Service                    # Authorization

Organization Service is depended by:
├── User Service                   # Users belong to org
├── Customer Service               # Customers belong to org
└── All other business services    # Multi-tenancy
```

### 2.2 Migration Tasks

#### Database
- [ ] Copy migration scripts for `organizations` table
- [ ] Copy migration scripts for `branches` table
- [ ] Create new database `organization_db`

#### Code Migration
- [ ] Copy `domain/organization/model/Organization.java`
- [ ] Copy `domain/organization/model/OrganizationStatus.java`
- [ ] Copy `domain/organization/repository/OrganizationRepository.java`
- [ ] Copy `domain/organization/service/OrganizationService.java`
- [ ] Copy `domain/organization/controller/OrganizationController.java`
- [ ] Copy `domain/organization/dto/*`
- [ ] Copy `domain/branch/*` (entire branch module)

#### CQRS & Event Sourcing
- [ ] Copy `domain/organization/event/OrganizationCreatedEvent.java`
- [ ] Copy `domain/organization/event/OrganizationUpdatedEvent.java`
- [ ] Copy `domain/organization/event/OrganizationDeletedEvent.java`
- [ ] Copy `domain/organization/model/OrganizationReadModel.java`
- [ ] Implement Kafka event producer
- [ ] Create topics: `domain.organization.events`

#### APIs
- [ ] **GET** `/api/organizations` - List organizations
- [ ] **POST** `/api/organizations` - Create organization
- [ ] **GET** `/api/organizations/{id}` - Get organization
- [ ] **PUT** `/api/organizations/{id}` - Update organization
- [ ] **DELETE** `/api/organizations/{id}` - Delete organization (soft delete)
- [ ] **GET** `/api/organizations/{id}/branches` - Get organization branches
- [ ] **GET** `/api/branches/{id}` - Get branch details
- [ ] **GET** `/api/branches/{id}/children` - Get child branches (hierarchy)

#### Testing
- [ ] Unit tests for OrganizationService
- [ ] Integration tests with database
- [ ] Event publishing tests
- [ ] Branch hierarchy tests

---

## 👤 Phase 1: User Service

### Priority: P1

**Timeline**: Week 5-6
**Effort**: 80-100 hours
**Risk**: MEDIUM-HIGH (integrates with Keycloak)

### 3.1 Components to Migrate

**Current Components**:
- [x] `domain/user/*`
- [x] `domain/userrole/*`
- [x] `domain/usergroup/*`
- [x] `domain/group/*`
- [x] `domain/grouprole/*`

**Dependencies**:
```
User Service depends on:
├── Keycloak                       # Authentication
├── IAM Service                    # Authorization
├── Organization Service           # User belongs to org
├── PostgreSQL                     # User data
└── Kafka                          # Event publishing/consuming

User Service is depended by:
├── Customer Service               # CreatedBy user
├── Task Service                   # Assigned to user
├── Notification Service           # Notify user
└── All other services             # Audit trail
```

### 3.2 Migration Tasks

#### Keycloak Integration
- [ ] Setup Keycloak realm: `crm`
- [ ] Create client: `crm-backend`
- [ ] Configure OIDC flow
- [ ] Setup user federation (if needed)
- [ ] Configure password policy
- [ ] Setup email templates

#### Code Migration
- [ ] Copy `domain/user/model/User.java`
- [ ] Copy `domain/user/repository/UserRepository.java`
- [ ] Copy `domain/user/service/UserService.java`
- [ ] Copy `domain/user/controller/UserController.java`
- [ ] Copy `domain/user/dto/*`
- [ ] Copy user-role, user-group mappings
- [ ] Copy group-role mappings

#### Authentication Proxy
- [ ] Implement `/api/auth/login` - Proxy to Keycloak
  ```java
  @PostMapping("/auth/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
      // Call Keycloak token endpoint
      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("grant_type", "password");
      body.add("client_id", keycloakClientId);
      body.add("client_secret", keycloakClientSecret);
      body.add("username", request.getUsername());
      body.add("password", request.getPassword());

      ResponseEntity<KeycloakTokenResponse> response = restTemplate
          .postForEntity(keycloakTokenEndpoint, body, KeycloakTokenResponse.class);

      // Transform to application token format
      return ResponseEntity.ok(new LoginResponse(
          response.getBody().getAccessToken(),
          response.getBody().getRefreshToken(),
          response.getBody().getExpiresIn()
      ));
  }
  ```

- [ ] Implement `/api/auth/refresh` - Refresh token
- [ ] Implement `/api/auth/logout` - Logout user

#### Event Handling
- [ ] Consume `OrganizationCreatedEvent` (create default admin user)
- [ ] Publish `UserCreatedEvent`
- [ ] Publish `UserUpdatedEvent`
- [ ] Publish `UserDeletedEvent`

#### APIs
- [ ] **GET** `/api/users` - List users (with data scope)
- [ ] **POST** `/api/users` - Create user (sync to Keycloak)
- [ ] **GET** `/api/users/{id}` - Get user
- [ ] **PUT** `/api/users/{id}` - Update user
- [ ] **DELETE** `/api/users/{id}` - Delete user (soft delete)
- [ ] **POST** `/api/users/{id}/roles` - Assign role
- [ ] **DELETE** `/api/users/{id}/roles/{roleId}` - Remove role
- [ ] **GET** `/api/users/me` - Get current user profile

#### Testing
- [ ] Mock Keycloak for tests
- [ ] Test user CRUD operations
- [ ] Test role assignment
- [ ] Test event publishing/consuming
- [ ] Test data scope filtering

---

## 👥 Phase 2: Customer Service

### Priority: P2

**Timeline**: Week 7-8
**Effort**: 80-100 hours
**Risk**: MEDIUM (high traffic service)

### 4.1 Components to Migrate

**Current Components**:
- [x] `domain/customer/*`

### 4.2 Migration Tasks

#### Code Migration
- [ ] Copy `domain/customer/model/Customer.java`
- [ ] Copy `domain/customer/repository/CustomerRepository.java`
- [ ] Copy `domain/customer/service/CustomerService.java`
- [ ] Copy `domain/customer/controller/CustomerController.java`
- [ ] Copy `domain/customer/dto/*`

#### Elasticsearch Integration (for search)
- [ ] Setup Elasticsearch cluster
- [ ] Create index: `customers`
- [ ] Implement sync from PostgreSQL to Elasticsearch
- [ ] Consume `CustomerCreatedEvent` → Index to ES
- [ ] Consume `CustomerUpdatedEvent` → Re-index to ES
- [ ] Consume `CustomerDeletedEvent` → Remove from ES

#### APIs
- [ ] **GET** `/api/customers?search=...` - Search customers (Elasticsearch)
- [ ] **GET** `/api/customers` - List customers (with data scope)
- [ ] **POST** `/api/customers` - Create customer
- [ ] **GET** `/api/customers/{id}` - Get customer
- [ ] **PUT** `/api/customers/{id}` - Update customer
- [ ] **DELETE** `/api/customers/{id}` - Delete customer
- [ ] **GET** `/api/customers/{id}/activities` - Get customer activities
- [ ] **GET** `/api/customers/stats` - Get statistics

#### Performance Optimization
- [ ] Implement read-through cache (Redis)
- [ ] Cache customer details (TTL: 5 minutes)
- [ ] Cache customer stats (TTL: 1 hour)
- [ ] Invalidate cache on update/delete

#### Testing
- [ ] Load test (target: 5k RPS per instance)
- [ ] Test Elasticsearch sync
- [ ] Test cache invalidation
- [ ] Test data scope filtering

---

## 📞 Phase 2: Contact Service

### Priority: P2

**Timeline**: Week 9-10
**Effort**: 60-80 hours
**Risk**: LOW

### 5.1 Components to Migrate

**Current Components**:
- [x] `domain/contact/*`

### 5.2 Migration Tasks

#### Code Migration
- [ ] Copy `domain/contact/model/Contact.java`
- [ ] Copy `domain/contact/repository/ContactRepository.java`
- [ ] Copy `domain/contact/service/ContactService.java`
- [ ] Copy `domain/contact/controller/ContactController.java`
- [ ] Copy `domain/contact/dto/*`

#### Event Handling
- [ ] Consume `CustomerCreatedEvent` (create primary contact)
- [ ] Publish `ContactCreatedEvent`

#### APIs
- [ ] **GET** `/api/contacts` - List contacts
- [ ] **POST** `/api/contacts` - Create contact
- [ ] **GET** `/api/contacts/{id}` - Get contact
- [ ] **PUT** `/api/contacts/{id}` - Update contact
- [ ] **DELETE** `/api/contacts/{id}` - Delete contact
- [ ] **GET** `/api/contacts/customer/{customerId}` - Get customer contacts

---

## ✅ Phase 2: Task Service

### Priority: P2

**Timeline**: Week 11-12
**Effort**: 80-100 hours
**Risk**: MEDIUM (complex workflow)

### 6.1 Components to Migrate

**Current Components**:
- [x] `domain/task/*`
- [x] `domain/activity/*`

### 6.2 Migration Tasks

#### Code Migration
- [ ] Copy `domain/task/*`
- [ ] Copy `domain/activity/*` (activity tracking)

#### Workflow Engine
- [ ] Design task status workflow (Draft → Open → In Progress → Done → Closed)
- [ ] Implement state machine
- [ ] Implement task assignment logic
- [ ] Implement task escalation (overdue tasks)

#### APIs
- [ ] **GET** `/api/tasks` - List tasks (assigned to me)
- [ ] **POST** `/api/tasks` - Create task
- [ ] **GET** `/api/tasks/{id}` - Get task
- [ ] **PUT** `/api/tasks/{id}` - Update task
- [ ] **DELETE** `/api/tasks/{id}` - Delete task
- [ ] **POST** `/api/tasks/{id}/assign` - Assign task to user
- [ ] **POST** `/api/tasks/{id}/complete` - Mark task as complete
- [ ] **GET** `/api/tasks/overdue` - Get overdue tasks

---

## 🔔 Phase 3: Notification Service

### Priority: P3

**Timeline**: Week 13-14
**Effort**: 80-120 hours
**Risk**: MEDIUM (multi-channel complexity)

### 7.1 Components to Migrate

**Current Components**:
- [x] `domain/notification/*`

### 7.2 Migration Tasks

#### Code Migration
- [ ] Copy `domain/notification/model/Notification.java`
- [ ] Copy `domain/notification/repository/NotificationRepository.java`
- [ ] Copy `domain/notification/service/NotificationService.java`
- [ ] Copy `domain/notification/controller/NotificationController.java`

#### Multi-Channel Support
- [ ] **Email Channel** (SMTP / SendGrid)
  - [ ] Implement email templates (Thymeleaf)
  - [ ] Implement batch email sending
  - [ ] Track email delivery status

- [ ] **SMS Channel** (Twilio)
  - [ ] Integrate Twilio SDK
  - [ ] Implement SMS templates
  - [ ] Track SMS delivery status

- [ ] **Push Notification Channel** (FCM)
  - [ ] Integrate Firebase Cloud Messaging
  - [ ] Implement push notification templates
  - [ ] Track push delivery status

- [ ] **WebSocket Channel** (STOMP)
  - [ ] Setup WebSocket server
  - [ ] Implement real-time notification push
  - [ ] Track online users

#### Event Consumers
- [ ] Consume `CustomerCreatedEvent` → Send welcome email
- [ ] Consume `TaskAssignedEvent` → Notify assignee
- [ ] Consume `UserCreatedEvent` → Send invitation email
- [ ] Consume `*` (all domain events) → Audit notification

#### Digest Mode
- [ ] Implement notification aggregation (batch notifications)
- [ ] Scheduler: Send digest every 6 hours
- [ ] User preference: Immediate vs Digest

#### APIs
- [ ] **GET** `/api/notifications` - Get user notifications
- [ ] **GET** `/api/notifications/unread` - Get unread notifications
- [ ] **POST** `/api/notifications/{id}/read` - Mark as read
- [ ] **POST** `/api/notifications/read-all` - Mark all as read
- [ ] **DELETE** `/api/notifications/{id}` - Delete notification
- [ ] **PUT** `/api/notifications/preferences` - Update user preferences

---

## 📎 Phase 3: Attachment Service

### Priority: P3

**Timeline**: Week 15-16
**Effort**: 60-80 hours
**Risk**: LOW-MEDIUM (file storage complexity)

### 8.1 Components to Migrate

**Current Components**:
- [x] `domain/attachment/*`
- [x] `common/storage/*` (MinIO integration)

### 8.2 Migration Tasks

#### MinIO Integration
- [ ] Setup MinIO cluster (3 nodes)
- [ ] Create bucket: `crm-attachments`
- [ ] Implement multipart upload (for large files)
- [ ] Implement presigned URL (for direct download)

#### Security
- [ ] Virus scanning integration (ClamAV)
- [ ] File type validation
- [ ] File size limits
- [ ] Access control (only authorized users can download)

#### Code Migration
- [ ] Copy `domain/attachment/model/Attachment.java`
- [ ] Copy `domain/attachment/service/AttachmentService.java`
- [ ] Copy `domain/attachment/controller/AttachmentController.java`
- [ ] Copy `common/storage/FileStorageService.java`
- [ ] Copy `common/storage/MinioConfig.java`

#### APIs
- [ ] **POST** `/api/attachments/upload` - Upload file (multipart)
- [ ] **GET** `/api/attachments/{id}/download` - Download file (presigned URL)
- [ ] **GET** `/api/attachments/{id}` - Get attachment metadata
- [ ] **DELETE** `/api/attachments/{id}` - Delete attachment
- [ ] **GET** `/api/attachments/entity/{entityType}/{entityId}` - Get attachments for entity

#### Testing
- [ ] Test file upload (small, medium, large files)
- [ ] Test virus scanning
- [ ] Test access control
- [ ] Test presigned URL expiration

---

## 🎓 Phase 4: Course Service (LMS)

### Priority: P4

**Timeline**: Week 17-18
**Effort**: 80-100 hours
**Risk**: MEDIUM

### 9.1 Components to Migrate

**Current Components**:
- [x] `domain/course/*` (Course, Module, Lesson, Enrollment, Certificate)

### 9.2 Migration Tasks

#### Code Migration
- [ ] Copy entire `domain/course/*` module
- [ ] Move to new service: `lms-service` or `course-service`

#### Video Streaming
- [ ] Integrate with video CDN (AWS CloudFront / Cloudflare Stream)
- [ ] Implement adaptive bitrate streaming (HLS)
- [ ] Track video watch progress

#### Quiz System
- [ ] Implement quiz engine
- [ ] Implement auto-grading
- [ ] Track quiz attempts and scores

#### APIs
- [ ] **GET** `/api/courses` - List published courses
- [ ] **GET** `/api/courses/{id}` - Get course details
- [ ] **POST** `/api/enrollments` - Enroll in course
- [ ] **GET** `/api/enrollments/user/{userId}` - Get user enrollments
- [ ] **POST** `/api/lessons/{id}/complete` - Mark lesson as complete
- [ ] **GET** `/api/enrollments/{id}/progress` - Get course progress
- [ ] **POST** `/api/enrollments/{id}/certificate` - Issue certificate

---

## 📝 Phase 4: Content Service (CMS)

### Priority: P4

**Timeline**: Week 19-20
**Effort**: 60-80 hours
**Risk**: LOW

### 10.1 Components to Migrate

**Current Components**:
- [x] `domain/content/*` (Content, Category, Tag)

### 10.2 Migration Tasks

#### Code Migration
- [ ] Copy entire `domain/content/*` module
- [ ] Move to new service: `cms-service` or `content-service`

#### Full-Text Search
- [ ] Index content to Elasticsearch
- [ ] Implement advanced search (title, body, tags, category)

#### SEO Optimization
- [ ] Generate sitemaps
- [ ] Implement slug-based URLs
- [ ] Meta description support

#### APIs
- [ ] **GET** `/api/content` - List published content
- [ ] **GET** `/api/content/slug/{slug}` - Get content by slug
- [ ] **GET** `/api/content/{id}` - Get content by ID
- [ ] **GET** `/api/content/search?q=...` - Search content
- [ ] **GET** `/api/categories` - List categories (tree)
- [ ] **GET** `/api/tags` - List tags

---

## 📄 Phase 4: Contract & Document Service

### Priority: P4

**Timeline**: Week 21-22
**Effort**: 80-100 hours
**Risk**: MEDIUM (PDF generation, digital signatures)

### 11.1 Components to Migrate

**Current Components**:
- [x] `domain/contract/*`
- [x] `domain/document/*`

### 11.2 Migration Tasks

#### PDF Generation
- [ ] Implement PDF templates (Thymeleaf + iText)
- [ ] Generate contracts from templates
- [ ] Generate invoices, reports

#### Digital Signatures
- [ ] Integrate DocuSign / Adobe Sign (optional)
- [ ] Implement e-signature workflow

#### Version Control
- [ ] Track document versions
- [ ] Compare document versions (diff)

#### APIs
- [ ] **GET** `/api/contracts` - List contracts
- [ ] **POST** `/api/contracts` - Create contract
- [ ] **GET** `/api/contracts/{id}/pdf` - Generate contract PDF
- [ ] **POST** `/api/contracts/{id}/sign` - Sign contract
- [ ] **GET** `/api/documents` - List documents
- [ ] **GET** `/api/documents/{id}/versions` - Get document versions

---

## 📊 Phase 5: Monitoring & Observability

### Priority: P5

**Timeline**: Week 23-24
**Effort**: 60-80 hours
**Risk**: LOW

### 12.1 Setup Tasks

#### Prometheus
- [ ] Deploy Prometheus server (HA mode, 2 instances)
- [ ] Configure service discovery (Kubernetes)
- [ ] Configure scrape configs for all services
- [ ] Setup recording rules (pre-aggregation)
- [ ] Setup alerting rules

#### Grafana
- [ ] Deploy Grafana (with persistent storage)
- [ ] Import dashboards:
  - [ ] System Overview Dashboard
  - [ ] API Gateway Dashboard
  - [ ] Service Health Dashboard
  - [ ] Database Dashboard
  - [ ] Cache Performance Dashboard
  - [ ] Business Metrics Dashboard
- [ ] Configure data sources (Prometheus, Loki, Tempo)
- [ ] Setup alerts (Email, Slack, PagerDuty)

#### Jaeger (Distributed Tracing)
- [ ] Deploy Jaeger (with Elasticsearch backend)
- [ ] Configure OpenTelemetry in all services
- [ ] Implement trace context propagation
- [ ] Configure sampling strategy (1% sampling)

#### Loki (Log Aggregation)
- [ ] Deploy Loki (with S3 storage backend)
- [ ] Deploy Promtail (log shipper)
- [ ] Configure log retention (30 days)
- [ ] Configure log queries

#### ELK Stack (Alternative to Loki)
- [ ] Deploy Elasticsearch cluster (3 nodes)
- [ ] Deploy Logstash (log processing)
- [ ] Deploy Kibana (visualization)
- [ ] Configure index lifecycle management

---

## 🚀 Phase 5: Load Testing & Optimization

### Priority: P5

**Timeline**: Week 25-26
**Effort**: 80-120 hours
**Risk**: MEDIUM-HIGH (may uncover performance issues)

### 13.1 Load Testing Plan

#### Tools
- [ ] Setup JMeter / Gatling / K6
- [ ] Create test scripts for all critical APIs
- [ ] Setup distributed load testing (10+ load generators)

#### Test Scenarios
- [ ] **Scenario 1**: Login flow (PKCE authentication)
  - Target: 1000 logins/sec
  - Duration: 30 minutes

- [ ] **Scenario 2**: Permission check (IAM service)
  - Target: 50k permission checks/sec
  - Duration: 60 minutes

- [ ] **Scenario 3**: Customer search (high traffic)
  - Target: 5k searches/sec
  - Duration: 30 minutes

- [ ] **Scenario 4**: Mixed workload (realistic simulation)
  - 100k concurrent users
  - Mix of read (80%) and write (20%) operations
  - Duration: 2 hours

- [ ] **Scenario 5**: Stress test (peak load)
  - Gradually increase load from 0 to 200k CCU
  - Find breaking point
  - Duration: 4 hours

#### Performance Tuning
- [ ] Tune JVM heap size (Xms, Xmx)
- [ ] Tune GC settings (G1GC, ZGC)
- [ ] Tune database connection pool size
- [ ] Tune Redis connection pool size
- [ ] Tune Kafka consumer threads
- [ ] Optimize SQL queries (EXPLAIN ANALYZE)
- [ ] Add missing database indexes
- [ ] Optimize cache TTL and eviction policies
- [ ] Tune Kubernetes resource limits

#### Chaos Engineering (Optional)
- [ ] Kill random pods (test resilience)
- [ ] Inject network latency
- [ ] Inject database failures
- [ ] Inject Redis failures
- [ ] Verify circuit breakers work
- [ ] Verify retry policies work

---

## ✅ Final Checklist

### Pre-Production
- [ ] All services deployed to staging
- [ ] All integration tests passing
- [ ] Load testing completed (100k CCU)
- [ ] Performance targets met (P99 < 200ms)
- [ ] Security audit completed
- [ ] Penetration testing completed
- [ ] Disaster recovery tested
- [ ] Database backup/restore tested
- [ ] Monitoring dashboards ready
- [ ] Alert rules configured
- [ ] Runbooks written
- [ ] On-call rotation established

### Production Readiness
- [ ] Production Kubernetes cluster ready
- [ ] Database cluster (Patroni HA) ready
- [ ] Redis cluster ready
- [ ] Kafka cluster ready
- [ ] Keycloak cluster ready
- [ ] MinIO cluster ready
- [ ] Elasticsearch cluster ready (optional)
- [ ] CDN configured
- [ ] DNS configured
- [ ] SSL certificates installed
- [ ] Firewall rules configured
- [ ] VPN access for developers
- [ ] Secrets stored in Vault/AWS Secrets Manager

### Go-Live
- [ ] Blue-green deployment strategy ready
- [ ] Rollback plan documented
- [ ] Communication plan (inform users)
- [ ] Support team trained
- [ ] War room scheduled
- [ ] Post-mortem template prepared

---

## 🎯 Success Metrics

### Technical Metrics
| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Concurrent Users | 100,000 | TBD | ⏳ |
| Requests/Second | 10,000 | TBD | ⏳ |
| API Latency P50 | <50ms | TBD | ⏳ |
| API Latency P99 | <200ms | TBD | ⏳ |
| Availability | 99.9% | TBD | ⏳ |
| Error Rate | <0.1% | TBD | ⏳ |
| IAM Cache Hit Rate | >95% | TBD | ⏳ |
| Database Query P99 | <100ms | TBD | ⏳ |

### Business Metrics
| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| User Satisfaction | >4.5/5 | TBD | ⏳ |
| Page Load Time | <2s | TBD | ⏳ |
| Time to Interactive | <3s | TBD | ⏳ |
| Zero Downtime Deployment | 100% | TBD | ⏳ |

---

## 📞 Support & Escalation

### Team Contacts
- **Architecture Team**: architecture@crm.com
- **DevOps Team**: devops@crm.com
- **Security Team**: security@crm.com
- **On-Call Engineer**: +1-xxx-xxx-xxxx (PagerDuty)

### Escalation Path
1. **L1 Support** → Application Team (respond in 15 min)
2. **L2 Support** → DevOps Team (respond in 30 min)
3. **L3 Support** → Architecture Team (respond in 1 hour)
4. **Critical Incident** → CTO (notify immediately)

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-22
**Next Review**: 2026-01-22
