# TUẦN 6-8: SECURITY, DOCUMENTATION & DEPLOYMENT

## 🎯 MỤC TIÊU TỔNG
- Hardening security trước production
- Complete documentation (API, deployment, user guide)
- Performance optimization
- Production deployment preparation
- Establish monitoring & alerting

---

## 📅 TUẦN 6: SECURITY HARDENING & PERFORMANCE

### **SPRINT 6.1: Security Fixes (Ngày 1-3)**

#### **DAY 1: Fix JWT Secret & Environment Variables**

**Current Issue**: JWT secret hardcoded trong application.yml

**Solution**:

#### **1.1. Update application.yml**
```yaml
# File: src/main/resources/application.yml

jwt:
  secret: ${JWT_SECRET:} # No default - must be provided
  expiration: ${JWT_EXPIRATION:3600000}
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}

  mail:
    host: ${EMAIL_HOST:smtp.gmail.com}
    port: ${EMAIL_PORT:587}
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}

redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}
  password: ${REDIS_PASSWORD:}
```

#### **1.2. Create .env.example**
```bash
# File: .env.example

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/crm
DATABASE_USERNAME=crm_user
DATABASE_PASSWORD=change_me_in_production

# JWT (MUST BE AT LEAST 256 BITS / 32 CHARACTERS)
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production-min-32-chars
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Email
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@example.com
EMAIL_PASSWORD=your-email-password

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password_2024

# Application
APP_BASE_URL=http://localhost:3000
NODE_ENV=development

# Frontend
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
```

#### **1.3. Create Secret Generator Utility**
```java
// File: src/main/java/com/neobrutalism/crm/utils/SecretGenerator.java

package com.neobrutalism.crm.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class SecretGenerator {

    public static void main(String[] args) {
        // Generate 256-bit (32-byte) secret
        SecureRandom random = new SecureRandom();
        byte[] secret = new byte[32];
        random.nextBytes(secret);

        String base64Secret = Base64.getEncoder().encodeToString(secret);

        System.out.println("Generated JWT Secret (Base64):");
        System.out.println(base64Secret);
        System.out.println("\nAdd this to your .env file:");
        System.out.println("JWT_SECRET=" + base64Secret);
    }
}
```

**Run to generate secret**:
```bash
java -cp target/classes com.neobrutalism.crm.utils.SecretGenerator
```

#### **1.4. Add Startup Validation**
```java
// File: src/main/java/com/neobrutalism/crm/config/SecurityValidation.java

@Component
@Slf4j
public class SecurityValidation implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<String> errors = new ArrayList<>();

        // Validate JWT secret
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            errors.add("JWT_SECRET is not set");
        } else if (jwtSecret.length() < 32) {
            errors.add("JWT_SECRET must be at least 32 characters");
        } else if (jwtSecret.contains("change-this") || jwtSecret.contains("your-secret")) {
            errors.add("JWT_SECRET appears to be using default value");
        }

        // Validate database password
        if (dbPassword.equals("postgres") || dbPassword.equals("password")) {
            errors.add("Database password appears to be using weak default");
        }

        if (!errors.isEmpty()) {
            log.error("SECURITY VALIDATION FAILED:");
            errors.forEach(error -> log.error("  - {}", error));

            if (!"dev".equals(System.getenv("SPRING_PROFILES_ACTIVE"))) {
                throw new IllegalStateException("Security validation failed. Cannot start application.");
            }
        } else {
            log.info("✓ Security validation passed");
        }
    }
}
```

---

#### **DAY 2: CORS, HTTPS, Security Headers**

**2.1. Configure CORS Properly**
```java
// File: src/main/java/com/neobrutalism/crm/config/SecurityConfig.java

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Only allow specific origins in production
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/auth/login", "/api/auth/register")
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
                )
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.disable()) // Use CSP instead
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}
```

**2.2. application-prod.yml**
```yaml
# File: src/main/resources/application-prod.yml

app:
  cors:
    allowed-origins: ${ALLOWED_ORIGINS:https://yourdomain.com}

server:
  port: 8080
  ssl:
    enabled: ${SSL_ENABLED:false}
    key-store: ${SSL_KEYSTORE_PATH:}
    key-store-password: ${SSL_KEYSTORE_PASSWORD:}
    key-store-type: PKCS12

  # Security headers
  servlet:
    session:
      cookie:
        http-only: true
        secure: true
        same-site: strict

spring:
  security:
    require-ssl: true
```

**2.3. Configure Rate Limiting**
```java
// File: src/main/java/com/neobrutalism/crm/config/RateLimitConfig.java

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter authRateLimiter() {
        // Max 5 login attempts per minute
        return RateLimiter.create(5.0);
    }

    @Bean
    public RateLimiter apiRateLimiter() {
        // Max 100 requests per second
        return RateLimiter.create(100.0);
    }
}

// Interceptor
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimiter apiRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!apiRateLimiter.tryAcquire()) {
            response.setStatus(429); // Too Many Requests
            return false;
        }
        return true;
    }
}
```

---

#### **DAY 3: Input Validation & SQL Injection Prevention**

**3.1. Add Bean Validation**
```java
// File: src/main/java/com/neobrutalism/crm/domain/task/dto/CreateTaskRequest.java

public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    @Email(message = "Invalid email format")
    private String notifyEmail;

    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Tags can only contain alphanumeric characters, hyphens and underscores")
    private String tags;

    // Prevent XSS
    @JsonDeserialize(using = XssStringDeserializer.class)
    private String richTextContent;
}
```

**3.2. XSS Prevention**
```java
// File: src/main/java/com/neobrutalism/crm/common/security/XssStringDeserializer.java

public class XssStringDeserializer extends JsonDeserializer<String> {

    private static final Policy policy = new HtmlPolicyBuilder()
        .allowElements("p", "br", "strong", "em", "u")
        .allowUrlProtocols("https")
        .toFactory();

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) return null;

        // Sanitize HTML
        return policy.sanitize(value);
    }
}
```

**3.3. SQL Injection Prevention (Already using JPA)**
```java
// GOOD: Using JPA named parameters
@Query("SELECT t FROM Task t WHERE t.organizationId = :orgId AND t.title LIKE %:search%")
List<Task> findByOrganizationAndTitle(@Param("orgId") String orgId, @Param("search") String search);

// BAD: Never do this
// String query = "SELECT * FROM tasks WHERE title = '" + userInput + "'";
```

**Deliverables Day 1-3:**
- ✅ JWT secret externalized
- ✅ All secrets in environment variables
- ✅ CORS restricted to allowed origins
- ✅ Security headers configured
- ✅ Rate limiting implemented
- ✅ Input validation on all endpoints
- ✅ XSS prevention
- ✅ CSRF protection

---

### **SPRINT 6.2: Performance Optimization (Ngày 4-7)**

#### **DAY 4-5: Database Optimization**

**4.1. Analyze Slow Queries**
```sql
-- Enable slow query logging
-- File: docker/postgresql.conf

log_min_duration_statement = 1000  # Log queries > 1 second
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
log_statement = 'all'
```

**4.2. Add Missing Indexes**
```sql
-- File: src/main/resources/db/migration/V202__Add_performance_indexes.sql

-- Task queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tasks_assigned_to_status
  ON tasks(assigned_to_id, status) WHERE deleted = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tasks_due_date_status
  ON tasks(due_date, status) WHERE deleted = false AND due_date IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tasks_organization_created
  ON tasks(organization_id, created_at DESC) WHERE deleted = false;

-- Notification queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notifications_user_status
  ON notifications(user_id, status) WHERE deleted = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notifications_created_at
  ON notifications(created_at DESC) WHERE deleted = false;

-- Customer queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_email
  ON customers(email) WHERE deleted = false AND email IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_phone
  ON customers(phone) WHERE deleted = false AND phone IS NOT NULL;

-- Full-text search indexes
CREATE INDEX IF NOT EXISTS idx_customers_name_trgm
  ON customers USING gin(name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_tasks_title_trgm
  ON tasks USING gin(title gin_trgm_ops);

-- Enable trigram extension for fuzzy search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Analyze tables
ANALYZE tasks;
ANALYZE notifications;
ANALYZE customers;
ANALYZE contacts;
```

**4.3. Optimize N+1 Query Problems**
```java
// File: src/main/java/com/neobrutalism/crm/domain/task/repository/TaskRepository.java

// BAD: N+1 problem
@Query("SELECT t FROM Task t WHERE t.organizationId = :orgId")
List<Task> findByOrganization(@Param("orgId") String orgId);

// GOOD: Use JOIN FETCH
@Query("SELECT t FROM Task t " +
       "LEFT JOIN FETCH t.assignedTo " +
       "LEFT JOIN FETCH t.createdBy " +
       "WHERE t.organizationId = :orgId AND t.deleted = false")
List<Task> findByOrganizationWithDetails(@Param("orgId") String orgId);

// For pagination with JOIN FETCH (avoid MultipleBagFetchException)
@Query("SELECT DISTINCT t FROM Task t " +
       "LEFT JOIN FETCH t.assignedTo " +
       "WHERE t.organizationId = :orgId AND t.deleted = false")
List<Task> findByOrganizationWithAssignee(@Param("orgId") String orgId);
```

**4.4. Add Database Connection Pool Tuning**
```yaml
# File: src/main/resources/application-prod.yml

spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000

  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
          batch_versioned_data: true
        order_inserts: true
        order_updates: true
        query:
          in_clause_parameter_padding: true
```

---

#### **DAY 6-7: Frontend Performance**

**6.1. Code Splitting & Lazy Loading**
```typescript
// File: src/app/admin/layout.tsx

import dynamic from 'next/dynamic'

// Lazy load heavy components
const TaskBoard = dynamic(() => import('@/components/tasks/task-board'), {
  loading: () => <TaskBoardSkeleton />,
  ssr: false, // Disable SSR if not needed
})

const NotificationCenter = dynamic(() => import('@/components/notifications/notification-center'), {
  loading: () => <div>Loading notifications...</div>,
})

export default function AdminLayout({ children }) {
  return (
    <div>
      <Suspense fallback={<Skeleton />}>
        <NotificationCenter />
      </Suspense>

      <main>{children}</main>
    </div>
  )
}
```

**6.2. Image Optimization**
```typescript
// File: src/components/ui/avatar.tsx

import Image from 'next/image'

export function Avatar({ src, alt }) {
  return (
    <div className="relative w-10 h-10">
      <Image
        src={src}
        alt={alt}
        fill
        sizes="40px"
        className="rounded-full object-cover"
        loading="lazy"
      />
    </div>
  )
}
```

**6.3. React Query Optimization**
```typescript
// File: src/lib/react-query-config.ts

export const queryClientConfig = {
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      refetchOnWindowFocus: false,
      retry: 1,
      // Optimize for performance
      structuralSharing: true,
      // Keep previous data while fetching
      keepPreviousData: true,
    },
    mutations: {
      retry: 0,
    },
  },
}
```

**6.4. Bundle Analysis**
```bash
# Add to package.json scripts
"analyze": "ANALYZE=true next build"

# Install bundle analyzer
pnpm add -D @next/bundle-analyzer
```

```javascript
// File: next.config.mjs

const withBundleAnalyzer = require('@next/bundle-analyzer')({
  enabled: process.env.ANALYZE === 'true',
})

module.exports = withBundleAnalyzer({
  // ... existing config
})
```

**Deliverables Day 4-7:**
- ✅ Database indexes optimized
- ✅ N+1 queries eliminated
- ✅ Connection pool tuned
- ✅ Frontend code splitting
- ✅ Image optimization
- ✅ Bundle size < 200KB (gzipped)

---

## 📅 TUẦN 7: DOCUMENTATION

### **SPRINT 7.1: API Documentation (Ngày 1-3)**

**1.1. Enhanced OpenAPI Configuration**
```java
// File: src/main/java/com/neobrutalism/crm/config/OpenApiConfig.java

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Neobrutalism CRM API")
                .version("1.0.0")
                .description("Comprehensive CRM system with CMS and LMS capabilities")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@example.com")
                    .url("https://example.com")
                )
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization")
                )
            )
            .externalDocs(new ExternalDocumentation()
                .description("Full Documentation")
                .url("https://docs.example.com")
            );
    }
}
```

**1.2. Add Operation Examples**
```java
// File: src/main/java/com/neobrutalism/crm/domain/task/controller/TaskController.java

@Operation(
    summary = "Create a new task",
    description = "Creates a new task with the specified details. Organization ID is automatically set from the authenticated user's context.",
    tags = {"Tasks"}
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "Task created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = TaskResponse.class),
            examples = @ExampleObject(value = """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "title": "Follow up with customer",
                  "description": "Call customer regarding proposal",
                  "status": "TODO",
                  "priority": "HIGH",
                  "category": "FOLLOW_UP",
                  "dueDate": "2025-12-31T23:59:59",
                  "assignedTo": {
                    "id": "user-123",
                    "fullName": "John Doe"
                  }
                }
                """)
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request data",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "error": "Validation failed",
                  "details": {
                    "title": "Title is required"
                  }
                }
                """)
        )
    ),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden")
})
@PostMapping
public ResponseEntity<TaskResponse> createTask(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Task creation request",
        required = true,
        content = @Content(
            examples = @ExampleObject(value = """
                {
                  "title": "Follow up with customer",
                  "description": "Call customer regarding proposal",
                  "priority": "HIGH",
                  "category": "FOLLOW_UP",
                  "dueDate": "2025-12-31T23:59:59",
                  "assignedToId": "user-123",
                  "tags": ["urgent", "sales"]
                }
                """)
        )
    )
    @RequestBody @Valid CreateTaskRequest request
) {
    // ...
}
```

**1.3. Generate Postman Collection**
```bash
# Add Maven plugin
<plugin>
    <groupId>io.swagger.codegen.v3</groupId>
    <artifactId>swagger-codegen-maven-plugin</artifactId>
    <version>3.0.46</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/target/openapi.json</inputSpec>
                <language>postman-collection</language>
                <output>${project.basedir}/docs/postman</output>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Deliverables Day 1-3:**
- ✅ OpenAPI spec complete (100% coverage)
- ✅ Request/response examples for all endpoints
- ✅ Postman collection generated
- ✅ Interactive API docs at /swagger-ui.html

---

### **SPRINT 7.2: Developer Documentation (Ngày 4-5)**

**Create comprehensive docs**:

```markdown
# File: DOCUMENTATION/DEVELOPMENT_GUIDE.md

# Development Guide

## Prerequisites

- Java 21 LTS
- Node.js 20+
- pnpm 9+
- Docker & Docker Compose
- PostgreSQL 16 (or use Docker)

## Local Setup

### 1. Clone Repository

\`\`\`bash
git clone https://github.com/your-org/neobrutalism-crm.git
cd neobrutalism-crm
\`\`\`

### 2. Setup Environment Variables

\`\`\`bash
# Copy example env
cp .env.example .env

# Generate JWT secret
java -cp target/classes com.neobrutalism.crm.utils.SecretGenerator

# Edit .env and add the generated secret
\`\`\`

### 3. Start Infrastructure

\`\`\`bash
# Start PostgreSQL, Redis, MinIO
docker-compose up -d postgres redis minio
\`\`\`

### 4. Run Database Migrations

\`\`\`bash
# Migrations run automatically on startup
# Or manually:
mvn flyway:migrate
\`\`\`

### 5. Start Backend

\`\`\`bash
# Development mode
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Backend will start at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
\`\`\`

### 6. Start Frontend

\`\`\`bash
# Install dependencies
pnpm install

# Start dev server
pnpm dev

# Frontend will start at http://localhost:3000
\`\`\`

## Development Workflow

### Creating a New Feature

1. Create feature branch: \`git checkout -b feature/my-feature\`
2. Write tests first (TDD)
3. Implement feature
4. Run tests: \`mvn test\` and \`pnpm test\`
5. Create pull request

### Database Migrations

Create new migration:

\`\`\`bash
# Name format: V{version}__{description}.sql
# Example: V203__Add_task_dependencies_table.sql
\`\`\`

### Running Tests

\`\`\`bash
# Backend
mvn clean test

# Frontend
pnpm test

# E2E (if configured)
pnpm test:e2e
\`\`\`

## Troubleshooting

### Common Issues

1. **Port already in use**
   - Backend (8080): Change in application.yml
   - Frontend (3000): Change in next.config.mjs

2. **Database connection failed**
   - Check PostgreSQL is running: \`docker ps\`
   - Verify credentials in .env

3. **JWT errors**
   - Ensure JWT_SECRET is set and >= 32 characters

## Best Practices

- Always run tests before committing
- Follow naming conventions
- Use TypeScript strict mode
- Write meaningful commit messages
- Keep PRs focused and small
```

```markdown
# File: DOCUMENTATION/DEPLOYMENT_GUIDE.md

# Deployment Guide

## Production Checklist

### Pre-Deployment

- [ ] All tests passing
- [ ] Security audit completed
- [ ] Performance testing done
- [ ] Database backup strategy in place
- [ ] Monitoring configured
- [ ] SSL certificates ready
- [ ] Environment variables set
- [ ] Secrets rotated

### Environment Setup

#### Production Environment Variables

\`\`\`bash
# Database
DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/crm
DATABASE_USERNAME=crm_prod
DATABASE_PASSWORD=<strong-password>

# JWT (Generate new secret for production!)
JWT_SECRET=<generated-256-bit-secret>

# Email
EMAIL_HOST=smtp.sendgrid.net
EMAIL_USERNAME=apikey
EMAIL_PASSWORD=<sendgrid-api-key>

# MinIO/S3
MINIO_ENDPOINT=https://s3.amazonaws.com
MINIO_ACCESS_KEY=<aws-access-key>
MINIO_SECRET_KEY=<aws-secret-key>

# Redis
REDIS_HOST=redis-prod.example.com
REDIS_PORT=6379
REDIS_PASSWORD=<redis-password>

# Application
APP_BASE_URL=https://crm.example.com
SPRING_PROFILES_ACTIVE=prod

# CORS
ALLOWED_ORIGINS=https://crm.example.com,https://www.crm.example.com
\`\`\`

### Docker Deployment

#### Build Images

\`\`\`bash
# Backend
docker build -t crm-backend:latest -f Dockerfile.backend .

# Frontend
docker build -t crm-frontend:latest -f Dockerfile.frontend .
\`\`\`

#### Docker Compose Production

\`\`\`yaml
# docker-compose.prod.yml
version: '3.8'

services:
  backend:
    image: crm-backend:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=\${DATABASE_URL}
    restart: unless-stopped
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2'
          memory: 2G

  frontend:
    image: crm-frontend:latest
    environment:
      - NODE_ENV=production
    restart: unless-stopped
    deploy:
      replicas: 2

  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - backend
      - frontend
\`\`\`

### Kubernetes Deployment (Optional)

\`\`\`yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: crm-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: crm-backend
  template:
    metadata:
      labels:
        app: crm-backend
    spec:
      containers:
      - name: backend
        image: crm-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: crm-secrets
              key: database-url
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
\`\`\`

### Monitoring Setup

#### Prometheus + Grafana

Already configured in docker-compose.yml

Access:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin)

#### Dashboards to Import

1. Spring Boot Dashboard: 11378
2. PostgreSQL Dashboard: 9628
3. Redis Dashboard: 11835

### Backup Strategy

#### Database Backups

\`\`\`bash
# Daily automated backup
0 2 * * * pg_dump -h localhost -U crm_user crm > /backups/crm_\$(date +\%Y\%m\%d).sql

# Retention: 30 days
find /backups -name "crm_*.sql" -mtime +30 -delete
\`\`\`

### Rollback Procedure

1. Stop new deployment
2. Restore database from backup
3. Deploy previous version
4. Verify functionality
5. Monitor logs

## Post-Deployment

- [ ] Verify all services running
- [ ] Check logs for errors
- [ ] Test critical flows
- [ ] Monitor performance metrics
- [ ] Verify backups working
```

**Deliverables Day 4-5:**
- ✅ Development guide complete
- ✅ Deployment guide with Docker & K8s
- ✅ Troubleshooting documentation
- ✅ Backup & rollback procedures

---

### **SPRINT 7.3: User Documentation (Ngày 6-7 - Vietnamese)**

```markdown
# File: DOCUMENTATION/USER_GUIDE_VI.md

# Hướng Dẫn Sử Dụng CRM

## Mục Lục

1. Đăng Nhập & Quản Lý Tài Khoản
2. Quản Lý Khách Hàng
3. Quản Lý Công Việc (Tasks)
4. Thông Báo
5. Quản Lý Khóa Học (LMS)
6. Quản Lý Nội Dung (CMS)

---

## 1. Đăng Nhập & Quản Lý Tài Khoản

### Đăng nhập lần đầu

1. Truy cập: https://crm.example.com
2. Nhập email và mật khẩu
3. Click "Đăng nhập"

### Đổi mật khẩu

1. Click vào avatar góc phải trên
2. Chọn "Cài đặt tài khoản"
3. Chọn tab "Bảo mật"
4. Nhập mật khẩu cũ và mật khẩu mới
5. Click "Lưu thay đổi"

---

## 2. Quản Lý Khách Hàng

### Thêm khách hàng mới

1. Vào menu "Khách hàng"
2. Click nút "+ Thêm khách hàng"
3. Điền thông tin:
   - Tên khách hàng *
   - Email
   - Số điện thoại
   - Địa chỉ
   - Loại khách hàng (Cá nhân/Doanh nghiệp)
4. Click "Lưu"

### Tìm kiếm khách hàng

- Sử dụng thanh tìm kiếm ở góc trên
- Hỗ trợ tìm theo: Tên, Email, SĐT
- Bộ lọc nâng cao: Loại, Trạng thái, Ngày tạo

---

## 3. Quản Lý Công Việc (Tasks)

### Tạo công việc mới

1. Vào "Công việc" → Click "+ Tạo công việc"
2. Điền thông tin:
   - **Tiêu đề**: Mô tả ngắn gọn công việc
   - **Mô tả**: Chi tiết công việc
   - **Ưu tiên**: Thấp / Trung bình / Cao / Khẩn cấp
   - **Danh mục**: Bán hàng, Hỗ trợ, Theo dõi, v.v.
   - **Hạn hoàn thành**: Chọn ngày
   - **Giao cho**: Chọn người phụ trách
3. Click "Tạo công việc"

### Kanban Board

- **Cột "Chưa làm"**: Công việc mới tạo
- **Cột "Đang làm"**: Công việc đang thực hiện
- **Cột "Hoàn thành"**: Công việc đã xong

**Kéo thả** để chuyển trạng thái công việc

### Thêm comment

1. Click vào công việc để xem chi tiết
2. Scroll xuống phần "Bình luận"
3. Nhập nội dung
4. Click "Đăng bình luận"

---

## 4. Thông Báo

### Xem thông báo

- Click icon chuông góc phải trên
- Thông báo chưa đọc hiện màu xanh
- Click vào thông báo để xem chi tiết

### Cài đặt thông báo

1. Vào "Cài đặt" → "Thông báo"
2. Chọn kênh nhận thông báo:
   - Email
   - Push notification
   - Trong ứng dụng
3. Chọn loại thông báo muốn nhận
4. Thiết lập giờ im lặng (nếu cần)
5. Click "Lưu"

---

## 5. Quản Lý Khóa Học (LMS)

### Tạo khóa học mới

1. Vào "Khóa học" → "Tạo khóa học"
2. Điền thông tin cơ bản:
   - Tên khóa học
   - Mô tả
   - Hình ảnh
   - Giá (nếu có)
3. Thêm Module:
   - Click "+ Thêm Module"
   - Nhập tên module
4. Thêm Bài giảng vào module:
   - Click "+ Thêm bài giảng"
   - Chọn loại: Video / Text / Quiz
   - Upload nội dung
5. Click "Xuất bản"

### Theo dõi học viên

- Vào "Khóa học" → Chọn khóa học
- Tab "Học viên" hiển thị:
  - Danh sách học viên
  - Tiến độ học tập (%)
  - Bài giảng đã hoàn thành

---

## 6. Quản Lý Nội Dung (CMS)

### Viết bài mới

1. Vào "Nội dung" → "Tạo bài viết"
2. Nhập tiêu đề và nội dung
3. Chọn danh mục
4. Thêm tags (từ khóa)
5. Chọn trạng thái:
   - **Nháp**: Chưa công khai
   - **Xuất bản**: Hiển thị công khai
6. Click "Lưu"

### Quản lý danh mục

1. Vào "Nội dung" → "Danh mục"
2. Click "+ Thêm danh mục"
3. Nhập tên và mô tả
4. Chọn danh mục cha (nếu tạo danh mục con)
5. Click "Lưu"

---

## FAQ (Câu hỏi thường gặp)

**Q: Làm sao để phân quyền cho nhân viên?**
A: Vào "Cài đặt" → "Người dùng" → Chọn user → "Vai trò" → Chọn vai trò phù hợp

**Q: Có thể xuất danh sách khách hàng ra Excel không?**
A: Có. Vào "Khách hàng" → Click nút "Xuất Excel" góc trên

**Q: Quên mật khẩu phải làm sao?**
A: Click "Quên mật khẩu" ở trang đăng nhập, làm theo hướng dẫn gửi về email

---

## Liên Hệ Hỗ Trợ

- Email: support@example.com
- Hotline: 1900-xxxx
- Live chat: Click icon chat góc dưới bên phải
```

**Deliverables Day 6-7:**
- ✅ User guide (Vietnamese)
- ✅ Screenshots/videos
- ✅ FAQ section
- ✅ Support contact info

---

## 📅 TUẦN 8: DEPLOYMENT & MONITORING

### **Final Production Deployment**

**Day 1-2**: Deploy to staging
**Day 3-5**: UAT testing
**Day 6**: Deploy to production
**Day 7**: Monitor & fix issues

---

## ✅ DEFINITION OF DONE - TUẦN 6-8

- [ ] All security issues fixed
- [ ] Performance optimized (< 2s page load)
- [ ] API documentation complete
- [ ] Developer docs complete
- [ ] User guide complete (Vietnamese)
- [ ] Deployed to production
- [ ] Monitoring active
- [ ] Backup automated
- [ ] Team trained
