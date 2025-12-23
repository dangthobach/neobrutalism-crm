# Kiến Trúc Microservices Chuẩn Enterprise - 100k CCU
## Blueprint Architecture for Neobrutalism CRM

**Version**: 1.0.0
**Date**: 2025-12-22
**Author**: Technical Architecture Team

---

## 📋 Tổng Quan Hệ Thống

### Mục tiêu Hệ thống
- **Concurrent Users**: 100,000 CCU
- **Availability**: 99.9% uptime
- **Latency**: P99 < 200ms
- **Throughput**: 10,000 requests/second
- **Scalability**: Horizontal scaling
- **Security**: Zero-trust architecture

### Công nghệ Core Stack
- **Backend**: Java 21 + Spring Boot 3.5.x + Spring Cloud 2024.x
- **Frontend**: React 19 + Next.js 16 + TanStack Query v5
- **Gateway**: Spring Cloud Gateway 4.x
- **Identity Provider**: Keycloak 24.x
- **Message Broker**: Apache Kafka 3.7.x
- **Cache**: Redis 7.x (Cluster Mode)
- **Database**: PostgreSQL 16.x (Patroni HA)
- **Service Mesh**: Istio 1.20.x
- **Observability**: Grafana Stack (Prometheus, Loki, Tempo)
- **Tracing**: OpenTelemetry + Jaeger
- **Container**: Docker + Kubernetes 1.29+

---

## 🏗️ Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL LAYER                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐               │
│  │   Web SPA    │   │  Mobile App  │   │  3rd Party   │               │
│  │   (React)    │   │   (Native)   │   │   Systems    │               │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘               │
│         │                  │                  │                         │
│         └──────────────────┼──────────────────┘                         │
└────────────────────────────┼────────────────────────────────────────────┘
                             │
                             │ HTTPS/OAuth2 PKCE
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         CDN LAYER (Cloudflare/AWS)                      │
│  • DDoS Protection  • WAF  • SSL/TLS Termination  • Edge Caching       │
└────────────────────────────┼────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    INGRESS LAYER (Kubernetes Ingress)                   │
│  • NGINX Ingress Controller  • Rate Limiting  • IP Whitelisting         │
└────────────────────────────┼────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Spring Cloud Gateway)                   │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  • Route Management           • Circuit Breaker                  │  │
│  │  • JWT Validation             • Request/Response Transformation  │  │
│  │  • Rate Limiting (Redis)      • API Versioning                   │  │
│  │  • Load Balancing             • Retry & Timeout                  │  │
│  │  • Request Aggregation        • CORS Handling                    │  │
│  │  • Cache Layer (Redis)        • Monitoring & Metrics             │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────┼────────────────────────────────────────────┘
                             │
                ┌────────────┴────────────┐
                │                         │
                ▼                         ▼
┌─────────────────────────┐  ┌─────────────────────────┐
│   Keycloak Cluster      │  │   IAM Service           │
│   (Identity Provider)   │  │   (Authorization)       │
│                         │  │                         │
│  • OIDC/OAuth2         │  │  • jCasbin Engine       │
│  • User Authentication │◄─┤  • Permission Check     │
│  • Token Generation    │  │  • ABAC/RBAC Logic      │
│  • SSO Integration     │  │  • Data Scope Filter    │
│  • Multi-tenant        │  │  • Audit Logging        │
│  • Password Policy     │  │  • L1/L2 Cache          │
└─────────────────────────┘  └─────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Organization    │  │  User Management │  │  Customer        │
│  Service         │  │  Service         │  │  Service         │
└──────────────────┘  └──────────────────┘  └──────────────────┘
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Contact         │  │  Task            │  │  Notification    │
│  Service         │  │  Service         │  │  Service         │
└──────────────────┘  └──────────────────┘  └──────────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    EVENT BUS (Apache Kafka Cluster)                     │
│  • 3 Brokers (HA)  • Replication Factor: 3  • Min ISR: 2               │
│  • Topics: domain-events, integration-events, audit-events              │
└─────────────────────────────────────────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  PostgreSQL      │  │  Redis Cluster   │  │  Elasticsearch   │
│  Cluster         │  │  (6 nodes)       │  │  Cluster         │
│  (Patroni HA)    │  │  • L2 Cache      │  │  • Logging       │
│                  │  │  • Session Store │  │  • Search        │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## 🔐 Authentication & Authorization Flow

### 1. Frontend PKCE Authentication Flow

```
┌─────────────┐                                          ┌──────────────┐
│  React SPA  │                                          │   Keycloak   │
│  (Browser)  │                                          │   Server     │
└──────┬──────┘                                          └──────┬───────┘
       │                                                        │
       │ 1. Generate PKCE code_verifier & code_challenge       │
       ├────────────────────────────────────────────────────────►
       │    GET /auth?response_type=code                       │
       │        &client_id=crm-frontend                        │
       │        &redirect_uri=https://app.crm.com/callback     │
       │        &code_challenge=xyz...                         │
       │        &code_challenge_method=S256                    │
       │                                                        │
       │ 2. User Login Page                                    │
       │◄────────────────────────────────────────────────────────
       │                                                        │
       │ 3. User enters credentials                            │
       ├────────────────────────────────────────────────────────►
       │                                                        │
       │ 4. Redirect with authorization_code                   │
       │◄────────────────────────────────────────────────────────
       │    302 /callback?code=abc123                          │
       │                                                        │
       │ 5. Exchange code for token with code_verifier         │
       ├────────────────────────────────────────────────────────►
       │    POST /token                                         │
       │    {code: "abc123", code_verifier: "xyz...",          │
       │     grant_type: "authorization_code"}                 │
       │                                                        │
       │ 6. Return access_token + refresh_token                │
       │◄────────────────────────────────────────────────────────
       │    {access_token: "...", refresh_token: "...",        │
       │     expires_in: 300}                                  │
       │                                                        │
       │ 7. Store tokens in memory (NOT localStorage)          │
       │    Use HTTP-only cookies for refresh_token            │
       │                                                        │
```

**Security Best Practices**:
- ✅ **PKCE Flow**: Prevents authorization code interception
- ✅ **Short-lived Access Token**: 5 minutes expiry
- ✅ **Long-lived Refresh Token**: 7 days expiry, HTTP-only cookie
- ✅ **Token Rotation**: Refresh token rotation on each refresh
- ✅ **No LocalStorage**: Tokens in memory only to prevent XSS
- ✅ **Silent Refresh**: Background token refresh before expiry

### 2. API Gateway Token Validation Flow

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌──────────┐
│  React SPA  │────►│  API Gateway │────►│ IAM Service │────►│ Service  │
└─────────────┘     └──────────────┘     └─────────────┘     └──────────┘
       │                    │                    │                  │
       │ 1. Request with    │                    │                  │
       │    Authorization   │                    │                  │
       │    Bearer token    │                    │                  │
       ├───────────────────►│                    │                  │
       │                    │ 2. Extract JWT     │                  │
       │                    │    from header     │                  │
       │                    ├───────────────────►│                  │
       │                    │ 3. Validate JWT    │                  │
       │                    │    signature with  │                  │
       │                    │    Keycloak pubkey │                  │
       │                    │    (cached in      │                  │
       │                    │     Redis - L2)    │                  │
       │                    │                    │                  │
       │                    │ 4. Check permission│                  │
       │                    │    via jCasbin     │                  │
       │                    │    (L1 Caffeine    │                  │
       │                    │     cache)         │                  │
       │                    │◄───────────────────│                  │
       │                    │ 5. Permission OK   │                  │
       │                    │                    │                  │
       │                    │ 6. Add claims to   │                  │
       │                    │    request header  │                  │
       │                    ├────────────────────────────────────►│
       │                    │    X-User-ID       │                  │
       │                    │    X-Tenant-ID     │                  │
       │                    │    X-Roles         │                  │
       │                    │    X-Data-Scope    │                  │
       │                    │                    │                  │
       │                    │ 7. Service processes request          │
       │                    │◄─────────────────────────────────────│
       │ 8. Response        │                    │                  │
       │◄───────────────────│                    │                  │
```

**Performance Optimization**:
- **L1 Cache (Caffeine)**: Permission check ~0.001ms, 1M ops/sec
- **L2 Cache (Redis)**: JWT pubkey cache ~0.5ms, 10K ops/sec
- **L3 Database**: jCasbin policies with composite indexes ~5ms
- **Hit Rate Target**: >95% on L1, >99% on L2

### 3. Authorization Decision Flow

```
┌──────────────────────────────────────────────────────────────┐
│              IAM Service - Authorization Engine               │
└──────────────────────────────────────────────────────────────┘
                             │
                ┌────────────┴────────────┐
                │  Check L1 Cache (Caffeine)
                │  Key: user::tenant::resource::action
                │
                ▼
         ┌─────────────┐
         │  Cache Hit? │
         └─────┬───────┘
               │
        ┌──────┴───────┐
        │ YES          │ NO
        ▼              ▼
  Return Result    ┌─────────────────┐
  (~0.001ms)       │ jCasbin Enforcer│
                   │ enforcer.enforce│
                   │ (user, tenant,  │
                   │  resource, action)
                   └────────┬────────┘
                            │
                   ┌────────┴────────┐
                   │ Check Policies: │
                   │ 1. User Roles   │
                   │ 2. Role Perms   │
                   │ 3. Regex Match  │
                   │ 4. Data Scope   │
                   └────────┬────────┘
                            │
                   ┌────────┴────────┐
                   │  ALLOW/DENY     │
                   │  Store in L1    │
                   │  TTL: 10 min    │
                   └─────────────────┘
                            │
                            ▼
                   Return Result
                   (~0.5-1ms)
```

---

## 🎯 Service Decomposition Strategy

### Domain-Driven Design Bounded Contexts

#### 1. **IAM Service** (Identity & Access Management)
**Responsibility**: Authentication proxy, Authorization, Permission management

**Technologies**:
- Spring Boot 3.5.x
- Spring Security 6.x
- jCasbin 1.55.x + JDBC Adapter
- Caffeine Cache (L1) + Redis (L2)
- PostgreSQL (Casbin policies)

**Endpoints**:
```
POST   /api/iam/auth/token/validate      # Validate JWT
POST   /api/iam/permissions/check        # Check permission
GET    /api/iam/permissions/user/{id}    # Get user permissions
POST   /api/iam/cache/invalidate         # Invalidate cache
```

**Database Schema**:
- `casbin_rule` - Policies (p, g, g2)
- `casbin_role` - Role definitions
- `casbin_cache_stats` - Cache metrics

**Scalability**:
- Stateless service
- Horizontal scaling: 3-10 instances
- L1 Cache per instance (10k entries)
- L2 Cache shared (Redis Cluster)

#### 2. **Organization Service**
**Responsibility**: Organization, Branch, Tenant management

**Key Features**:
- Multi-tenant isolation
- Branch hierarchy management
- Organization lifecycle (CQRS)
- Event sourcing for audit

**Endpoints**:
```
GET    /api/organizations
POST   /api/organizations
GET    /api/organizations/{id}/branches
POST   /api/organizations/{id}/branches
GET    /api/branches/{id}/children       # Get child branches
```

**Events Published**:
- `OrganizationCreatedEvent`
- `OrganizationUpdatedEvent`
- `BranchCreatedEvent`

**Scalability**: 2-5 instances

#### 3. **User Service**
**Responsibility**: User management, Profile, User-Role assignment

**Key Features**:
- User CRUD operations
- Password management (via Keycloak)
- User profile management
- Role assignment
- Data scope management

**Endpoints**:
```
GET    /api/users
POST   /api/users
PUT    /api/users/{id}
POST   /api/users/{id}/roles
DELETE /api/users/{id}/roles/{roleId}
GET    /api/users/{id}/permissions       # Aggregated view
```

**Integration**:
- Keycloak for authentication
- IAM Service for authorization
- Notification Service for email

**Scalability**: 3-10 instances

#### 4. **Customer Service**
**Responsibility**: Customer management, Customer lifecycle

**Key Features**:
- Customer CRUD with data scope
- Customer segmentation
- Customer search (Elasticsearch)
- Customer activity tracking

**Endpoints**:
```
GET    /api/customers?search=...&dataScope=...
POST   /api/customers
GET    /api/customers/{id}
PUT    /api/customers/{id}
GET    /api/customers/{id}/activities
```

**Scalability**: 5-15 instances (high traffic)

#### 5. **Contact Service**
**Responsibility**: Contact management, Interaction tracking

**Scalability**: 3-10 instances

#### 6. **Task Service**
**Responsibility**: Task management, Workflow

**Scalability**: 3-8 instances

#### 7. **Notification Service**
**Responsibility**: Multi-channel notifications (Email, SMS, Push, WebSocket)

**Key Features**:
- Email via SMTP (MailHog dev, SendGrid prod)
- SMS via Twilio
- Push notifications via FCM
- Real-time via WebSocket
- Notification templates (Thymeleaf)
- Digest mode (batch notifications)

**Architecture**:
```
Notification Service
├── REST API (Create notification)
├── Kafka Consumer (Domain events)
├── Template Engine (Thymeleaf)
├── Channel Adapters
│   ├── EmailAdapter (SMTP)
│   ├── SMSAdapter (Twilio)
│   ├── PushAdapter (FCM)
│   └── WebSocketAdapter (STOMP)
└── Scheduler (Digest mode)
```

**Scalability**: 3-5 instances

#### 8. **Attachment Service**
**Responsibility**: File storage, File metadata

**Technologies**:
- MinIO for object storage
- PostgreSQL for metadata
- Virus scanning (ClamAV)

**Scalability**: 2-5 instances

#### 9. **Course Service** (LMS)
**Responsibility**: Course management, Module, Lesson

**Scalability**: 3-8 instances

#### 10. **Content Service** (CMS)
**Responsibility**: Content management, Category, Tag

**Scalability**: 2-5 instances

---

## 🌐 API Gateway Configuration

### Spring Cloud Gateway Features

**File**: `gateway-service/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    gateway:
      # Global filters
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
        - name: CircuitBreaker
          args:
            name: defaultCircuitBreaker
            fallbackUri: forward:/fallback
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 100  # tokens/sec
            redis-rate-limiter.burstCapacity: 200   # max tokens
            redis-rate-limiter.requestedTokens: 1
        - name: Retry
          args:
            retries: 3
            statuses: BAD_GATEWAY, SERVICE_UNAVAILABLE
            methods: GET, POST
            backoff:
              firstBackoff: 50ms
              maxBackoff: 500ms
              factor: 2
              basedOnPreviousValue: false

      # Routes configuration
      routes:
        # IAM Service
        - id: iam-service
          uri: lb://iam-service
          predicates:
            - Path=/api/iam/**
          filters:
            - name: JwtAuthenticationFilter
            - StripPrefix=2
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 500
                redis-rate-limiter.burstCapacity: 1000

        # Organization Service
        - id: organization-service
          uri: lb://organization-service
          predicates:
            - Path=/api/organizations/**,/api/branches/**
          filters:
            - name: JwtAuthenticationFilter
            - name: AuthorizationFilter
              args:
                requiredRole: ROLE_USER
            - StripPrefix=2
            - name: CircuitBreaker
              args:
                name: organizationCircuitBreaker
                fallbackUri: forward:/fallback/organization

        # User Service
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - JwtAuthenticationFilter
            - AuthorizationFilter
            - StripPrefix=2

        # Customer Service (High traffic)
        - id: customer-service
          uri: lb://customer-service
          predicates:
            - Path=/api/customers/**
          filters:
            - JwtAuthenticationFilter
            - AuthorizationFilter
            - StripPrefix=2
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 1000
                redis-rate-limiter.burstCapacity: 2000
            - name: CacheResponseFilter
              args:
                ttl: 60  # Cache GET requests for 60s

      # Discovery configuration
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

# Resilience4j Circuit Breaker
resilience4j:
  circuitbreaker:
    instances:
      defaultCircuitBreaker:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true

  # Rate limiter
  ratelimiter:
    instances:
      defaultRateLimiter:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 0s

  # Bulkhead
  bulkhead:
    instances:
      defaultBulkhead:
        maxConcurrentCalls: 100
        maxWaitDuration: 10ms

# Redis configuration for rate limiting
spring:
  redis:
    cluster:
      nodes:
        - redis-node-1:6379
        - redis-node-2:6379
        - redis-node-3:6379
      max-redirects: 3
    password: ${REDIS_PASSWORD}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 100
        max-idle: 50
        min-idle: 10
```

### Custom Filters

#### 1. JwtAuthenticationFilter
```java
@Component
public class JwtAuthenticationFilter implements GatewayFilter, Ordered {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange);

        if (token == null) {
            return unauthorized(exchange);
        }

        return validateTokenWithCache(token)
            .flatMap(valid -> {
                if (!valid) {
                    return unauthorized(exchange);
                }

                Claims claims = jwtTokenProvider.getClaims(token);
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-ID", claims.get("sub", String.class))
                    .header("X-Tenant-ID", claims.get("tenantId", String.class))
                    .header("X-Roles", claims.get("roles", String.class))
                    .header("X-Data-Scope", claims.get("dataScope", String.class))
                    .header("X-Branch-ID", claims.get("branchId", String.class))
                    .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            })
            .onErrorResume(e -> {
                log.error("JWT validation error", e);
                return unauthorized(exchange);
            });
    }

    private Mono<Boolean> validateTokenWithCache(String token) {
        String cacheKey = "jwt:valid:" + token;

        // Check L2 cache (Redis)
        return redisTemplate.opsForValue().get(cacheKey)
            .map(Boolean::parseBoolean)
            .switchIfEmpty(
                Mono.fromCallable(() -> jwtTokenProvider.validateToken(token))
                    .flatMap(valid -> {
                        // Cache result for 1 minute
                        return redisTemplate.opsForValue()
                            .set(cacheKey, valid.toString(), Duration.ofMinutes(1))
                            .thenReturn(valid);
                    })
            );
    }

    @Override
    public int getOrder() {
        return -100; // Execute early
    }
}
```

#### 2. AuthorizationFilter
```java
@Component
public class AuthorizationFilter implements GatewayFilter, Ordered {

    @Autowired
    private WebClient iamServiceClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // Call IAM Service to check permission
        return iamServiceClient.post()
            .uri("/api/iam/permissions/check")
            .bodyValue(Map.of(
                "userId", userId,
                "tenantId", tenantId,
                "resource", path,
                "action", method
            ))
            .retrieve()
            .bodyToMono(PermissionCheckResponse.class)
            .flatMap(response -> {
                if (response.isAllowed()) {
                    return chain.filter(exchange);
                } else {
                    return forbidden(exchange);
                }
            })
            .onErrorResume(e -> {
                log.error("Permission check failed", e);
                return forbidden(exchange);
            });
    }

    @Override
    public int getOrder() {
        return -90; // After JWT validation
    }
}
```

---

## 💾 Database Strategy

### PostgreSQL HA Architecture with Patroni

```
┌─────────────────────────────────────────────────────────┐
│              HAProxy (Load Balancer)                    │
│  Port 5000 (Write)      Port 5001 (Read Replica)       │
└────────────┬──────────────────────┬─────────────────────┘
             │                      │
    ┌────────┴─────────┐   ┌────────┴─────────┐
    │                  │   │                  │
    ▼                  ▼   ▼                  ▼
┌─────────┐       ┌─────────┐       ┌─────────┐
│ Patroni │       │ Patroni │       │ Patroni │
│ Node 1  │◄─────►│ Node 2  │◄─────►│ Node 3  │
│(Primary)│       │(Replica)│       │(Replica)│
│         │       │         │       │         │
│PostgreSQL       │PostgreSQL       │PostgreSQL
│  16.x   │       │  16.x   │       │  16.x   │
└────┬────┘       └────┬────┘       └────┬────┘
     │                 │                 │
     └────────┬────────┴────────┬────────┘
              │                 │
              ▼                 ▼
      ┌──────────────┐   ┌──────────────┐
      │   etcd 1     │   │   etcd 2     │
      │   (DCS)      │◄─►│   (DCS)      │
      └──────────────┘   └──────────────┘
              ▲                 ▲
              └────────┬────────┘
                       │
                 ┌──────────────┐
                 │   etcd 3     │
                 │   (DCS)      │
                 └──────────────┘
```

**Features**:
- **Automatic Failover**: <30s RTO (Recovery Time Objective)
- **Synchronous Replication**: Zero data loss (RPO=0)
- **Read Scaling**: Read replicas for read-heavy workloads
- **Connection Pooling**: PgBouncer (1000 → 100 connections)

### Database Per Service Pattern

```
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  IAM Service     │    │  User Service    │    │ Customer Service │
└────────┬─────────┘    └────────┬─────────┘    └────────┬─────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  iam_db          │    │  user_db         │    │ customer_db      │
│  • casbin_rule   │    │  • users         │    │  • customers     │
│  • casbin_role   │    │  • user_roles    │    │  • customer_tags │
│  • cache_stats   │    │  • user_groups   │    │  • interactions  │
└──────────────────┘    └──────────────────┘    └──────────────────┘
         │                       │                       │
         └───────────────────────┴───────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │  Shared PostgreSQL      │
                    │  Cluster (Patroni HA)   │
                    │  • Logical Databases    │
                    │  • Row-Level Security   │
                    │  • Connection Pooling   │
                    └─────────────────────────┘
```

**Why Database Per Service**:
- ✅ **Service Independence**: Each service owns its data
- ✅ **Schema Evolution**: Independent schema changes
- ✅ **Technology Diversity**: Can use different DB types if needed
- ✅ **Failure Isolation**: One service DB issue doesn't affect others

**Shared Infrastructure**:
- Single PostgreSQL cluster with multiple logical databases
- Shared connection pooling (PgBouncer)
- Shared backup/restore infrastructure

---

## 🚀 Performance Optimization Strategies

### 1. Caching Strategy (Multi-Layer)

```
┌─────────────────────────────────────────────────────────────┐
│                    Caching Hierarchy                         │
└─────────────────────────────────────────────────────────────┘

Request
   │
   ▼
┌────────────────────┐
│ L1: API Gateway    │  ~0.1ms   • Response cache (Redis)
│     Cache          │            • 60s TTL for GET requests
└────────┬───────────┘            • Invalidate on POST/PUT/DELETE
         │ miss
         ▼
┌────────────────────┐
│ L2: IAM Service    │  ~0.001ms • Permission cache (Caffeine)
│     L1 Cache       │            • 10k entries, 10min TTL
└────────┬───────────┘            • 95%+ hit rate
         │ miss
         ▼
┌────────────────────┐
│ L3: IAM Service    │  ~0.5ms   • JWT pubkey cache (Redis)
│     L2 Cache       │            • Shared across instances
└────────┬───────────┘            • 30min TTL
         │ miss
         ▼
┌────────────────────┐
│ L4: Database       │  ~5ms     • jCasbin policies
│     with Indexes   │            • Composite indexes
└────────────────────┘            • Connection pooling
```

**Cache Invalidation Strategy**:
- **Event-Driven**: Kafka event → Invalidate cache
- **TTL-Based**: Automatic expiry after TTL
- **Manual**: Admin API to force invalidation

### 2. Database Query Optimization

**Composite Indexes** (Already implemented):
```sql
-- IAM Service (casbin_rule table)
CREATE INDEX idx_casbin_rule_v1_v0_ptype ON casbin_rule(v1, v0, ptype);
CREATE INDEX idx_casbin_rule_v1_v2 ON casbin_rule(v1, v2);
CREATE INDEX idx_casbin_rule_v1_v0_v2 ON casbin_rule(v1, v0, v2);
CREATE INDEX idx_casbin_rule_ptype_v0 ON casbin_rule(ptype, v0);
CREATE INDEX idx_casbin_rule_v1_ptype ON casbin_rule(v1, ptype);

-- User Service
CREATE INDEX idx_users_tenant_branch ON users(organization_id, branch_id);
CREATE INDEX idx_users_email ON users(email) WHERE deleted = false;
CREATE INDEX idx_users_status ON users(status) WHERE deleted = false;

-- Customer Service
CREATE INDEX idx_customers_tenant_created ON customers(organization_id, created_at DESC);
CREATE INDEX idx_customers_search ON customers USING GIN(to_tsvector('english', name || ' ' || email));
```

**Connection Pooling** (PgBouncer):
```ini
[databases]
iam_db = host=postgres-primary port=5432 dbname=iam_db
user_db = host=postgres-primary port=5432 dbname=user_db
customer_db = host=postgres-primary port=5432 dbname=customer_db

[pgbouncer]
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 100
min_pool_size = 10
reserve_pool_size = 25
reserve_pool_timeout = 5
max_db_connections = 100
```

### 3. API Gateway Optimization

**Request Aggregation** (BFF Pattern):
```java
@RestController
@RequestMapping("/api/bff")
public class BffController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping("/dashboard")
    public Mono<DashboardData> getDashboard() {
        // Parallel calls to multiple services
        Mono<UserProfile> userProfile = webClientBuilder.build()
            .get().uri("lb://user-service/api/users/me")
            .retrieve().bodyToMono(UserProfile.class);

        Mono<List<Notification>> notifications = webClientBuilder.build()
            .get().uri("lb://notification-service/api/notifications/unread")
            .retrieve().bodyToFlux(Notification.class).collectList();

        Mono<Statistics> stats = webClientBuilder.build()
            .get().uri("lb://customer-service/api/customers/stats")
            .retrieve().bodyToMono(Statistics.class);

        // Combine results
        return Mono.zip(userProfile, notifications, stats)
            .map(tuple -> new DashboardData(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3()
            ));
    }
}
```

**Benefits**:
- ✅ Reduces frontend-to-backend round trips
- ✅ Single authentication check
- ✅ Parallel execution of service calls
- ✅ Centralized error handling

---

## 📊 Monitoring & Observability

### Metrics Collection (Prometheus + Grafana)

**Key Metrics to Monitor**:

| Category | Metric | Alert Threshold |
|----------|--------|----------------|
| **Gateway** | Request Rate | >10k/s (capacity planning) |
| | Error Rate | >1% (critical) |
| | P99 Latency | >200ms (warning) |
| | Circuit Breaker Open | >0 (warning) |
| **IAM Service** | Permission Check Rate | >50k/s (capacity) |
| | L1 Cache Hit Rate | <90% (warning) |
| | L2 Cache Hit Rate | <95% (warning) |
| | Authorization Latency P99 | >50ms (warning) |
| **Services** | CPU Usage | >80% (warning) |
| | Memory Usage | >85% (critical) |
| | DB Connection Pool | >80% (warning) |
| | Kafka Consumer Lag | >1000 (critical) |
| **Database** | Active Connections | >800/1000 (warning) |
| | Replication Lag | >100ms (warning) |
| | Query P99 Latency | >100ms (warning) |

**Grafana Dashboards**:
1. **System Overview**: CPU, Memory, Disk, Network
2. **API Gateway**: Request rate, Error rate, Latency
3. **Service Health**: Instance count, Health status, Circuit breakers
4. **Database**: Connections, Query performance, Replication lag
5. **Cache Performance**: Hit rate, Eviction rate, Memory usage
6. **Business Metrics**: Users online, Requests/user, Top endpoints

### Distributed Tracing (Jaeger + OpenTelemetry)

**Trace Context Propagation**:
```
Request ID: 550e8400-e29b-41d4-a716-446655440000
Trace ID:   7af9e7a8-4e2c-4c9e-a5e3-8b7d9e5a1b3c

┌────────────────────────────────────────────────────────────────┐
│ Span 1: API Gateway                        [0ms - 150ms]       │
│  ├─ JWT Validation                         [2ms - 5ms]         │
│  ├─ Permission Check (IAM)                 [5ms - 8ms]         │
│  └─ Route to Service                       [8ms - 150ms]       │
│     │                                                           │
│     ├─ Span 2: Customer Service            [10ms - 145ms]      │
│     │  ├─ Data Scope Filter                [12ms - 15ms]       │
│     │  ├─ Database Query                   [15ms - 80ms]       │
│     │  └─ Response Serialization           [80ms - 85ms]       │
│     │                                                           │
│     └─ Span 3: Notification Service        [90ms - 140ms]      │
│        ├─ Kafka Producer                   [92ms - 95ms]       │
│        └─ WebSocket Broadcast              [95ms - 140ms]      │
└────────────────────────────────────────────────────────────────┘
```

**Implementation**:
```java
@Configuration
public class TracingConfig {

    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(
                        BatchSpanProcessor.builder(
                            JaegerGrpcSpanExporter.builder()
                                .setEndpoint("http://jaeger:4317")
                                .build()
                        ).build()
                    )
                    .build()
            )
            .build();
    }
}
```

### Logging Strategy (ELK Stack)

**Log Levels**:
- **ERROR**: System errors, unhandled exceptions
- **WARN**: Degraded performance, retry attempts
- **INFO**: Key business events (user login, order created)
- **DEBUG**: Detailed flow (dev/staging only)

**Structured Logging** (JSON format):
```json
{
  "timestamp": "2025-12-22T10:30:45.123Z",
  "level": "INFO",
  "service": "customer-service",
  "instance": "customer-service-pod-3",
  "traceId": "7af9e7a8-4e2c-4c9e-a5e3-8b7d9e5a1b3c",
  "spanId": "8b7d9e5a1b3c",
  "userId": "user-123",
  "tenantId": "org-456",
  "message": "Customer created successfully",
  "customerId": "cust-789",
  "duration_ms": 145
}
```

---

## 🔒 Security Best Practices

### 1. Zero-Trust Architecture

```
┌────────────────────────────────────────────────────────────┐
│                   Zero-Trust Principles                     │
└────────────────────────────────────────────────────────────┘

1. **Never Trust, Always Verify**
   • Every request authenticated & authorized
   • No implicit trust between services

2. **Least Privilege Access**
   • Services have minimal required permissions
   • Short-lived credentials (JWT 5min expiry)

3. **Service-to-Service Authentication**
   • mTLS for inter-service communication
   • Service identity via certificates

4. **Network Segmentation**
   • Services in private network
   • API Gateway as only public entry point
   • Database not accessible externally

5. **Audit Everything**
   • All authorization decisions logged
   • Immutable audit trail in separate system
```

### 2. API Security Headers

```yaml
# API Gateway adds security headers
spring:
  cloud:
    gateway:
      default-filters:
        - AddResponseHeader=X-Content-Type-Options, nosniff
        - AddResponseHeader=X-Frame-Options, DENY
        - AddResponseHeader=X-XSS-Protection, 1; mode=block
        - AddResponseHeader=Strict-Transport-Security, max-age=31536000; includeSubDomains
        - AddResponseHeader=Content-Security-Policy, default-src 'self'
        - AddResponseHeader=Referrer-Policy, strict-origin-when-cross-origin
        - RemoveResponseHeader=Server
```

### 3. Input Validation

**Bean Validation** (JSR-380):
```java
@RestController
@RequestMapping("/api/customers")
@Validated
public class CustomerController {

    @PostMapping
    public ResponseEntity<Customer> createCustomer(
        @Valid @RequestBody CustomerRequest request
    ) {
        // Spring automatically validates @Valid annotations
        Customer customer = customerService.create(request);
        return ResponseEntity.ok(customer);
    }
}

public record CustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 200, message = "Name must be 2-200 characters")
    String name,

    @Email(message = "Invalid email format")
    String email,

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    String phone
) {}
```

### 4. SQL Injection Prevention

**JPA/Hibernate** with parameterized queries:
```java
// ✅ SAFE: Parameterized query
@Query("SELECT c FROM Customer c WHERE c.name LIKE %:search% AND c.organizationId = :tenantId")
List<Customer> searchCustomers(
    @Param("search") String search,
    @Param("tenantId") UUID tenantId
);

// ❌ UNSAFE: String concatenation (DON'T DO THIS)
// String query = "SELECT * FROM customers WHERE name LIKE '%" + search + "%'";
```

**Specification Pattern** (Type-safe queries):
```java
public class CustomerSpecifications {

    public static Specification<Customer> hasName(String name) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Customer> belongsToTenant(UUID tenantId) {
        return (root, query, cb) ->
            cb.equal(root.get("organizationId"), tenantId);
    }
}

// Usage (completely safe)
Specification<Customer> spec = CustomerSpecifications
    .hasName(searchTerm)
    .and(CustomerSpecifications.belongsToTenant(tenantId));
List<Customer> customers = customerRepository.findAll(spec);
```

---

## 🔄 Event-Driven Architecture with Kafka

### Kafka Cluster Setup

```
┌────────────────────────────────────────────────────────┐
│             Kafka Cluster (3 Brokers)                  │
│  • Replication Factor: 3                               │
│  • Min In-Sync Replicas: 2                             │
│  • Acks: all (strongest durability)                    │
└────────────────────────────────────────────────────────┘
         │                    │                    │
    ┌────┴────┐         ┌────┴────┐         ┌────┴────┐
    │ Broker 1│         │ Broker 2│         │ Broker 3│
    │ Port    │         │ Port    │         │ Port    │
    │ 9092    │◄───────►│ 9093    │◄───────►│ 9094    │
    └─────────┘         └─────────┘         └─────────┘
```

### Event Topics

| Topic | Partitions | Retention | Purpose |
|-------|-----------|-----------|---------|
| `domain.organization.events` | 5 | 7 days | Organization lifecycle events |
| `domain.user.events` | 10 | 7 days | User lifecycle events |
| `domain.customer.events` | 20 | 7 days | Customer events (high volume) |
| `integration.notification.events` | 5 | 3 days | Notification triggers |
| `audit.events` | 3 | 90 days | Audit trail (compliance) |

### Event Schema (CloudEvents Standard)

```json
{
  "specversion": "1.0",
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "source": "https://api.crm.com/customer-service",
  "type": "com.neobrutalism.crm.customer.created.v1",
  "datacontenttype": "application/json",
  "time": "2025-12-22T10:30:45.123Z",
  "subject": "customer/cust-123",
  "data": {
    "customerId": "cust-123",
    "name": "Acme Corp",
    "email": "contact@acme.com",
    "organizationId": "org-456",
    "createdBy": "user-789",
    "timestamp": "2025-12-22T10:30:45.123Z"
  }
}
```

### Event Producer (Customer Service)

```java
@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private KafkaTemplate<String, CustomerEvent> kafkaTemplate;

    @Transactional
    public Customer createCustomer(CustomerRequest request) {
        // 1. Create customer in database
        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer = customerRepository.save(customer);

        // 2. Publish event to Kafka
        CustomerCreatedEvent event = new CustomerCreatedEvent(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getOrganizationId(),
            getCurrentUserId(),
            Instant.now()
        );

        kafkaTemplate.send(
            "domain.customer.events",
            customer.getId().toString(),  // Partition key
            event
        );

        return customer;
    }
}
```

### Event Consumer (Notification Service)

```java
@Service
public class CustomerEventConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(
        topics = "domain.customer.events",
        groupId = "notification-service",
        concurrency = "5"  // 5 consumer threads
    )
    public void handleCustomerEvent(
        @Payload CustomerEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        if (event instanceof CustomerCreatedEvent created) {
            // Send welcome email
            notificationService.sendWelcomeEmail(
                created.getEmail(),
                created.getName()
            );

            // Send internal notification
            notificationService.notifyAdmins(
                "New customer created: " + created.getName()
            );
        }
    }
}
```

### Idempotent Event Processing

```java
@Service
public class IdempotentEventProcessor {

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Transactional
    public void processEvent(String eventId, Runnable handler) {
        // Check if event already processed
        Optional<IdempotencyRecord> existing =
            idempotencyRepository.findById(eventId);

        if (existing.isPresent()) {
            log.info("Event {} already processed, skipping", eventId);
            return;
        }

        // Process event
        handler.run();

        // Mark as processed
        IdempotencyRecord record = new IdempotencyRecord();
        record.setEventId(eventId);
        record.setProcessedAt(Instant.now());
        idempotencyRepository.save(record);
    }
}
```

---

## 🚢 Deployment Strategy

### Kubernetes Deployment

**Namespace Structure**:
```
crm-system/
├── gateway/         # API Gateway
├── iam/             # IAM Service
├── organization/    # Organization Service
├── user/            # User Service
├── customer/        # Customer Service
├── notification/    # Notification Service
└── infrastructure/  # Shared infrastructure
    ├── redis/
    ├── kafka/
    └── keycloak/
```

**Service Deployment Example** (Customer Service):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: customer-service
  namespace: crm-system
  labels:
    app: customer-service
    version: v1
spec:
  replicas: 5  # Start with 5, scale to 15
  selector:
    matchLabels:
      app: customer-service
  template:
    metadata:
      labels:
        app: customer-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
      - name: customer-service
        image: registry.crm.com/customer-service:1.0.0
        imagePullPolicy: IfNotPresent
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres-primary:5000/customer_db"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-credentials
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-credentials
              key: password
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-broker-0:9092,kafka-broker-1:9093,kafka-broker-2:9094"
        - name: SPRING_REDIS_CLUSTER_NODES
          value: "redis-node-1:6379,redis-node-2:6379,redis-node-3:6379"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        volumeMounts:
        - name: config
          mountPath: /config
          readOnly: true
      volumes:
      - name: config
        configMap:
          name: customer-service-config
---
apiVersion: v1
kind: Service
metadata:
  name: customer-service
  namespace: crm-system
  labels:
    app: customer-service
spec:
  selector:
    app: customer-service
  ports:
  - name: http
    port: 8080
    targetPort: 8080
    protocol: TCP
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: customer-service-hpa
  namespace: crm-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: customer-service
  minReplicas: 5
  maxReplicas: 15
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "1000"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 5 min cooldown
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60   # 1 min warmup
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
```

### CI/CD Pipeline (GitLab CI)

```yaml
# .gitlab-ci.yml
stages:
  - test
  - build
  - deploy-staging
  - deploy-production

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  DOCKER_REGISTRY: registry.crm.com
  NAMESPACE_STAGING: crm-staging
  NAMESPACE_PROD: crm-production

# Test Stage
test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn clean test
    - mvn jacoco:report
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml
      coverage_report:
        coverage_format: cobertura
        path: target/site/jacoco/jacoco.xml

# Build Stage
build:
  stage: build
  image: docker:24-dind
  services:
    - docker:24-dind
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $DOCKER_REGISTRY
    - docker build -t $DOCKER_REGISTRY/customer-service:$CI_COMMIT_SHA .
    - docker tag $DOCKER_REGISTRY/customer-service:$CI_COMMIT_SHA $DOCKER_REGISTRY/customer-service:latest
    - docker push $DOCKER_REGISTRY/customer-service:$CI_COMMIT_SHA
    - docker push $DOCKER_REGISTRY/customer-service:latest
  only:
    - main
    - develop

# Deploy to Staging
deploy-staging:
  stage: deploy-staging
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context staging-cluster
    - kubectl set image deployment/customer-service customer-service=$DOCKER_REGISTRY/customer-service:$CI_COMMIT_SHA -n $NAMESPACE_STAGING
    - kubectl rollout status deployment/customer-service -n $NAMESPACE_STAGING --timeout=5m
  environment:
    name: staging
    url: https://staging-api.crm.com
  only:
    - develop

# Deploy to Production (Manual)
deploy-production:
  stage: deploy-production
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context prod-cluster
    - kubectl set image deployment/customer-service customer-service=$DOCKER_REGISTRY/customer-service:$CI_COMMIT_SHA -n $NAMESPACE_PROD
    - kubectl rollout status deployment/customer-service -n $NAMESPACE_PROD --timeout=10m
  environment:
    name: production
    url: https://api.crm.com
  when: manual
  only:
    - main
```

---

## 📦 Service Migration Plan

### Phase 1: Extract IAM Service (Week 1-2)
**Priority**: CRITICAL - Foundation for other services

**Tasks**:
1. Create `iam-service` module
2. Move authentication/authorization code
3. Setup jCasbin with database
4. Implement L1/L2 cache
5. Create REST API for permission checks
6. Update API Gateway to call IAM service
7. Load testing (target: 50k ops/sec)

**Current Code Locations**:
- `src/main/java/com/neobrutalism/crm/common/security/*`
- `src/main/java/com/neobrutalism/crm/domain/permission/*`
- `src/main/java/com/neobrutalism/crm/domain/role/*`

**Migration Strategy**:
```
Current Monolith                  IAM Service
├── SecurityConfig       ────►    ├── SecurityConfig
├── JwtTokenProvider     ────►    ├── JwtTokenProvider
├── PermissionService    ────►    ├── PermissionService
├── CasbinConfig         ────►    ├── CasbinConfig
└── CasbinCacheService   ────►    └── CasbinCacheService

Shared by all services            Consumed by all services
```

### Phase 2: Extract Organization Service (Week 3-4)
**Priority**: HIGH - Core domain

**Tasks**:
1. Create `organization-service` module
2. Move Organization, Branch entities
3. Setup database schema
4. Implement CQRS pattern
5. Publish domain events to Kafka
6. API Gateway routing

**Current Code Locations**:
- `src/main/java/com/neobrutalism/crm/domain/organization/*`
- `src/main/java/com/neobrutalism/crm/domain/branch/*`

### Phase 3: Extract User Service (Week 5-6)
**Priority**: HIGH

**Tasks**:
1. Create `user-service` module
2. Move User, UserRole entities
3. Integrate with Keycloak
4. Consume Organization events
5. Implement user search (Elasticsearch)

**Current Code Locations**:
- `src/main/java/com/neobrutalism/crm/domain/user/*`
- `src/main/java/com/neobrutalism/crm/domain/userrole/*`

### Phase 4: Extract Customer Service (Week 7-8)
**Priority**: MEDIUM

**Current Code Locations**:
- `src/main/java/com/neobrutalism/crm/domain/customer/*`
- `src/main/java/com/neobrutalism/crm/domain/contact/*`

### Phase 5: Extract Supporting Services (Week 9-12)
**Priority**: MEDIUM-LOW

- Task Service
- Notification Service
- Attachment Service
- Course Service (LMS)
- Content Service (CMS)

---

## 📐 Code Structure per Service

### Standard Service Structure

```
customer-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/neobrutalism/crm/customer/
│   │   │       ├── api/                    # REST Controllers
│   │   │       │   ├── CustomerController.java
│   │   │       │   └── dto/
│   │   │       │       ├── CustomerRequest.java
│   │   │       │       └── CustomerResponse.java
│   │   │       ├── domain/                 # Domain Model
│   │   │       │   ├── model/
│   │   │       │   │   ├── Customer.java
│   │   │       │   │   └── CustomerStatus.java
│   │   │       │   ├── repository/
│   │   │       │   │   └── CustomerRepository.java
│   │   │       │   ├── service/
│   │   │       │   │   └── CustomerDomainService.java
│   │   │       │   └── event/
│   │   │       │       ├── CustomerCreatedEvent.java
│   │   │       │       └── CustomerUpdatedEvent.java
│   │   │       ├── application/            # Application Services
│   │   │       │   ├── CustomerService.java
│   │   │       │   └── CustomerQueryService.java
│   │   │       ├── infrastructure/         # Infrastructure
│   │   │       │   ├── config/
│   │   │       │   │   ├── DatabaseConfig.java
│   │   │       │   │   ├── KafkaConfig.java
│   │   │       │   │   └── SecurityConfig.java
│   │   │       │   ├── messaging/
│   │   │       │   │   ├── CustomerEventProducer.java
│   │   │       │   │   └── OrganizationEventConsumer.java
│   │   │       │   └── client/
│   │   │       │       └── IamServiceClient.java
│   │   │       └── CustomerServiceApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__Create_customers_table.sql
│   │           └── V2__Add_indexes.sql
│   └── test/
│       └── java/
│           └── com/neobrutalism/crm/customer/
│               ├── api/
│               │   └── CustomerControllerTest.java
│               ├── domain/
│               │   └── CustomerServiceTest.java
│               └── integration/
│                   └── CustomerIntegrationTest.java
├── pom.xml
├── Dockerfile
└── k8s/
    ├── deployment.yaml
    ├── service.yaml
    └── hpa.yaml
```

---

## 🎯 Performance Target & Capacity Planning

### Target Metrics for 100k CCU

| Metric | Target | Notes |
|--------|--------|-------|
| **Concurrent Users** | 100,000 | Logged-in users |
| **Requests/Second** | 10,000 | Average load |
| **Peak RPS** | 30,000 | 3x average for spikes |
| **API Latency P50** | <50ms | Median response time |
| **API Latency P99** | <200ms | 99th percentile |
| **Availability** | 99.9% | Max 43min downtime/month |
| **Error Rate** | <0.1% | <10 errors per 10k requests |

### Resource Estimation

**API Gateway**:
- Instances: 3-5
- CPU: 2 cores per instance
- Memory: 4GB per instance
- Handles: ~5-10k RPS per instance

**IAM Service** (Critical Path):
- Instances: 5-10
- CPU: 4 cores per instance
- Memory: 8GB per instance (L1 cache)
- Handles: 50-100k permission checks/sec total

**Business Services** (Customer, User, etc.):
- Instances: 3-15 per service
- CPU: 2-4 cores per instance
- Memory: 4-8GB per instance
- Handles: 1-3k RPS per instance

**Database** (PostgreSQL Cluster):
- Nodes: 3 (1 primary + 2 replicas)
- CPU: 8-16 cores per node
- Memory: 32-64GB per node
- Connections: 1000 max (100 per service via pooling)
- IOPS: 10,000+ (SSD storage)

**Redis Cluster**:
- Nodes: 6 (3 primary + 3 replica)
- Memory: 8GB per node (48GB total)
- Operations: 100k ops/sec total
- Eviction: LRU policy

**Kafka Cluster**:
- Brokers: 3
- CPU: 4 cores per broker
- Memory: 16GB per broker
- Disk: 500GB SSD per broker
- Throughput: 100MB/s per broker

**Total Infrastructure**:
- CPU Cores: ~150-200
- Memory: ~300-400GB
- Storage: ~5TB (database + Kafka)
- Network: 10Gbps

### Cost Estimation (AWS Equivalent)

| Component | Instance Type | Count | Monthly Cost |
|-----------|--------------|-------|--------------|
| API Gateway | t3.large | 5 | $370 |
| IAM Service | r5.xlarge | 10 | $1,700 |
| Business Services | t3.xlarge | 50 | $3,700 |
| PostgreSQL | r5.4xlarge | 3 | $3,600 |
| Redis | r5.2xlarge | 6 | $3,600 |
| Kafka | m5.2xlarge | 3 | $1,800 |
| Load Balancer | ALB | 2 | $40 |
| **Total** | | | **~$15,000/month** |

Note: Does not include data transfer, storage, backups

---

## 🔧 Development Setup

### Prerequisites
- JDK 21
- Node.js 20+
- Docker & Docker Compose
- Maven 3.9+
- kubectl & Helm (for K8s)
- Git

### Local Development Environment

**Start Infrastructure**:
```bash
# Start PostgreSQL, Redis, Kafka, Keycloak
docker-compose -f docker-compose-dev.yml up -d

# Verify services are healthy
docker-compose ps
```

**Start Services** (for development):
```bash
# Terminal 1: API Gateway
cd gateway-service
mvn spring-boot:run

# Terminal 2: IAM Service
cd iam-service
mvn spring-boot:run

# Terminal 3: Customer Service
cd customer-service
mvn spring-boot:run

# Terminal 4: Frontend
cd frontend
npm run dev
```

**Environment Variables** (`.env.local`):
```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/crm_dev
DATABASE_USERNAME=crm_user
DATABASE_PASSWORD=crm_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Keycloak
KEYCLOAK_URL=http://localhost:8180
KEYCLOAK_REALM=crm
KEYCLOAK_CLIENT_ID=crm-backend
KEYCLOAK_CLIENT_SECRET=your-secret

# Service URLs
IAM_SERVICE_URL=http://localhost:8081
USER_SERVICE_URL=http://localhost:8082
CUSTOMER_SERVICE_URL=http://localhost:8083
```

---

## 📚 Documentation & Resources

### Technical Documentation
- [API Gateway Configuration](./docs/gateway-config.md)
- [IAM Service Architecture](./docs/iam-service.md)
- [Event Schema Registry](./docs/event-schemas.md)
- [Database Migrations](./docs/database-migrations.md)
- [Monitoring Setup](./docs/monitoring-setup.md)

### Runbooks
- [Service Deployment](./runbooks/deploy-service.md)
- [Database Failover](./runbooks/database-failover.md)
- [Incident Response](./runbooks/incident-response.md)
- [Scaling Services](./runbooks/scaling-services.md)

### API Documentation
- Swagger UI: `http://api.crm.com/swagger-ui.html`
- OpenAPI Spec: `http://api.crm.com/v3/api-docs`

---

## ✅ Checklist for Production Readiness

### Security
- [ ] Keycloak configured with strong password policy
- [ ] JWT signing with RS256 (asymmetric keys)
- [ ] mTLS between services
- [ ] Network policies in Kubernetes
- [ ] Secrets stored in Vault/AWS Secrets Manager
- [ ] Security scanning in CI/CD (Snyk, OWASP Dependency Check)
- [ ] Penetration testing completed

### Observability
- [ ] Prometheus metrics exposed by all services
- [ ] Grafana dashboards created
- [ ] Alerting rules configured
- [ ] Distributed tracing with Jaeger
- [ ] Centralized logging with ELK
- [ ] APM configured (optional: New Relic, Datadog)

### Reliability
- [ ] Circuit breakers configured
- [ ] Retry policies defined
- [ ] Timeout policies set
- [ ] Database connection pooling
- [ ] Rate limiting enabled
- [ ] Chaos engineering tests (optional: Chaos Monkey)

### Performance
- [ ] Load testing completed (100k CCU)
- [ ] Database indexes optimized
- [ ] Caching strategy validated
- [ ] CDN configured for static assets
- [ ] Image optimization

### Compliance
- [ ] GDPR compliance (data privacy)
- [ ] Audit logging enabled
- [ ] Data retention policies
- [ ] Backup and restore tested
- [ ] Disaster recovery plan

### Deployment
- [ ] Blue-green deployment strategy
- [ ] Automated rollback on failure
- [ ] Database migration strategy
- [ ] Feature flags for gradual rollout
- [ ] Staging environment identical to prod

---

## 🎓 Conclusion

This microservices architecture blueprint provides a production-ready, scalable foundation for building a high-performance CRM system that supports 100,000 concurrent users. The architecture emphasizes:

✅ **Security**: Zero-trust with Keycloak + IAM Service
✅ **Performance**: Multi-layer caching, optimized databases
✅ **Scalability**: Horizontal scaling with Kubernetes
✅ **Reliability**: Circuit breakers, retry, failover
✅ **Observability**: Comprehensive monitoring and tracing
✅ **Maintainability**: Clean architecture, domain-driven design

**Next Steps**:
1. Review and approve architecture
2. Setup infrastructure (Kubernetes cluster, databases)
3. Begin Phase 1: IAM Service extraction
4. Implement CI/CD pipeline
5. Load testing and optimization
6. Production deployment

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-22
**Maintained By**: Technical Architecture Team
**Contact**: architecture@crm.com
