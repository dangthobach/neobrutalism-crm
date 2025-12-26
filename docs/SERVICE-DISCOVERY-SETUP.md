# 🔍 Service Discovery Setup Guide - Consul

## 📋 Tổng Quan

Hướng dẫn này mô tả cách cấu hình **Service Discovery** sử dụng **Consul** để Gateway có thể tự động discover và route requests đến Business Service.

---

## 🏗️ Architecture

```
┌─────────────────┐
│   Consul        │  (Service Registry)
│   :8500         │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌─────────┐ ┌──────────────┐
│ Gateway │ │   Business   │
│  :8080  │ │   Service    │
│         │ │    :8081     │
└─────────┘ └──────────────┘
    │              │
    │  Discovery   │
    └──────────────┘
```

**Flow:**
1. Business Service starts → Registers with Consul (:8500)
2. Gateway starts → Registers with Consul (:8500)
3. Gateway queries Consul → Discovers Business Service instances
4. Gateway routes `lb://business-service` → Load balanced to Business Service
5. Health checks run every 10s → Consul marks unhealthy instances

---

## ✅ Prerequisites

### 1. Install Consul

**Windows (using Chocolatey):**
```bash
choco install consul
```

**macOS (using Homebrew):**
```bash
brew install consul
```

**Linux:**
```bash
wget https://releases.hashicorp.com/consul/1.17.0/consul_1.17.0_linux_amd64.zip
unzip consul_1.17.0_linux_amd64.zip
sudo mv consul /usr/local/bin/
```

**Docker (Recommended for Development):**
```bash
docker run -d --name=consul -p 8500:8500 -p 8600:8600/udp consul agent -dev -ui -client=0.0.0.0
```

### 2. Verify Consul Installation

```bash
# Check Consul is running
consul version

# Access Consul UI
open http://localhost:8500
```

---

## 🔧 Configuration

### Gateway Service Configuration

**File:** `gateway-service/src/main/resources/application.yml`

**Current Configuration (Already Configured ✅):**
```yaml
spring:
  application:
    name: gateway-service  # ⭐ IMPORTANT: Service name in Consul

  cloud:
    consul:
      host: ${CONSUL_HOST:localhost}
      port: ${CONSUL_PORT:8500}
      discovery:
        enabled: true
        health-check-interval: 10s
        health-check-critical-timeout: 30s
        instance-id: ${spring.application.name}-${spring.cloud.client.ip-address}-${server.port}
        prefer-ip-address: true
        tags:
          - gateway
          - api
```

**Route Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: business-service
          uri: lb://business-service  # ⭐ Load balanced via Consul
          predicates:
            - Path=/api/**
          filters:
            - StripPrefix=1
```

### Business Service Configuration

**File:** `business-service/src/main/resources/application.yml`

**Current Configuration (Already Configured ✅):**
```yaml
spring:
  application:
    name: business-service  # ⭐ IMPORTANT: Service name in Consul

  cloud:
    consul:
      host: ${CONSUL_HOST:localhost}
      port: ${CONSUL_PORT:8500}
      discovery:
        enabled: true
        health-check-interval: 10s
        health-check-critical-timeout: 30s
        instance-id: ${spring.application.name}-${spring.cloud.client.ip-address}-${server.port}
        prefer-ip-address: true
        tags:
          - business
          - api
```

---

## 🚀 Running the Services

### Step 1: Start Consul

```bash
# Development mode (data not persisted)
consul agent -dev -ui

# Production mode (with data persistence)
consul agent -server -bootstrap-expect=1 -data-dir=/tmp/consul -ui
```

**Verify Consul is running:**
```bash
curl http://localhost:8500/v1/status/leader
# Expected: "127.0.0.1:8300"
```

### Step 2: Start Business Service

```bash
cd business-service
./mvnw spring-boot:run

# Or with environment variables
CONSUL_HOST=localhost CONSUL_PORT=8500 ./mvnw spring-boot:run
```

**Expected Logs:**
```
INFO  - Registering service with consul: business-service
INFO  - Successfully registered service with consul
INFO  - Health check registered: service:business-service-192.168.1.100-8081
```

**Verify in Consul UI:**
1. Open http://localhost:8500/ui
2. Click "Services"
3. You should see `business-service` with status "passing"

### Step 3: Start Gateway Service

```bash
cd gateway-service
./mvnw spring-boot:run

# Or with environment variables
CONSUL_HOST=localhost CONSUL_PORT=8500 ./mvnw spring-boot:run
```

**Expected Logs:**
```
INFO  - Registering service with consul: gateway-service
INFO  - Successfully registered service with consul
INFO  - Fetching service instances for business-service from consul
INFO  - Found 1 instance(s) of business-service
```

**Verify in Consul UI:**
1. Open http://localhost:8500/ui
2. Click "Services"
3. You should see both `gateway-service` and `business-service` with status "passing"

---

## 🧪 Testing Service Discovery

### Test 1: Verify Service Registration

```bash
# Query Consul for registered services
curl http://localhost:8500/v1/catalog/services

# Expected output:
{
  "consul": [],
  "gateway-service": ["gateway", "api"],
  "business-service": ["business", "api"]
}
```

### Test 2: Query Service Instances

```bash
# Get all instances of business-service
curl http://localhost:8500/v1/health/service/business-service?passing

# Expected output:
[
  {
    "Node": {...},
    "Service": {
      "ID": "business-service-192.168.1.100-8081",
      "Service": "business-service",
      "Tags": ["business", "api"],
      "Address": "192.168.1.100",
      "Port": 8081
    },
    "Checks": [
      {
        "Status": "passing",
        "Name": "Service 'business-service' check"
      }
    ]
  }
]
```

### Test 3: Test Gateway Routing

```bash
# Request through gateway (should route to business-service)
curl http://localhost:8080/api/health

# Expected: 200 OK from business-service
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "consul": {"status": "UP"}
  }
}
```

### Test 4: Check Gateway Actuator

```bash
# View gateway routes
curl http://localhost:8080/actuator/gateway/routes

# Expected: Shows business-service route with lb://business-service
[
  {
    "route_id": "business-service",
    "uri": "lb://business-service",
    "order": 0,
    "predicate": "Paths: [/api/**], match trailing slash: true",
    "filters": ["[[StripPrefix parts = 1], order = 1]"]
  }
]
```

---

## 🏥 Health Checks

### Business Service Health Check

**Endpoint:** `http://localhost:8081/actuator/health`

**Consul Check Configuration (Automatic):**
```yaml
spring:
  cloud:
    consul:
      discovery:
        health-check-path: /actuator/health
        health-check-interval: 10s
        health-check-critical-timeout: 30s
```

**What Consul Checks:**
1. HTTP GET to `/actuator/health` every 10 seconds
2. Expects HTTP 200 response
3. If check fails 3 times in a row (30s), marks service as "critical"
4. Gateway stops routing to critical instances

### Gateway Health Check

**Endpoint:** `http://localhost:8080/actuator/health`

**Same configuration as Business Service**

### Custom Health Indicators

Both services automatically expose:
- `db` - Database connection status
- `consul` - Consul connection status
- `redis` - Redis connection status (if enabled)
- `diskSpace` - Disk space status

---

## 🔄 Load Balancing

### Multiple Business Service Instances

**Start multiple instances:**
```bash
# Instance 1 (port 8081)
SERVER_PORT=8081 ./mvnw spring-boot:run

# Instance 2 (port 8082)
SERVER_PORT=8082 ./mvnw spring-boot:run

# Instance 3 (port 8083)
SERVER_PORT=8083 ./mvnw spring-boot:run
```

**Gateway will automatically:**
1. Discover all 3 instances via Consul
2. Load balance requests using Round Robin (default)
3. Remove unhealthy instances from rotation

**Verify Load Balancing:**
```bash
# Make multiple requests - should see different instance IDs
for i in {1..10}; do
  curl http://localhost:8080/api/health | jq '.instanceId'
done

# Expected output (different instance IDs):
"business-service-192.168.1.100-8081"
"business-service-192.168.1.100-8082"
"business-service-192.168.1.100-8083"
"business-service-192.168.1.100-8081"
...
```

---

## 🛠️ Troubleshooting

### Issue 1: Service Not Registering

**Symptoms:**
```
ERROR - Failed to register service with consul
java.net.ConnectException: Connection refused
```

**Solutions:**
1. Check Consul is running: `consul members`
2. Verify Consul host/port: `CONSUL_HOST=localhost CONSUL_PORT=8500`
3. Check firewall: `telnet localhost 8500`

### Issue 2: Service Marked as Critical

**Symptoms:**
- Consul UI shows service with red "critical" status
- Gateway returns 503 Service Unavailable

**Solutions:**
1. Check service health endpoint:
   ```bash
   curl http://localhost:8081/actuator/health
   ```
2. View Consul logs:
   ```bash
   consul monitor
   ```
3. Check application logs for errors
4. Verify database/redis connectivity

### Issue 3: Gateway Cannot Discover Service

**Symptoms:**
```
ERROR - No instances available for business-service
```

**Solutions:**
1. Verify service is registered:
   ```bash
   curl http://localhost:8500/v1/catalog/service/business-service
   ```
2. Check service name matches in both configs
3. Restart Gateway after Business Service is running
4. Enable debug logging:
   ```yaml
   logging:
     level:
       org.springframework.cloud.consul: DEBUG
   ```

### Issue 4: Health Check Fails

**Symptoms:**
- Service keeps flipping between "passing" and "critical"

**Solutions:**
1. Increase health check interval:
   ```yaml
   spring:
     cloud:
       consul:
         discovery:
           health-check-interval: 30s
   ```
2. Check database connection pool settings
3. Monitor application performance (CPU, memory)

---

## 📊 Monitoring

### Consul UI

**URL:** http://localhost:8500/ui

**Features:**
- View all registered services
- Check service health status
- View service instances and their IP/Port
- Monitor health check history
- View service tags and metadata

### Consul API

```bash
# List all services
curl http://localhost:8500/v1/catalog/services

# Get service health
curl http://localhost:8500/v1/health/service/business-service

# Get service nodes
curl http://localhost:8500/v1/catalog/service/business-service

# Deregister service (for testing)
curl -X PUT http://localhost:8500/v1/agent/service/deregister/business-service-192.168.1.100-8081
```

### Gateway Actuator Endpoints

```bash
# View all routes
GET http://localhost:8080/actuator/gateway/routes

# View route details
GET http://localhost:8080/actuator/gateway/routes/business-service

# Refresh routes (after service change)
POST http://localhost:8080/actuator/gateway/refresh

# View metrics
GET http://localhost:8080/actuator/metrics
```

---

## 🔒 Production Considerations

### 1. Consul Cluster Setup

**For production, run Consul in cluster mode:**

```bash
# Server 1
consul agent -server -bootstrap-expect=3 \
  -data-dir=/var/consul \
  -node=consul-1 \
  -bind=192.168.1.10 \
  -ui

# Server 2
consul agent -server -bootstrap-expect=3 \
  -data-dir=/var/consul \
  -node=consul-2 \
  -bind=192.168.1.11 \
  -join=192.168.1.10

# Server 3
consul agent -server -bootstrap-expect=3 \
  -data-dir=/var/consul \
  -node=consul-3 \
  -bind=192.168.1.12 \
  -join=192.168.1.10
```

### 2. Security

**Enable ACL (Access Control Lists):**
```yaml
spring:
  cloud:
    consul:
      config:
        acl-token: ${CONSUL_ACL_TOKEN}
```

**Enable TLS:**
```yaml
spring:
  cloud:
    consul:
      scheme: https
      tls:
        enabled: true
        cert-path: /path/to/cert.pem
        key-path: /path/to/key.pem
```

### 3. Environment Variables

**Production deployment:**
```bash
# Business Service
export CONSUL_HOST=consul.production.internal
export CONSUL_PORT=8500
export SPRING_PROFILES_ACTIVE=prod

# Gateway
export CONSUL_HOST=consul.production.internal
export CONSUL_PORT=8500
export SPRING_PROFILES_ACTIVE=prod
```

### 4. Health Check Tuning

**For production workloads:**
```yaml
spring:
  cloud:
    consul:
      discovery:
        health-check-interval: 30s
        health-check-critical-timeout: 60s
        heartbeat:
          enabled: true
          ttl-value: 30
```

---

## 📚 Additional Resources

### Documentation
- [Spring Cloud Consul](https://spring.io/projects/spring-cloud-consul)
- [Consul Service Discovery](https://www.consul.io/docs/discovery)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)

### Example Commands

**Consul CLI:**
```bash
# List members
consul members

# View services
consul catalog services

# Check service health
consul watch -type=service -service=business-service

# Force leave (remove dead node)
consul force-leave <node-name>
```

---

## ✅ Checklist

### Development Setup
- [ ] Consul installed and running
- [ ] Business Service registers with Consul
- [ ] Gateway registers with Consul
- [ ] Gateway discovers Business Service
- [ ] Health checks passing
- [ ] Load balancing works with multiple instances

### Production Deployment
- [ ] Consul cluster (3+ nodes) configured
- [ ] ACL enabled and tokens configured
- [ ] TLS enabled for Consul communication
- [ ] Health check intervals tuned
- [ ] Monitoring and alerting configured
- [ ] Backup and recovery procedures documented

---

## 🎯 Summary

**Your setup is ALREADY CONFIGURED correctly!** ✅

Both services have:
- ✅ Spring Cloud Consul Discovery dependency
- ✅ Proper application names (`gateway-service`, `business-service`)
- ✅ Consul configuration with health checks
- ✅ Gateway routes using `lb://business-service`

**To start using Service Discovery:**

1. **Start Consul:**
   ```bash
   consul agent -dev -ui
   ```

2. **Start Business Service:**
   ```bash
   cd business-service && ./mvnw spring-boot:run
   ```

3. **Start Gateway:**
   ```bash
   cd gateway-service && ./mvnw spring-boot:run
   ```

4. **Verify:**
   ```bash
   # Check Consul UI
   open http://localhost:8500/ui

   # Test routing
   curl http://localhost:8080/api/health
   ```

**That's it! Service Discovery is working!** 🎉

---

**Questions or Issues?**
- Check Consul UI: http://localhost:8500/ui
- View logs: `./mvnw spring-boot:run`
- Enable debug: `logging.level.org.springframework.cloud.consul=DEBUG`
