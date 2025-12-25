# Gateway Service Setup Guide

## Tổng quan

Hệ thống đã được tách thành 2 services:

1. **gateway-service**: API Gateway hiệu năng cao với các tính năng:
   - Spring Cloud Gateway (reactive, non-blocking)
   - Service Discovery (Consul)
   - Rate Limiting (Redis-based)
   - L1 Cache (Caffeine) và L2 Cache (Redis)
   - Request Coalescing
   - Circuit Breaker
   - Load Balancing

2. **business-service**: Business logic service (code hiện tại đã được di chuyển)

## Cấu trúc Project

```
.
├── pom.xml                    # Parent POM
├── business-service/          # Business Service Module
│   ├── pom.xml
│   ├── src/main/java/...
│   └── src/main/resources/...
└── gateway-service/           # Gateway Service Module
    ├── pom.xml
    ├── src/main/java/...
    └── src/main/resources/...
```

## Build và Run

### Build cả 2 services

```bash
mvn clean install
```

### Build từng service riêng

```bash
# Build gateway-service
cd gateway-service
mvn clean install

# Build business-service
cd business-service
mvn clean install
```

### Run với Maven

```bash
# Run gateway-service (port 8080)
cd gateway-service
mvn spring-boot:run

# Run business-service (port 8081)
cd business-service
mvn spring-boot:run
```

### Run với Docker Compose

```bash
# Start tất cả services (Consul, Redis, PostgreSQL, Gateway, Business Service)
docker-compose -f docker-compose.gateway.yml up -d

# Xem logs
docker-compose -f docker-compose.gateway.yml logs -f

# Stop tất cả services
docker-compose -f docker-compose.gateway.yml down
```

## Cấu hình

### Gateway Service (Port 8080)

- **Service Discovery**: Consul (localhost:8500)
- **Rate Limiting**: Redis-based, configurable per route
- **Caching**: L1 (Caffeine) + L2 (Redis)
- **Request Coalescing**: Enabled for GET/HEAD requests

### Business Service (Port 8081)

- **Service Discovery**: Consul (localhost:8500)
- **Database**: PostgreSQL
- **Cache**: Redis

### Environment Variables

```bash
# Consul
CONSUL_HOST=localhost
CONSUL_PORT=8500

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password_2024

# PostgreSQL
POSTGRES_DB=neobrutalism_crm
POSTGRES_USER=crm_user
POSTGRES_PASSWORD=crm_password_2024

# JWT
JWT_SECRET=your-secret-key-change-this-in-production-min-256-bits
```

## API Endpoints

### Gateway Service

- **API Gateway**: `http://localhost:8080/api/**`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/prometheus`

### Business Service (Direct Access)

- **API**: `http://localhost:8081/api/**`
- **Health Check**: `http://localhost:8081/actuator/health`
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`

## Tính năng Gateway

### 1. Rate Limiting

- **Default**: 100 requests/second, burst 200
- **Per-IP**: 50 requests/second, burst 100
- **Per-User**: 200 requests/second, burst 400

Cấu hình trong `gateway-service/src/main/resources/application.yml`:

```yaml
gateway:
  rate-limit:
    enabled: true
    default-limit:
      replenish-rate: 100
      burst-capacity: 200
```

### 2. Caching (L1 + L2)

- **L1 Cache**: Caffeine (in-memory, 10k entries, 5min TTL)
- **L2 Cache**: Redis (distributed, 10min TTL)
- **Response Cache**: GET/HEAD requests cached for 60 seconds

### 3. Request Coalescing

- Groups identical requests within 100ms window
- Reduces backend load for high-traffic scenarios
- Enabled for GET and HEAD methods

### 4. Circuit Breaker

- Failure rate threshold: 50%
- Wait duration: 10 seconds
- Sliding window: 10 requests
- Minimum calls: 5

### 5. Service Discovery

- **Consul**: Automatic service registration and discovery
- **Health Checks**: 10s interval
- **Load Balancing**: Round-robin by default

## Monitoring

### Consul UI

- URL: `http://localhost:8500`
- View registered services and health status

### Prometheus

- URL: `http://localhost:9090`
- Metrics from both gateway and business services

### Grafana

- URL: `http://localhost:3001`
- Username: `admin`
- Password: `admin`

## Performance

Gateway được tối ưu cho:
- **100k+ CCU**: Reactive architecture, non-blocking I/O
- **Minimal Roundtrips**: L1/L2 caching, request coalescing
- **High Throughput**: Connection pooling, efficient routing

## Troubleshooting

### Service không register vào Consul

1. Kiểm tra Consul đang chạy: `http://localhost:8500`
2. Kiểm tra logs: `docker-compose logs gateway-service`
3. Kiểm tra network: `docker network ls`

### Rate limit errors

1. Kiểm tra Redis connection
2. Tăng rate limit trong `application.yml`
3. Kiểm tra logs: `docker-compose logs gateway-service`

### Cache không hoạt động

1. Kiểm tra Redis connection
2. Kiểm tra cache configuration trong `application.yml`
3. Xem cache stats qua actuator endpoints

## Migration Notes

- Code hiện tại đã được di chuyển vào `business-service/`
- Không có thay đổi logic, chỉ tách module
- Production code vẫn hoạt động bình thường
- Gateway chạy trên port 8080, business-service trên port 8081

