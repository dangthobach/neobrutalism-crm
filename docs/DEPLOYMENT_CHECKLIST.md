# Migration System - Deployment Checklist

## ✅ Pre-Deployment Verification

### 1. Build Success
```bash
cd d:\project\neobrutalism-crm
mvn clean package -DskipTests
```
**Status:** ✅ BUILD SUCCESS
- Warnings: 19 (Lombok @Builder defaults - non-critical)
- Errors: 0
- JAR created: `target/crm-backend-1.0.0-SNAPSHOT.jar`

---

### 2. Code Changes Summary

#### Files Created (13 files)
1. ✅ `config/AsyncConfig.java` - Thread pool configuration
2. ✅ `config/MigrationCacheConfig.java` - Redis cache config
3. ✅ `application/migration/config/MigrationWebSocketConfig.java` - WebSocket
4. ✅ `application/migration/service/MigrationProgressBroadcaster.java` - Progress broadcaster
5. ✅ `docs/MIGRATION_OPTIMIZATION_SUMMARY.md` - Main documentation
6. ✅ `docs/METADATA_PARSER_OPTIMIZATION.md` - Metadata parser details
7. ✅ `docs/DEPLOYMENT_CHECKLIST.md` - This file
8. ✅ `resources/db/migration/V202__Add_migration_performance_indexes.sql` - Database indexes

#### Files Modified (11 files)
1. ✅ `ExcelMigrationService.java` - Streaming hash, Semaphore, Pageable
2. ✅ `ExcelMetadataParser.java` - Streaming SAX parser
3. ✅ `ExcelDimensionValidator.java` - Public method
4. ✅ `MigrationController.java` - Async upload, WebSocket deprecation
5. ✅ `MigrationProgressService.java` - Redis caching
6. ✅ `RecoveryService.java` - Fixed method call
7. ✅ `StagingHSBGHopDongRepository.java` - Added Pageable
8. ✅ `StagingHSBGCifRepository.java` - Added Pageable
9. ✅ `StagingHSBGTapRepository.java` - Added Pageable
10. ✅ `application.yml` - JDBC batch config
11. ✅ `CrmApplication.java` - No changes needed (already has @EnableScheduling)

---

## 🚀 Deployment Steps

### Step 1: Database Migration
```bash
# Run Flyway migration to create indexes
./mvnw flyway:migrate

# Verify indexes created
psql -U postgres -d crm_db -c "\d+ staging_hsbg_hop_dong"
```

**Expected indexes:**
- `idx_staging_hopdong_insert`
- `idx_staging_hopdong_validation`
- `idx_staging_hopdong_duplicate`
- `idx_migration_sheet_job_status`
- `idx_migration_job_hash`
- And 10 more...

---

### Step 2: Redis Configuration
```yaml
# Verify Redis is running
docker ps | grep redis

# If not running, start Redis
docker run -d --name redis -p 6379:6379 redis:latest

# Test connection
redis-cli ping
# Expected: PONG
```

---

### Step 3: Application Configuration

**Verify `application.yml`:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 1000           # ✅ ADDED
          fetch_size: 1000           # ✅ ADDED
        order_inserts: true          # ✅ ADDED
        order_updates: true          # ✅ ADDED

  cache:
    type: redis                      # ✅ EXISTS
    cache-names:
      - migration-progress           # ✅ NOT NEEDED (handled by MigrationCacheConfig)
```

---

### Step 4: Build and Deploy
```bash
# Clean build
mvn clean package -DskipTests

# Run application
java -jar target/crm-backend-1.0.0-SNAPSHOT.jar

# Or with Spring Boot plugin
./mvnw spring-boot:run
```

---

### Step 5: Verify Services Running

**Check logs for:**
```
✅ Initialized excelMigrationExecutor with core=2, max=3, queue=10
✅ Initialized fileUploadExecutor with core=4, max=8, queue=20
✅ WebSocket message broker configured for migration progress
✅ WebSocket endpoint registered: /ws/migration
✅ Configured migration progress cache with 5s TTL
```

**Test endpoints:**
```bash
# 1. Health check
curl http://localhost:8080/actuator/health

# 2. WebSocket endpoint (browser console)
const socket = new SockJS('http://localhost:8080/ws/migration');
console.log(socket.readyState); // Should be 1 (OPEN)
```

---

## 🧪 Testing

### Test 1: Small File Upload (100 rows)
```bash
curl -X POST http://localhost:8080/api/migration/upload \
  -F "file=@test_100_rows.xlsx" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Expected response:
# HTTP 202 Accepted
# { "id": "uuid", "status": "PENDING", ... }
```

### Test 2: Progress WebSocket
```javascript
const socket = new SockJS('http://localhost:8080/ws/migration');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    stompClient.subscribe('/topic/migration/JOB_ID', (message) => {
        const progress = JSON.parse(message.body);
        console.log('Progress:', progress.overallProgress + '%');
    });
});
```

### Test 3: Large File (200k rows)
```bash
# Upload large file
curl -X POST http://localhost:8080/api/migration/upload \
  -F "file=@large_200k_rows.xlsx" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Monitor memory usage
jconsole # Connect to process
# Heap should stay < 2GB
```

---

## 📊 Monitoring

### Metrics to Watch

**1. Memory Usage**
```bash
# JVM heap usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Expected: < 2GB during 5M record migration
```

**2. Thread Pools**
```bash
# Excel migration executor
curl http://localhost:8080/actuator/metrics/executor.active?tag=name:excelMigrationExecutor

# Expected: active <= 3, queued <= 10
```

**3. Cache Hit Rate**
```bash
# Redis cache statistics
curl http://localhost:8080/actuator/metrics/cache.gets?tag=cache:migration-progress

# Expected hit rate: > 80%
```

**4. Database Queries**
```sql
-- Check slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE query LIKE '%staging_hsbg%'
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Expected: < 50ms for findValidRecordsForInsert
```

---

## 🚨 Rollback Plan

### If Issues Occur

**1. Revert to Previous Version**
```bash
# Stop application
kill -9 PID

# Deploy previous JAR
java -jar target/crm-backend-PREVIOUS.jar
```

**2. Revert Database Migration**
```bash
# Rollback Flyway migration
./mvnw flyway:undo

# Or manually drop indexes
psql -U postgres -d crm_db
DROP INDEX IF EXISTS idx_staging_hopdong_insert;
-- ... drop all V202 indexes
```

**3. Disable New Features**
```yaml
# Temporarily disable WebSocket
# Comment out in application.yml or add:
spring:
  websocket:
    enabled: false

# Use old SSE endpoint instead
GET /api/migration/jobs/{jobId}/progress/stream
```

---

## ✅ Post-Deployment Verification

### Success Criteria

- [ ] Application starts without errors
- [ ] Redis connection successful (check logs)
- [ ] WebSocket endpoint accessible
- [ ] Upload endpoint returns 202 Accepted
- [ ] Progress updates via WebSocket
- [ ] Database indexes created (15+ indexes)
- [ ] Memory usage < 2GB for 5M records
- [ ] Processing throughput > 30k records/sec
- [ ] No OOM errors in logs

### Performance Benchmarks

Test with 5M records (3 sheets × 200k):

| Metric | Target | Actual |
|--------|--------|--------|
| Upload time | < 10s | ___ |
| Total processing time | < 5 min | ___ |
| Memory peak | < 2GB | ___ |
| DB queries/sec | < 50 | ___ |
| Cache hit rate | > 80% | ___ |
| Throughput | > 30k rec/s | ___ |

---

## 📞 Support

### Common Issues

**Issue: `Cannot find symbol: resumeSheetProcessing`**
- Fixed in RecoveryService.java line 87
- Calls `processSheet()` instead

**Issue: WebSocket connection refused**
- Check if port 8080 is open
- Verify CORS settings in MigrationWebSocketConfig
- Check firewall rules

**Issue: High memory usage**
- Verify Semaphore is working (check logs for "Acquired semaphore permit")
- Check thread pool limits (max 3 concurrent sheets)
- Monitor with JConsole

**Issue: Slow progress queries**
- Verify Redis is running and connected
- Check cache hit rate > 80%
- Ensure indexes are created

---

## 📝 Final Checklist

Before going to production:

- [x] Build successful (no compilation errors)
- [ ] All tests pass (`mvn test`)
- [ ] Database migration executed
- [ ] Redis running and configured
- [ ] WebSocket endpoint tested
- [ ] Upload with small file successful
- [ ] Upload with large file successful
- [ ] Memory usage monitored < 2GB
- [ ] Thread pools not saturated
- [ ] Cache hit rate > 80%
- [ ] Documentation updated
- [ ] Team briefed on new features
- [ ] Rollback plan prepared

---

**Deployment Date:** _____________

**Deployed By:** _____________

**Verified By:** _____________

**Status:** ⬜ SUCCESS  ⬜ PARTIAL  ⬜ FAILED

**Notes:**
```
_________________________________
_________________________________
_________________________________
```
