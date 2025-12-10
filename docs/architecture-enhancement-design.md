# Architecture Enhancement Design Document

**Project:** Neobrutalism CRM - Enhancement Roadmap
**Document Type:** BMAD DESIGN Package
**Based On:** Brainstorming Session 2025-12-07
**Version:** 1.0.0
**Date:** 2025-12-08
**Status:** Ready for Implementation

---

## Executive Summary

This architecture document defines the design for enhancing the Neobrutalism CRM with 7 strategic features identified through systematic brainstorming. The design follows the **Hybrid Approach: Enterprise Security + Modern UX** strategy, implementing features over a 30-week timeline.

**Key Design Principles:**
- **Maximize Reuse:** Leverage existing Casbin, WebSocket, Spring Batch, React Query infrastructure
- **Minimize Risk:** Build on proven patterns already in codebase
- **Incremental Delivery:** Quick wins (1-2 sprints) before strategic investments (6 sprints)
- **SOLID Principles:** Maintain clean architecture with clear boundaries
- **Modern Patterns:** Event sourcing, CQRS, microservices-ready design

**Enhancement Scope:**
1. **#30** - Automated Dependency Updates (Week 1)
2. **#19** - Command Palette (Weeks 2-6)
3. **#3** - Transaction Integrity (Weeks 2-6)
4. **#1** - Granular Authorization (Weeks 7-12)
5. **#4** - Policy Conflict Detection (Weeks 13-18)
6. **#2** - High-Performance Reporting (Weeks 19-24)
7. **#9** - Real-Time Collaboration (Weeks 25-30)

---

## Table of Contents

1. [Domain Design](#1-domain-design)
2. [Repository Design](#2-repository-design)
3. [Application Service Design](#3-application-service-design)
4. [API Contract Design](#4-api-contract-design)
5. [Data Design](#5-data-design)
6. [API-Model-DB Mapping](#6-api-model-db-mapping-matrix)
7. [Cross-Cutting Design](#7-cross-cutting-design)

---

# 1. DOMAIN DESIGN

## 1.1 Aggregate Design

### 1.1.1 Permission Aggregate (Enhanced)

**Aggregate Root:** `PermissionPolicy`

**Purpose:** Extends existing Casbin-based permission system with data scope and conflict detection

**Aggregate Boundary:**
```
PermissionPolicy (Root)
├── PolicyRule (Entity)
│   ├── subject: String (role/user)
│   ├── tenantId: String
│   ├── resource: String
│   ├── action: String
│   ├── dataScope: DataScopeEnum
│   └── conditions: List<PolicyCondition>
├── PolicyConflict (Value Object)
│   ├── conflictType: ConflictTypeEnum
│   ├── severity: SeverityEnum
│   ├── affectedPolicies: List<PolicyRule>
│   └── resolutionSuggestion: String
└── PolicyAuditLog (Entity)
    ├── operation: PolicyOperationEnum
    ├── performedBy: UUID
    ├── timestamp: Instant
    └── changes: PolicyChangeDiff
```

**Invariants:**
- A PolicyRule must have unique combination of (subject, tenantId, resource, action, dataScope)
- DataScope SELF_ONLY cannot coexist with ALL for same role on same resource
- PolicyConflict must reference at least 2 PolicyRules
- PolicyAuditLog is append-only (no updates or deletes)

**Domain Events:**
- `PolicyRuleCreatedEvent`
- `PolicyRuleUpdatedEvent`
- `PolicyRuleDeletedEvent`
- `PolicyConflictDetectedEvent`
- `PolicyConflictResolvedEvent`

---

### 1.1.2 Reporting Aggregate (New)

**Aggregate Root:** `ReportExecution`

**Purpose:** Asynchronous report generation and caching

**Aggregate Boundary:**
```
ReportExecution (Root)
├── reportDefinitionId: UUID
├── requestedBy: UUID
├── tenantId: String
├── status: ReportStatusEnum
├── parameters: Map<String, Object>
├── startTime: Instant
├── completionTime: Instant
├── resultLocation: String (S3/MinIO URL)
├── rowCount: Long
├── errorMessage: String
└── processingMetrics: ExecutionMetrics (Value Object)
    ├── queryTime: Duration
    ├── renderTime: Duration
    ├── cacheHit: Boolean
    └── memoryUsed: Long

ReportDefinition (Entity - separate aggregate)
├── name: String
├── description: String
├── query: String (SQL template)
├── parameters: List<ReportParameter>
├── outputFormat: FormatEnum
├── cacheDuration: Duration
└── accessRoles: List<String>
```

**Invariants:**
- ReportExecution status must transition: PENDING → RUNNING → COMPLETED/FAILED
- Only PENDING reports can transition to RUNNING
- COMPLETED reports are immutable
- resultLocation must be set when status = COMPLETED
- errorMessage must be set when status = FAILED

**Domain Events:**
- `ReportRequestedEvent`
- `ReportStartedEvent`
- `ReportCompletedEvent`
- `ReportFailedEvent`
- `ReportCachedResultServedEvent`

---

### 1.1.3 Presence Aggregate (New)

**Aggregate Root:** `UserPresence`

**Purpose:** Track user presence on specific entities for real-time collaboration

**Aggregate Boundary:**
```
UserPresence (Root)
├── userId: UUID
├── tenantId: String
├── activeSession: Session (Value Object)
│   ├── sessionId: UUID
│   ├── connectedAt: Instant
│   ├── lastHeartbeat: Instant
│   └── device: String
└── viewingEntities: Set<EntityReference> (Value Object)
    ├── entityType: String (CUSTOMER, TASK, etc.)
    ├── entityId: UUID
    ├── viewMode: ViewModeEnum (VIEWING, EDITING)
    └── startedAt: Instant

CollaborationLock (Entity - separate from UserPresence)
├── entityType: String
├── entityId: UUID
├── lockedBy: UUID
├── lockType: LockTypeEnum (SOFT, HARD)
├── acquiredAt: Instant
├── expiresAt: Instant
└── metadata: Map<String, String>
```

**Invariants:**
- User can have at most 1 active session per device
- User can be "viewing" multiple entities but "editing" only 1 at a time
- lastHeartbeat must be within 30 seconds for active session
- CollaborationLock expires after 5 minutes if not renewed
- Only one HARD lock per entity at a time

**Domain Events:**
- `UserPresenceJoinedEvent`
- `UserPresenceLeftEvent`
- `UserStartedEditingEvent`
- `UserStoppedEditingEvent`
- `CollaborationLockAcquiredEvent`
- `CollaborationLockReleasedEvent`

---

### 1.1.4 Command Palette Aggregate (New)

**Aggregate Root:** `CommandRegistry`

**Purpose:** Dynamic command palette for power users

**Aggregate Boundary:**
```
CommandRegistry (Root)
├── tenantId: String
└── commands: List<Command> (Entity)
    ├── commandId: UUID
    ├── name: String
    ├── description: String
    ├── keywords: List<String>
    ├── category: CommandCategoryEnum
    ├── requiredPermission: PermissionRequirement (Value Object)
    ├── action: CommandAction (Value Object)
    │   ├── actionType: ActionTypeEnum (NAVIGATE, API_CALL, CUSTOM)
    │   ├── target: String
    │   └── parameters: Map<String, Object>
    └── enabled: Boolean

UserCommandHistory (Entity - separate aggregate)
├── userId: UUID
├── recentCommands: CircularBuffer<CommandExecution>
│   ├── commandId: UUID
│   ├── executedAt: Instant
│   └── result: ExecutionResult
└── favoriteCommands: Set<UUID>
```

**Invariants:**
- Command name must be unique within tenant
- Keywords are case-insensitive
- CommandRegistry is immutable (rebuilt on system changes)
- UserCommandHistory keeps only last 50 executions
- Disabled commands do not appear in search results

**Domain Events:**
- `CommandRegistered`
- `CommandExecuted`
- `CommandAddedToFavorites`

---

### 1.1.5 Transaction Integrity Enhancement (Existing User/Customer/Task Aggregates)

**Enhancement:** Add idempotency and retry metadata to existing aggregates

**Value Objects Added:**
```
IdempotencyKey (Value Object)
├── key: String (UUID or client-provided)
├── createdAt: Instant
├── expiresAt: Instant
├── requestHash: String
└── responseSnapshot: String (serialized)

RetryMetadata (Value Object)
├── attemptNumber: Int
├── lastAttemptAt: Instant
├── nextRetryAt: Instant
├── exponentialBackoff: Duration
└── maxAttempts: Int
```

**Enhancement to Existing Aggregates:**
- `User`, `Customer`, `Contact`, `Task`, `Activity` all get:
  - `version: Long` (for optimistic locking)
  - `lastModifiedBy: UUID`
  - `lastModifiedAt: Instant`

**No new aggregates, just enhancement patterns**

---

## 1.2 Domain Rules Refinement

### 1.2.1 Permission Domain Rules

**Business Rule:** Data Scope Hierarchy
```
RULE: DataScope-Hierarchy
PRE-CONDITIONS:
  - User has role R with dataScope D on resource RES
  - User requests data with owner O
POST-CONDITIONS:
  - IF D = ALL THEN return all data
  - IF D = DEPARTMENT THEN return data where O.department = User.department
  - IF D = SELF_ONLY THEN return data where O.id = User.id
  - ELSE deny access
VALIDATION:
  - Department must exist in organization hierarchy
  - Owner field must be specified in resource schema
```

**Business Rule:** Policy Conflict Detection
```
RULE: No-Conflicting-Scopes
PRE-CONDITIONS:
  - Role R has policy P1 with scope S1 on resource RES
  - Attempting to add policy P2 with scope S2 on same RES for same role R
POST-CONDITIONS:
  - IF S1 = ALL AND S2 = SELF_ONLY THEN conflict = TRUE
  - IF S1 = DEPARTMENT AND S2 = ALL THEN conflict = FALSE (escalation allowed)
  - IF S1 = S2 THEN duplicate = TRUE (not a conflict, just redundant)
VALIDATION:
  - Conflict severity = HIGH if escalating from ALL to restricted
  - Conflict severity = MEDIUM if ambiguous (DEPARTMENT + SELF_ONLY)
```

**Business Rule:** Policy Audit Retention
```
RULE: Audit-Compliance
PRE-CONDITIONS:
  - PolicyAuditLog created for policy change
POST-CONDITIONS:
  - AuditLog must be retained for minimum 2 years
  - AuditLog cannot be modified or deleted
  - AuditLog must include full before/after state
VALIDATION:
  - Audit entry must pass checksum validation
  - Timestamp must be tamper-proof
```

---

### 1.2.2 Reporting Domain Rules

**Business Rule:** Report Execution Priority
```
RULE: Report-Priority-Queue
PRE-CONDITIONS:
  - Report R requested by user U with priority P
  - Queue Q has N pending reports
POST-CONDITIONS:
  - IF P = HIGH AND user has premium role THEN place at front of Q
  - IF P = NORMAL THEN place at end of Q
  - IF N > 100 THEN reject with "queue full" error
VALIDATION:
  - Only users with role ADMIN or PREMIUM can set HIGH priority
  - Queue capacity enforced at service boundary
```

**Business Rule:** Report Cache Invalidation
```
RULE: Cache-Freshness
PRE-CONDITIONS:
  - Report R has cached result CR with timestamp T
  - Report definition D has cacheDuration CD
  - Current time = NOW
POST-CONDITIONS:
  - IF NOW - T > CD THEN invalidate cache, re-execute
  - IF underlying data changed (detected via event) THEN invalidate immediately
  - ELSE serve from cache
VALIDATION:
  - Cache key includes all parameter values
  - Cache must be tenant-isolated
```

**Business Rule:** Report Size Limits
```
RULE: Report-Size-Limits
PRE-CONDITIONS:
  - Report R generating N rows
POST-CONDITIONS:
  - IF N > 1,000,000 THEN switch to streaming mode
  - IF N > 10,000,000 THEN reject with "dataset too large"
  - ELSE buffer in memory
VALIDATION:
  - Row count estimated before full execution
  - Streaming threshold configurable per tenant
```

---

### 1.2.3 Presence Domain Rules

**Business Rule:** Presence Timeout
```
RULE: Active-Session-Timeout
PRE-CONDITIONS:
  - UserPresence P has lastHeartbeat = T
  - Current time = NOW
POST-CONDITIONS:
  - IF NOW - T > 30 seconds THEN mark session INACTIVE
  - IF session INACTIVE THEN remove from presence list
  - IF user was editing entity E THEN release lock on E
VALIDATION:
  - Heartbeat check runs every 10 seconds
  - Grace period of 5 seconds before timeout
```

**Business Rule:** Collaboration Lock Acquisition
```
RULE: Lock-Acquisition
PRE-CONDITIONS:
  - User U requests lock L on entity E
  - Entity E has existing lock EL (or none)
POST-CONDITIONS:
  - IF EL = none THEN grant lock to U
  - IF EL.lockedBy = U THEN extend lock expiry
  - IF EL.lockedBy != U AND EL.lockType = SOFT THEN notify conflict, allow
  - IF EL.lockedBy != U AND EL.lockType = HARD THEN deny lock
VALIDATION:
  - Lock expiry must be within 1-10 minutes
  - User must have EDIT permission on entity E
```

**Business Rule:** Presence Privacy
```
RULE: Presence-Visibility
PRE-CONDITIONS:
  - User U viewing entity E
  - User V also viewing entity E
  - U and V in same tenant T
POST-CONDITIONS:
  - U can see V's presence IF U has permission to view E
  - Presence includes: name, view mode, duration
  - Presence EXCLUDES: IP address, device details
VALIDATION:
  - Permission check runs on presence subscription
  - Privacy-sensitive fields never transmitted
```

---

### 1.2.4 Command Palette Domain Rules

**Business Rule:** Command Authorization
```
RULE: Command-Access-Control
PRE-CONDITIONS:
  - User U executes command C
  - Command C requires permission P
POST-CONDITIONS:
  - IF user has permission P THEN execute command
  - ELSE deny with "insufficient permissions" error
VALIDATION:
  - Permission check uses existing Casbin enforcer
  - Permission includes data scope restrictions
```

**Business Rule:** Command Search Ranking
```
RULE: Command-Search-Relevance
PRE-CONDITIONS:
  - User U searches for query Q
  - Commands C1, C2, ... Cn match query Q
POST-CONDITIONS:
  - Rank by: exact name match > keyword match > fuzzy match
  - Boost commands in user's recent history by +10 points
  - Boost commands in user's favorites by +20 points
  - Return top 10 results
VALIDATION:
  - Search must complete in < 100ms
  - Results filtered by user permissions
```

---

### 1.2.5 Transaction Integrity Domain Rules

**Business Rule:** Idempotency Enforcement
```
RULE: Idempotent-Operations
PRE-CONDITIONS:
  - Request R with idempotency key K
  - Key K exists in cache with result RS
POST-CONDITIONS:
  - IF K exists AND requestHash matches THEN return cached RS
  - IF K exists AND requestHash differs THEN reject with "key mismatch"
  - IF K not exists THEN process request, cache result
VALIDATION:
  - Idempotency key valid for 24 hours
  - Request hash includes body + critical headers
  - Cache storage in Redis with tenant isolation
```

**Business Rule:** Optimistic Locking
```
RULE: Version-Based-Concurrency
PRE-CONDITIONS:
  - User U1 reads entity E with version V1
  - User U2 updates entity E (version becomes V2)
  - User U1 attempts update with version V1
POST-CONDITIONS:
  - IF current version != V1 THEN reject with "stale version"
  - IF current version = V1 THEN apply update, increment version
VALIDATION:
  - Version check atomic with update
  - Client receives latest version in error response
```

**Business Rule:** Retry with Exponential Backoff
```
RULE: Transient-Failure-Retry
PRE-CONDITIONS:
  - Operation O fails with transient error E
  - Retry metadata M shows N attempts < MAX_ATTEMPTS
POST-CONDITIONS:
  - IF N < 3 THEN retry after 2^N seconds
  - IF N = 3 THEN retry after 8 seconds (max backoff)
  - IF N >= MAX_ATTEMPTS THEN fail permanently
VALIDATION:
  - Only retryable errors trigger retry (connection, timeout)
  - Non-retryable errors (validation, auth) fail immediately
```

---

# 2. REPOSITORY DESIGN

## 2.1 PermissionPolicyRepository

**Responsibilities:**
- Persist Casbin policies to database (extends existing CasbinAdapter)
- Query policies by role, tenant, resource
- Detect policy conflicts via graph traversal queries
- Audit log persistence

**Interface:**
```java
interface PermissionPolicyRepository extends JpaRepository<PermissionPolicy, UUID> {

    // Existing Casbin adapter methods (already implemented)
    List<PolicyRule> findByTenantId(String tenantId);
    List<PolicyRule> findBySubjectAndTenantId(String subject, String tenantId);

    // New methods for data scope
    List<PolicyRule> findByResourceAndTenantId(String resource, String tenantId);
    List<PolicyRule> findBySubjectAndResourceAndTenantId(
        String subject, String resource, String tenantId);

    @Query("SELECT p FROM PolicyRule p WHERE p.tenantId = :tenantId " +
           "AND p.dataScope = :scope")
    List<PolicyRule> findByDataScope(String tenantId, DataScopeEnum scope);

    // Conflict detection
    @Query("SELECT p1, p2 FROM PolicyRule p1, PolicyRule p2 " +
           "WHERE p1.tenantId = :tenantId AND p2.tenantId = :tenantId " +
           "AND p1.subject = p2.subject AND p1.resource = p2.resource " +
           "AND p1.dataScope != p2.dataScope AND p1.id < p2.id")
    List<Object[]> findConflictingPolicies(String tenantId);

    // Audit
    @Query("SELECT a FROM PolicyAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    Page<PolicyAuditLog> findAuditLogs(
        String tenantId, Instant start, Instant end, Pageable pageable);
}
```

**Data Access Constraints:**
- **Transaction Isolation:** READ_COMMITTED (prevent dirty reads)
- **Locking Strategy:** Optimistic locking on PolicyRule (version field)
- **Tenant Isolation:** All queries MUST include tenantId filter
- **Indexing:** Composite index on (tenantId, subject, resource, action)

**Specification Pattern:**
```java
class PolicySpecification {
    static Specification<PolicyRule> hasDataScope(DataScopeEnum scope);
    static Specification<PolicyRule> hasSubject(String subject);
    static Specification<PolicyRule> inTenant(String tenantId);
    static Specification<PolicyRule> onResource(String resource);

    // Composite
    static Specification<PolicyRule> conflictsWith(PolicyRule other);
}
```

---

## 2.2 ReportExecutionRepository

**Responsibilities:**
- Store report execution metadata
- Query pending/running reports for queue management
- Retrieve completed reports for cache lookup

**Interface:**
```java
interface ReportExecutionRepository extends JpaRepository<ReportExecution, UUID> {

    // Queue management
    @Query("SELECT r FROM ReportExecution r WHERE r.tenantId = :tenantId " +
           "AND r.status = 'PENDING' ORDER BY r.priority DESC, r.createdAt ASC")
    List<ReportExecution> findPendingReports(String tenantId, Pageable pageable);

    long countByTenantIdAndStatusIn(String tenantId, List<ReportStatusEnum> statuses);

    // Cache lookup
    @Query("SELECT r FROM ReportExecution r WHERE r.tenantId = :tenantId " +
           "AND r.reportDefinitionId = :defId AND r.status = 'COMPLETED' " +
           "AND r.parameters = :params " +
           "AND r.completionTime > :cacheExpiry " +
           "ORDER BY r.completionTime DESC")
    Optional<ReportExecution> findCachedResult(
        String tenantId, UUID defId, Map<String, Object> params, Instant cacheExpiry);

    // User reports
    Page<ReportExecution> findByRequestedByAndTenantId(
        UUID userId, String tenantId, Pageable pageable);

    // Cleanup
    @Modifying
    @Query("DELETE FROM ReportExecution r WHERE r.status = 'COMPLETED' " +
           "AND r.completionTime < :before")
    int deleteOldCompletedReports(Instant before);
}
```

**Data Access Constraints:**
- **Transaction Isolation:** READ_COMMITTED for queries, REPEATABLE_READ for status updates
- **Locking Strategy:** Pessimistic lock when transitioning PENDING → RUNNING
- **Pagination:** Default page size 50, max 1000
- **Indexing:**
  - Composite: (tenantId, status, priority, createdAt)
  - Composite: (tenantId, reportDefinitionId, parameters, completionTime)

---

## 2.3 UserPresenceRepository

**Responsibilities:**
- Store active user presence sessions
- Query users viewing specific entities
- Cleanup stale sessions

**Interface:**
```java
interface UserPresenceRepository extends JpaRepository<UserPresence, UUID> {

    // Presence queries
    @Query("SELECT p FROM UserPresence p WHERE p.tenantId = :tenantId " +
           "AND :entityRef MEMBER OF p.viewingEntities " +
           "AND p.activeSession.lastHeartbeat > :cutoff")
    List<UserPresence> findUsersViewingEntity(
        String tenantId, EntityReference entityRef, Instant cutoff);

    Optional<UserPresence> findByUserIdAndTenantId(UUID userId, String tenantId);

    // Session management
    @Modifying
    @Query("DELETE FROM UserPresence p WHERE p.activeSession.lastHeartbeat < :cutoff")
    int removeStalePresences(Instant cutoff);

    @Query("SELECT COUNT(p) FROM UserPresence p WHERE p.tenantId = :tenantId " +
           "AND p.activeSession.lastHeartbeat > :cutoff")
    long countActiveUsers(String tenantId, Instant cutoff);
}

interface CollaborationLockRepository extends JpaRepository<CollaborationLock, UUID> {

    Optional<CollaborationLock> findByEntityTypeAndEntityId(
        String entityType, UUID entityId);

    List<CollaborationLock> findByLockedByAndTenantId(UUID userId, String tenantId);

    @Modifying
    @Query("DELETE FROM CollaborationLock l WHERE l.expiresAt < :now")
    int removeExpiredLocks(Instant now);
}
```

**Data Access Constraints:**
- **Transaction Isolation:** READ_UNCOMMITTED (acceptable for presence data)
- **Locking Strategy:** None (ephemeral data)
- **TTL:** Automatic cleanup of records older than 5 minutes
- **Indexing:**
  - Index on (entityType, entityId) for lock queries
  - Index on (tenantId, lastHeartbeat) for presence queries

---

## 2.4 CommandRegistryRepository

**Responsibilities:**
- Store command definitions (relatively static)
- Query commands by tenant for palette display

**Interface:**
```java
interface CommandRepository extends JpaRepository<Command, UUID> {

    @Query("SELECT c FROM Command c WHERE c.tenantId = :tenantId " +
           "AND c.enabled = true ORDER BY c.category, c.name")
    List<Command> findEnabledCommands(String tenantId);

    @Query("SELECT c FROM Command c WHERE c.tenantId = :tenantId " +
           "AND c.enabled = true " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR EXISTS (SELECT k FROM c.keywords k WHERE LOWER(k) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<Command> searchCommands(String tenantId, String query);

    Optional<Command> findByNameAndTenantId(String name, String tenantId);
}

interface UserCommandHistoryRepository extends JpaRepository<UserCommandHistory, UUID> {

    Optional<UserCommandHistory> findByUserIdAndTenantId(UUID userId, String tenantId);

    @Modifying
    @Query("UPDATE UserCommandHistory h SET h.recentCommands = " +
           ":updated WHERE h.userId = :userId")
    void updateRecentCommands(UUID userId, CircularBuffer<CommandExecution> updated);
}
```

**Data Access Constraints:**
- **Caching:** Command definitions cached for 5 minutes (rarely change)
- **Read-Heavy:** 99% reads, 1% writes
- **Indexing:**
  - Full-text index on (name, keywords) for search
  - Index on (tenantId, enabled) for list queries

---

## 2.5 Idempotency Key Repository (Redis)

**Responsibilities:**
- Store idempotency keys with expiry
- Fast lookup for duplicate request detection

**Interface:**
```java
interface IdempotencyKeyRepository {

    /**
     * Store idempotency key with result
     * @param key Idempotency key (UUID)
     * @param tenantId Tenant ID
     * @param requestHash SHA-256 hash of request
     * @param response Serialized response
     * @param ttl Time to live (24 hours)
     */
    void store(String key, String tenantId, String requestHash,
               String response, Duration ttl);

    /**
     * Retrieve cached response for idempotency key
     * @return Optional of (requestHash, response)
     */
    Optional<IdempotencyEntry> get(String key, String tenantId);

    /**
     * Check if key exists
     */
    boolean exists(String key, String tenantId);

    /**
     * Delete key (for manual invalidation)
     */
    void delete(String key, String tenantId);
}
```

**Data Access Constraints:**
- **Storage:** Redis with tenant-prefixed keys: `idempotency:{tenantId}:{key}`
- **TTL:** 24 hours automatic expiry
- **Atomic Operations:** Use Redis transactions for get-or-set
- **Clustering:** Redis cluster compatible (keys sharded by tenant)

---

# 3. APPLICATION SERVICE DESIGN

## 3.1 PermissionPolicyService (Enhanced)

**Use Case:** Apply Data Scope to Permission Check

**Service Name:** `PermissionPolicyService.checkPermissionWithDataScope()`

**Input DTO:**
```java
class PermissionCheckRequest {
    UUID userId;
    String tenantId;
    String resource;
    String action;
    UUID resourceOwnerId;  // NEW
    String resourceDepartment;  // NEW
}
```

**Output DTO:**
```java
class PermissionCheckResponse {
    boolean granted;
    DataScopeEnum appliedScope;
    String reason;  // for audit
    List<String> requiredRoles;
}
```

**Application Workflow:**
```
1. VALIDATE input
   - userId exists in tenant
   - resource is valid
   - action is in allowed set

2. RETRIEVE user roles from Casbin
   - enforcer.getRolesForUserInDomain(userId, tenantId)

3. FOR EACH role R:
   - QUERY policies with dataScope for R on resource
   - IF policy found:
     - IF dataScope = ALL → GRANT
     - IF dataScope = DEPARTMENT:
       - IF user.department = resource.department → GRANT
       - ELSE → DENY
     - IF dataScope = SELF_ONLY:
       - IF user.id = resource.ownerId → GRANT
       - ELSE → DENY

4. IF any role grants → RETURN granted=true
   ELSE → RETURN granted=false

5. LOG audit event (decision, reason, scope applied)
```

**Domain Services Required:**
- `CasbinEnforcer` (existing)
- `UserRepository` (existing)
- `PolicyAuditService` (new)

**Cross-Cutting Concerns:**
- **Validation:** Input sanitization, tenant verification
- **Logging:** DEBUG level for all permission checks
- **Authorization:** N/A (this IS the authorization service)
- **Caching:** Cache user roles for 5 minutes

---

## 3.2 PolicyConflictDetectionService (New)

**Use Case:** Detect and Report Policy Conflicts

**Service Name:** `PolicyConflictDetectionService.runConflictScan()`

**Input DTO:**
```java
class ConflictScanRequest {
    String tenantId;
    boolean autoFix;  // attempt automatic resolution
    ScanScopeEnum scope;  // ALL_POLICIES, ROLE_SPECIFIC, RESOURCE_SPECIFIC
}
```

**Output DTO:**
```java
class ConflictScanResponse {
    int conflictsDetected;
    int conflictsResolved;
    List<PolicyConflict> conflicts;
    Duration scanDuration;
}
```

**Application Workflow:**
```
1. VALIDATE scan request
   - tenantId exists
   - user has ADMIN role

2. LOAD all policies for tenant from repository
   - PolicyRepository.findByTenantId(tenantId)

3. BUILD policy graph
   - Nodes = PolicyRules
   - Edges = conflicts (same subject+resource, different scope)

4. TRAVERSE graph to find conflict paths
   - Detect: ALL + SELF_ONLY on same resource
   - Detect: ambiguous inheritance (role hierarchy)

5. FOR EACH conflict:
   - CLASSIFY severity (HIGH, MEDIUM, LOW)
   - GENERATE resolution suggestion
   - IF autoFix AND severity = LOW:
     - APPLY resolution
     - LOG action

6. PUBLISH ConflictDetectedEvent for HIGH severity

7. STORE scan results in audit log

8. RETURN scan response
```

**Domain Services Required:**
- `PolicyGraphBuilder` (new)
- `ConflictClassifier` (new)
- `PolicyResolutionEngine` (new)

**Cross-Cutting Concerns:**
- **Validation:** Admin-only operation
- **Logging:** INFO level for conflicts, WARN for high severity
- **Authorization:** Requires ADMIN role
- **Event Publishing:** Publish to WebSocket for real-time alerts
- **Idempotency:** Scan is read-only, inherently idempotent

---

## 3.3 ReportExecutionService (New)

**Use Case:** Request Report Generation

**Service Name:** `ReportExecutionService.requestReport()`

**Input DTO:**
```java
class ReportRequest {
    UUID reportDefinitionId;
    Map<String, Object> parameters;
    ReportPriorityEnum priority;
    ReportFormatEnum outputFormat;  // PDF, EXCEL, CSV
}
```

**Output DTO:**
```java
class ReportResponse {
    UUID executionId;
    ReportStatusEnum status;
    int queuePosition;  // if PENDING
    String downloadUrl;  // if COMPLETED (from cache)
    Instant estimatedCompletionTime;
}
```

**Application Workflow:**
```
1. VALIDATE request
   - reportDefinitionId exists
   - user has permission to run report
   - parameters match definition schema

2. CHECK cache for recent execution
   - ReportRepository.findCachedResult(defId, params)
   - IF cache hit AND fresh:
     - RETURN cached result immediately
     - PUBLISH ReportCachedResultServedEvent

3. IF no cache hit:
   - CREATE ReportExecution entity (status=PENDING)
   - SAVE to repository
   - ADD to execution queue (Redis/RabbitMQ)

4. RETURN execution ID and queue position

5. BACKGROUND WORKER (separate process):
   - POLL queue
   - FOR EACH pending report:
     - UPDATE status to RUNNING
     - EXECUTE query (streaming if large)
     - RENDER output format
     - UPLOAD result to MinIO/S3
     - UPDATE status to COMPLETED
     - PUBLISH ReportCompletedEvent (WebSocket notification)
```

**Domain Services Required:**
- `ReportDefinitionRepository` (existing)
- `ReportQueryExecutor` (new)
- `ReportRenderer` (new - PDF, Excel libraries)
- `ObjectStorageService` (existing - MinIO integration)
- `QueueService` (new - Redis/RabbitMQ)

**Cross-Cutting Concerns:**
- **Validation:** JSON schema validation for parameters
- **Logging:** INFO for requests, ERROR for failures
- **Authorization:** Check role permissions on report definition
- **Idempotency:** Use report hash as idempotency key (same params = same execution)
- **Event Publishing:** WebSocket notification on completion

---

## 3.4 UserPresenceService (New)

**Use Case:** Join Presence on Entity

**Service Name:** `UserPresenceService.joinPresence()`

**Input DTO:**
```java
class JoinPresenceRequest {
    UUID userId;
    String entityType;  // CUSTOMER, TASK, etc.
    UUID entityId;
    ViewModeEnum viewMode;  // VIEWING, EDITING
}
```

**Output DTO:**
```java
class PresenceResponse {
    UUID sessionId;
    List<UserPresenceInfo> otherUsers;  // who else is here
    CollaborationLockInfo lockInfo;  // current lock holder
}
```

**Application Workflow:**
```
1. VALIDATE request
   - user has permission to view/edit entity
   - entity exists

2. UPSERT UserPresence
   - IF user already has presence → UPDATE
   - ELSE → CREATE new

3. ADD entity to viewingEntities set

4. IF viewMode = EDITING:
   - ATTEMPT to acquire collaboration lock
   - IF lock held by another → RETURN soft conflict
   - ELSE → GRANT lock

5. QUERY other users on same entity
   - UserPresenceRepository.findUsersViewingEntity()

6. PUBLISH UserPresenceJoinedEvent (WebSocket)
   - Broadcast to all users on entity
   - Include user name, view mode

7. RETURN presence response

8. START heartbeat timer (client sends every 10s)
```

**Domain Services Required:**
- `EntityPermissionChecker` (existing)
- `CollaborationLockManager` (new)
- `WebSocketService` (existing)

**Cross-Cutting Concerns:**
- **Validation:** Entity type whitelist, permission check
- **Logging:** DEBUG level for presence events
- **Authorization:** Entity-level permission check
- **Real-Time:** WebSocket broadcast on presence changes

---

## 3.5 CommandPaletteService (New)

**Use Case:** Search Commands

**Service Name:** `CommandPaletteService.searchCommands()`

**Input DTO:**
```java
class CommandSearchRequest {
    String query;
    int maxResults;  // default 10
    boolean includeDisabled;  // default false
}
```

**Output DTO:**
```java
class CommandSearchResponse {
    List<CommandResult> commands;
    Duration searchTime;
}

class CommandResult {
    UUID commandId;
    String name;
    String description;
    List<String> keywords;
    double relevanceScore;
    boolean isFavorite;
    boolean isRecentlyUsed;
}
```

**Application Workflow:**
```
1. VALIDATE query
   - not empty
   - < 100 characters

2. LOAD enabled commands for tenant
   - FROM cache if available
   - ELSE query CommandRepository

3. FILTER by user permissions
   - FOR EACH command:
     - IF requiredPermission exists:
       - CHECK PermissionService
       - EXCLUDE if denied

4. SCORE commands by relevance
   - Exact name match: +100
   - Keyword match: +50
   - Fuzzy match: +10
   - Recent usage: +10
   - Favorite: +20

5. SORT by score descending

6. RETURN top N results

7. LOG search query for analytics
```

**Domain Services Required:**
- `CommandRepository` (new)
- `PermissionService` (existing)
- `UserCommandHistoryRepository` (new)

**Cross-Cutting Concerns:**
- **Validation:** Query sanitization (prevent injection)
- **Logging:** Analytics logging for popular searches
- **Authorization:** Command-level permission filtering
- **Caching:** Cache command list for 5 minutes
- **Performance:** Search must complete in <100ms

---

## 3.6 TransactionRetryService (Enhanced)

**Use Case:** Execute Operation with Retry

**Service Name:** `TransactionRetryService.executeWithRetry()`

**Input DTO:**
```java
class RetryableOperation<T> {
    Callable<T> operation;
    RetryPolicy policy;  // maxAttempts, backoff strategy
    Predicate<Exception> isRetryable;
}
```

**Output DTO:**
```java
class OperationResult<T> {
    T result;
    int attemptCount;
    Duration totalTime;
    List<Exception> failures;  // if any
}
```

**Application Workflow:**
```
1. INITIALIZE retry metadata
   - attemptNumber = 0
   - maxAttempts = policy.maxAttempts

2. WHILE attemptNumber < maxAttempts:
   - TRY:
     - EXECUTE operation()
     - IF success → RETURN result
   - CATCH exception:
     - IF isRetryable(exception):
       - INCREMENT attemptNumber
       - CALCULATE backoff = 2^attemptNumber seconds
       - SLEEP(backoff)
       - LOG retry attempt
     - ELSE:
       - THROW exception immediately

3. IF all attempts failed:
   - LOG permanent failure
   - THROW final exception

4. RETURN result with metadata
```

**Domain Services Required:**
- N/A (utility service)

**Cross-Cutting Concerns:**
- **Validation:** Operation must not be null
- **Logging:** WARN on retry, ERROR on permanent failure
- **Monitoring:** Metrics on retry rate, success after N attempts
- **Thread Safety:** Use synchronized if needed

---

## 3.7 IdempotencyService (New)

**Use Case:** Ensure Idempotent Request Processing

**Service Name:** `IdempotencyService.executeIdempotent()`

**Input DTO:**
```java
class IdempotentRequest<T> {
    String idempotencyKey;
    Supplier<T> operation;
    Duration ttl;  // default 24 hours
}
```

**Output DTO:**
```java
class IdempotentResponse<T> {
    T result;
    boolean fromCache;
    Instant cachedAt;  // if fromCache=true
}
```

**Application Workflow:**
```
1. VALIDATE idempotency key
   - not null
   - valid UUID format

2. COMPUTE request hash
   - SHA-256 of operation context

3. CHECK Redis cache
   - key = "idempotency:{tenantId}:{key}"
   - IF exists:
     - GET cached entry
     - IF requestHash matches → RETURN cached result
     - ELSE → THROW "key mismatch" error

4. IF not exists:
   - ACQUIRE distributed lock (Redis)
   - DOUBLE-CHECK cache (race condition)
   - IF still not exists:
     - EXECUTE operation
     - STORE result in cache with TTL
   - RELEASE lock

5. RETURN result
```

**Domain Services Required:**
- `IdempotencyKeyRepository` (Redis)
- `DistributedLockService` (Redis/Redisson)

**Cross-Cutting Concerns:**
- **Validation:** Key format validation
- **Logging:** INFO for cache hits, DEBUG for execution
- **Concurrency:** Distributed lock prevents duplicate execution
- **Monitoring:** Metrics on cache hit rate

---

# 4. API CONTRACT DESIGN

## 4.1 Permission Policy APIs

### 4.1.1 Check Permission with Data Scope

**Endpoint:** `POST /api/permissions/check-with-scope`

**Method:** POST

**Request Schema:**
```json
{
  "userId": "uuid",
  "resource": "string",  // e.g., "/api/customers"
  "action": "string",  // e.g., "READ"
  "resourceOwnerId": "uuid",
  "resourceDepartment": "string"
}
```

**Response Schema:**
```json
{
  "granted": boolean,
  "appliedScope": "ALL | DEPARTMENT | SELF_ONLY",
  "reason": "string",
  "requiredRoles": ["string"]
}
```

**Error Model:**
```json
{
  "error": "string",
  "message": "string",
  "timestamp": "ISO8601"
}
```

**HTTP Status Codes:**
- `200 OK` - Permission check completed
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing/invalid token
- `403 Forbidden` - Insufficient permissions to check
- `500 Internal Server Error` - System error

---

### 4.1.2 Add Policy with Data Scope

**Endpoint:** `POST /api/permissions/policies`

**Method:** POST

**Request Schema:**
```json
{
  "subject": "string",  // role code
  "resource": "string",
  "action": "string",
  "dataScope": "ALL | DEPARTMENT | SELF_ONLY",
  "conditions": [
    {
      "field": "string",
      "operator": "string",
      "value": "any"
    }
  ]
}
```

**Response Schema:**
```json
{
  "policyId": "uuid",
  "created": boolean,
  "conflicts": [
    {
      "conflictType": "string",
      "severity": "HIGH | MEDIUM | LOW",
      "message": "string",
      "suggestion": "string"
    }
  ]
}
```

**HTTP Status Codes:**
- `201 Created` - Policy created successfully
- `400 Bad Request` - Invalid policy definition
- `409 Conflict` - Policy conflicts detected
- `500 Internal Server Error`

---

### 4.1.3 Scan for Policy Conflicts

**Endpoint:** `POST /api/permissions/conflicts/scan`

**Method:** POST

**Request Schema:**
```json
{
  "autoFix": boolean,
  "scope": "ALL_POLICIES | ROLE_SPECIFIC | RESOURCE_SPECIFIC",
  "roleCode": "string",  // if ROLE_SPECIFIC
  "resource": "string"  // if RESOURCE_SPECIFIC
}
```

**Response Schema:**
```json
{
  "conflictsDetected": integer,
  "conflictsResolved": integer,
  "conflicts": [
    {
      "id": "uuid",
      "type": "string",
      "severity": "HIGH | MEDIUM | LOW",
      "affectedPolicies": [
        {
          "policyId": "uuid",
          "subject": "string",
          "resource": "string",
          "dataScope": "string"
        }
      ],
      "suggestion": "string",
      "autoFixed": boolean
    }
  ],
  "scanDuration": "duration"
}
```

**HTTP Status Codes:**
- `200 OK` - Scan completed
- `403 Forbidden` - Not admin
- `500 Internal Server Error`

---

## 4.2 Reporting APIs

### 4.2.1 Request Report

**Endpoint:** `POST /api/reports/executions`

**Method:** POST

**Request Schema:**
```json
{
  "reportDefinitionId": "uuid",
  "parameters": {
    "key": "value"
  },
  "priority": "HIGH | NORMAL | LOW",
  "outputFormat": "PDF | EXCEL | CSV"
}
```

**Response Schema:**
```json
{
  "executionId": "uuid",
  "status": "PENDING | RUNNING | COMPLETED | FAILED",
  "queuePosition": integer,
  "downloadUrl": "string",  // if COMPLETED
  "estimatedCompletionTime": "ISO8601"
}
```

**HTTP Status Codes:**
- `201 Created` - Report queued
- `200 OK` - Cached result returned
- `400 Bad Request` - Invalid parameters
- `503 Service Unavailable` - Queue full

---

### 4.2.2 Get Report Status

**Endpoint:** `GET /api/reports/executions/{executionId}`

**Method:** GET

**Response Schema:**
```json
{
  "executionId": "uuid",
  "status": "PENDING | RUNNING | COMPLETED | FAILED",
  "progress": integer,  // 0-100
  "startTime": "ISO8601",
  "completionTime": "ISO8601",
  "resultLocation": "string",
  "downloadUrl": "string",
  "rowCount": integer,
  "errorMessage": "string"
}
```

**HTTP Status Codes:**
- `200 OK` - Status retrieved
- `404 Not Found` - Execution not found

---

### 4.2.3 Download Report

**Endpoint:** `GET /api/reports/executions/{executionId}/download`

**Method:** GET

**Response:** Binary file stream

**HTTP Status Codes:**
- `200 OK` - File download
- `404 Not Found` - Report not found
- `410 Gone` - Report expired

**Headers:**
```
Content-Type: application/pdf | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet | text/csv
Content-Disposition: attachment; filename="report-{executionId}.{ext}"
```

---

## 4.3 Presence & Collaboration APIs

### 4.3.1 Join Presence

**Endpoint:** `POST /api/presence/join`

**Method:** POST

**Request Schema:**
```json
{
  "entityType": "CUSTOMER | TASK | CONTACT",
  "entityId": "uuid",
  "viewMode": "VIEWING | EDITING"
}
```

**Response Schema:**
```json
{
  "sessionId": "uuid",
  "otherUsers": [
    {
      "userId": "uuid",
      "userName": "string",
      "viewMode": "VIEWING | EDITING",
      "joinedAt": "ISO8601"
    }
  ],
  "lockInfo": {
    "locked": boolean,
    "lockedBy": "uuid",
    "lockedByName": "string",
    "lockType": "SOFT | HARD",
    "expiresAt": "ISO8601"
  }
}
```

**HTTP Status Codes:**
- `200 OK` - Joined successfully
- `403 Forbidden` - No permission on entity
- `409 Conflict` - Hard lock held by another

---

### 4.3.2 Leave Presence

**Endpoint:** `POST /api/presence/leave`

**Method:** POST

**Request Schema:**
```json
{
  "sessionId": "uuid"
}
```

**Response Schema:**
```json
{
  "success": boolean
}
```

**HTTP Status Codes:**
- `200 OK` - Left successfully
- `404 Not Found` - Session not found

---

### 4.3.3 Heartbeat

**Endpoint:** `POST /api/presence/heartbeat`

**Method:** POST

**Request Schema:**
```json
{
  "sessionId": "uuid"
}
```

**Response Schema:**
```json
{
  "success": boolean,
  "expiresAt": "ISO8601"
}
```

**HTTP Status Codes:**
- `200 OK` - Heartbeat received
- `404 Not Found` - Session expired

---

### 4.3.4 WebSocket Presence Channel

**Channel:** `/topic/presence/{entityType}/{entityId}`

**Message Types:**

**USER_JOINED:**
```json
{
  "type": "USER_JOINED",
  "userId": "uuid",
  "userName": "string",
  "viewMode": "VIEWING | EDITING",
  "timestamp": "ISO8601"
}
```

**USER_LEFT:**
```json
{
  "type": "USER_LEFT",
  "userId": "uuid",
  "timestamp": "ISO8601"
}
```

**LOCK_ACQUIRED:**
```json
{
  "type": "LOCK_ACQUIRED",
  "userId": "uuid",
  "userName": "string",
  "lockType": "SOFT | HARD",
  "expiresAt": "ISO8601"
}
```

---

## 4.4 Command Palette APIs

### 4.4.1 Search Commands

**Endpoint:** `GET /api/commands/search`

**Method:** GET

**Query Parameters:**
- `q` (required) - Search query
- `limit` (optional, default=10) - Max results

**Response Schema:**
```json
{
  "commands": [
    {
      "commandId": "uuid",
      "name": "string",
      "description": "string",
      "keywords": ["string"],
      "category": "NAVIGATION | ACTION | REPORT",
      "relevanceScore": number,
      "isFavorite": boolean,
      "isRecentlyUsed": boolean,
      "action": {
        "type": "NAVIGATE | API_CALL | CUSTOM",
        "target": "string",
        "parameters": {}
      }
    }
  ],
  "searchTime": "duration"
}
```

**HTTP Status Codes:**
- `200 OK` - Search completed
- `400 Bad Request` - Invalid query

---

### 4.4.2 Execute Command

**Endpoint:** `POST /api/commands/{commandId}/execute`

**Method:** POST

**Request Schema:**
```json
{
  "parameters": {
    "key": "value"
  }
}
```

**Response Schema:**
```json
{
  "success": boolean,
  "result": "any",
  "executionTime": "duration"
}
```

**HTTP Status Codes:**
- `200 OK` - Command executed
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Command not found

---

### 4.4.3 Add to Favorites

**Endpoint:** `POST /api/commands/{commandId}/favorite`

**Method:** POST

**Response Schema:**
```json
{
  "success": boolean
}
```

**HTTP Status Codes:**
- `200 OK` - Added to favorites
- `404 Not Found` - Command not found

---

## 4.5 Idempotency APIs

All write operations (POST, PUT, DELETE) support idempotency via header:

**Header:** `Idempotency-Key: {uuid}`

**Behavior:**
- If key exists and request matches → return cached response
- If key exists and request differs → return 409 Conflict
- If key doesn't exist → process request, cache response

**Error Response (409):**
```json
{
  "error": "IDEMPOTENCY_KEY_MISMATCH",
  "message": "Request differs from original with same key",
  "originalRequest": {
    "method": "POST",
    "path": "/api/customers",
    "timestamp": "ISO8601"
  }
}
```

---

## 4.6 Pagination Format (Standard)

All list endpoints support pagination:

**Query Parameters:**
- `page` (default=0) - Page number
- `size` (default=50, max=1000) - Page size
- `sort` (optional) - Sort field
- `direction` (optional, default=ASC) - Sort direction

**Response Wrapper:**
```json
{
  "content": [],
  "pageable": {
    "pageNumber": integer,
    "pageSize": integer,
    "sort": {
      "sorted": boolean,
      "unsorted": boolean,
      "empty": boolean
    }
  },
  "totalElements": integer,
  "totalPages": integer,
  "last": boolean,
  "first": boolean,
  "numberOfElements": integer
}
```

---

# 5. DATA DESIGN (DB SCHEMA)

## 5.1 Permission Policy Tables

### 5.1.1 permission_policies (extends existing casbin_rule)

**Purpose:** Store Casbin policies with data scope

```sql
CREATE TABLE permission_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ptype VARCHAR(10) NOT NULL,  -- 'p' or 'g'
    v0 VARCHAR(255),  -- subject (role/user)
    v1 VARCHAR(255),  -- tenant_id
    v2 VARCHAR(255),  -- resource
    v3 VARCHAR(255),  -- action
    v4 VARCHAR(50),   -- data_scope (NEW)
    v5 TEXT,          -- conditions JSON (NEW)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,  -- optimistic locking

    CONSTRAINT uk_policy UNIQUE (v0, v1, v2, v3, v4)
);

-- Indexes
CREATE INDEX idx_policy_tenant ON permission_policies(v1);
CREATE INDEX idx_policy_subject ON permission_policies(v0, v1);
CREATE INDEX idx_policy_resource ON permission_policies(v2, v1);
CREATE INDEX idx_policy_scope ON permission_policies(v4, v1);
```

**Columns:**
- `id`: UUID primary key
- `ptype`: Policy type ('p' for permission, 'g' for grouping)
- `v0-v3`: Standard Casbin fields (subject, tenant, resource, action)
- `v4`: **NEW** - Data scope enum (ALL, DEPARTMENT, SELF_ONLY)
- `v5`: **NEW** - Additional conditions as JSON
- `created_at`, `updated_at`: Audit timestamps
- `version`: Optimistic locking version

**Constraints:**
- `uk_policy`: Unique constraint on (subject, tenant, resource, action, scope)

---

### 5.1.2 policy_conflicts

**Purpose:** Store detected policy conflicts

```sql
CREATE TABLE policy_conflicts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    conflict_type VARCHAR(50) NOT NULL,  -- SCOPE_MISMATCH, AMBIGUOUS_INHERITANCE, etc.
    severity VARCHAR(20) NOT NULL,  -- HIGH, MEDIUM, LOW
    affected_policy_ids UUID[] NOT NULL,
    resolution_suggestion TEXT,
    detected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    resolved_by UUID,
    auto_resolved BOOLEAN DEFAULT FALSE,

    CONSTRAINT chk_severity CHECK (severity IN ('HIGH', 'MEDIUM', 'LOW'))
);

CREATE INDEX idx_conflict_tenant ON policy_conflicts(tenant_id, resolved_at);
CREATE INDEX idx_conflict_severity ON policy_conflicts(severity, resolved_at);
```

---

### 5.1.3 policy_audit_logs

**Purpose:** Immutable audit trail for policy changes

```sql
CREATE TABLE policy_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    operation VARCHAR(20) NOT NULL,  -- CREATE, UPDATE, DELETE, CONFLICT_SCAN
    policy_id UUID,
    performed_by UUID NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    before_state JSONB,
    after_state JSONB,
    change_reason TEXT,
    ip_address INET,

    CONSTRAINT chk_operation CHECK (operation IN ('CREATE', 'UPDATE', 'DELETE', 'CONFLICT_SCAN'))
);

CREATE INDEX idx_audit_tenant_time ON policy_audit_logs(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_policy ON policy_audit_logs(policy_id);
CREATE INDEX idx_audit_user ON policy_audit_logs(performed_by, timestamp DESC);

-- Prevent updates/deletes (append-only)
CREATE RULE no_update_audit AS ON UPDATE TO policy_audit_logs DO INSTEAD NOTHING;
CREATE RULE no_delete_audit AS ON DELETE TO policy_audit_logs DO INSTEAD NOTHING;
```

---

## 5.2 Reporting Tables

### 5.2.1 report_definitions

**Purpose:** Store reusable report templates

```sql
CREATE TABLE report_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    query_template TEXT NOT NULL,  -- SQL with {{param}} placeholders
    parameters JSONB NOT NULL,  -- parameter schema
    output_format VARCHAR(20) DEFAULT 'EXCEL',
    cache_duration INTERVAL DEFAULT '1 hour',
    access_roles TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,

    CONSTRAINT uk_report_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_report_tenant ON report_definitions(tenant_id, enabled);
```

---

### 5.2.2 report_executions

**Purpose:** Track report execution history and results

```sql
CREATE TABLE report_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    report_definition_id UUID NOT NULL REFERENCES report_definitions(id),
    requested_by UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(10) DEFAULT 'NORMAL',
    parameters JSONB NOT NULL,
    parameters_hash VARCHAR(64) NOT NULL,  -- SHA-256 for cache lookup
    start_time TIMESTAMP,
    completion_time TIMESTAMP,
    result_location TEXT,  -- S3/MinIO URL
    download_url TEXT,  -- presigned URL
    row_count BIGINT,
    file_size BIGINT,
    error_message TEXT,
    query_time INTERVAL,
    render_time INTERVAL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_priority CHECK (priority IN ('HIGH', 'NORMAL', 'LOW'))
);

CREATE INDEX idx_execution_tenant_status ON report_executions(tenant_id, status, priority, created_at);
CREATE INDEX idx_execution_cache_lookup ON report_executions(tenant_id, report_definition_id, parameters_hash, completion_time DESC);
CREATE INDEX idx_execution_user ON report_executions(requested_by, created_at DESC);

-- Partitioning by created_at (monthly)
CREATE TABLE report_executions_2025_01 PARTITION OF report_executions
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
-- Add more partitions as needed
```

---

## 5.3 Presence & Collaboration Tables

### 5.3.1 user_presences

**Purpose:** Track active user sessions and viewing activity

```sql
CREATE TABLE user_presences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    session_id UUID NOT NULL,
    connected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_heartbeat TIMESTAMP NOT NULL DEFAULT NOW(),
    device VARCHAR(255),

    CONSTRAINT uk_user_session UNIQUE (user_id, tenant_id, session_id)
);

CREATE INDEX idx_presence_heartbeat ON user_presences(tenant_id, last_heartbeat);
CREATE INDEX idx_presence_user ON user_presences(user_id, tenant_id);

-- Auto-cleanup stale presences (older than 5 minutes)
CREATE INDEX idx_presence_cleanup ON user_presences(last_heartbeat) WHERE last_heartbeat < NOW() - INTERVAL '5 minutes';
```

---

### 5.3.2 presence_entity_views

**Purpose:** Track which entities users are viewing/editing

```sql
CREATE TABLE presence_entity_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_presence_id UUID NOT NULL REFERENCES user_presences(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    view_mode VARCHAR(20) NOT NULL,  -- VIEWING, EDITING
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_presence_entity UNIQUE (user_presence_id, entity_type, entity_id),
    CONSTRAINT chk_view_mode CHECK (view_mode IN ('VIEWING', 'EDITING'))
);

CREATE INDEX idx_entity_viewers ON presence_entity_views(entity_type, entity_id, view_mode);
```

---

### 5.3.3 collaboration_locks

**Purpose:** Manage entity-level edit locks

```sql
CREATE TABLE collaboration_locks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    locked_by UUID NOT NULL,
    lock_type VARCHAR(10) NOT NULL,  -- SOFT, HARD
    acquired_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    metadata JSONB,

    CONSTRAINT uk_entity_lock UNIQUE (entity_type, entity_id),
    CONSTRAINT chk_lock_type CHECK (lock_type IN ('SOFT', 'HARD'))
);

CREATE INDEX idx_lock_expiry ON collaboration_locks(expires_at);
CREATE INDEX idx_lock_user ON collaboration_locks(locked_by, tenant_id);

-- Auto-cleanup expired locks
CREATE INDEX idx_lock_cleanup ON collaboration_locks(expires_at) WHERE expires_at < NOW();
```

---

## 5.4 Command Palette Tables

### 5.4.1 commands

**Purpose:** Store command definitions for palette

```sql
CREATE TABLE commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    keywords TEXT[],
    category VARCHAR(50) NOT NULL,
    required_permission JSONB,  -- {resource, action}
    action_type VARCHAR(20) NOT NULL,  -- NAVIGATE, API_CALL, CUSTOM
    action_target TEXT NOT NULL,
    action_parameters JSONB,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_command_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_action_type CHECK (action_type IN ('NAVIGATE', 'API_CALL', 'CUSTOM'))
);

CREATE INDEX idx_command_tenant ON commands(tenant_id, enabled);
CREATE INDEX idx_command_search ON commands USING GIN (to_tsvector('english', name || ' ' || array_to_string(keywords, ' ')));
```

---

### 5.4.2 user_command_history

**Purpose:** Track user command usage for personalization

```sql
CREATE TABLE user_command_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    command_id UUID NOT NULL REFERENCES commands(id) ON DELETE CASCADE,
    executed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    execution_time INTERVAL,
    success BOOLEAN,

    CONSTRAINT uk_user_tenant UNIQUE (user_id, tenant_id)
);

CREATE INDEX idx_history_user_recent ON user_command_history(user_id, executed_at DESC);
CREATE INDEX idx_history_command ON user_command_history(command_id, executed_at DESC);

-- Circular buffer: keep only last 50 per user
-- (enforced in application logic)
```

---

### 5.4.3 user_favorite_commands

**Purpose:** Store user's favorite commands

```sql
CREATE TABLE user_favorite_commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    command_id UUID NOT NULL REFERENCES commands(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_user_favorite UNIQUE (user_id, tenant_id, command_id)
);

CREATE INDEX idx_favorite_user ON user_favorite_commands(user_id, tenant_id);
```

---

## 5.5 Idempotency Storage (Redis)

**Redis Keys:**
```
idempotency:{tenantId}:{key} -> JSON
{
  "requestHash": "sha256",
  "response": "serialized",
  "timestamp": "ISO8601"
}
```

**TTL:** 24 hours

**Example:**
```
idempotency:tenant-123:550e8400-e29b-41d4-a716-446655440000
{
  "requestHash": "abc123...",
  "response": "{\"id\":\"...\",\"status\":\"success\"}",
  "timestamp": "2025-12-08T10:00:00Z"
}
```

---

## 5.6 Materialized Views for Reports (Performance)

### 5.6.1 mv_customer_summary

**Purpose:** Pre-aggregated customer metrics

```sql
CREATE MATERIALIZED VIEW mv_customer_summary AS
SELECT
    c.id,
    c.tenant_id,
    c.name,
    c.status,
    COUNT(DISTINCT co.id) as contact_count,
    COUNT(DISTINCT t.id) as task_count,
    COUNT(DISTINCT a.id) as activity_count,
    SUM(t.estimated_hours) as total_estimated_hours,
    MAX(a.activity_date) as last_activity_date,
    c.created_at,
    c.updated_at
FROM customers c
LEFT JOIN contacts co ON c.id = co.customer_id
LEFT JOIN tasks t ON c.id = t.customer_id
LEFT JOIN activities a ON c.id = a.related_to_id AND a.related_to_type = 'CUSTOMER'
GROUP BY c.id, c.tenant_id, c.name, c.status, c.created_at, c.updated_at;

CREATE UNIQUE INDEX idx_customer_summary_pk ON mv_customer_summary(id);
CREATE INDEX idx_customer_summary_tenant ON mv_customer_summary(tenant_id);

-- Refresh strategy: every 30 minutes
CREATE OR REPLACE FUNCTION refresh_customer_summary()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_summary;
END;
$$ LANGUAGE plpgsql;

-- Scheduled refresh (via pg_cron or application)
```

---

### 5.6.2 mv_task_metrics

**Purpose:** Pre-aggregated task metrics

```sql
CREATE MATERIALIZED VIEW mv_task_metrics AS
SELECT
    t.id,
    t.tenant_id,
    t.title,
    t.status,
    t.priority,
    t.assigned_to,
    COUNT(DISTINCT c.id) as comment_count,
    COUNT(DISTINCT cl.id) as checklist_item_count,
    SUM(CASE WHEN cl.completed THEN 1 ELSE 0 END) as completed_checklist_items,
    t.estimated_hours,
    t.actual_hours,
    t.created_at,
    t.due_date
FROM tasks t
LEFT JOIN task_comments c ON t.id = c.task_id
LEFT JOIN checklist_items cl ON t.id = cl.task_id
GROUP BY t.id, t.tenant_id, t.title, t.status, t.priority, t.assigned_to,
         t.estimated_hours, t.actual_hours, t.created_at, t.due_date;

CREATE UNIQUE INDEX idx_task_metrics_pk ON mv_task_metrics(id);
CREATE INDEX idx_task_metrics_tenant_status ON mv_task_metrics(tenant_id, status);
CREATE INDEX idx_task_metrics_assigned ON mv_task_metrics(assigned_to, status);
```

---

## 5.7 Audit Columns (Standard)

All new tables include standard audit columns:

```sql
created_at TIMESTAMP NOT NULL DEFAULT NOW(),
updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
created_by UUID,
updated_by UUID,
deleted_at TIMESTAMP,  -- soft delete
```

**Trigger for updated_at:**
```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to each table
CREATE TRIGGER update_permission_policies_updated_at
    BEFORE UPDATE ON permission_policies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

---

## 5.8 Partitioning Strategy

### 5.8.1 Time-Based Partitioning (report_executions)

**Partition by month:**
```sql
CREATE TABLE report_executions (
    -- columns...
) PARTITION BY RANGE (created_at);

-- Create partitions
CREATE TABLE report_executions_2025_01 PARTITION OF report_executions
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE report_executions_2025_02 PARTITION OF report_executions
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

-- Add indexes to each partition
CREATE INDEX idx_execution_2025_01_status ON report_executions_2025_01(status);
```

**Retention Policy:**
- Keep completed reports for 90 days
- Archive to cold storage after 30 days
- Delete after 90 days

---

## 5.9 Indexing Strategy

### 5.9.1 Multi-Tenant Indexes

All tenant-scoped tables have:
```sql
CREATE INDEX idx_{table}_tenant ON {table}(tenant_id);
CREATE INDEX idx_{table}_tenant_created ON {table}(tenant_id, created_at DESC);
```

### 5.9.2 Composite Indexes

For frequent query patterns:
```sql
-- Permission checks
CREATE INDEX idx_policy_check ON permission_policies(v0, v1, v2, v3, v4);

-- Report cache lookup
CREATE INDEX idx_report_cache ON report_executions(
    tenant_id, report_definition_id, parameters_hash, completion_time DESC
) WHERE status = 'COMPLETED';

-- Presence lookups
CREATE INDEX idx_presence_active ON user_presences(tenant_id, last_heartbeat)
WHERE last_heartbeat > NOW() - INTERVAL '5 minutes';
```

### 5.9.3 Full-Text Search Indexes

For command palette search:
```sql
CREATE INDEX idx_command_fulltext ON commands
USING GIN (to_tsvector('english', name || ' ' || description || ' ' || array_to_string(keywords, ' ')));
```

---

# 6. API–MODEL–DB MAPPING MATRIX

## 6.1 Permission APIs Mapping

| API Operation | Domain Aggregate | Entity/Value Object | Table / Columns |
|---------------|------------------|----------------------|------------------|
| POST /api/permissions/check-with-scope | PermissionPolicy | PolicyRule, DataScopeEnum | permission_policies (v0-v5) |
| POST /api/permissions/policies | PermissionPolicy | PolicyRule, PolicyConflict | permission_policies + policy_conflicts |
| GET /api/permissions/policies | PermissionPolicy | PolicyRule | permission_policies (filtered by tenant) |
| DELETE /api/permissions/policies/{id} | PermissionPolicy | PolicyRule, PolicyAuditLog | permission_policies + policy_audit_logs |
| POST /api/permissions/conflicts/scan | PermissionPolicy | PolicyConflict | policy_conflicts + permission_policies (join) |
| GET /api/permissions/audit-logs | PermissionPolicy | PolicyAuditLog | policy_audit_logs (paginated) |

**Data Flow:**
1. API → Controller → Service → Repository
2. Service performs business logic (conflict detection)
3. Repository queries permission_policies + policy_conflicts
4. Audit logged to policy_audit_logs

---

## 6.2 Reporting APIs Mapping

| API Operation | Domain Aggregate | Entity/Value Object | Table / Columns |
|---------------|------------------|----------------------|------------------|
| POST /api/reports/executions | ReportExecution | ReportExecution, ExecutionMetrics | report_executions (create) |
| GET /api/reports/executions/{id} | ReportExecution | ReportExecution, ExecutionMetrics | report_executions (by id) |
| GET /api/reports/executions | ReportExecution | ReportExecution | report_executions (paginated by user) |
| GET /api/reports/executions/{id}/download | ReportExecution | Binary stream | report_executions.result_location → MinIO |
| POST /api/reports/definitions | ReportDefinition | ReportDefinition, ReportParameter | report_definitions (create) |
| GET /api/reports/definitions | ReportDefinition | ReportDefinition | report_definitions (list) |

**Data Flow:**
1. API → Controller → Service → Queue (Redis/RabbitMQ)
2. Background worker picks from queue
3. Worker executes query against materialized views (mv_customer_summary, mv_task_metrics)
4. Worker renders output (PDF/Excel) and uploads to MinIO
5. Worker updates report_executions.status and result_location
6. WebSocket notification sent to user

---

## 6.3 Presence APIs Mapping

| API Operation | Domain Aggregate | Entity/Value Object | Table / Columns |
|---------------|------------------|----------------------|------------------|
| POST /api/presence/join | UserPresence | UserPresence, Session, EntityReference | user_presences + presence_entity_views |
| POST /api/presence/leave | UserPresence | UserPresence | user_presences (delete) |
| POST /api/presence/heartbeat | UserPresence | Session | user_presences.last_heartbeat (update) |
| GET /api/presence/entity/{type}/{id} | UserPresence | List<UserPresenceInfo> | user_presences + presence_entity_views (join) |
| POST /api/presence/lock | CollaborationLock | CollaborationLock | collaboration_locks (create) |
| DELETE /api/presence/lock/{id} | CollaborationLock | CollaborationLock | collaboration_locks (delete) |

**Data Flow:**
1. API → Controller → Service → Repository
2. Service queries user_presences + presence_entity_views (join)
3. WebSocket broadcasts presence events to `/topic/presence/{entityType}/{entityId}`
4. Background job cleans up stale presences (heartbeat < 5 min)

---

## 6.4 Command Palette APIs Mapping

| API Operation | Domain Aggregate | Entity/Value Object | Table / Columns |
|---------------|------------------|----------------------|------------------|
| GET /api/commands/search | CommandRegistry | Command, CommandResult | commands (full-text search) + user_favorite_commands + user_command_history |
| POST /api/commands/{id}/execute | Command | CommandExecution | user_command_history (record) |
| POST /api/commands/{id}/favorite | UserCommandHistory | FavoriteCommand | user_favorite_commands (create) |
| GET /api/commands/favorites | UserCommandHistory | List<Command> | user_favorite_commands + commands (join) |
| GET /api/commands/recent | UserCommandHistory | List<Command> | user_command_history + commands (join, ORDER BY executed_at DESC LIMIT 10) |

**Data Flow:**
1. API → Controller → Service (cached)
2. Service loads commands from cache (5 min TTL)
3. Service filters by user permissions (Casbin check)
4. Service scores by relevance + recent usage + favorites
5. Returns top N results

---

## 6.5 Transaction Integrity Mapping

| API Operation | Domain Aggregate | Entity/Value Object | Table / Columns |
|---------------|------------------|----------------------|------------------|
| POST /api/customers (idempotent) | Customer | IdempotencyKey | customers (if not exists in Redis cache) + Redis idempotency cache |
| PUT /api/customers/{id} (versioned) | Customer | OptimisticLock (version) | customers.version (compare-and-swap) |
| Any retryable operation | N/A | RetryMetadata | Application-level (not persisted) |

**Data Flow:**
1. API → Controller → IdempotencyService.executeIdempotent()
2. IdempotencyService checks Redis: `idempotency:{tenantId}:{key}`
3. If cache hit → return cached response
4. If cache miss → execute operation, cache result (24h TTL)
5. For versioned updates: check `customers.version` matches, increment on success

---

## 6.6 Enhanced Existing Aggregates

| API Operation | Domain Aggregate | Entity/Value Object | Table / Columns |
|---------------|------------------|----------------------|------------------|
| PUT /api/customers/{id} | Customer (enhanced) | OptimisticLock | customers.version (NEW) + customers.updated_by (NEW) |
| PUT /api/tasks/{id} | Task (enhanced) | OptimisticLock | tasks.version (NEW) + tasks.updated_by (NEW) |
| PUT /api/users/{id} | User (enhanced) | OptimisticLock | users.version (NEW) + users.updated_by (NEW) |

**Schema Changes (Existing Tables):**
```sql
ALTER TABLE customers ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE tasks ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE users ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE customers ADD COLUMN updated_by UUID;
ALTER TABLE tasks ADD COLUMN updated_by UUID;
ALTER TABLE users ADD COLUMN updated_by UUID;
```

---

# 7. CROSS-CUTTING DESIGN

## 7.1 Logging Design

### 7.1.1 Log Levels

| Level | Usage |
|-------|-------|
| TRACE | Detailed flow tracking (disabled in prod) |
| DEBUG | Permission checks, cache hits, query execution |
| INFO | API requests, report generation, presence events |
| WARN | Retry attempts, soft conflicts, performance warnings |
| ERROR | Failed operations, exceptions, hard conflicts |

### 7.1.2 Structured Logging Format

**JSON Format:**
```json
{
  "timestamp": "ISO8601",
  "level": "INFO",
  "logger": "com.neobrutalism.crm.service.PermissionService",
  "message": "Permission check completed",
  "context": {
    "userId": "uuid",
    "tenantId": "string",
    "resource": "/api/customers",
    "action": "READ",
    "granted": true,
    "dataScope": "DEPARTMENT",
    "duration": 15
  },
  "traceId": "uuid"  // for distributed tracing
}
```

### 7.1.3 Log Aggregation

- **ELK Stack:** Elasticsearch + Logstash + Kibana
- **Retention:** 30 days hot storage, 90 days cold storage
- **Alerting:** ERROR logs trigger PagerDuty/Slack alerts

---

## 7.2 Monitoring Metrics

### 7.2.1 Application Metrics (Prometheus)

**Permission Service:**
```
permission_checks_total{granted="true|false", scope="ALL|DEPARTMENT|SELF_ONLY"}
permission_check_duration_seconds{quantile="0.5|0.9|0.99"}
policy_conflicts_detected_total{severity="HIGH|MEDIUM|LOW"}
```

**Reporting Service:**
```
report_executions_total{status="COMPLETED|FAILED"}
report_execution_duration_seconds{quantile="0.5|0.9|0.99"}
report_queue_size{priority="HIGH|NORMAL|LOW"}
report_cache_hit_ratio
```

**Presence Service:**
```
active_users_gauge
presence_sessions_total
collaboration_locks_active{lock_type="SOFT|HARD"}
heartbeat_failures_total
```

**Command Palette:**
```
command_searches_total
command_executions_total{success="true|false"}
command_search_duration_seconds{quantile="0.5|0.9|0.99"}
```

**Transaction Integrity:**
```
idempotency_cache_hits_total
idempotency_cache_misses_total
optimistic_lock_conflicts_total
retry_attempts_total{result="success|failure"}
```

### 7.2.2 Infrastructure Metrics

- **Database:** Connection pool usage, query duration, deadlocks
- **Redis:** Memory usage, eviction rate, key count
- **WebSocket:** Active connections, messages/sec, disconnects
- **MinIO:** Object count, storage usage, upload/download latency

---

## 7.3 Error Codes Standardization

### 7.3.1 Error Code Format

`{DOMAIN}-{CODE}-{HTTP_STATUS}`

Example: `PERM-1001-403`

### 7.3.2 Error Code Catalog

**Permission Errors (PERM-xxxx):**
- `PERM-1001-403`: Insufficient permissions
- `PERM-1002-409`: Policy conflict detected
- `PERM-1003-400`: Invalid data scope
- `PERM-1004-404`: Policy not found

**Reporting Errors (REPORT-xxxx):**
- `REPORT-2001-503`: Queue full
- `REPORT-2002-400`: Invalid parameters
- `REPORT-2003-500`: Query execution failed
- `REPORT-2004-410`: Report expired

**Presence Errors (PRESENCE-xxxx):**
- `PRESENCE-3001-409`: Entity locked by another user
- `PRESENCE-3002-403`: No permission on entity
- `PRESENCE-3003-404`: Session not found
- `PRESENCE-3004-408`: Heartbeat timeout

**Command Palette Errors (COMMAND-xxxx):**
- `COMMAND-4001-404`: Command not found
- `COMMAND-4002-403`: Insufficient permission for command
- `COMMAND-4003-400`: Invalid command parameters

**Idempotency Errors (IDEMPOTENCY-xxxx):**
- `IDEMPOTENCY-5001-409`: Key mismatch
- `IDEMPOTENCY-5002-400`: Invalid key format

### 7.3.3 Error Response Format

```json
{
  "error": {
    "code": "PERM-1001-403",
    "message": "Insufficient permissions to access resource",
    "details": {
      "resource": "/api/customers/123",
      "action": "UPDATE",
      "requiredScope": "DEPARTMENT",
      "userScope": "SELF_ONLY"
    },
    "timestamp": "ISO8601",
    "traceId": "uuid",
    "helpUrl": "https://docs.example.com/errors/PERM-1001"
  }
}
```

---

## 7.4 Security Model

### 7.4.1 Authentication

**Existing JWT mechanism (enhanced):**
- Access token: 1 hour expiry
- Refresh token: 7 days expiry
- Token rotation on refresh
- Account lockout: 5 failed attempts, 30 min lockout

**No changes to existing auth**

### 7.4.2 Authorization (Enhanced)

**Permission Check Flow:**
```
1. Extract JWT from Authorization header
2. Validate token signature and expiry
3. Extract userId and tenantId from claims
4. Call PermissionService.checkPermissionWithDataScope()
5. Apply data scope filter to query
6. Return filtered results
```

**Data Scope Enforcement:**
```java
// Example: Customer query with data scope
@GetMapping("/api/customers")
public Page<Customer> getCustomers(Pageable pageable, Principal principal) {
    UUID userId = extractUserId(principal);
    String tenantId = extractTenantId(principal);

    // Check permission with data scope
    PermissionCheckResponse perm = permissionService.checkPermissionWithDataScope(
        userId, tenantId, "/api/customers", "READ", null, null
    );

    if (!perm.isGranted()) {
        throw new ForbiddenException("PERM-1001-403");
    }

    // Apply data scope to query
    Specification<Customer> spec = switch (perm.getAppliedScope()) {
        case ALL -> Specification.where(null);
        case DEPARTMENT -> CustomerSpecifications.inDepartment(user.getDepartment());
        case SELF_ONLY -> CustomerSpecifications.ownedBy(userId);
    };

    return customerRepository.findAll(spec, pageable);
}
```

### 7.4.3 RBAC + ABAC Hybrid

**Casbin Model (Enhanced):**
```ini
[request_definition]
r = sub, dom, obj, act, scope

[policy_definition]
p = sub, dom, obj, act, scope, eft

[role_definition]
g = _, _, _

[policy_effect]
e = some(where (p.eft == allow))

[matchers]
m = g(r.sub, p.sub, r.dom) && r.dom == p.dom && keyMatch(r.obj, p.obj) && regexMatch(r.act, p.act) && r.scope >= p.scope
```

**Scope Hierarchy:**
```
ALL (3) > DEPARTMENT (2) > SELF_ONLY (1)
```

User must have scope >= policy scope to access

---

## 7.5 Transaction Boundaries

### 7.5.1 Transaction Isolation Levels

| Service | Operation | Isolation Level | Reason |
|---------|-----------|-----------------|--------|
| PermissionService | checkPermission() | READ_COMMITTED | Prevent dirty reads of policies |
| PermissionService | addPolicy() | SERIALIZABLE | Prevent concurrent policy conflicts |
| ReportService | requestReport() | READ_COMMITTED | Standard read consistency |
| ReportWorker | executeReport() | REPEATABLE_READ | Prevent phantom reads during long query |
| PresenceService | joinPresence() | READ_UNCOMMITTED | Acceptable for ephemeral presence data |
| CollaborationService | acquireLock() | SERIALIZABLE | Prevent double-lock acquisition |
| CommandService | executeCommand() | READ_COMMITTED | Standard |
| IdempotencyService | cacheResult() | N/A (Redis atomic) | Redis single-threaded |

### 7.5.2 Transaction Scope

**Rule:** One aggregate = One transaction

**Examples:**
```java
// CORRECT: Single aggregate transaction
@Transactional(isolation = Isolation.READ_COMMITTED)
public Customer updateCustomer(UUID id, CustomerDTO dto) {
    Customer customer = repository.findById(id).orElseThrow();
    customer.update(dto);
    return repository.save(customer);
}

// WRONG: Multiple aggregates in one transaction
@Transactional
public void updateCustomerAndTasks(UUID customerId, CustomerDTO dto, List<TaskDTO> tasks) {
    customerRepository.save(...);  // Customer aggregate
    taskRepository.saveAll(...);    // Task aggregate - WRONG!
}

// CORRECT: Saga pattern for multiple aggregates
public void updateCustomerAndTasks(UUID customerId, CustomerDTO dto, List<TaskDTO> tasks) {
    // Step 1: Update customer
    Customer customer = customerService.update(customerId, dto);

    // Step 2: Publish event
    eventPublisher.publish(new CustomerUpdatedEvent(customer));

    // Step 3: Event handler updates tasks (separate transaction)
}
```

---

## 7.6 Idempotency Strategy

### 7.6.1 Idempotent Operations

**All write operations support idempotency via header:**

```http
POST /api/customers
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "name": "Acme Corp",
  ...
}
```

### 7.6.2 Implementation Pattern

```java
@PostMapping("/api/customers")
public ResponseEntity<Customer> createCustomer(
    @RequestHeader(value = "Idempotency-Key", required = false) String key,
    @RequestBody CustomerDTO dto,
    Principal principal
) {
    String tenantId = extractTenantId(principal);

    // If idempotency key provided, use it
    if (key != null) {
        IdempotentResponse<Customer> response = idempotencyService.executeIdempotent(
            new IdempotentRequest<>(
                key,
                () -> customerService.create(dto, tenantId),
                Duration.ofHours(24)
            )
        );

        if (response.isFromCache()) {
            return ResponseEntity.ok()
                .header("X-Idempotent-Replay", "true")
                .body(response.getResult());
        }

        return ResponseEntity.status(201).body(response.getResult());
    }

    // Normal execution without idempotency
    Customer customer = customerService.create(dto, tenantId);
    return ResponseEntity.status(201).body(customer);
}
```

### 7.6.3 Cache Storage

**Redis Key Structure:**
```
idempotency:{tenantId}:{key} -> {
  "requestHash": "sha256",
  "response": "serialized",
  "timestamp": "ISO8601"
}
```

**TTL:** 24 hours

**Request Hash:** SHA-256 of (method + path + body)

---

## 7.7 Caching Strategy

### 7.7.1 Cache Layers

**L1 Cache (Application - Caffeine):**
- Command definitions: 5 min TTL
- User permissions: 5 min TTL
- Report definitions: 10 min TTL

**L2 Cache (Redis):**
- Report results: 1-24 hours TTL (configurable per report)
- Idempotency keys: 24 hours TTL
- Session data: 30 min TTL

**L3 Cache (Database - Materialized Views):**
- Customer summary: Refresh every 30 min
- Task metrics: Refresh every 15 min

### 7.7.2 Cache Invalidation

**Event-Driven Invalidation:**
```java
@EventListener
public void onPolicyChanged(PolicyChangedEvent event) {
    // Invalidate permission cache for affected users
    cacheManager.getCache("permissions")
        .evict(event.getTenantId());
}

@EventListener
public void onReportDefinitionUpdated(ReportDefinitionUpdatedEvent event) {
    // Invalidate all cached reports for this definition
    reportExecutionRepository.invalidateCacheForDefinition(event.getDefinitionId());
}
```

**Time-Based Refresh:**
```java
@Scheduled(fixedRate = 1800000) // 30 minutes
public void refreshMaterializedViews() {
    jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_summary");
    jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_task_metrics");
}
```

---

## 7.8 Event Publishing

### 7.8.1 Domain Events

**Event Types:**
- `PolicyRuleCreatedEvent`
- `PolicyConflictDetectedEvent`
- `ReportCompletedEvent`
- `UserPresenceJoinedEvent`
- `CollaborationLockAcquiredEvent`
- `CommandExecutedEvent`

**Event Structure:**
```java
public abstract class DomainEvent {
    private UUID eventId;
    private String eventType;
    private Instant timestamp;
    private String tenantId;
    private Map<String, Object> payload;
}
```

### 7.8.2 Event Publishing Channels

**Internal (Application Event Bus):**
- Cache invalidation
- Saga orchestration
- Audit logging

**External (WebSocket):**
- Real-time notifications
- Presence updates
- Report completion alerts

**Example:**
```java
@Service
public class ReportService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private WebSocketService webSocketService;

    public void completeReport(UUID executionId) {
        // ... complete report logic

        // Publish internal event (for cache, audit)
        eventPublisher.publishEvent(new ReportCompletedEvent(execution));

        // Publish external event (for user notification)
        webSocketService.sendToUser(
            execution.getRequestedBy(),
            "/queue/notifications",
            new NotificationMessage("Report completed", execution.getId())
        );
    }
}
```

---

## 7.9 API Versioning

**Strategy:** URL-based versioning

**Format:** `/api/v{version}/{resource}`

**Example:**
```
/api/v1/permissions/policies  (current)
/api/v2/permissions/policies  (future - breaking changes)
```

**Backward Compatibility:**
- v1 supported for 12 months after v2 release
- Deprecation headers: `Deprecation: true`, `Sunset: 2026-01-01`

---

## 7.10 Rate Limiting

### 7.10.1 Rate Limit Tiers

| Tier | Requests/Minute | Burst | Apply To |
|------|-----------------|-------|----------|
| Anonymous | 10 | 20 | Unauthenticated requests |
| Standard | 100 | 200 | Regular users |
| Premium | 500 | 1000 | Premium users |
| Admin | 1000 | 2000 | Admin users |

### 7.10.2 Implementation (Redis + Bucket4j)

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String userId = extractUserId(request);
        String key = "rate-limit:" + userId;

        Bucket bucket = bucketResolver.resolve(userId);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining",
                String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            response.setStatus(429);
            response.addHeader("X-Rate-Limit-Retry-After",
                String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            return false;
        }
    }
}
```

---

## 7.11 Health Checks

### 7.11.1 Endpoints

**Liveness:** `GET /actuator/health/liveness`
- Checks: Application running, no deadlocks

**Readiness:** `GET /actuator/health/readiness`
- Checks: Database connection, Redis connection, Casbin loaded

**Custom Health Indicators:**
```java
@Component
public class CasbinHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        if (enforcer.isLoaded()) {
            return Health.up()
                .withDetail("policies", enforcer.getPolicyCount())
                .build();
        }
        return Health.down()
            .withDetail("error", "Casbin policies not loaded")
            .build();
    }
}
```

---

## 7.12 Distributed Tracing

**OpenTelemetry Integration:**
```yaml
spring:
  sleuth:
    enabled: true
    sampler:
      probability: 1.0  # 100% sampling in dev, 10% in prod
  zipkin:
    base-url: http://zipkin:9411
```

**Trace Context Propagation:**
- Trace ID injected into logs
- Trace ID passed via HTTP headers (`X-B3-TraceId`)
- Trace spans created for: API calls, database queries, cache hits, event publishing

---

# 8. IMPLEMENTATION SEQUENCING

## 8.1 Phase 0: Week 1 (Foundation)

**Deliverables:**
- #30 Automated Dependency Updates (Renovate setup)

**Database Changes:** None
**API Changes:** None
**Risk:** Zero

---

## 8.2 Phase 1: Weeks 2-6 (Quick Wins + Foundation)

**Deliverables:**
- #19 Command Palette (Quick Win version)
- #3 Transaction Integrity (Quick Win version)

**Database Changes:**
- Add `commands` table
- Add `user_command_history` table
- Add `version` column to existing tables (customers, tasks, users)
- Set up Redis for idempotency keys

**API Changes:**
- `GET /api/commands/search`
- `POST /api/commands/{id}/execute`
- Add `Idempotency-Key` header support to write endpoints

**Risk:** Low (isolated features)

---

## 8.3 Phase 2: Weeks 7-12 (Security Foundation)

**Deliverables:**
- #1 Granular Authorization (Full version)

**Database Changes:**
- Extend `permission_policies` with `v4` (data_scope) and `v5` (conditions)
- Create `policy_audit_logs` table
- Add indexes on (tenant, subject, resource, action, scope)

**API Changes:**
- `POST /api/permissions/check-with-scope`
- `POST /api/permissions/policies` (with dataScope)
- Enhance existing permission check APIs

**Risk:** Medium (touches core security)

---

## 8.4 Phase 3: Weeks 13-18 (Security Intelligence)

**Deliverables:**
- #4 Policy Conflict Detection

**Database Changes:**
- Create `policy_conflicts` table

**API Changes:**
- `POST /api/permissions/conflicts/scan`
- `GET /api/permissions/conflicts`

**Risk:** Low (read-only analysis)

---

## 8.5 Phase 4: Weeks 19-24 (Business Intelligence)

**Deliverables:**
- #2 High-Performance Reporting (Full version)

**Database Changes:**
- Create `report_definitions` table
- Create `report_executions` table (partitioned)
- Create materialized views (`mv_customer_summary`, `mv_task_metrics`)

**API Changes:**
- `POST /api/reports/executions`
- `GET /api/reports/executions/{id}`
- `GET /api/reports/executions/{id}/download`
- WebSocket: `/topic/reports/{userId}`

**Risk:** Medium (performance optimization critical)

---

## 8.6 Phase 5: Weeks 25-30 (Real-Time Collaboration)

**Deliverables:**
- #9 Real-Time Collaboration (Full version)

**Database Changes:**
- Create `user_presences` table
- Create `presence_entity_views` table
- Create `collaboration_locks` table

**API Changes:**
- `POST /api/presence/join`
- `POST /api/presence/leave`
- `POST /api/presence/heartbeat`
- WebSocket: `/topic/presence/{entityType}/{entityId}`

**Risk:** Medium (WebSocket scaling)

---

# 9. CONCLUSION

This architecture design document provides a comprehensive blueprint for implementing 7 strategic enhancements to the Neobrutalism CRM over a 30-week period. The design:

✅ **Leverages Existing Infrastructure:** Builds on Casbin, WebSocket, Spring Batch, React Query
✅ **Follows SOLID Principles:** Clear aggregate boundaries, single responsibility
✅ **Minimizes Risk:** Incremental delivery, quick wins before strategic bets
✅ **Maintains Consistency:** Standard patterns for pagination, error handling, caching
✅ **Enables Scale:** Partitioning, materialized views, Redis caching
✅ **Ensures Security:** RBAC+ABAC, data scope enforcement, audit trails
✅ **Supports Monitoring:** Structured logging, Prometheus metrics, distributed tracing

**Ready for Implementation:** Week 1 can start immediately with zero-risk dependency automation.

---

**Document Status:** ✅ Complete and Ready for Development
**Next Steps:** Review with technical team, create sprint tickets, begin Week 1 implementation
**Architect:** Winston (BMAD System Architect)
**Date:** 2025-12-08
