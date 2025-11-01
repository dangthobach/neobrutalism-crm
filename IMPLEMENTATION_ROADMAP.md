# Implementation Roadmap - Neobrutalism CRM

## ✅ COMPLETED (Phase 1-3 + Task 4.1 & 4.4)

### Infrastructure & Framework
- ✅ Base entities (BaseEntity, AuditableEntity, SoftDeletableEntity, StatefulEntity, AggregateRoot)
- ✅ Base repositories (BaseRepository, SoftDeleteRepository, StatefulRepository)
- ✅ Base services (BaseService, AuditableService, SoftDeleteService, StatefulService)
- ✅ CQRS pattern implementation
- ✅ Event Sourcing (EventStore, OutboxPattern)
- ✅ Multi-tenancy support
- ✅ Data Scope security (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)

### Security & Auth (95% complete)
- ✅ JWT authentication
- ✅ Casbin integration for RBAC
- ✅ User management (full CRUD + status transitions)
- ✅ Role management
- ✅ Group management (hierarchical)
- ✅ Permission assignment (User-Role, User-Group, Group-Role, Role-Menu)
- ✅ Menu system with permissions
- ✅ API endpoint discovery and permissions
- ⚠️ Need: Token rotation, Token blacklist, Security headers

### Domain Models
- ✅ Organization (full CRUD)
- ✅ User (full CRUD)
- ✅ Role (full CRUD)
- ✅ Group (full CRUD + hierarchy)
- ✅ **Branch (full CRUD + hierarchy) - JUST COMPLETED**
- ✅ Menu (hierarchical)
- ✅ MenuTab
- ✅ MenuScreen
- ✅ ApiEndpoint
- ✅ **Customer entities (Customer, CustomerType, CustomerStatus, Industry) - JUST CREATED**
- ✅ **Contact entities (Contact, ContactRole, ContactStatus) - JUST CREATED**

### DevOps
- ✅ **Docker Compose setup with PostgreSQL + Redis - JUST COMPLETED**
- ✅ **Dockerfile for Spring Boot app - JUST COMPLETED**
- ✅ **.dockerignore - JUST COMPLETED**

---

## 🔄 IN PROGRESS (Current Session)

### Task 5.1: Customer & Contact Management
- ✅ Customer entities created
- ✅ Contact entities created
- ⏳ **NEXT: Customer & Contact repositories** (see code below)
- ⏳ Customer & Contact services
- ⏳ Customer & Contact DTOs
- ⏳ Customer & Contact controllers
- ⏳ Database migrations

---

## 📋 REMAINING WORK

### Priority 1: Complete Customer Module (IMMEDIATE)

#### Files to Create:

**1. Repositories:**
```
src/main/java/com/neobrutalism/crm/domain/customer/repository/CustomerRepository.java
src/main/java/com/neobrutalism/crm/domain/contact/repository/ContactRepository.java
```

**2. DTOs:**
```
src/main/java/com/neobrutalism/crm/domain/customer/dto/CustomerRequest.java
src/main/java/com/neobrutalism/crm/domain/customer/dto/CustomerResponse.java
src/main/java/com/neobrutalism/crm/domain/contact/dto/ContactRequest.java
src/main/java/com/neobrutalism/crm/domain/contact/dto/ContactResponse.java
```

**3. Services:**
```
src/main/java/com/neobrutalism/crm/domain/customer/service/CustomerService.java
src/main/java/com/neobrutalism/crm/domain/contact/service/ContactService.java
```

**4. Controllers:**
```
src/main/java/com/neobrutalism/crm/domain/customer/controller/CustomerController.java
src/main/java/com/neobrutalism/crm/domain/contact/controller/ContactController.java
```

**5. Database Migration:**
```
src/main/resources/db/migration/V108__Create_customer_contact_tables.sql
```

---

### Priority 2: Security Hardening

#### Task 4.3: Security Enhancements

**1. Refresh Token Rotation:**
```java
// Files to create/modify:
- src/main/java/com/neobrutalism/crm/common/security/RefreshTokenService.java
- src/main/java/com/neobrutalism/crm/common/security/RefreshToken.java (entity)
- Update AuthController.java to handle token rotation
```

**2. Token Blacklist (Redis):**
```java
// Files to create:
- src/main/java/com/neobrutalism/crm/common/security/TokenBlacklistService.java
- Update JwtAuthenticationFilter.java to check blacklist
```

**3. Security Headers:**
```java
// File to create:
- src/main/java/com/neobrutalism/crm/config/SecurityHeadersConfig.java
// Add to SecurityConfig:
- Content-Security-Policy
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- Strict-Transport-Security
- X-XSS-Protection
```

**4. JWT Secret from Environment:**
```yaml
# Update application.yml:
jwt:
  secret: ${JWT_SECRET:default-secret-key-change-in-production}
  expiration: ${JWT_EXPIRATION:3600000}
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}
```

**5. IP-based Rate Limiting:**
```java
// Files to create:
- src/main/java/com/neobrutalism/crm/common/ratelimit/IpRateLimitFilter.java
- Update RateLimitConfig.java
```

---

### Priority 3: Rate Limiting Configuration

#### Task 4.4: Role-based Rate Limiting

**1. Update RateLimitConfig:**
```java
// Configure different limits per role:
- SUPER_ADMIN: 10000 requests/min
- ADMIN: 1000 requests/min
- USER: 100 requests/min
- PUBLIC: 20 requests/min
```

**2. Add Rate Limit Headers:**
```java
// Add to responses:
- X-RateLimit-Limit
- X-RateLimit-Remaining
- X-RateLimit-Reset
- Retry-After (when limited)
```

---

### Priority 4: File Management System (Task 6.1)

#### Entities:
```
src/main/java/com/neobrutalism/crm/domain/attachment/model/Attachment.java
src/main/java/com/neobrutalism/crm/domain/attachment/model/AttachmentType.java
src/main/java/com/neobrutalism/crm/domain/attachment/model/FileMetadata.java
```

#### MinIO Integration:
```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

```java
// Files to create:
- src/main/java/com/neobrutalism/crm/config/MinioConfig.java
- src/main/java/com/neobrutalism/crm/common/service/FileStorageService.java
- src/main/java/com/neobrutalism/crm/domain/attachment/controller/AttachmentController.java
```

```yaml
# Add to application.yml:
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket: ${MINIO_BUCKET:crm-attachments}
```

```yaml
# Add to docker-compose.yml:
minio:
  image: minio/minio:latest
  container_name: crm-minio
  command: server /data --console-address ":9001"
  ports:
    - "9000:9000"
    - "9001:9001"
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  volumes:
    - minio_data:/data
```

---

### Priority 5: Notification System (Task 6.2)

#### Entities:
```
src/main/java/com/neobrutalism/crm/domain/notification/model/Notification.java
src/main/java/com/neobrutalism/crm/domain/notification/model/NotificationTemplate.java
src/main/java/com/neobrutalism/crm/domain/notification/model/NotificationPreference.java
src/main/java/com/neobrutalism/crm/domain/notification/model/NotificationType.java
src/main/java/com/neobrutalism/crm/domain/notification/model/NotificationChannel.java
```

#### WebSocket Configuration:
```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

```java
// Files to create:
- src/main/java/com/neobrutalism/crm/config/WebSocketConfig.java
- src/main/java/com/neobrutalism/crm/common/websocket/WebSocketHandler.java
- src/main/java/com/neobrutalism/crm/domain/notification/service/NotificationService.java
- src/main/java/com/neobrutalism/crm/domain/notification/service/EmailService.java
- src/main/java/com/neobrutalism/crm/domain/notification/controller/NotificationController.java
```

#### Email Integration:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

```yaml
# Add to application.yml:
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

---

### Priority 6: Reporting & Dashboard (Task 6.3)

#### Services:
```
src/main/java/com/neobrutalism/crm/domain/report/service/ReportService.java
src/main/java/com/neobrutalism/crm/domain/report/service/DashboardService.java
src/main/java/com/neobrutalism/crm/domain/report/service/MetricsCollectorService.java
```

#### Controllers:
```
src/main/java/com/neobrutalism/crm/domain/report/controller/ReportController.java
src/main/java/com/neobrutalism/crm/domain/report/controller/DashboardController.java
```

#### Reports to Implement:
1. **Sales Pipeline Report**
   - GET /api/reports/sales-pipeline
   - Opportunities by stage
   - Conversion rates
   - Average deal size

2. **Customer Acquisition Report**
   - GET /api/reports/customer-acquisition
   - New customers by period
   - Lead sources effectiveness
   - Customer lifetime value

3. **Activity Report**
   - GET /api/reports/activity
   - Tasks completed
   - Appointments scheduled
   - Follow-ups pending

4. **User Performance Report**
   - GET /api/reports/user-performance
   - Sales by user
   - Tasks by user
   - Response times

#### Dashboard Widgets:
1. Sales Funnel (visualization)
2. Revenue Charts (line/bar)
3. Activity Heatmap
4. Top Performers (leaderboard)
5. Recent Activities (timeline)
6. Upcoming Tasks
7. KPI Cards (total customers, revenue, etc.)

---

## 🎯 ESTIMATED EFFORT

### Remaining Work Breakdown:

| Task | Effort | Priority |
|------|--------|----------|
| Complete Customer/Contact Module | 4-6 hours | P0 |
| Security Hardening (Task 4.3) | 6-8 hours | P1 |
| Rate Limiting Enhancement | 2-3 hours | P1 |
| File Management System | 8-10 hours | P2 |
| Notification System | 10-12 hours | P2 |
| Reporting & Dashboard | 12-15 hours | P3 |
| **TOTAL** | **42-54 hours** | - |

---

## 🚀 QUICK START (After Completion)

### Development Mode:
```bash
# Start PostgreSQL + Redis
docker-compose up -d postgres redis

# Run Spring Boot app
mvn spring-boot:run
```

### Production Mode:
```bash
# Build and start all services
docker-compose up -d

# Check logs
docker-compose logs -f crm-backend
```

### With Debug Tools:
```bash
# Start with PgAdmin + Redis Commander
docker-compose --profile debug up -d
```

Access:
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- PgAdmin: http://localhost:5050 (admin@crm.local / admin_password_2024)
- Redis Commander: http://localhost:8081

---

## 📝 NOTES

### Current Status:
- **Phase 1-3**: 100% complete (Base framework, Security, Core domains)
- **Phase 4**: 50% complete (Branch Management ✅, Security hardening pending)
- **Phase 5**: 20% complete (Customer entities created, need services/controllers)
- **Phase 6**: 0% complete (File management, Notifications, Reporting)

### Next Immediate Steps:
1. ✅ Complete Customer & Contact repositories
2. ✅ Create DTOs (Request/Response)
3. ✅ Implement Services with business logic
4. ✅ Create Controllers with all CRUD endpoints
5. ✅ Create database migration SQL
6. ✅ Test the endpoints
7. Move to Security Hardening

---

**Last Updated:** 2025-11-01
**Session Progress:** Branch Management + Docker Setup + Customer/Contact Entities Created
**Next Session:** Complete Customer/Contact Module (repositories → services → controllers → migrations)
