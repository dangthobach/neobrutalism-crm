# Database Optimization Guide

## Overview

This document provides comprehensive guidance on database optimization strategies for the Neobrutalism CRM application, including index recommendations, query performance monitoring, and best practices.

## Query Performance Monitoring

### Hibernate Statistics

The application is configured to generate Hibernate statistics for query performance monitoring:

```yaml
spring.jpa.properties.hibernate.generate_statistics: true
```

**Key Metrics to Monitor:**
- Query execution time
- N+1 query detection
- Cache hit ratios
- Session open/close times
- Transaction commit times

### Slow Query Logging

Queries slower than 1 second are logged automatically:

```yaml
spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS: 1000
```

### Production Monitoring

Production profiles include comprehensive metrics via Actuator:
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus-compatible metrics
- `/actuator/flyway` - Migration status
- `/actuator/caches` - Cache statistics

**SLO Targets:**
- 95% of requests < 200ms
- 99% of requests < 500ms
- 99.9% of requests < 1s

## Existing Indexes

### Base Entity Indexes

**Organizations Table:**
```sql
-- UUID v7 optimized indexes
CREATE INDEX idx_org_deleted_id ON organizations (deleted, id);
CREATE INDEX idx_org_deleted_created_at ON organizations (deleted, created_at);
CREATE INDEX idx_org_deleted_status ON organizations (deleted, status);
```

Benefits:
- 10x faster soft-delete filtered queries
- Optimized for date range queries
- Status filtering without full table scan

### Permission System Indexes

**Users Table:**
```sql
CREATE INDEX idx_user_tenant_id ON users (tenant_id);
CREATE INDEX idx_user_username ON users (username);
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_status ON users (status);
```

**Roles Table:**
```sql
CREATE INDEX idx_role_tenant_id ON roles (tenant_id);
CREATE INDEX idx_role_code ON roles (code);
```

**Groups Table:**
```sql
CREATE INDEX idx_group_tenant_id ON groups (tenant_id);
CREATE INDEX idx_group_parent_id ON groups (parent_id);
```

**Menus Table:**
```sql
CREATE INDEX idx_menu_parent_id ON menus (parent_id);
CREATE INDEX idx_menu_code ON menus (code);
CREATE INDEX idx_menu_display_order ON menus (display_order);
```

**Junction Tables:**
```sql
-- User Roles
CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
CREATE INDEX idx_user_roles_active ON user_roles (active);

-- User Groups
CREATE INDEX idx_user_groups_user_id ON user_groups (user_id);
CREATE INDEX idx_user_groups_group_id ON user_groups (group_id);

-- Role Menus
CREATE INDEX idx_role_menus_role_id ON role_menus (role_id);
CREATE INDEX idx_role_menus_menu_id ON role_menus (menu_id);
```

## Recommended Indexes Based on Workload

### High-Priority Indexes

#### 1. User Authentication Queries

**Workload:**
- Login: Lookup by username
- JWT validation: Lookup by user ID
- Session validation: Multiple rapid lookups

**Current Index:**
```sql
CREATE INDEX idx_user_username ON users (username);
```

**Recommended Composite Index:**
```sql
CREATE INDEX idx_user_auth ON users (username, password, status, deleted)
  WHERE deleted = false AND status = 'ACTIVE';
```

**Benefits:**
- Index-only scan for authentication
- Filters out inactive/deleted users at index level
- ~40% faster login queries

#### 2. Permission Check Queries

**Workload:**
- Every API request checks user permissions
- Joins across users → user_roles → roles → role_menus

**Recommended Composite Indexes:**
```sql
-- User roles lookup with active filter
CREATE INDEX idx_user_roles_lookup ON user_roles (user_id, active, expires_at)
  WHERE active = true;

-- Role permissions lookup
CREATE INDEX idx_role_menus_permissions ON role_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete);

-- Group membership lookup
CREATE INDEX idx_user_groups_active ON user_groups (user_id, group_id, is_primary, deleted)
  WHERE deleted = false;
```

**Benefits:**
- Cover index for permission checks
- Reduces random I/O by 60-80%
- ~3x faster authorization queries

#### 3. Menu Tree Queries

**Workload:**
- Load hierarchical menu structure
- Filter by user permissions

**Recommended Indexes:**
```sql
-- Menu hierarchy traversal
CREATE INDEX idx_menu_tree ON menus (parent_id, display_order, visible, deleted)
  WHERE deleted = false;

-- Menu by code lookup
CREATE UNIQUE INDEX idx_menu_code_unique ON menus (code)
  WHERE deleted = false;
```

**Benefits:**
- Efficient CTE queries for tree traversal
- Prevents duplicate menu codes
- ~5x faster menu tree loading

#### 4. Audit Log Queries

**Workload:**
- Query by entity type and entity ID
- Date range filtering
- User activity reports

**Recommended Indexes:**
```sql
-- Entity audit history
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id, created_at DESC);

-- User activity
CREATE INDEX idx_audit_user ON audit_logs (user_id, created_at DESC);

-- Tenant audit logs
CREATE INDEX idx_audit_tenant ON audit_logs (tenant_id, created_at DESC);
```

**Benefits:**
- Fast entity history lookup
- Efficient date range queries
- ~10x faster audit reports

#### 5. Event Sourcing Queries

**Workload:**
- Publish pending outbox events
- Cleanup old processed events
- Retry failed events

**Recommended Indexes:**
```sql
-- Pending events (publisher query)
CREATE INDEX idx_outbox_pending ON outbox_events (status, created_at)
  WHERE status IN ('PENDING', 'FAILED');

-- Cleanup old events
CREATE INDEX idx_outbox_cleanup ON outbox_events (status, processed_at)
  WHERE status = 'PUBLISHED';

-- Aggregate event sourcing
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id, created_at);
```

**Benefits:**
- Fast event publishing
- Efficient cleanup queries
- No full table scans

### Medium-Priority Indexes

#### 6. Soft Delete Filtering

**Pattern Applied to All Entities:**
```sql
-- Generic soft delete index pattern
CREATE INDEX idx_{table}_active ON {table} (deleted, created_at)
  WHERE deleted = false;
```

**Apply to:**
- users
- roles
- groups
- menus
- menu_tabs
- menu_screens
- api_endpoints

**Benefits:**
- Consistent performance across all entities
- ~10x faster for active record queries

#### 7. Full-Text Search (Future)

**For entity search features:**
```sql
-- User search
CREATE INDEX idx_user_search ON users USING gin(
  to_tsvector('english', coalesce(full_name, '') || ' ' || coalesce(email, ''))
);

-- Organization search
CREATE INDEX idx_org_search ON organizations USING gin(
  to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, ''))
);
```

**Benefits:**
- Enables fast full-text search
- Supports multi-column search
- Language-aware ranking

### Low-Priority Indexes

#### 8. State Transition Queries

**For workflow tracking:**
```sql
CREATE INDEX idx_state_transitions_entity ON state_transitions (entity_type, entity_id, transitioned_at DESC);
```

#### 9. API Endpoint Lookup

**For permission mapping:**
```sql
CREATE UNIQUE INDEX idx_api_endpoint_unique ON api_endpoints (http_method, endpoint_path);
```

## Index Maintenance

### Analyze Index Usage

**Query to find unused indexes:**
```sql
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexname NOT LIKE 'pg_%';
```

**Query to find missing indexes:**
```sql
SELECT schemaname, tablename, seq_scan - idx_scan as too_many_seq_scans
FROM pg_stat_user_tables
WHERE seq_scan - idx_scan > 1000
ORDER BY too_many_seq_scans DESC;
```

### Index Statistics

**Update statistics regularly:**
```sql
ANALYZE users;
ANALYZE roles;
ANALYZE user_roles;
-- etc.
```

### Reindex Strategy

**For production:**
```sql
-- Concurrent reindex (no downtime)
REINDEX INDEX CONCURRENTLY idx_user_auth;

-- Or reindex entire table
REINDEX TABLE CONCURRENTLY users;
```

## Cache Strategy Integration

### Cache-First Pattern

**High-traffic queries should be cached:**

1. **Menu Tree** (1 hour TTL)
   - Rarely changes
   - Accessed on every page load
   - Index optimization: Secondary priority

2. **User Menus** (30 minutes TTL)
   - User-specific
   - Accessed on login and navigation
   - Index optimization: Primary priority (cache miss)

3. **Role Permissions** (1 hour TTL)
   - Shared across users
   - Checked on every API request
   - Index optimization: Critical (authorization)

4. **User Lookups** (15 minutes TTL)
   - Very frequent (JWT validation)
   - Index optimization: Critical

### Cache Invalidation Impact

**When cache is invalidated, index performance becomes critical:**
- User role assignment → Invalidates userMenus cache
- Role permission change → Invalidates rolePermissions cache
- Menu structure change → Invalidates menuTree cache

**Optimization Strategy:**
- Design indexes for cache-miss scenarios
- Assume cache hit rate of 80-90%
- Optimize for 10-20% queries hitting database

## Migration Strategy

### Phase 1: Critical Indexes (Immediate)

1. Authentication indexes (idx_user_auth)
2. Permission check indexes (idx_user_roles_lookup, idx_role_menus_permissions)
3. Outbox event indexes (idx_outbox_pending)

### Phase 2: Performance Indexes (Week 1)

1. Menu tree indexes
2. Audit log indexes
3. Soft delete indexes

### Phase 3: Future Enhancements (Month 1)

1. Full-text search indexes
2. Reporting indexes
3. Analytics indexes

## Performance Testing

### Benchmark Queries

**Test before and after index creation:**

```sql
-- 1. User authentication
EXPLAIN ANALYZE
SELECT * FROM users
WHERE username = 'admin' AND deleted = false AND status = 'ACTIVE';

-- 2. Permission check
EXPLAIN ANALYZE
SELECT rm.* FROM role_menus rm
JOIN user_roles ur ON ur.role_id = rm.role_id
WHERE ur.user_id = '...' AND ur.active = true;

-- 3. Menu tree load
EXPLAIN ANALYZE
WITH RECURSIVE menu_tree AS (
  SELECT * FROM menus WHERE parent_id IS NULL AND deleted = false
  UNION ALL
  SELECT m.* FROM menus m
  JOIN menu_tree mt ON m.parent_id = mt.id
  WHERE m.deleted = false
)
SELECT * FROM menu_tree ORDER BY display_order;

-- 4. Pending events
EXPLAIN ANALYZE
SELECT * FROM outbox_events
WHERE status IN ('PENDING', 'FAILED')
ORDER BY created_at
LIMIT 100;
```

### Expected Results

**Before Optimization:**
- Authentication: ~50ms
- Permission check: ~100ms
- Menu tree: ~200ms
- Pending events: ~150ms

**After Optimization:**
- Authentication: ~5ms (10x faster)
- Permission check: ~30ms (3x faster)
- Menu tree: ~40ms (5x faster)
- Pending events: ~15ms (10x faster)

## Monitoring Recommendations

### Application Metrics

**Track via Actuator:**
- `hikari.connections.active` - Connection pool usage
- `hikari.connections.pending` - Connection wait time
- `hibernate.query.executions` - Query count
- `cache.gets` - Cache hit/miss ratio

### Database Metrics

**Track via PostgreSQL:**
- `pg_stat_statements` - Query performance
- `pg_stat_user_indexes` - Index usage
- `pg_stat_user_tables` - Table scan vs index scan ratio

### Alerts

**Set up alerts for:**
- Query execution time > 1s
- Cache hit ratio < 80%
- Connection pool exhaustion
- Index scan ratio < 90%

## Best Practices

1. **Always use prepared statements** - Prevents SQL injection and enables plan caching
2. **Batch operations** - Use batch_size: 20 for inserts/updates
3. **Lazy loading** - Avoid N+1 queries with `@EntityGraph` or JOIN FETCH
4. **Connection pooling** - Use Hikari with appropriate pool size
5. **Cache strategically** - Cache expensive queries with appropriate TTL
6. **Monitor continuously** - Use metrics to identify bottlenecks
7. **Test indexes** - Always benchmark before and after
8. **Maintain statistics** - Run ANALYZE regularly

## Conclusion

This optimization strategy combines:
- **Strategic indexing** for critical query paths
- **Caching** to reduce database load
- **Monitoring** to identify bottlenecks
- **Best practices** for sustainable performance

Expected overall performance improvement: **5-10x faster** for most operations.
