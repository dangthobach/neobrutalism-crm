# Backend Architecture - Neobrutalism CRM

**Generated:** 2025-12-07
**Project:** Neobrutalism CRM
**Technology Stack:** Spring Boot 3.5.7 + Java 21 + PostgreSQL

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Domain Model](#domain-model)
3. [Entity Relationships](#entity-relationships)
4. [Domain-Driven Design Patterns](#domain-driven-design-patterns)
5. [Database Schema](#database-schema)
6. [Service Layer](#service-layer)
7. [Repository Layer](#repository-layer)
8. [CQRS Implementation](#cqrs-implementation)

---

## Architecture Overview

The backend follows a **Domain-Driven Design (DDD)** architecture with clear separation of concerns:

### Architectural Layers

```
┌─────────────────────────────────────────┐
│         API/Controller Layer            │
│    (REST endpoints, DTOs, Validation)   │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         Application Layer               │
│    (Use cases, Orchestration)           │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         Domain Layer                    │
│    (Entities, Domain Logic, Events)     │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      Infrastructure Layer               │
│    (Persistence, External Services)     │
└─────────────────────────────────────────┘
```

### Key Architectural Patterns

- **Domain-Driven Design (DDD)** - Aggregate roots, value objects, domain events
- **CQRS** - Separate read and write models for complex queries
- **Multi-Tenancy** - Tenant-aware entities with isolation
- **Event Sourcing** - Domain events for audit trails
- **State Machine** - Controlled entity state transitions
- **Repository Pattern** - Data access abstraction

---

## Domain Model

### Domain Package Structure

The system consists of **25 domain packages** organized by business capability:

#### Core CRM Domains

1. **user** - User accounts and authentication
2. **organization** - Multi-tenant organizations
3. **branch** - Hierarchical organizational units
4. **role** - RBAC roles
5. **group** - User teams/groups
6. **customer** - Customer/company accounts
7. **contact** - Individual contact persons
8. **activity** - CRM activities (calls, meetings, emails)
9. **task** - Task/todo management

#### Permission System Domains

10. **permission** - Permission definitions
11. **userrole** - User-Role assignments
12. **usergroup** - User-Group assignments
13. **grouprole** - Group-Role assignments
14. **rolemenu** - Role-Menu permissions

#### UI/Navigation Domains

15. **menu** - Navigation menu system
16. **menuscreen** - Menu-Screen associations
17. **menutab** - Menu tab configurations
18. **apiendpoint** - API endpoint registry

#### Content Management Domains

19. **content** - CMS content management
20. **notification** - Notification system
21. **attachment** - File attachment management

#### LMS Domains

22. **course** - LMS course management

#### Master Data Domains

23. **contract** - Contract/loan document tracking
24. **document** - Document volume tracking

---

## Entity Relationships

### Aggregate Root Entities

The system defines **9 primary aggregate roots** that serve as consistency boundaries:

#### 1. User Aggregate

**Location:** `src/main/java/com/neobrutalism/crm/domain/user/model/User.java`

**Type:** `TenantAwareAggregateRoot<UserStatus>`

**Key Fields:**
- `username` (unique, 3-50 chars)
- `email` (unique, validated)
- `passwordHash`, `firstName`, `lastName`
- `phone`, `avatar`
- `organizationId`, `branchId`
- `dataScope` (SELF_ONLY, CURRENT_BRANCH, ALL_BRANCHES)
- `memberTier` (FREE, PREMIUM, ENTERPRISE)
- `lastLoginAt`, `lastLoginIp`
- `failedLoginAttempts`, `lockedUntil`

**State Transitions:**
```
PENDING → ACTIVE, INACTIVE
ACTIVE → SUSPENDED, LOCKED, INACTIVE
SUSPENDED → ACTIVE, INACTIVE
LOCKED → ACTIVE, INACTIVE
INACTIVE → ACTIVE
```

**Business Logic:**
- Account locking after 5 failed attempts (30 min)
- Failed login tracking
- Successful login recording
- Full name computation

**Domain Events:**
- `UserCreatedEvent`
- `UserStatusChangedEvent`

---

#### 2. Organization Aggregate

**Location:** `src/main/java/com/neobrutalism/crm/domain/organization/model/Organization.java`

**Type:** `TenantAwareAggregateRoot<OrganizationStatus>`

**Key Fields:**
- `name` (2-200 chars)
- `code` (unique, 2-50 chars, A-Z0-9_- only)
- `description`, `email`, `phone`
- `address`, `website`

**State Transitions:**
```
DRAFT → ACTIVE, ARCHIVED
ACTIVE → SUSPENDED, INACTIVE
SUSPENDED → ACTIVE, INACTIVE
INACTIVE → ACTIVE, ARCHIVED
ARCHIVED → (terminal state)
```

---

#### 3. Customer Aggregate

**Location:** `src/main/java/com/neobrutalism/crm/domain/customer/model/Customer.java`

**Type:** `TenantAwareAggregateRoot<CustomerStatus>`

**Key Fields:**
- `code` (unique per org), `companyName`, `legalName`
- `customerType` (B2B, B2C, etc.)
- `industry` (enum)
- `taxId`, `email`, `phone`, `website`
- `billingAddress`, `shippingAddress`
- `city`, `state`, `country`, `postalCode`
- `ownerId` (account manager)
- `branchId`, `organizationId`
- `annualRevenue`, `employeeCount`
- `acquisitionDate`, `lastContactDate`, `nextFollowupDate`
- `leadSource`, `creditLimit`, `paymentTermsDays`
- `rating` (1-5), `isVip`
- `tags`, `notes`

**State Transitions:**
```
LEAD → PROSPECT, INACTIVE, BLACKLISTED
PROSPECT → ACTIVE, INACTIVE, BLACKLISTED
ACTIVE → INACTIVE, CHURNED, BLACKLISTED
INACTIVE → ACTIVE, CHURNED, BLACKLISTED
CHURNED → PROSPECT, ACTIVE
BLACKLISTED → (terminal state)
```

**Composite Indexes:**
- (tenant_id, status, deleted)
- (tenant_id, type, deleted)
- (tenant_id, is_vip, deleted)

---

#### 4. Task Aggregate

**Location:** `src/main/java/com/neobrutalism/crm/domain/task/model/Task.java`

**Type:** `TenantAwareAggregateRoot<TaskStatus>`

**Key Fields:**
- `title`, `description`
- `priority` (LOW, MEDIUM, HIGH, URGENT, CRITICAL)
- Assignment: `assignedToId`, `assignedById`
- Polymorphic: `relatedToType`, `relatedToId`
- Timing: `dueDate`, `completedAt`
- `estimatedHours`, `actualHours`
- `progressPercentage` (0-100)
- `checklist` (JSON TEXT)

**State Transitions:**
```
TODO → IN_PROGRESS, CANCELLED
IN_PROGRESS → IN_REVIEW, COMPLETED, CANCELLED, TODO
IN_REVIEW → COMPLETED, IN_PROGRESS
COMPLETED, CANCELLED → (terminal states)
ON_HOLD → IN_PROGRESS, CANCELLED
```

**Related Entities:**
- `Comment` (OneToMany) - Threaded comments with soft delete
- `ChecklistItem` (OneToMany) - Task checklist items
- `TaskActivity` (OneToMany) - Audit trail

---

### Entity Inheritance Hierarchy

```
BaseEntity (id: UUID, tenantId: String, createdAt: Instant, updatedAt: Instant)
├── AuditableEntity (createdBy: UUID, updatedBy: UUID)
│   ├── Notification
│   └── Attachment
├── TenantAwareEntity (tenantId: String, deleted: Boolean)
│   ├── TenantAwareAggregateRoot<S> (status management + domain events)
│   │   ├── User<UserStatus>
│   │   ├── Organization<OrganizationStatus>
│   │   ├── Role<RoleStatus>
│   │   ├── Branch<BranchStatus>
│   │   ├── Group<GroupStatus>
│   │   ├── Customer<CustomerStatus>
│   │   ├── Contact<ContactStatus>
│   │   ├── Activity<ActivityStatus>
│   │   └── Task<TaskStatus>
│   ├── RoleMenu
│   ├── UserRole
│   ├── UserGroup
│   ├── GroupRole
│   └── Comment
├── SoftDeletableEntity (deleted: Boolean)
│   ├── StatefulEntity<S>
│   │   ├── Content<ContentStatus>
│   │   └── Course<CourseStatus>
│   ├── Menu
│   ├── ContentCategory
│   ├── CourseModule
│   └── Enrollment
└── ApiEndpoint (direct extension)
```

---

### Key Relationships

#### Many-to-One Relationships

1. `Contact.customer` → `Customer`
2. `Activity.owner` → `User`
3. `Task.assignedTo` → `User`
4. `Content.author` → `User`
5. `Course.instructor` → `User`
6. `Enrollment.user` → `User`
7. `Enrollment.course` → `Course`
8. `Branch.parent` → `Branch` (self-referential)
9. `Group.parent` → `Group` (self-referential)
10. `Menu.parent` → `Menu` (self-referential)

#### One-to-Many Relationships

1. `Customer` ← `Contacts` (customerId)
2. `Course` → `CourseModules` (cascade ALL, orphanRemoval=true)
3. `CourseModule` → `Lessons` (cascade ALL, orphanRemoval=true)
4. `Task` → `Comments` (oneToMany)
5. `Task` → `ChecklistItems` (oneToMany)

#### Many-to-Many Relationships

1. `Content` ↔ `ContentCategory` (join table: content_category_mappings)
2. `Content` ↔ `ContentTag` (join table: content_tag_mappings)

#### Polymorphic Associations

1. **Activity**: `relatedToType`, `relatedToId` (CUSTOMER, CONTACT, OPPORTUNITY)
2. **Task**: `relatedToType`, `relatedToId` (CUSTOMER, CONTACT, OPPORTUNITY, ACTIVITY)
3. **Attachment**: `entityType`, `entityId` (can attach to any entity)
4. **Notification**: `entityType`, `entityId` (can reference any entity)

---

## Domain-Driven Design Patterns

### 1. Aggregate Root Pattern

**Implementation:** `TenantAwareAggregateRoot<S>` base class

**Features:**
- Status state machine
- Domain event publishing
- Tenant isolation
- Multi-tenancy support via filters

**Aggregates:**
- User, Organization, Role, Branch, Group
- Customer, Contact, Activity, Task

### 2. Value Objects / Enums

**Status Enums:**
- `UserStatus`, `ContactStatus`, `CustomerStatus`
- `ActivityStatus`, `TaskStatus`, `CourseStatus`
- `ContentStatus`, `EnrollmentStatus`, `NotificationStatus`

**Type Enums:**
- `ActivityType`, `ActivityPriority`, `TaskPriority`
- `ContactRole`, `CustomerType`, `ContentType`, `CourseLevel`

### 3. State Machine / FSM Pattern

**Base Class:** `StatefulEntity<S>`

**Abstract Methods:**
- `getAllowedTransitions()` - Define valid state transitions
- `getInitialStatus()` - Define starting state
- `onStatusChanged()` - Handle state change side effects

**Example:**
```java
public class Customer extends TenantAwareAggregateRoot<CustomerStatus> {
    @Override
    protected Map<CustomerStatus, Set<CustomerStatus>> getAllowedTransitions() {
        return Map.of(
            LEAD, Set.of(PROSPECT, INACTIVE, BLACKLISTED),
            PROSPECT, Set.of(ACTIVE, INACTIVE, BLACKLISTED),
            ACTIVE, Set.of(INACTIVE, CHURNED, BLACKLISTED),
            INACTIVE, Set.of(ACTIVE, CHURNED, BLACKLISTED),
            CHURNED, Set.of(PROSPECT, ACTIVE)
        );
    }
}
```

### 4. Soft Delete Pattern

**Implementation:** `SoftDeletableEntity` base class

**Features:**
- `deleted` boolean flag
- Preserves data for auditing
- Hidden from queries via @Filter

**Used By:**
- Menu, ContentCategory, CourseModule, Enrollment

### 5. Domain Events

**Pattern:** Events registered during aggregate operations

**Examples:**
- `UserCreatedEvent`, `UserStatusChangedEvent`
- `OrganizationCreatedEvent`, `OrganizationStatusChangedEvent`
- `RoleCreatedEvent`, `RoleStatusChangedEvent`

**Integration:** Event handlers for async processing

### 6. Repository Pattern

**Implementation:** Spring Data JPA repositories

**Count:** 30+ repository interfaces

**Examples:**
- `UserRepository`, `CustomerRepository`, `ContactRepository`
- `ActivityRepository`, `TaskRepository`

**Features:**
- Automatic CRUD operations
- Custom query methods
- Pagination support
- Specification support for dynamic queries

### 7. CQRS Pattern

**Write Model:**
- `Content` entity (command side)
- Full entity with all relationships

**Read Model:**
- `ContentReadModel` entity (query side)
- Denormalized for performance
- Contains: author info, category/tag names (comma-separated), SEO fields

**Separate Repositories:**
- Write: `ContentRepository`
- Read: `ContentReadModelRepository`

### 8. Hierarchical Data Structures

**Supported By:**
- Branch (parent-child + materialized path)
- Group (parent-child + materialized path)
- Menu (parent-child hierarchy)
- ContentCategory (parent-child hierarchy)

**Fields:**
- `parentId` - Reference to parent
- `level` - Depth in tree
- `path` - Materialized path (e.g., /HN-001/HN-001-001)

### 9. Multi-Tenancy Pattern

**Implementation:**
- `TenantContext.getCurrentTenant()` - Thread-local context
- `@FilterDef` and `@Filter` annotations for SQL filtering
- All tenant-aware entities include `tenant_id` column
- Automatic tenant assignment in `@PrePersist`

**Data Scope Control:**
- `User.dataScope` determines visibility:
  - ALL_BRANCHES: Management level
  - CURRENT_BRANCH: Operations level
  - SELF_ONLY: Individual contributor level

---

## Database Schema

### Statistics

- **Total Tables:** ~60+ tables
- **Total Entities:** 62 entities
- **Migrations:** 20+ Flyway migration files (V1-V203)

### Key Indexes

#### User Table (users)
```sql
idx_user_username (unique)
idx_user_email (unique)
idx_user_org_id
idx_user_deleted_id (deleted, id)
idx_user_status
idx_user_tenant
idx_user_tenant_org_deleted (tenant_id, organization_id, deleted)
idx_user_tenant_status_deleted (tenant_id, status, deleted)
idx_user_branch
idx_user_last_login
```

#### Customer Table (customers)
```sql
idx_customer_code
idx_customer_email
idx_customer_status
idx_customer_type
idx_customer_tenant_id
idx_customer_owner_id
idx_customer_deleted_id
idx_customer_tenant_status_deleted (composite)
idx_customer_tenant_type_deleted (composite)
idx_customer_tenant_vip_deleted (composite)
idx_customer_org_branch
idx_customer_acquisition_date
idx_customer_last_contact
idx_customer_company_name
```

#### Organization Table (organizations)
```sql
idx_org_name
idx_org_code
idx_org_status
idx_org_deleted_id
idx_org_deleted_created_at
idx_org_deleted_status
```

### Performance Optimizations

**Phase 1: Performance Optimization Indexes (V200)**
- Tenant-aware composite indexes for multi-tenant queries
- Materialized path indexes for hierarchy navigation
- Foreign key column indexes for relationships

**Migration-Specific Tables (V201-V202)**
- Excel migration tables with performance indexes
- Staging tables for bulk operations

**Cleanup Migrations (V203)**
- Dropped unused staging columns
- Removed redundant indexes

---

## Service Layer

### Service Organization

**Count:** 30+ service classes

**Responsibilities:**
- Business logic implementation
- Aggregate coordination
- Complex workflows
- Transaction management

**Examples:**
- `UserService` - User account management
- `CustomerService` - Customer lifecycle management
- `TaskService` - Task operations and workflow
- `ActivityService` - CRM activity management
- `PermissionService` - Authorization logic
- `CasbinPolicyManager` - Policy synchronization

### Service Patterns

**Transactional Operations:**
```java
@Service
@Transactional
public class CustomerService {
    @Transactional(readOnly = true)
    public Customer getById(UUID id) { ... }

    public Customer create(CreateCustomerRequest request) { ... }

    public void convertToActive(UUID id, String reason) { ... }
}
```

**Domain Event Publishing:**
```java
public Customer create(CreateCustomerRequest request) {
    Customer customer = new Customer(...);
    customer.registerEvent(new CustomerCreatedEvent(customer));
    return customerRepository.save(customer);
}
```

---

## Repository Layer

### Repository Count

**Total:** 30+ Spring Data JPA repositories

### Repository Features

**Automatic CRUD:**
```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByOrganizationId(UUID organizationId);
    List<User> findByStatus(UserStatus status);
}
```

**Custom Queries:**
```java
@Query("SELECT u FROM User u WHERE u.deleted = false AND u.status = :status")
List<User> findActiveByStatus(@Param("status") UserStatus status);
```

**Specifications:**
```java
public class UserSpecifications {
    public static Specification<User> hasUsername(String username) {
        return (root, query, cb) ->
            cb.equal(root.get("username"), username);
    }
}
```

---

## CQRS Implementation

### Content Domain

**Write Model (Command Side):**
- Entity: `Content`
- Repository: `ContentRepository`
- Operations: Create, Update, Delete, Publish
- Full entity with all relationships

**Read Model (Query Side):**
- Entity: `ContentReadModel`
- Repository: `ContentReadModelRepository`
- Denormalized fields:
  - Author name (instead of authorId)
  - Category names (comma-separated)
  - Tag names (comma-separated)
  - SEO fields
  - View count
- Optimized for queries

**Synchronization:**
- Write operations trigger read model updates
- Event-driven or scheduled sync
- Eventual consistency model

### Organization Domain

**Read Model:**
- `OrganizationReadModel` for optimized queries
- Separate query controller: `OrganizationQueryController`
- Endpoints: `/api/organizations/query/**`

---

## Summary Statistics

| Category | Count |
|----------|-------|
| Domain Packages | 25 |
| Entity Models | 62 |
| Aggregate Roots | 9 |
| Repositories | 30+ |
| Services | 30+ |
| DTOs/Request-Response | 50+ |
| Enumerations | 25+ |
| Event Classes | 20+ |
| Domain Event Handlers | 10+ |
| Total Java Files | 307 |

---

## Technology Stack

- **Framework:** Spring Boot 3.5.7
- **Language:** Java 21
- **ORM:** Spring Data JPA (Hibernate)
- **Database:** PostgreSQL
- **Migration:** Flyway
- **Validation:** Jakarta Validation
- **Lombok:** Code generation
- **UUID:** UUID v7 (time-ordered) via uuid-creator

---

## Next Steps

For detailed API documentation, see [api-contracts-backend.md](./api-contracts-backend.md).

For data model details, see [data-models-backend.md](./data-models-backend.md).

For security architecture, see [security-architecture.md](./security-architecture.md).
