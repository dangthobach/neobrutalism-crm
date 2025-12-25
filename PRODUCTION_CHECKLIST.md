# ✅ Production Deployment Checklist

Checklist đầy đủ trước khi deploy Neobrutalism CRM lên Production environment.

---

## 🔒 SECURITY (Critical)

### Authentication & Authorization
- [x] ✅ **CORS Origins Restricted** - Không dùng wildcard `*`
  - File: [gateway-service/src/main/resources/application.yml](gateway-service/src/main/resources/application.yml#L16)
  - File: [business-service/src/main/java/com/neobrutalism/crm/config/SecurityConfig.java](business-service/src/main/java/com/neobrutalism/crm/config/SecurityConfig.java#L144)
  - Config: `CORS_ALLOWED_ORIGINS` env variable

- [ ] **Change All Default Passwords**
  - [ ] PostgreSQL: `DB_PASSWORD`
  - [ ] Redis: `REDIS_PASSWORD`
  - [ ] Keycloak: `KEYCLOAK_ADMIN_PASSWORD`
  - [ ] Keycloak Client Secrets: `KEYCLOAK_CLIENT_SECRET`
  - [ ] JWT Secret: `JWT_SECRET` (min 256 bits)

- [ ] **Update Keycloak Realm**
  - [ ] Change admin credentials in [docker/keycloak/realms/crm-realm.json](docker/keycloak/realms/crm-realm.json)
  - [ ] Update `redirect_uris` với production URLs
  - [ ] Update `web_origins` với production frontend URLs

- [ ] **Enable HTTPS**
  - [ ] SSL/TLS certificates installed
  - [ ] Redirect HTTP → HTTPS
  - [ ] HSTS header enabled (already in SecurityConfig)

---

## 💾 DATA PERSISTENCE

### Redis Persistence
- [x] ✅ **AOF + RDB Enabled**
  - Master-1, Master-2: Full persistence configured
  - TODO: Update Master-3, Slave-1, Slave-2, Slave-3 (see [docker/fix-redis-persistence.sh](docker/fix-redis-persistence.sh))

### PostgreSQL Replication
- [x] ✅ **Master-Slave Replication**
  - Master (Write): Port 5432
  - Replica (Read): Port 5433
  - Init script: [docker/init-replication.sql](docker/init-replication.sql)

### Backup Jobs
- [x] ✅ **Backup Scripts Created**
  - PostgreSQL: [docker/backup/backup-postgres.sh](docker/backup/backup-postgres.sh)
  - Redis Cluster: [docker/backup/backup-redis.sh](docker/backup/backup-redis.sh)
  - Keycloak: [docker/backup/backup-keycloak.sh](docker/backup/backup-keycloak.sh)
  - All: [docker/backup/backup-all.sh](docker/backup/backup-all.sh)

- [ ] **Setup Cron Jobs**
  ```bash
  crontab -e
  # Add lines from docker/backup/crontab.example
  ```

- [ ] **Test Backup & Restore**
  ```bash
  # Test PostgreSQL backup
  ./docker/backup/backup-postgres.sh

  # Test restore
  pg_restore -h localhost -U crm_user -d test_db /var/backups/postgres/latest.dump
  ```

- [ ] **S3 Backup (Optional)**
  - [ ] Configure AWS credentials
  - [ ] Enable S3 sync in backup scripts
  - [ ] Test S3 upload

---

## 📊 MONITORING & ALERTS

### Prometheus & Alertmanager
- [x] ✅ **Prometheus Configured**
  - Config: [docker/prometheus.yml](docker/prometheus.yml)
  - Alert rules: [docker/prometheus/alerts.yml](docker/prometheus/alerts.yml)
  - Web UI: http://localhost:9090

- [x] ✅ **Alertmanager Added**
  - Config: [docker/alertmanager/alertmanager.yml](docker/alertmanager/alertmanager.yml)
  - Web UI: http://localhost:9093

- [ ] **Configure Alert Recipients**
  - [ ] Update `ALERT_EMAIL` in .env
  - [ ] Configure SMTP settings (Gmail/SendGrid/AWS SES)
  - [ ] Test email alerts
  - [ ] (Optional) Setup Slack webhook
  - [ ] (Optional) Setup PagerDuty for critical alerts

- [ ] **Test Alerts**
  ```bash
  # Trigger test alert
  docker stop crm-gateway

  # Wait 2 minutes, check Alertmanager
  curl http://localhost:9093/api/v2/alerts

  # Restart service
  docker start crm-gateway
  ```

### Grafana Dashboards
- [ ] **Import Dashboards**
  - [ ] JVM Dashboard
  - [ ] HTTP Metrics Dashboard
  - [ ] Redis Cluster Dashboard
  - [ ] PostgreSQL Dashboard
  - Access: http://localhost:3001 (admin/admin)

---

## 🚀 PERFORMANCE TUNING

### Database
- [ ] **Connection Pool**
  - [ ] Review HikariCP settings (max 100 per instance)
  - [ ] Monitor connection usage via Prometheus

- [ ] **Query Optimization**
  - [ ] Run EXPLAIN ANALYZE on slow queries
  - [ ] Add missing indexes
  - [ ] Optimize getUserRoles query (see roadmap)

### Redis
- [ ] **Memory Settings**
  - [ ] Verify maxmemory: 512MB per node
  - [ ] Monitor memory usage
  - [ ] Adjust eviction policy if needed

### JVM
- [ ] **Heap Size**
  ```yaml
  environment:
    JAVA_OPTS: >
      -Xms512m
      -Xmx2g
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
  ```

---

## 🧪 LOAD TESTING

- [ ] **JMeter Test Plan**
  - [ ] Install JMeter
  - [ ] Create test scenarios
  - [ ] Run 100k CCU simulation

- [ ] **Performance Metrics**
  | Metric | Target | Status |
  |--------|--------|--------|
  | Response Time (p95) | < 500ms | ⏸️ Pending |
  | Error Rate | < 0.1% | ⏸️ Pending |
  | Throughput | > 15k req/s | ⏸️ Pending |
  | CPU Usage | < 70% | ⏸️ Pending |
  | Memory Usage | < 80% | ⏸️ Pending |

---

## 🌐 INFRASTRUCTURE

### Docker Compose
- [ ] **Update Environment Variables**
  ```bash
  cp .env.example .env
  nano .env  # Update all CHANGE_ME values
  ```

- [ ] **Start Stack**
  ```bash
  docker-compose -f docker-compose.microservices.yml up -d
  ```

- [ ] **Verify Health Checks**
  ```bash
  docker-compose -f docker-compose.microservices.yml ps
  ```

### Service Discovery
- [ ] **Consul (if using)**
  - [ ] Verify all services registered
  - Access: http://localhost:8500

### Networking
- [ ] **Firewall Rules**
  - [ ] Allow: 8080 (Gateway), 8180 (Keycloak)
  - [ ] Block: 5432, 6379, 7000-7005 (internal only)

---

## 📋 POST-DEPLOYMENT

### Smoke Tests
- [ ] **Gateway Health**
  ```bash
  curl http://localhost:8080/actuator/health
  ```

- [ ] **IAM Service Health**
  ```bash
  curl http://localhost:8081/actuator/health
  ```

- [ ] **Keycloak**
  ```bash
  curl http://localhost:8180/realms/neobrutalism-crm/.well-known/openid-configuration
  ```

- [ ] **Redis Cluster**
  ```bash
  docker exec -it crm-redis-master-1 redis-cli -c -p 7000 -a redis_password_2024 CLUSTER INFO
  ```

- [ ] **PostgreSQL Replication**
  ```bash
  docker exec -it crm-postgres-master psql -U crm_user -d neobrutalism_crm -c "SELECT * FROM pg_stat_replication;"
  ```

### Monitoring
- [ ] **Verify Metrics Collection**
  - Prometheus: http://localhost:9090/targets
  - Check all targets are UP

- [ ] **Check Logs**
  ```bash
  docker-compose -f docker-compose.microservices.yml logs -f --tail=100
  ```

---

## 📝 DOCUMENTATION

- [ ] **Update README**
  - [ ] Production deployment instructions
  - [ ] Environment variables list
  - [ ] Troubleshooting guide

- [ ] **Runbook**
  - [ ] Incident response procedures
  - [ ] Rollback strategy
  - [ ] Contact information

---

## 🔄 ROLLBACK PLAN

### Quick Rollback
```bash
# 1. Stop new version
docker-compose -f docker-compose.microservices.yml down

# 2. Restore database (if needed)
pg_restore -h localhost -U crm_user -d neobrutalism_crm /var/backups/postgres/backup_before_deploy.dump

# 3. Start previous version
git checkout <previous-commit>
docker-compose -f docker-compose.microservices.yml up -d
```

### Data Rollback
- [ ] Database backup taken before deployment
- [ ] Redis snapshot taken before deployment
- [ ] Keycloak realm exported before deployment

---

## ✅ FINAL CHECKLIST

Before going live:

- [ ] All passwords changed
- [ ] HTTPS enabled
- [ ] Backups tested
- [ ] Alerts configured and tested
- [ ] Load testing completed
- [ ] Smoke tests passed
- [ ] Monitoring dashboards ready
- [ ] Team trained on runbook
- [ ] Rollback plan tested

---

**Approval Signatures:**

- DevOps Lead: _________________ Date: _______
- Security Officer: _________________ Date: _______
- Product Owner: _________________ Date: _______
