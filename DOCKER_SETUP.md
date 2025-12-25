# 🐳 Docker Setup Guide - Neobrutalism CRM

Hướng dẫn triển khai Neobrutalism CRM với Docker Compose cho môi trường **Production-Ready** với **100k CCU**.

## 📋 Tổng Quan Kiến Trúc

### Services Stack

| Service | Port | Mô tả |
|---------|------|-------|
| **Keycloak** | 8180 | Identity Provider (OAuth2/OIDC) |
| **Gateway** | 8080 | API Gateway (BFF Pattern) |
| **IAM Service** | 8081 | Authentication & Authorization |
| **PostgreSQL Master** | 5432 | Database chính (Write) |
| **PostgreSQL Replica** | 5433 | Database replica (Read) |
| **Redis Cluster** | 7000-7005 | 3 Master + 3 Slave nodes |
| **MinIO** | 9000, 9001 | Object Storage |
| **Prometheus** | 9090 | Metrics collection |
| **Grafana** | 3001 | Monitoring dashboard |
| **Zipkin** | 9411 | Distributed tracing |
| **MailHog** | 8025 | Email testing (dev only) |

---

## 🚀 Quick Start

### 1. Chuẩn Bị

```bash
# Clone repository
git clone <your-repo>
cd neobrutalism-crm

# Copy environment file
cp .env.example .env

# Update passwords trong .env
nano .env
```

### 2. Start Full Stack

```bash
# Start tất cả services
docker-compose -f docker-compose.microservices.yml up -d

# Kiểm tra logs
docker-compose -f docker-compose.microservices.yml logs -f

# Kiểm tra health
docker-compose -f docker-compose.microservices.yml ps
```

### 3. Verify Services

```bash
# Keycloak Admin Console
http://localhost:8180/admin
# Username: admin / Password: admin

# Gateway Health Check
curl http://localhost:8080/actuator/health

# IAM Service Health Check
curl http://localhost:8081/actuator/health

# Grafana Dashboard
http://localhost:3001
# Username: admin / Password: admin

# MinIO Console
http://localhost:9001
# Username: minioadmin / Password: minioadmin123
```

---

## 🔧 Chi Tiết Cấu Hình

### PostgreSQL Replication

**Master-Slave Streaming Replication:**

```
┌─────────────────┐       WAL Stream      ┌─────────────────┐
│ PostgreSQL      │ ──────────────────────>│ PostgreSQL      │
│ Master (5432)   │                        │ Replica (5433)  │
│ Write Operations│                        │ Read Operations │
└─────────────────┘                        └─────────────────┘
```

**Cách hoạt động:**
1. Master database nhận tất cả **WRITE** operations
2. Replica tự động sync qua **WAL (Write-Ahead Log)**
3. Application routing: `@Transactional(readOnly=true)` → Replica

**Test replication:**

```bash
# Connect to master
docker exec -it crm-postgres-master psql -U crm_user -d neobrutalism_crm

# Kiểm tra replication status
SELECT * FROM pg_stat_replication;

# Connect to replica
docker exec -it crm-postgres-replica psql -U crm_user -d neobrutalism_crm

# Verify data đã sync
SELECT count(*) FROM users;
```

---

### Redis Cluster

**6-Node Cluster (3 Master + 3 Slave):**

```
Master-1 (7000) ────> Slave-1 (7003)
Master-2 (7001) ────> Slave-2 (7004)
Master-3 (7002) ────> Slave-3 (7005)
```

**Hash Slot Distribution:**
- Master-1: Slots 0-5460
- Master-2: Slots 5461-10922
- Master-3: Slots 10923-16383

**Test cluster:**

```bash
# Check cluster info
docker exec -it crm-redis-master-1 redis-cli -c -p 7000 -a redis_password_2024 CLUSTER INFO

# Check cluster nodes
docker exec -it crm-redis-master-1 redis-cli -c -p 7000 -a redis_password_2024 CLUSTER NODES

# Test set/get (auto redirect to correct node)
docker exec -it crm-redis-master-1 redis-cli -c -p 7000 -a redis_password_2024
> SET user:123 "John Doe"
-> Redirected to slot [5007] located at 172.18.0.5:7001
OK

> GET user:123
"John Doe"
```

**Failover test:**

```bash
# Stop master-1
docker stop crm-redis-master-1

# Slave-1 tự động promote thành master
docker exec -it crm-redis-slave-1 redis-cli -c -p 7003 -a redis_password_2024 ROLE
# Output: master

# Restart master-1 (sẽ trở thành slave)
docker start crm-redis-master-1
```

---

### Keycloak Setup

**Realm đã được tự động import:** `neobrutalism-crm`

**Default Users:**

| Username | Password | Role | Email |
|----------|----------|------|-------|
| admin | admin123 | ADMIN | admin@neobrutalism.com |
| demo | demo123 | USER | demo@neobrutalism.com |

**OAuth2 Clients:**

| Client ID | Type | Secret | Redirect URI |
|-----------|------|--------|--------------|
| gateway-client | OAuth2 Client | gateway-secret-... | http://localhost:8080/login/oauth2/code/* |
| iam-service | Resource Server | iam-service-secret-... | N/A (bearer-only) |

**Test OAuth2 Flow:**

```bash
# 1. Get Authorization Code
curl -X GET "http://localhost:8180/realms/neobrutalism-crm/protocol/openid-connect/auth?client_id=gateway-client&redirect_uri=http://localhost:8080/callback&response_type=code&scope=openid"

# 2. Exchange code for token
curl -X POST "http://localhost:8180/realms/neobrutalism-crm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "client_id=gateway-client" \
  -d "client_secret=gateway-secret-change-in-production" \
  -d "code=<authorization_code>" \
  -d "redirect_uri=http://localhost:8080/callback"

# 3. Verify token
curl -X POST "http://localhost:8180/realms/neobrutalism-crm/protocol/openid-connect/token/introspect" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=<access_token>" \
  -d "client_id=gateway-client" \
  -d "client_secret=gateway-secret-change-in-production"
```

---

## 📊 Monitoring

### Prometheus Metrics

**Access:** http://localhost:9090

**Key Metrics:**

```promql
# Request rate
rate(http_server_requests_seconds_count[1m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# 95th percentile latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Redis cache hit rate
redis_cache_hits_total / (redis_cache_hits_total + redis_cache_misses_total)

# Database connection pool usage
hikaricp_connections_active / hikaricp_connections_max
```

### Grafana Dashboards

**Access:** http://localhost:3001

**Pre-configured dashboards:**
- JVM Metrics (Heap, GC, Threads)
- HTTP Metrics (Requests, Errors, Latency)
- Redis Cluster Metrics
- PostgreSQL Metrics
- Circuit Breaker Status

---

## 🔒 Security Checklist

### Production Security

- [ ] **Change all default passwords** trong `.env`
- [ ] **Generate strong JWT_SECRET** (min 256 bits)
- [ ] **Update Keycloak client secrets**
- [ ] **Enable HTTPS** (add TLS certificates)
- [ ] **Restrict CORS origins** (không dùng `*`)
- [ ] **Enable firewall rules** (chỉ expose cần thiết)
- [ ] **Enable Redis AUTH** (đã có password)
- [ ] **Enable PostgreSQL SSL** (production)
- [ ] **Rotate credentials** định kỳ (90 ngày)
- [ ] **Enable audit logging** (Keycloak events)

### Network Security

```yaml
# docker-compose.microservices.yml
networks:
  crm-network:
    driver: bridge
    internal: true  # ⚠️ Add this for production
```

---

## 🔧 Troubleshooting

### Redis Cluster không tạo được

```bash
# Xóa volumes cũ
docker-compose -f docker-compose.microservices.yml down -v

# Restart
docker-compose -f docker-compose.microservices.yml up -d redis-cluster-init

# Kiểm tra logs
docker logs crm-redis-cluster-init
```

### PostgreSQL Replica không sync

```bash
# Kiểm tra replication slot trên master
docker exec -it crm-postgres-master psql -U crm_user -d neobrutalism_crm
SELECT * FROM pg_replication_slots;

# Xem WAL sender
SELECT * FROM pg_stat_replication;

# Restart replica
docker-compose -f docker-compose.microservices.yml restart postgres-replica
```

### Keycloak không start

```bash
# Kiểm tra database connection
docker logs crm-keycloak

# Thường do keycloak-db chưa ready
docker-compose -f docker-compose.microservices.yml restart keycloak
```

### Gateway không route đến IAM Service

```bash
# Kiểm tra Consul service discovery (nếu dùng)
curl http://localhost:8500/v1/catalog/services

# Kiểm tra Gateway logs
docker logs crm-gateway

# Verify health check
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

---

## 📈 Performance Tuning

### Database Connection Pool

**HikariCP Settings** (production):

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 100      # Tăng từ 30
      minimum-idle: 20             # Tăng từ 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

### Redis Memory Policy

**Eviction Policy:**

```bash
# allkeys-lru: Xóa key ít dùng nhất khi hết memory
redis-cli -p 7000 -a redis_password_2024 CONFIG SET maxmemory-policy allkeys-lru

# Verify
redis-cli -p 7000 -a redis_password_2024 INFO memory
```

### JVM Heap Size

**Gateway & IAM Service:**

```bash
# Trong Dockerfile hoặc docker-compose
JAVA_OPTS: >
  -Xms512m
  -Xmx2g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+PrintGCDetails
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/tmp/heapdump.hprof
```

---

## 🧪 Load Testing

### Test với 100k CCU

**JMeter Test Plan:**

```bash
# Install JMeter
brew install jmeter  # macOS
# hoặc download từ https://jmeter.apache.org

# Run test
jmeter -n -t loadtest/100k-ccu.jmx -l results.jtl -e -o report/

# Scenarios:
# - 100,000 concurrent users
# - 10 requests/minute per user = 16,700 req/s
# - Test duration: 30 minutes
# - Ramp-up: 10 minutes
```

**Expected Performance:**

| Metric | Target | Critical |
|--------|--------|----------|
| Response Time (p95) | < 500ms | < 1000ms |
| Error Rate | < 0.1% | < 1% |
| Throughput | > 15,000 req/s | > 10,000 req/s |
| CPU Usage | < 70% | < 90% |
| Memory Usage | < 80% | < 95% |

---

## 🔄 Backup & Restore

### PostgreSQL Backup

```bash
# Full backup
docker exec crm-postgres-master pg_dump -U crm_user -Fc neobrutalism_crm > backup_$(date +%Y%m%d).dump

# Restore
docker exec -i crm-postgres-master pg_restore -U crm_user -d neobrutalism_crm < backup_20241225.dump
```

### Redis Backup

```bash
# RDB snapshot (tự động mỗi 15 phút nếu có thay đổi)
docker exec crm-redis-master-1 redis-cli -a redis_password_2024 BGSAVE

# Copy snapshot
docker cp crm-redis-master-1:/data/dump.rdb ./redis-backup.rdb
```

### Keycloak Backup

```bash
# Export realm
docker exec crm-keycloak /opt/keycloak/bin/kc.sh export --dir /tmp --realm neobrutalism-crm

# Copy exported file
docker cp crm-keycloak:/tmp/neobrutalism-crm-realm.json ./keycloak-backup.json
```

---

## 📚 Tài Liệu Tham Khảo

- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Redis Cluster Tutorial](https://redis.io/docs/management/scaling/)
- [PostgreSQL Replication](https://www.postgresql.org/docs/current/warm-standby.html)
- [FAPI Security Profile](https://openid.net/specs/openid-financial-api-part-2-1_0.html)

---

## 🆘 Support

Nếu gặp vấn đề:
1. Kiểm tra logs: `docker-compose -f docker-compose.microservices.yml logs <service>`
2. Verify health: `docker-compose -f docker-compose.microservices.yml ps`
3. Restart service: `docker-compose -f docker-compose.microservices.yml restart <service>`
4. Clean restart: `docker-compose -f docker-compose.microservices.yml down -v && docker-compose -f docker-compose.microservices.yml up -d`

**Port Conflicts:**
```bash
# Kiểm tra port đang dùng
netstat -an | grep LISTEN | grep <port>

# Kill process
kill -9 $(lsof -t -i:<port>)
```

---

**Happy Deploying! 🚀**
