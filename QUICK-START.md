# 🚀 Quick Start Guide

## Cách Nhanh Nhất Để Chạy Hệ Thống

### Prerequisites

1. **Java 21** - `java -version`
2. **Maven** - `mvn -version`
3. **Consul** - `consul version` (hoặc Docker)

### Option 1: Sử Dụng Scripts (Recommended)

#### Windows:
```batch
# Bắt đầu tất cả services
scripts\start-all.bat

# Kiểm tra health
scripts\check-services.bat

# Dừng tất cả services
scripts\stop-all.bat
```

#### macOS/Linux:
```bash
# Cấp quyền thực thi
chmod +x scripts/*.sh

# Bắt đầu tất cả services
./scripts/start-all.sh

# Kiểm tra health
./scripts/check-services.sh

# Dừng tất cả services
./scripts/stop-all.sh
```

### Option 2: Manual Start

#### 1. Start Consul

**Docker (Easiest):**
```bash
docker run -d --name=consul -p 8500:8500 -p 8600:8600/udp consul agent -dev -ui -client=0.0.0.0
```

**Native:**
```bash
consul agent -dev -ui
```

#### 2. Start Business Service

```bash
cd business-service
./mvnw spring-boot:run
```

#### 3. Start Gateway

```bash
cd gateway-service
./mvnw spring-boot:run
```

---

## 🌐 Truy Cập Services

| Service | URL | Description |
|---------|-----|-------------|
| **Gateway** | http://localhost:8080 | API Gateway - Main entry point |
| **Business API** | http://localhost:8081 | Business Service (direct) |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | API Documentation |
| **Consul UI** | http://localhost:8500/ui | Service Discovery Dashboard |

---

## ✅ Verify Setup

### 1. Check All Services Healthy

```bash
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Business
curl http://localhost:8500/v1/status/leader # Consul
```

### 2. Check Service Discovery

```bash
# List registered services
curl http://localhost:8500/v1/catalog/services

# Expected output:
{
  "consul": [],
  "gateway-service": ["gateway", "api"],
  "business-service": ["business", "api"]
}
```

### 3. Test Gateway Routing

```bash
# Request through Gateway (routes to Business Service)
curl http://localhost:8080/api/health

# Expected: 200 OK from Business Service
```

---

## 🧪 Test với Token Blacklist (NEW!)

### 1. Login và lấy token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

### 2. Sử dụng token để truy cập API

```bash
TOKEN="eyJhbGc..."

curl http://localhost:8080/api/customers \
  -H "Authorization: Bearer $TOKEN"

# Expected: 200 OK with customer list
```

### 3. Logout (blacklist token)

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"

# Token is now blacklisted
```

### 4. Thử sử dụng token đã blacklist

```bash
curl http://localhost:8080/api/customers \
  -H "Authorization: Bearer $TOKEN"

# Expected: 401 Unauthorized
# Message: "Token has been revoked"
```

**Lưu ý:** Token blacklist sử dụng L1 cache (Caffeine) với latency ~0.001ms! ⚡

---

## 📊 Monitoring Casbin Policies (NEW!)

### 1. Xem policy statistics

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8081/api/casbin/monitoring/stats

# Response:
{
  "total_policy_count": 8542,
  "alert_level": "NORMAL",
  "tenant_count": 12,
  "thresholds": {
    "warning": 10000,
    "critical": 50000
  }
}
```

### 2. Xem role hierarchy

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8081/api/casbin/hierarchy

# Response:
{
  "enabled": true,
  "total_roles_with_inheritance": 5,
  "hierarchy": {
    "ADMIN": ["SUPER_ADMIN"],
    "MANAGER": ["ADMIN"],
    "USER": ["MANAGER"]
  }
}
```

### 3. Thêm role inheritance

```bash
curl -X POST "http://localhost:8081/api/casbin/hierarchy/inherit?role=MANAGER&parentRole=ADMIN&domain=default" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Response:
{
  "message": "Role inheritance added: MANAGER now inherits from ADMIN"
}
```

### 4. Xem cache statistics

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8081/api/casbin/cache/stats

# Response:
{
  "l1_hit_rate_percent": "94.23",
  "l1_hits": 9423,
  "l1_misses": 577,
  "cache_size": 3421
}
```

---

## 📈 Performance Monitoring

### Prometheus Metrics

```bash
# Gateway metrics
curl http://localhost:8080/actuator/prometheus

# Business metrics
curl http://localhost:8081/actuator/prometheus
```

### Gateway Routes

```bash
# List all routes
curl http://localhost:8080/actuator/gateway/routes

# Refresh routes (after config change)
curl -X POST http://localhost:8080/actuator/gateway/refresh
```

---

## 🔧 Common Issues

### Issue: Consul not found

**Solution:**
```bash
# Docker
docker run -d --name=consul -p 8500:8500 consul agent -dev -ui -client=0.0.0.0

# Or install natively
# macOS: brew install consul
# Windows: choco install consul
```

### Issue: Service not registering

**Check logs:**
```bash
# Windows
type logs\business-service.log
type logs\gateway-service.log

# Linux/macOS
tail -f logs/business-service.log
tail -f logs/gateway-service.log
```

**Verify Consul:**
```bash
curl http://localhost:8500/v1/catalog/services
```

### Issue: Port already in use

**Find and kill process:**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /F /PID <PID>

# Linux/macOS
lsof -ti:8080 | xargs kill -9
```

---

## 📚 Next Steps

1. **Explore APIs:** http://localhost:8081/swagger-ui.html
2. **Read Full Documentation:**
   - [Service Discovery Setup](docs/SERVICE-DISCOVERY-SETUP.md)
   - [100K CCU Optimizations](docs/100K-CCU-OPTIMIZATIONS.md)
3. **Configure for Production:** See [DEPLOYMENT.md](docs/DEPLOYMENT.md)

---

## 🎯 Architecture Summary

```
User Request
    ↓
Gateway (:8080)
    ├─ JWT Validation
    ├─ Token Blacklist Check (L1 + L2 cache) ⭐ NEW
    ├─ Rate Limiting
    └─ Route to Business Service
         ↓
    Consul (:8500)
         ├─ Service Discovery
         └─ Health Checks
         ↓
Business Service (:8081)
    ├─ Casbin Authorization (L1 cache) ⭐ NEW
    │   └─ Policy Monitoring ⭐ NEW
    │   └─ Role Hierarchy ⭐ NEW
    ├─ Business Logic
    └─ Database (PostgreSQL/H2)
```

**Key Features:**
- ✅ **100K CCU Ready** - Virtual Threads, L1/L2 caching
- ✅ **Token Blacklist** - L1 cache hit ~0.001ms
- ✅ **Casbin Monitoring** - Real-time alerts on policy explosion
- ✅ **Role Hierarchy** - 30-70% policy reduction
- ✅ **Service Discovery** - Automatic failover & load balancing
- ✅ **Circuit Breaker** - Resilience4j protection
- ✅ **Rate Limiting** - Per-user, per-IP limits

---

**🎉 Happy Coding!**

Need help? Check [docs/](docs/) or open an issue.
