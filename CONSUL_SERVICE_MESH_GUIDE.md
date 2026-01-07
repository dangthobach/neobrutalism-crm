# Consul Service Mesh - Hướng dẫn Triển khai Đầy đủ

## Tổng quan

Tài liệu này hướng dẫn triển khai **Consul Service Mesh** đầy đủ cho hệ thống **neobrutalism-crm**, bao gồm tất cả các tính năng Service Mesh:

✅ **Service Discovery** - Tự động đăng ký và phát hiện services
✅ **Load Balancing** - Client-side load balancing qua Envoy
✅ **Retry / Timeout** - Tự động retry và timeout policies
✅ **Circuit Breaker** - Fault tolerance với outlier detection
✅ **mTLS / Zero-trust** - Mã hóa tự động giữa các services
✅ **Traffic Shaping** - Canary deployments, traffic splitting
✅ **Observability** - Metrics, tracing, logging tích hợp
✅ **Policy Enforcement** - Service intentions & security policies
✅ **Canary / Blue-Green** - Progressive deployment strategies

---

## Kiến trúc Service Mesh

```
┌─────────────────────────────────────────────────────────────────┐
│                     CONSUL SERVER                               │
│  • Service Registry & Health Checks                             │
│  • Key/Value Store (Configuration)                              │
│  • Connect (Service Mesh Control Plane)                         │
│  • Service Intentions (Security Policies)                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
    ┌────────────────┼────────────────┬────────────────┐
    │                │                │                │
┌───▼─────────┐  ┌──▼──────────┐  ┌─▼───────────┐ ┌──▼──────┐
│  Gateway    │  │  Business   │  │  IAM        │ │  Other  │
│  Service    │  │  Service    │  │  Service    │ │ Services│
│  :8080      │  │  :8081      │  │  :8081      │ │         │
├─────────────┤  ├─────────────┤  ├─────────────┤ ├─────────┤
│ Envoy Proxy │  │ Envoy Proxy │  │ Envoy Proxy │ │  Envoy  │
│ (Sidecar)   │  │ (Sidecar)   │  │ (Sidecar)   │ │Sidecar  │
│ :20000      │  │ :20000      │  │ :20000      │ │ :20000  │
└─────────────┘  └─────────────┘  └─────────────┘ └─────────┘
       │                │                │
       └────────────────┼────────────────┘
                        │
              mTLS Encrypted Traffic
                        │
       ┌────────────────┴────────────────┐
       │                                 │
┌──────▼──────┐               ┌─────────▼─────────┐
│ Observability│               │  Infrastructure   │
│    Stack     │               │    Services       │
├──────────────┤               ├───────────────────┤
│ • Prometheus │               │ • PostgreSQL      │
│ • Grafana    │               │ • Redis Cluster   │
│ • Jaeger     │               │ • MinIO           │
│ • Alertmgr   │               │ • Keycloak        │
└──────────────┘               └───────────────────┘
```

---

## Cấu trúc File

```
neobrutalism-crm/
├── consul/
│   ├── config/
│   │   ├── consul-server.json              # Consul server configuration
│   │   ├── gateway-service.json            # Gateway service registration
│   │   ├── business-service.json           # Business service registration
│   │   ├── iam-service.json                # IAM service registration
│   │   ├── proxy-defaults.json             # Global proxy settings
│   │   ├── intentions/
│   │   │   ├── gateway-to-business.json    # Security policies
│   │   │   ├── gateway-to-iam.json
│   │   │   └── business-to-iam.json
│   │   ├── service-defaults/
│   │   │   ├── gateway-defaults.json       # Service-specific settings
│   │   │   ├── business-defaults.json
│   │   │   └── iam-defaults.json
│   │   ├── traffic-management/
│   │   │   ├── business-service-router.json    # Traffic routing rules
│   │   │   ├── business-service-splitter.json  # Traffic splitting (canary)
│   │   │   └── business-service-resolver.json  # Service resolution & LB
│   │   └── resilience/
│   │       ├── circuit-breaker-config.json # Circuit breaker settings
│   │       └── timeout-config.json         # Timeout policies
│   ├── observability/
│   │   ├── prometheus/
│   │   │   ├── consul-prometheus.yml       # Prometheus config
│   │   │   └── alerts/
│   │   │       └── service-mesh-alerts.yml # Alert rules
│   │   ├── grafana/
│   │   │   └── provisioning/
│   │   │       └── datasources/
│   │   │           └── datasources.yml     # Grafana datasources
│   │   └── jaeger/
│   │       └── jaeger-config.yml           # Jaeger tracing config
│   └── scripts/
│       ├── register-services.sh            # Service registration script
│       ├── health-check.sh                 # Health check script
│       └── canary-deployment.sh            # Canary deployment manager
├── docker-compose.service-mesh.yml         # Full service mesh deployment
└── CONSUL_SERVICE_MESH_GUIDE.md           # This file
```

---

## Yêu cầu Hệ thống

### Software Requirements
- Docker 24.0+
- Docker Compose 2.20+
- Java 21 (JRE)
- curl, jq (cho scripts)
- 8GB RAM tối thiểu (khuyến nghị 16GB)
- 20GB disk space

### Ports được sử dụng

| Service | Port | Mô tả |
|---------|------|-------|
| **Consul Server** | 8500 | HTTP API & UI |
| | 8600 | DNS |
| | 8502 | gRPC |
| **Gateway Service** | 8080 | HTTP API |
| **Business Service** | 8081 | HTTP API |
| **IAM Service** | 8081 | HTTP API (mapped to 8082) |
| **PostgreSQL** | 5432 | Database |
| **Redis** | 6379 | Cache |
| **MinIO** | 9000, 9001 | Object storage |
| **Keycloak** | 8180 | OAuth2/OIDC |
| **Prometheus** | 9090 | Metrics |
| **Grafana** | 3000 | Visualization |
| **Jaeger** | 16686 | Tracing UI |
| **Alertmanager** | 9093 | Alerts |
| **Envoy Admin** | 19000-19001 | Proxy admin |

---

## Triển khai Nhanh (Quick Start)

### Bước 1: Build Services

```bash
# Build Gateway Service
cd gateway-service
mvn clean package -DskipTests
cd ..

# Build Business Service
cd business-service
mvn clean package -DskipTests
cd ..
```

### Bước 2: Khởi động Service Mesh

```bash
# Start tất cả services (infrastructure + microservices + observability)
docker-compose -f docker-compose.service-mesh.yml up -d

# Xem logs
docker-compose -f docker-compose.service-mesh.yml logs -f
```

### Bước 3: Đợi Services Ready

```bash
# Wait for Consul
until curl -sf http://localhost:8500/v1/status/leader | grep -q .; do
  echo "Waiting for Consul..."
  sleep 2
done

# Wait for Gateway (có thể mất 1-2 phút)
until curl -sf http://localhost:8080/actuator/health | grep -q UP; do
  echo "Waiting for Gateway Service..."
  sleep 5
done

echo "✅ Services are ready!"
```

### Bước 4: Đăng ký Services và Policies

```bash
cd consul/scripts

# Make scripts executable
chmod +x *.sh

# Register all services and configure mesh
./register-services.sh
```

### Bước 5: Verify Deployment

```bash
# Run health check
./health-check.sh

# Hoặc kiểm tra thủ công
curl http://localhost:8500/v1/catalog/services
```

### Bước 6: Truy cập UI

| Service | URL | Credentials |
|---------|-----|-------------|
| **Consul UI** | http://localhost:8500 | - |
| **Grafana** | http://localhost:3000 | admin / admin123 |
| **Jaeger** | http://localhost:16686 | - |
| **Prometheus** | http://localhost:9090 | - |
| **MinIO Console** | http://localhost:9001 | minioadmin / minioadmin123 |
| **Keycloak** | http://localhost:8180 | admin / admin123 |
| **MailHog** | http://localhost:8025 | - |

---

## Chi tiết Tính năng

### 1. Service Discovery

**Tự động đăng ký services:**

```json
// consul/config/business-service.json
{
  "service": {
    "name": "business-service",
    "port": 8081,
    "checks": [
      {
        "http": "http://business-service:8081/actuator/health",
        "interval": "10s"
      }
    ],
    "connect": {
      "sidecar_service": {}
    }
  }
}
```

**Kiểm tra services:**

```bash
# List all services
curl http://localhost:8500/v1/catalog/services

# Get service health
curl http://localhost:8500/v1/health/service/business-service

# DNS lookup
dig @localhost -p 8600 business-service.service.consul
```

---

### 2. Load Balancing

Service Resolver cấu hình load balancing strategy:

```json
// consul/config/traffic-management/business-service-resolver.json
{
  "LoadBalancer": {
    "Policy": "least_request",
    "LeastRequestConfig": {
      "ChoiceCount": 2
    }
  }
}
```

**Load balancing policies có sẵn:**
- `random` - Random selection
- `round_robin` - Round robin
- `least_request` - Chọn upstream có ít requests nhất (khuyến nghị)
- `ring_hash` - Consistent hashing
- `maglev` - Maglev consistent hashing

---

### 3. Circuit Breaker & Outlier Detection

Circuit breaker tự động cô lập services không khỏe:

```json
// consul/config/resilience/circuit-breaker-config.json
{
  "outlier_detection": {
    "consecutive_5xx": 5,              // Sau 5 lỗi liên tiếp
    "interval": "10s",                 // Kiểm tra mỗi 10s
    "base_ejection_time": "30s",       // Loại bỏ 30s
    "max_ejection_percent": 50,        // Tối đa 50% instances
    "enforcing_consecutive_5xx": 100   // 100% enforcement
  },
  "circuit_breakers": {
    "thresholds": [{
      "max_connections": 1024,
      "max_pending_requests": 1024,
      "max_requests": 512,
      "max_retries": 3
    }]
  }
}
```

**Kiểm tra circuit breaker status:**

```bash
# Via Envoy admin interface
curl http://localhost:19000/stats | grep circuit_breakers

# Via Prometheus
curl http://localhost:9090/api/v1/query?query=envoy_cluster_circuit_breakers_default_rq_open
```

---

### 4. Retry & Timeout Policies

**Global proxy defaults với retry:**

```json
// consul/config/proxy-defaults.json
{
  "EnvoyExtensions": [{
    "Name": "builtin/http/retry",
    "Arguments": {
      "RetryOn": "5xx,gateway-error,reset,connect-failure,refused-stream",
      "NumRetries": 3,
      "PerTryTimeout": "3s"
    }
  }]
}
```

**Service-specific timeouts:**

```yaml
# Gateway → Business: 5s connection timeout
# Gateway → IAM: 3s connection timeout
# Idle timeout: 60s
```

**Test retry behavior:**

```bash
# Simulate service failure
docker stop business-service

# Gateway sẽ tự động retry 3 lần
curl -v http://localhost:8080/api/users

# Restart service
docker start business-service
```

---

### 5. mTLS & Zero-Trust Security

**Tự động mTLS giữa services:**

Consul Connect tự động:
- Generate certificates cho mỗi service
- Rotate certificates định kỳ
- Verify mTLS connections qua Envoy sidecar

**Service Intentions (Security Policies):**

```json
// consul/config/intentions/gateway-to-business.json
{
  "Kind": "service-intentions",
  "Name": "business-service",
  "Sources": [{
    "Name": "gateway-service",
    "Action": "allow",
    "Permissions": [{
      "HTTP": {
        "PathPrefix": "/api/",
        "Methods": ["GET", "POST", "PUT", "DELETE"]
      }
    }]
  }]
}
```

**Default behavior:**
- ❌ Deny all traffic by default
- ✅ Explicitly allow với intentions
- 🔒 All traffic encrypted với mTLS

**Kiểm tra mTLS:**

```bash
# Check certificates
curl http://localhost:8500/v1/agent/connect/ca/roots

# View intentions
curl http://localhost:8500/v1/connect/intentions
```

---

### 6. Traffic Management (Canary Deployments)

**Service Router** - Route traffic based on conditions:

```json
// Route to canary if X-Debug header present
{
  "Match": {
    "HTTP": {
      "Header": [{"Name": "X-Debug", "Present": true}]
    }
  },
  "Destination": {
    "ServiceSubset": "canary"
  }
}
```

**Service Splitter** - Split traffic by percentage:

```json
// 90% stable, 10% canary
{
  "Splits": [
    {"Weight": 90, "ServiceSubset": "stable"},
    {"Weight": 10, "ServiceSubset": "canary"}
  ]
}
```

**Canary Deployment Workflow:**

```bash
# 1. Deploy canary version
docker-compose up -d business-service-canary

# 2. Start with 10% traffic
./consul/scripts/canary-deployment.sh business-service 10

# 3. Monitor metrics in Grafana
# Check error rates, latency, etc.

# 4. Gradually increase traffic
./canary-deployment.sh business-service 25
./canary-deployment.sh business-service 50
./canary-deployment.sh business-service 100

# 5. Promote canary to stable
# Update service metadata version to stable
```

**Test canary routing:**

```bash
# Hit stable version (90%)
curl http://localhost:8080/api/users

# Hit canary version (with debug header)
curl -H "X-Debug: true" http://localhost:8080/api/users
```

---

### 7. Observability

#### 7.1 Metrics (Prometheus)

**Service metrics được thu thập:**
- HTTP request rate, latency, errors
- JVM metrics (heap, threads, GC)
- Connection pool metrics (HikariCP)
- Cache metrics (Caffeine, Redis)
- Envoy sidecar metrics
- Consul health checks

**Prometheus targets:**

```yaml
# Service discovery tự động
- job_name: 'consul-services'
  consul_sd_configs:
    - server: 'consul-server:8500'
      datacenter: 'dc1'

# Actuator endpoints
- job_name: 'gateway-service-actuator'
  metrics_path: '/actuator/prometheus'

# Envoy sidecars
- job_name: 'envoy-sidecars'
  metrics_path: '/stats/prometheus'
```

**Useful Prometheus queries:**

```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# P95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Circuit breaker open
envoy_cluster_circuit_breakers_default_rq_open

# Service instances
up{job=~".*-service.*"}
```

#### 7.2 Distributed Tracing (Jaeger)

**Trace propagation:**
- Automatic context propagation via Envoy
- OpenTelemetry integration
- Support cho Zipkin, Jaeger protocols

**View traces:**
1. Go to http://localhost:16686
2. Select service: `gateway-service`
3. Click "Find Traces"
4. Analyze latency spans

**Trace context headers:**
- `x-request-id` - Request ID
- `x-b3-traceid` - Trace ID (B3 format)
- `x-b3-spanid` - Span ID

#### 7.3 Dashboards (Grafana)

**Pre-configured datasources:**
- Prometheus (metrics)
- Jaeger (traces)
- Consul (service health)

**Recommended dashboards:**
- **Service Mesh Overview**: Import ID 13421
- **Envoy Global**: Import ID 11022
- **Spring Boot**: Import ID 12464
- **JVM Micrometer**: Import ID 4701

**Import dashboard:**
```bash
# In Grafana UI
1. Go to Dashboards → Import
2. Enter dashboard ID (e.g., 13421)
3. Select Prometheus datasource
4. Click Import
```

#### 7.4 Alerting

**Alert rules configured:**

| Alert | Condition | Severity |
|-------|-----------|----------|
| ServiceDown | Service không healthy > 2min | Critical |
| HighErrorRate | Error rate > 5% trong 5min | Warning |
| HighResponseTime | P95 latency > 1s trong 10min | Warning |
| CircuitBreakerOpen | Circuit breaker mở > 2min | Critical |
| HighConnectionPoolUsage | Connection pool > 80% trong 5min | Warning |
| DatabaseConnectionPoolExhaustion | DB pool > 90% | Critical |

**View alerts:**
- Prometheus: http://localhost:9090/alerts
- Alertmanager: http://localhost:9093

---

### 8. Policy Enforcement

**Service Intentions** kiểm soát:
- Service-to-service communication
- HTTP path và method restrictions
- Header-based routing

**Ví dụ: Deny all, allow specific:**

```bash
# Set default deny
curl -X PUT http://localhost:8500/v1/connect/intentions/exact \
  -d '{
    "SourceName": "*",
    "DestinationName": "*",
    "Action": "deny"
  }'

# Allow specific communication
curl -X PUT http://localhost:8500/v1/config \
  --data @consul/config/intentions/gateway-to-business.json
```

---

## Quản lý & Vận hành

### Health Checks

```bash
# Run comprehensive health check
./consul/scripts/health-check.sh

# Check specific service
curl http://localhost:8500/v1/health/service/business-service?passing

# Check Envoy sidecar
curl http://localhost:19000/stats
curl http://localhost:19000/clusters
```

### Service Registration

```bash
# Re-register all services
./consul/scripts/register-services.sh

# Register single service
curl -X PUT http://localhost:8500/v1/agent/service/register \
  --data @consul/config/business-service.json

# Deregister service
curl -X PUT http://localhost:8500/v1/agent/service/deregister/business-service-1
```

### Configuration Updates

```bash
# Update service defaults
curl -X PUT http://localhost:8500/v1/config \
  --data @consul/config/service-defaults/business-defaults.json

# Update traffic split
./consul/scripts/canary-deployment.sh business-service 25

# View current config
curl http://localhost:8500/v1/config/service-defaults/business-service
```

### Logs

```bash
# View all logs
docker-compose -f docker-compose.service-mesh.yml logs -f

# Specific service
docker-compose -f docker-compose.service-mesh.yml logs -f gateway-service
docker-compose -f docker-compose.service-mesh.yml logs -f consul-server

# Envoy access logs (nếu enabled)
docker-compose -f docker-compose.service-mesh.yml logs -f gateway-envoy
```

### Backup & Restore

```bash
# Backup Consul data
docker exec consul-server consul snapshot save /tmp/backup.snap
docker cp consul-server:/tmp/backup.snap ./consul-backup-$(date +%Y%m%d).snap

# Restore
docker cp ./consul-backup.snap consul-server:/tmp/backup.snap
docker exec consul-server consul snapshot restore /tmp/backup.snap
```

---

## Troubleshooting

### Service không đăng ký được

```bash
# 1. Check Consul server
docker-compose ps consul-server
curl http://localhost:8500/v1/status/leader

# 2. Check service logs
docker-compose logs gateway-service | grep -i consul

# 3. Verify configuration
curl http://localhost:8500/v1/agent/services
```

### Envoy sidecar không kết nối

```bash
# 1. Check sidecar status
docker-compose ps gateway-envoy

# 2. Check Envoy logs
docker-compose logs gateway-envoy

# 3. Verify service registration
curl http://localhost:8500/v1/catalog/service/gateway-service

# 4. Check Envoy admin
curl http://localhost:19000/clusters
curl http://localhost:19000/config_dump
```

### Circuit breaker luôn mở

```bash
# 1. Check upstream health
curl http://localhost:8500/v1/health/service/business-service

# 2. Check Envoy stats
curl http://localhost:19000/stats | grep outlier_detection

# 3. Reduce consecutive_5xx threshold tạm thời
# Edit consul/config/resilience/circuit-breaker-config.json
# Change "consecutive_5xx": 5 → 10

# 4. Re-apply config
curl -X PUT http://localhost:8500/v1/config \
  --data @consul/config/resilience/circuit-breaker-config.json
```

### High latency

```bash
# 1. Check metrics
curl http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(http_server_requests_seconds_bucket[5m]))

# 2. View traces in Jaeger
# http://localhost:16686 → Find slow traces

# 3. Check connection pools
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# 4. Check cache hit ratio
curl http://localhost:8080/actuator/metrics/cache.gets
```

### Service Discovery không hoạt động

```bash
# 1. Verify Consul DNS
dig @localhost -p 8600 business-service.service.consul

# 2. Check service tags
curl http://localhost:8500/v1/catalog/service/business-service

# 3. Verify Spring Cloud Consul config
docker-compose exec gateway-service env | grep CONSUL
```

---

## Performance Tuning

### Envoy Resource Limits

```json
// consul/config/service-defaults/gateway-defaults.json
{
  "Limits": {
    "MaxConnections": 1024,        // Tăng nếu high load
    "MaxPendingRequests": 1024,
    "MaxConcurrentRequests": 512
  }
}
```

### Circuit Breaker Tuning

```json
{
  "consecutive_5xx": 5,              // Giảm = aggressive CB
  "base_ejection_time": "30s",       // Thời gian loại bỏ instance
  "max_ejection_percent": 50         // Tăng nếu cần aggressive
}
```

### Connection Pool Tuning

```yaml
# gateway-service application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30      # Tăng nếu nhiều DB queries
      minimum-idle: 10
      connection-timeout: 30000
```

### Cache Tuning

```yaml
# Caffeine cache
cache:
  caffeine:
    spec: maximumSize=10000,expireAfterWrite=10m
```

---

## Security Best Practices

### 1. Enable ACLs (Production)

```json
// consul-server.json
{
  "acl": {
    "enabled": true,
    "default_policy": "deny",
    "enable_token_persistence": true
  }
}
```

### 2. TLS for Consul Communication

```bash
# Generate certificates
consul tls ca create
consul tls cert create -server -dc dc1

# Configure Consul với TLS
# Update consul-server.json
```

### 3. Secrets Management

```bash
# Use environment variables, not hardcoded
# Or integrate với Vault
docker-compose.service-mesh.yml:
  environment:
    POSTGRES_PASSWORD: ${DB_PASSWORD}
    REDIS_PASSWORD: ${REDIS_PASSWORD}
```

### 4. Network Segmentation

```yaml
# docker-compose.service-mesh.yml
networks:
  consul-mesh:
    driver: bridge
    internal: true  # No external access
  public:
    driver: bridge
```

---

## Migration từ Existing Setup

### Từ docker-compose.gateway.yml

```bash
# 1. Backup current setup
docker-compose -f docker-compose.gateway.yml down
docker volume ls  # Note volumes

# 2. Migrate volumes (nếu cần)
# Copy data từ old volumes sang new volumes

# 3. Start service mesh
docker-compose -f docker-compose.service-mesh.yml up -d

# 4. Verify migration
./consul/scripts/health-check.sh
```

### Rollback Plan

```bash
# If issues, rollback to previous setup
docker-compose -f docker-compose.service-mesh.yml down
docker-compose -f docker-compose.gateway.yml up -d
```

---

## Monitoring & Metrics

### Key Metrics to Monitor

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| Error Rate | < 1% | > 5% |
| P95 Latency | < 500ms | > 1s |
| CPU Usage | < 70% | > 85% |
| Memory Usage | < 80% | > 90% |
| Connection Pool | < 70% | > 85% |
| Circuit Breaker Open | 0 | > 0 |

### Grafana Dashboards

```bash
# Import pre-built dashboards
1. Service Mesh Overview (ID: 13421)
2. Consul Dashboard (ID: 10642)
3. Envoy Stats (ID: 11022)
4. Spring Boot 2.1 Statistics (ID: 12464)
5. JVM Micrometer (ID: 4701)
```

---

## Appendix

### A. Service Mesh Components

| Component | Version | Purpose |
|-----------|---------|---------|
| Consul | 1.17 | Service discovery & mesh control plane |
| Envoy | 1.28 | Sidecar proxy |
| Prometheus | latest | Metrics collection |
| Grafana | latest | Visualization |
| Jaeger | latest | Distributed tracing |
| Alertmanager | latest | Alert management |

### B. Environment Variables

```bash
# Consul
CONSUL_HTTP_ADDR=http://localhost:8500

# Spring Cloud Consul
SPRING_CLOUD_CONSUL_HOST=consul-server
SPRING_CLOUD_CONSUL_PORT=8500
SPRING_CLOUD_CONSUL_DISCOVERY_ENABLED=true

# Services
SPRING_PROFILES_ACTIVE=dev
JAVA_OPTS=-Xms512m -Xmx1g
```

### C. Useful Commands Cheat Sheet

```bash
# Health checks
curl http://localhost:8500/v1/health/state/any
curl http://localhost:8080/actuator/health

# Service catalog
curl http://localhost:8500/v1/catalog/services
curl http://localhost:8500/v1/catalog/service/business-service

# Configuration
curl http://localhost:8500/v1/config
curl http://localhost:8500/v1/config/service-defaults/business-service

# Intentions
curl http://localhost:8500/v1/connect/intentions

# Metrics
curl http://localhost:9090/api/v1/query?query=up
curl http://localhost:8080/actuator/prometheus

# Tracing
curl http://localhost:16686/api/services
```

---

## Kết luận

Bạn đã triển khai thành công **Consul Service Mesh** đầy đủ tính năng cho hệ thống neobrutalism-crm!

**Next Steps:**
1. ✅ Test từng tính năng (circuit breaker, retry, canary)
2. ✅ Configure alerts phù hợp với SLA
3. ✅ Import Grafana dashboards
4. ✅ Document runbooks cho team
5. ✅ Plan cho production deployment

**Support:**
- Consul Docs: https://www.consul.io/docs
- Envoy Docs: https://www.envoyproxy.io/docs
- Service Mesh Patterns: https://www.consul.io/docs/connect

---

**Tác giả:** Claude Sonnet 4.5 🤖
**Ngày tạo:** 2025-12-28
**Version:** 1.0.0
