# Consul Service Mesh - Deployment Checklist

## Pre-Deployment Checklist

### System Requirements
- [ ] Docker Desktop 24.0+ installed
- [ ] Docker Compose 2.20+ installed
- [ ] Java 21 JDK installed
- [ ] Maven 3.9+ installed
- [ ] 8GB RAM available (16GB recommended)
- [ ] 20GB disk space free
- [ ] Git Bash / WSL installed (Windows)
- [ ] curl and jq installed (for scripts)

### Port Availability
- [ ] Port 8080 (Gateway Service)
- [ ] Port 8081 (Business Service)
- [ ] Port 8500 (Consul UI)
- [ ] Port 5432 (PostgreSQL)
- [ ] Port 6379 (Redis)
- [ ] Port 9000-9001 (MinIO)
- [ ] Port 8180 (Keycloak)
- [ ] Port 3000 (Grafana)
- [ ] Port 9090 (Prometheus)
- [ ] Port 16686 (Jaeger)

---

## Build Phase

### 1. Build Gateway Service
```bash
cd gateway-service
mvn clean package -DskipTests
```
- [ ] Build successful
- [ ] JAR file created in target/
- [ ] File size > 50MB

### 2. Build Business Service
```bash
cd business-service
mvn clean package -DskipTests
```
- [ ] Build successful
- [ ] JAR file created in target/
- [ ] File size > 50MB

---

## Infrastructure Deployment

### 3. Start Docker Compose
```bash
docker-compose -f docker-compose.service-mesh.yml up -d
```
- [ ] All containers started
- [ ] No error messages
- [ ] `docker-compose ps` shows all services as "Up"

### 4. Verify Consul Server
```bash
curl http://localhost:8500/v1/status/leader
```
- [ ] Returns non-empty response (e.g., "172.20.0.10:8300")
- [ ] Consul UI accessible at http://localhost:8500
- [ ] UI shows "1 Server" in datacenter

### 5. Verify Infrastructure Services

#### PostgreSQL
```bash
docker exec postgres pg_isready -U crm_user -d crm_db
```
- [ ] Returns "accepting connections"

#### Redis
```bash
docker exec redis redis-cli ping
```
- [ ] Returns "PONG"

#### MinIO
```bash
curl http://localhost:9000/minio/health/live
```
- [ ] Returns 200 OK
- [ ] Console accessible at http://localhost:9001

#### Keycloak
```bash
curl http://localhost:8180/health
```
- [ ] Returns health status
- [ ] Admin console accessible at http://localhost:8180

---

## Service Deployment

### 6. Wait for Application Services

#### Gateway Service
```bash
# Wait for health check (may take 60-90 seconds)
until curl -sf http://localhost:8080/actuator/health | grep UP; do
  echo "Waiting for Gateway..."
  sleep 5
done
```
- [ ] Health check returns "UP"
- [ ] No exceptions in logs: `docker-compose logs gateway-service`

#### Business Service
```bash
# Wait for health check
until curl -sf http://localhost:8081/actuator/health | grep UP; do
  echo "Waiting for Business Service..."
  sleep 5
done
```
- [ ] Health check returns "UP"
- [ ] Flyway migrations completed
- [ ] No exceptions in logs: `docker-compose logs business-service`

---

## Service Mesh Configuration

### 7. Register Services with Consul
```bash
cd consul/scripts
chmod +x *.sh
./register-services.sh
```
- [ ] Script completes successfully
- [ ] All services registered:
  - [ ] gateway-service
  - [ ] business-service
  - [ ] iam-service
- [ ] Service defaults configured
- [ ] Service intentions created
- [ ] Traffic management policies applied
- [ ] Resilience policies configured

### 8. Verify Service Registration
```bash
curl http://localhost:8500/v1/catalog/services | jq
```
- [ ] gateway-service listed
- [ ] business-service listed
- [ ] iam-service listed (if implemented)
- [ ] Sidecar proxies listed

### 9. Check Envoy Sidecars

#### Gateway Envoy
```bash
docker-compose ps gateway-envoy
curl http://localhost:19000/clusters
```
- [ ] Container running
- [ ] Admin interface accessible
- [ ] business-service cluster configured

#### Business Envoy
```bash
docker-compose ps business-envoy
curl http://localhost:19001/clusters
```
- [ ] Container running
- [ ] Admin interface accessible
- [ ] Upstream clusters configured

---

## Observability Setup

### 10. Verify Prometheus
```bash
curl http://localhost:9090/-/healthy
```
- [ ] Returns "Prometheus is Healthy"
- [ ] UI accessible at http://localhost:9090
- [ ] Targets visible at http://localhost:9090/targets
- [ ] All targets in "UP" state

### 11. Verify Grafana
```bash
curl http://localhost:3000/api/health
```
- [ ] Returns health status
- [ ] Login works (admin / admin123)
- [ ] Datasources configured:
  - [ ] Prometheus
  - [ ] Jaeger
  - [ ] Consul

### 12. Import Grafana Dashboards
- [ ] Service Mesh Overview (ID: 13421)
- [ ] Envoy Global (ID: 11022)
- [ ] Spring Boot (ID: 12464)
- [ ] JVM Micrometer (ID: 4701)

### 13. Verify Jaeger
```bash
curl http://localhost:16686/api/services
```
- [ ] UI accessible at http://localhost:16686
- [ ] Services listed (gateway-service, business-service)
- [ ] Can view traces

---

## Health Checks

### 14. Run Comprehensive Health Check
```bash
cd consul/scripts
./health-check.sh
```
- [ ] Consul server healthy
- [ ] All services passing health checks
- [ ] Envoy sidecars healthy
- [ ] Service intentions configured
- [ ] Observability stack accessible

### 15. Verify Service Mesh Features

#### Service Discovery
```bash
# DNS resolution
dig @localhost -p 8600 business-service.service.consul
```
- [ ] Returns service IP address

#### Load Balancing
```bash
# Check Envoy load balancing
curl http://localhost:19000/clusters | grep business-service
```
- [ ] Shows upstream endpoints
- [ ] Least request policy active

#### Circuit Breaker
```bash
# Check circuit breaker config
curl http://localhost:19000/config_dump | jq '.configs[] | select(.outlier_detection)'
```
- [ ] Outlier detection configured
- [ ] Thresholds set correctly

#### mTLS
```bash
# Check certificates
curl http://localhost:8500/v1/agent/connect/ca/roots | jq
```
- [ ] Root CA certificate present
- [ ] Valid certificate chain

---

## Functional Testing

### 16. Test API Endpoints

#### Gateway Health
```bash
curl http://localhost:8080/actuator/health
```
- [ ] Returns "UP"
- [ ] All components healthy

#### Business Service via Gateway
```bash
curl http://localhost:8080/api/users
```
- [ ] Returns response (may need auth)
- [ ] No errors in logs

### 17. Test Service Communication

#### Direct to Business Service
```bash
curl http://localhost:8081/actuator/health
```
- [ ] Returns "UP"

#### Through Service Mesh
```bash
# All requests should go through Envoy sidecars
# Check Envoy stats
curl http://localhost:19000/stats | grep -E "cluster.*business-service.*rq_total"
```
- [ ] Request count increasing
- [ ] No connection errors

---

## Performance Testing

### 18. Basic Load Test
```bash
# Simple load test (if ab installed)
ab -n 1000 -c 10 http://localhost:8080/actuator/health
```
- [ ] No errors
- [ ] Avg response time < 100ms
- [ ] All requests successful

### 19. Monitor Metrics During Load
- [ ] Check Prometheus: http://localhost:9090
  - [ ] Request rate increasing
  - [ ] Latency within acceptable range
  - [ ] No error spikes
- [ ] Check Grafana dashboards
  - [ ] Service graphs updating
  - [ ] No alerts firing

---

## Security Validation

### 20. Verify Service Intentions
```bash
curl http://localhost:8500/v1/connect/intentions | jq
```
- [ ] gateway → business intention exists
- [ ] gateway → iam intention exists
- [ ] business → iam intention exists
- [ ] All intentions have correct permissions

### 21. Test mTLS Enforcement
```bash
# Try to connect directly to Envoy sidecar (should fail without cert)
curl http://localhost:20000/
```
- [ ] Connection refused or TLS error
- [ ] mTLS enforced

---

## Canary Deployment Test

### 22. Test Traffic Splitting
```bash
cd consul/scripts
./canary-deployment.sh business-service 10
```
- [ ] Script executes successfully
- [ ] Traffic split updated (90/10)
- [ ] Can verify in Consul UI

### 23. Monitor Canary Traffic
- [ ] Grafana shows traffic split
- [ ] Both subsets receiving requests
- [ ] Error rates similar

### 24. Rollback Canary
```bash
./canary-deployment.sh business-service 0
```
- [ ] All traffic back to stable
- [ ] No errors during rollback

---

## Documentation & Training

### 25. Review Documentation
- [ ] Read [CONSUL_SERVICE_MESH_GUIDE.md](../CONSUL_SERVICE_MESH_GUIDE.md)
- [ ] Review [SERVICE_MESH_ARCHITECTURE.md](SERVICE_MESH_ARCHITECTURE.md)
- [ ] Bookmark [consul/README.md](README.md) for quick reference

### 26. Team Knowledge Transfer
- [ ] Share documentation with team
- [ ] Schedule walkthrough session
- [ ] Create runbooks for common operations
- [ ] Document troubleshooting procedures

---

## Alerting Configuration

### 27. Configure Alert Channels

#### Slack Integration (Optional)
```yaml
# Edit alertmanager config
receivers:
  - name: 'slack'
    slack_configs:
      - api_url: 'YOUR_WEBHOOK_URL'
        channel: '#alerts'
```
- [ ] Slack webhook configured
- [ ] Test alert sent successfully

#### Email Notifications (Optional)
```yaml
receivers:
  - name: 'email'
    email_configs:
      - to: 'team@example.com'
        from: 'alerts@example.com'
```
- [ ] SMTP configured
- [ ] Test email received

### 28. Test Alerts
```bash
# Trigger test alert
curl -X POST http://localhost:9093/api/v1/alerts -d '[{
  "labels": {"alertname": "TestAlert", "severity": "warning"},
  "annotations": {"summary": "Test alert"}
}]'
```
- [ ] Alert visible in Alertmanager UI
- [ ] Notification received (if configured)

---

## Backup & Recovery

### 29. Test Consul Backup
```bash
docker exec consul-server consul snapshot save /tmp/backup.snap
docker cp consul-server:/tmp/backup.snap ./consul-backup.snap
```
- [ ] Snapshot created successfully
- [ ] Backup file exists and has size > 0

### 30. Document Restore Procedure
- [ ] Restore steps documented
- [ ] Recovery time objective (RTO) defined
- [ ] Recovery point objective (RPO) defined

---

## Production Readiness

### 31. Security Hardening
- [ ] Enable Consul ACLs (for production)
- [ ] Configure TLS for Consul API
- [ ] Rotate default passwords:
  - [ ] Grafana admin
  - [ ] PostgreSQL
  - [ ] MinIO
  - [ ] Keycloak
- [ ] Configure secrets management
- [ ] Network segmentation (if needed)

### 32. Performance Tuning
- [ ] Review and adjust connection pool sizes
- [ ] Tune cache sizes based on usage
- [ ] Optimize circuit breaker thresholds
- [ ] Configure appropriate timeouts

### 33. Monitoring & Logging
- [ ] Log aggregation configured (optional)
- [ ] Log retention policy defined
- [ ] Metrics retention appropriate (30 days default)
- [ ] Critical alerts defined and tested

### 34. Disaster Recovery
- [ ] Backup strategy defined
- [ ] Restore procedure tested
- [ ] Failover plan documented
- [ ] Contact list for incidents

---

## Go-Live Checklist

### 35. Final Verification
- [ ] All services healthy
- [ ] All tests passing
- [ ] Documentation complete
- [ ] Team trained
- [ ] Alerts configured
- [ ] Backup tested
- [ ] Runbooks created

### 36. Communication
- [ ] Stakeholders informed
- [ ] Go-live schedule communicated
- [ ] Support channels established
- [ ] Rollback plan ready

### 37. Post-Deployment
- [ ] Monitor for 24 hours
- [ ] Review metrics and alerts
- [ ] Collect feedback from team
- [ ] Document lessons learned
- [ ] Schedule retrospective

---

## Maintenance Tasks

### Weekly
- [ ] Review Grafana dashboards
- [ ] Check alert history
- [ ] Review service health trends
- [ ] Check disk space usage

### Monthly
- [ ] Review and update documentation
- [ ] Analyze performance trends
- [ ] Update dependencies (if needed)
- [ ] Review security advisories

### Quarterly
- [ ] Disaster recovery drill
- [ ] Capacity planning review
- [ ] Team training refresh
- [ ] Architecture review

---

## Common Issues & Quick Fixes

### Service won't register
```bash
# Check Consul connection
docker exec gateway-service curl http://consul-server:8500/v1/status/leader

# Re-register manually
./consul/scripts/register-services.sh
```

### Envoy sidecar failing
```bash
# Check logs
docker-compose logs gateway-envoy

# Restart sidecar
docker-compose restart gateway-envoy
```

### High latency
```bash
# Check traces
# → http://localhost:16686

# Check connection pools
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Circuit breaker open
```bash
# Check upstream health
curl http://localhost:8500/v1/health/service/business-service

# View Envoy stats
curl http://localhost:19000/stats | grep outlier
```

---

## Success Criteria

✅ **Service Mesh Operational:**
- All services registered and healthy
- mTLS enabled and enforced
- Service discovery working
- Load balancing active
- Circuit breakers configured

✅ **Observability:**
- Metrics collected in Prometheus
- Dashboards visible in Grafana
- Traces captured in Jaeger
- Alerts configured and tested

✅ **Security:**
- Service intentions configured
- mTLS certificates valid
- Zero-trust networking active

✅ **Operations:**
- Documentation complete
- Team trained
- Runbooks created
- Backup/restore tested

---

**Date:** _______________
**Deployed by:** _______________
**Verified by:** _______________
**Approved by:** _______________

**Notes:**
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________
