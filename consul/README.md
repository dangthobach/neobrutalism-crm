# Consul Service Mesh - Quick Reference

## 🚀 Quick Start

```bash
# 1. Build services
mvn clean package -DskipTests

# 2. Start service mesh
docker-compose -f docker-compose.service-mesh.yml up -d

# 3. Register services
cd consul/scripts
./register-services.sh

# 4. Check health
./health-check.sh
```

## 📊 UIs & Dashboards

| Service | URL | Credentials |
|---------|-----|-------------|
| Consul UI | http://localhost:8500 | - |
| Grafana | http://localhost:3000 | admin / admin123 |
| Jaeger | http://localhost:16686 | - |
| Prometheus | http://localhost:9090 | - |

## ✅ Features Checklist

- ✅ Service Discovery - Tự động đăng ký services
- ✅ Load Balancing - Least request policy
- ✅ Retry / Timeout - 3 retries, configurable timeouts
- ✅ Circuit Breaker - 5 consecutive errors → 30s ejection
- ✅ mTLS - Automatic encryption between services
- ✅ Traffic Shaping - Canary deployments (90/10 split)
- ✅ Observability - Prometheus + Grafana + Jaeger
- ✅ Policy Enforcement - Service intentions
- ✅ Canary / Blue-Green - Progressive deployments

## 📁 Directory Structure

```
consul/
├── config/                    # Service configurations
│   ├── *.json                # Service registrations
│   ├── intentions/           # Security policies
│   ├── service-defaults/     # Service-specific settings
│   ├── traffic-management/   # Routing, splitting, resolving
│   └── resilience/           # Circuit breaker, timeouts
├── observability/            # Monitoring configs
│   ├── prometheus/
│   ├── grafana/
│   └── jaeger/
└── scripts/                  # Helper scripts
    ├── register-services.sh  # Register all services
    ├── health-check.sh       # Health checks
    └── canary-deployment.sh  # Canary manager
```

## 🔧 Common Commands

### Service Management

```bash
# View all services
curl http://localhost:8500/v1/catalog/services

# Check service health
curl http://localhost:8500/v1/health/service/business-service

# Re-register service
curl -X PUT http://localhost:8500/v1/agent/service/register \
  --data @config/business-service.json
```

### Traffic Management

```bash
# Canary deployment (10% traffic)
./scripts/canary-deployment.sh business-service 10

# Increase to 50%
./scripts/canary-deployment.sh business-service 50

# Full rollout
./scripts/canary-deployment.sh business-service 100
```

### Monitoring

```bash
# Prometheus query
curl 'http://localhost:9090/api/v1/query?query=up'

# Envoy stats
curl http://localhost:19000/stats

# Service metrics
curl http://localhost:8080/actuator/prometheus
```

## 🐛 Troubleshooting

### Service not registering

```bash
# Check Consul connection
docker-compose logs gateway-service | grep -i consul

# Verify service config
curl http://localhost:8500/v1/agent/services
```

### Circuit breaker always open

```bash
# Check upstream health
curl http://localhost:8500/v1/health/service/business-service

# View Envoy outlier detection
curl http://localhost:19000/stats | grep outlier
```

### High latency

```bash
# View traces
# → http://localhost:16686

# Check connection pools
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

## 📚 Full Documentation

See [CONSUL_SERVICE_MESH_GUIDE.md](../CONSUL_SERVICE_MESH_GUIDE.md) for complete documentation.

## 🔐 Security

- **mTLS**: Automatic encryption between services
- **Intentions**: Allow/deny service communication
- **Zero-trust**: Deny by default, explicit allow

## 🎯 Service Mesh Architecture

```
Client → Gateway (Envoy) → Business Service (Envoy) → Database
              ↓                      ↓
         IAM Service (Envoy)    Consul Server
                                (Service Discovery)
              ↓
         Observability Stack
         (Prometheus/Grafana/Jaeger)
```

## 📊 Metrics & Alerts

| Alert | Threshold | Severity |
|-------|-----------|----------|
| Service Down | > 2min | Critical |
| High Error Rate | > 5% | Warning |
| High Latency | P95 > 1s | Warning |
| Circuit Breaker Open | > 0 | Critical |

## 🚦 Traffic Splitting

Current configuration: **90% stable / 10% canary**

Modify: Edit `config/traffic-management/business-service-splitter.json`

## 📞 Support

For issues or questions, refer to:
- [Consul Documentation](https://www.consul.io/docs)
- [Envoy Documentation](https://www.envoyproxy.io/docs)
- Main guide: [CONSUL_SERVICE_MESH_GUIDE.md](../CONSUL_SERVICE_MESH_GUIDE.md)
