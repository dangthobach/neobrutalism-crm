# Microservices Architecture

This directory contains the microservices implementation for the Neobrutalism CRM system.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                          │
│  (Next.js Frontend, Mobile Apps, Third-party clients)       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway (Port 8080)                  │
│  • Request routing                                           │
│  • JWT validation with L1 cache                             │
│  • Rate limiting                                             │
│  • Circuit breaker                                           │
│  • Request/Response transformation                           │
└────────┬────────────────────────────────┬───────────────────┘
         │                                │
         ▼                                ▼
┌────────────────────┐         ┌──────────────────────────────┐
│   IAM Service      │         │   Monolith (Strangler Fig)   │
│   (Port 8081)      │         │   (Port 8082)                │
│                    │         │                              │
│  • Authentication  │         │  • Customers                 │
│  • Authorization   │         │  • Contacts                  │
│  • jCasbin RBAC    │         │  • Content (CMS)             │
│  • L1+L2 caching   │         │  • Courses (LMS)             │
└────────┬───────────┘         └──────────┬───────────────────┘
         │                                │
         ▼                                ▼
┌─────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                       │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  PostgreSQL  │  │ Redis Cluster│  │   Keycloak   │     │
│  │  (Port 5432) │  │ (6379-6381)  │  │  (Port 8180) │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Prometheus  │  │   Grafana    │  │    Zipkin    │     │
│  │  (Port 9090) │  │  (Port 3001) │  │  (Port 9411) │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Services

### 1. API Gateway (`microservices/gateway`)
**Purpose**: Entry point for all client requests

**Features**:
- Dynamic routing with canary deployment support
- JWT validation with L1 Caffeine cache (100K entries, <1ms)
- Rate limiting (1000 req/s per user)
- Circuit breaker (Resilience4j)
- Request/Response transformation
- CORS configuration

**Tech Stack**:
- Spring Cloud Gateway 2023.0.1
- Resilience4j 2.2.0
- Caffeine cache
- Redis for rate limiting

**Port**: 8080

### 2. IAM Service (`microservices/iam-service`)
**Purpose**: Authentication proxy and authorization engine

**Features**:
- Keycloak integration for authentication
- jCasbin for RBAC/ABAC authorization
- Multi-tier caching (L1 Caffeine + L2 Redis + L3 DB)
- Permission pre-loading
- Cache invalidation with Redis pub/sub
- Materialized views for fast permission queries

**Tech Stack**:
- Spring Boot 3.5.7 WebFlux (Reactive)
- jCasbin 1.55.0
- R2DBC PostgreSQL (Reactive)
- Redis cluster
- Keycloak Admin Client

**Port**: 8081

## Performance Targets

### 100K CCU Support
- **Request throughput**: 10,000 req/s average, 50,000 req/s peak
- **Latency**:
  - P50: < 50ms
  - P95: < 200ms
  - P99: < 500ms
- **Cache hit rate**: > 95%

### Caching Strategy
```
Request → Gateway
    ↓
L1 Cache (Caffeine) → ~0.001ms latency, 95% hit rate
    ↓ (miss)
L2 Cache (Redis) → ~0.5ms latency, 99% hit rate
    ↓ (miss)
L3 Database → ~5ms latency, 100% hit rate
```

### Resource Allocation
- **Gateway**: 2-4 GB heap, 2-4 CPU cores
- **IAM Service**: 1-2 GB heap, 1-2 CPU cores
- **Redis Cluster**: 3 nodes, 256MB memory per node
- **PostgreSQL**: Connection pool 20-100 connections

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 21 JDK (for local development)
- Maven 3.8+
- Node.js 18+ (for frontend)

### Quick Start with Docker Compose

1. **Start all services**:
```bash
docker-compose -f docker-compose.microservices.yml up -d
```

2. **Check service health**:
```bash
# Gateway
curl http://localhost:8080/actuator/health

# IAM Service
curl http://localhost:8081/actuator/health

# Keycloak
curl http://localhost:8180/health/ready
```

3. **Access services**:
- Gateway: http://localhost:8080
- IAM Service: http://localhost:8081
- Keycloak Admin: http://localhost:8180 (admin/admin)
- Grafana: http://localhost:3001 (admin/admin)
- Prometheus: http://localhost:9090
- Zipkin: http://localhost:9411

### Local Development

1. **Build Gateway**:
```bash
cd microservices/gateway
mvn clean package
java -jar target/gateway-0.0.1-SNAPSHOT.jar
```

2. **Build IAM Service**:
```bash
cd microservices/iam-service
mvn clean package
java -jar target/iam-service-0.0.1-SNAPSHOT.jar
```

### Environment Variables

#### Gateway
```env
SPRING_PROFILES_ACTIVE=dev
IAM_SERVICE_URL=http://localhost:8081
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
JWT_SECRET=your-secret-key-min-256-bits
FEATURE_IAM_SERVICE_ENABLED=true
FEATURE_IAM_SERVICE_PERCENTAGE=100
```

#### IAM Service
```env
SPRING_PROFILES_ACTIVE=dev
DB_HOST=localhost
DB_PORT=5432
DB_NAME=iam_db
REDIS_NODE_1=localhost:6379
REDIS_NODE_2=localhost:6380
REDIS_NODE_3=localhost:6381
KEYCLOAK_SERVER_URL=http://localhost:8180
KEYCLOAK_REALM=crm
```

## Migration Strategy (Strangler Fig Pattern)

### Phase 1: Setup Infrastructure (Week 1-2)
- ✅ Create Gateway service
- ✅ Create IAM service
- ✅ Setup Docker Compose
- ✅ Setup Keycloak
- ✅ Setup Redis cluster
- ⏳ Setup monitoring (Prometheus + Grafana)

### Phase 2: Canary Deployment (Week 3-6)
- Route 1% of authentication traffic to IAM service
- Monitor metrics and errors
- Gradually increase: 5% → 10% → 25% → 50% → 100%
- Rollback capability at any stage

### Phase 3: Service Decomposition (Week 7-20)
- Customer Service (Week 7-10)
- Contact Service (Week 11-14)
- Content Service (Week 15-17)
- Course Service (Week 18-20)

### Phase 4: Data Migration (Week 21-24)
- Use Foreign Data Wrapper (FDW) initially
- Gradual data replication
- Switch to independent databases
- Decommission monolith database connections

### Phase 5: Cleanup (Week 25-26)
- Remove feature toggles
- Remove FDW connections
- Performance optimization
- Documentation

## API Examples

### Authentication Flow (PKCE)

1. **Frontend initiates login**:
```javascript
// Generate PKCE verifier and challenge
const codeVerifier = generateRandomString(128);
const codeChallenge = base64UrlEncode(sha256(codeVerifier));

// Redirect to Keycloak
window.location.href = `http://localhost:8180/realms/crm/protocol/openid-connect/auth?` +
  `client_id=crm-frontend&` +
  `redirect_uri=http://localhost:3000/callback&` +
  `response_type=code&` +
  `scope=openid profile email&` +
  `code_challenge=${codeChallenge}&` +
  `code_challenge_method=S256`;
```

2. **Exchange code for token**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=AUTH_CODE" \
  -d "redirect_uri=http://localhost:3000/callback" \
  -d "client_id=crm-frontend" \
  -d "code_verifier=CODE_VERIFIER"
```

3. **Use access token**:
```bash
curl http://localhost:8080/api/customers \
  -H "Authorization: Bearer ACCESS_TOKEN"
```

### Permission Check

```bash
curl -X POST http://localhost:8081/api/v1/auth/check-permission \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "tenantId": "tenant1",
    "resource": "/api/customers/456",
    "action": "GET"
  }'
```

### Assign Role to User

```bash
curl -X POST http://localhost:8081/api/v1/permissions/roles/assign \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "roleId": "role:admin",
    "tenantId": "tenant1"
  }'
```

## Monitoring

### Key Metrics to Monitor

**Gateway Metrics**:
- `http_server_requests_seconds` - Request latency
- `resilience4j_circuitbreaker_state` - Circuit breaker state
- `cache_gets_total{cache="jwtValidation",result="hit"}` - Cache hit rate
- `rate_limiter_available_permissions` - Rate limit status

**IAM Service Metrics**:
- `permission_check_seconds` - Permission check latency
- `cache_gets_total{cache="permissions"}` - Permission cache hits
- `casbin_enforcer_duration_seconds` - Casbin enforcement time

**Infrastructure Metrics**:
- Redis: `redis_connected_clients`, `redis_used_memory_bytes`
- PostgreSQL: `pg_stat_database_tup_returned`, `pg_stat_database_xact_commit`

### Health Checks

```bash
# Gateway health
curl http://localhost:8080/actuator/health

# IAM Service health
curl http://localhost:8081/actuator/health

# Check all services
docker-compose -f docker-compose.microservices.yml ps
```

## Troubleshooting

### Gateway can't connect to IAM Service
```bash
# Check IAM Service is running
docker logs crm-iam-service

# Check network connectivity
docker exec crm-gateway ping iam-service
```

### High cache miss rate
```bash
# Check cache statistics
curl http://localhost:8080/actuator/metrics/cache.gets
curl http://localhost:8081/actuator/metrics/cache.gets

# Increase cache size in application.yml
permission.cache.l1.max-size: 100000
```

### Redis connection issues
```bash
# Check Redis cluster status
docker exec crm-redis-1 redis-cli -a redis_password_2024 cluster info

# Check connection from service
docker exec crm-gateway redis-cli -h redis-node-1 -a redis_password_2024 ping
```

## Testing

### Load Testing with K6

```javascript
// load-test.js
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp up to 100 users
    { duration: '5m', target: 1000 },  // Ramp up to 1000 users
    { duration: '5m', target: 1000 },  // Stay at 1000 users
    { duration: '2m', target: 0 },     // Ramp down
  ],
};

export default function () {
  let res = http.get('http://localhost:8080/api/customers', {
    headers: { 'Authorization': 'Bearer ' + __ENV.ACCESS_TOKEN },
  });
  check(res, { 'status is 200': (r) => r.status === 200 });
}
```

Run test:
```bash
k6 run -e ACCESS_TOKEN=your_token load-test.js
```

## Security

### JWT Validation
- Gateway validates JWT signature with Keycloak public key
- JWT cached in L1 (Caffeine) for 60 seconds
- Expired tokens automatically rejected

### Permission Caching
- Permissions cached with user context
- Cache invalidated on role/permission changes
- Redis pub/sub for cross-instance invalidation

### Rate Limiting
- 1000 requests/second per user (configurable)
- Token bucket algorithm
- Distributed rate limiting via Redis

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.

## License

MIT License - see [LICENSE](../LICENSE) for details.
