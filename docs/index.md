# Neobrutalism CRM - Documentation Index

**Generated:** 2025-12-07
**Project:** Neobrutalism CRM
**Version:** 1.0.0-SNAPSHOT
**Documentation Status:** ✅ Complete

---

## 📋 Table of Contents

1. [Quick Start](#quick-start)
2. [Project Documentation](#project-documentation)
3. [Architecture Documentation](#architecture-documentation)
4. [API Documentation](#api-documentation)
5. [Security Documentation](#security-documentation)
6. [Domain Documentation](#domain-documentation)
7. [Existing Documentation](#existing-documentation)
8. [Development Guides](#development-guides)

---

## 🚀 Quick Start

### For New Developers

1. **Start Here:** [Project Overview](./project-overview.md) - Understand the system architecture
2. **Backend:** [Backend Architecture](./architecture-backend.md) - Domain models and patterns
3. **Frontend:** [Frontend Architecture](./architecture-frontend.md) - React components and state management
4. **API:** [API Contracts](./api-contracts-backend.md) - REST API reference
5. **Security:** [Security Architecture](./security-architecture.md) - Authentication and authorization

### For API Developers

1. [API Contracts](./api-contracts-backend.md) - Complete REST API catalog (500+ endpoints)
2. [Security Architecture](./security-architecture.md) - Authentication flow and JWT tokens
3. [Backend Architecture](./architecture-backend.md) - Domain models and business logic

### For Frontend Developers

1. [Frontend Architecture](./architecture-frontend.md) - Component library and hooks
2. [API Contracts](./api-contracts-backend.md) - API endpoints to integrate
3. [Security Architecture](./security-architecture.md) - Frontend authentication and permissions

---

## 📚 Project Documentation

### [Project Overview](./project-overview.md)

**What it covers:**
- Executive summary
- Technology stack (Frontend + Backend)
- Project structure (monorepo)
- Key features
- Integration points
- Development setup

**Quick Facts:**
- **Frontend:** Next.js 16.0.4 + React 19.0.0 + TypeScript 5.1.6
- **Backend:** Spring Boot 3.5.7 + Java 21 + PostgreSQL
- **Architecture:** Multi-part application (Frontend + Backend separation)

---

## 🏗️ Architecture Documentation

### [Backend Architecture](./architecture-backend.md)

**Comprehensive backend documentation covering:**

**Domain Model (25 domains, 62 entities):**
- Core CRM Domains: User, Organization, Customer, Contact, Activity, Task
- Permission System: Role, Group, UserRole, GroupRole, RoleMenu
- UI/Navigation: Menu, MenuScreen, MenuTab, ApiEndpoint
- Content Management: Content, Course, Notification, Attachment
- Master Data: Contract, DocumentVolume

**Key Patterns:**
- Domain-Driven Design (DDD)
- Aggregate Root Pattern (9 aggregates)
- State Machine Pattern (controlled transitions)
- CQRS Pattern (Content, Organization)
- Repository Pattern (30+ repositories)
- Multi-Tenancy Pattern (tenant isolation)

**Database:**
- 60+ tables
- 20+ Flyway migrations
- Optimized indexes for multi-tenant queries
- Materialized path for hierarchies

**Statistics:**
- Domain Packages: 25
- Entity Models: 62
- Repositories: 30+
- Services: 30+
- Total Java Files: 307

---

### [Frontend Architecture](./architecture-frontend.md)

**Comprehensive frontend documentation covering:**

**Component Architecture:**
- UI Components: 70+ base components (Shadcn/ui + Radix UI)
- Feature Components: 70+ business components
- Custom Hooks: 40+ data fetching and state management hooks
- API Service Modules: 28+ typed service modules

**State Management:**
- React Query integration (TanStack Query 5.90.5)
- Optimistic updates
- Adaptive polling (notifications)
- Query key factories

**Key Features:**
- JWT authentication with auto-refresh
- Permission-based UI guards
- WebSocket integration (STOMP over SockJS)
- Form validation (Zod schemas)
- Type safety (TypeScript strict mode)

**Performance Optimizations:**
- Request caching with TTL
- Request deduplication
- Code splitting (Next.js App Router)
- Memory-efficient pagination
- WebSocket connection pooling

**Statistics:**
- UI Components: 70+
- Feature Components: 70+
- Custom Hooks: 40+
- API Endpoints: 250+
- Total TypeScript Interfaces: 100+

---

## 🔌 API Documentation

### [API Contracts - Backend](./api-contracts-backend.md)

**Complete REST API reference:**

**Coverage:**
- Total Endpoints: 500+
- Controllers: 39
- Functional Areas: 11

**API Categories:**
1. **Authentication APIs** - Login, logout, refresh, current user
2. **User Management APIs** - CRUD, search, status operations, menus
3. **Organization Management APIs** - CRUD, query API (CQRS read model)
4. **Role & Group Management APIs** - Hierarchical structures
5. **Task Management APIs** - Kanban board, comments, checklists, bulk operations
6. **Customer & Contact APIs** - CRM operations, status transitions
7. **Activity Management APIs** - CRM activities, time-based queries
8. **Notification APIs** - Real-time notifications, WebSocket
9. **Menu & Permission APIs** - Navigation, role-menu permissions
10. **Content & Course APIs** - CMS, LMS functionality
11. **Attachment & File APIs** - File upload/download

**Common Patterns:**
- Pagination (page, size, sortBy, sortDirection)
- Bulk operations (assign, status change, delete)
- Hierarchical data (tree structures)
- Status transitions (state machines)
- Standard response format (ApiResponse wrapper)

**Authentication:**
- Bearer token: `Authorization: Bearer <token>`
- Tenant context: `X-Tenant-Id` header
- HTTP status codes: 200, 201, 400, 401, 403, 404, 500

---

## 🔒 Security Documentation

### [Security Architecture](./security-architecture.md)

**Multi-layered security implementation:**

**Authentication:**
- JWT token-based (HS256 with SHA-256 HMAC)
- Access tokens (1 hour) + Refresh tokens (7 days)
- Token rotation with reuse detection
- Account lockout (5 failed attempts, 30 min)
- BCrypt password hashing (cost factor 12)

**Authorization:**
- Casbin RBAC (role-based access control)
- Multi-tenant policy isolation
- Fine-grained permissions (canView, canCreate, canEdit, canDelete)
- Resource-level enforcement
- Role hierarchy support

**Token Management:**
- Refresh token rotation with suspicious activity detection
- Token blacklist for immediate invalidation
- User-level token revocation (password change, security breach)
- Session limits (5 concurrent sessions per user)
- Automatic cleanup (expired tokens)

**Security Headers:**
- Content-Security-Policy (CSP)
- X-Frame-Options: SAMEORIGIN
- Strict-Transport-Security (HSTS)
- X-Content-Type-Options: nosniff
- Referrer-Policy: no-referrer

**Identified Vulnerabilities:**
1. ⚠️ CRITICAL: Default JWT secret (must override in production)
2. ⚠️ HIGH: Frontend token storage in localStorage (recommend HttpOnly cookies)
3. ⚠️ MEDIUM: CSP unsafe-inline/unsafe-eval (XSS risk)

**Security Checklist:**
- [ ] Change JWT secret to strong random value
- [ ] Enable HTTPS/HSTS in production
- [ ] Fix CSP unsafe directives
- [ ] Implement HttpOnly cookie storage
- [ ] Configure rate limiting
- [ ] Set up monitoring and alerting

---

## 📊 Domain Documentation

### Domain Model Overview

**Total Domains:** 25
**Total Entities:** 62
**Aggregate Roots:** 9

**Core CRM Domains:**
1. **user** - User accounts and authentication
2. **organization** - Multi-tenant organizations
3. **customer** - Customer/company accounts
4. **contact** - Individual contact persons
5. **activity** - CRM activities (calls, meetings)
6. **task** - Task/todo management

**Permission System:**
- **role** - RBAC roles
- **group** - User teams/groups
- **userrole** - User-Role assignments
- **rolemenu** - Role-Menu permissions

**Content Management:**
- **content** - CMS content
- **course** - LMS courses
- **notification** - Notification system
- **attachment** - File management

**State Machines:**
- User: PENDING → ACTIVE → SUSPENDED/LOCKED
- Customer: LEAD → PROSPECT → ACTIVE → CHURNED
- Task: TODO → IN_PROGRESS → IN_REVIEW → COMPLETED
- Organization: DRAFT → ACTIVE → SUSPENDED → ARCHIVED

**Entity Relationships:**
- Many-to-One: Contact → Customer, Task → User
- One-to-Many: Customer ← Contacts, Task → Comments
- Many-to-Many: Content ↔ Category, Content ↔ Tag
- Polymorphic: Activity.relatedTo (CUSTOMER/CONTACT/OPPORTUNITY)

---

## 📖 Existing Documentation

### Feature-Specific Documentation

Located in `/docs/` directory:

**Menu System:**
- [MENU_AUTHORIZATION_GUIDE.md](./MENU_AUTHORIZATION_GUIDE.md) - Menu authorization implementation
- [MENU_SYNC_GUIDE.md](./MENU_SYNC_GUIDE.md) - Menu synchronization guide
- [MENU_SYSTEM_SUMMARY.md](./MENU_SYSTEM_SUMMARY.md) - Menu system overview

**Performance & Optimization:**
- [MIGRATION_MEMORY_MANAGEMENT.md](./MIGRATION_MEMORY_MANAGEMENT.md) - Migration optimization
- [UPLOAD_MEMORY_OPTIMIZATION.md](./UPLOAD_MEMORY_OPTIMIZATION.md) - File upload optimization
- [MEMORY_LEAK_FIX_SUMMARY.md](./MEMORY_LEAK_FIX_SUMMARY.md) - Memory leak fixes

### Scripts

Located in `/scripts/` directory - automation and utility scripts.

---

## 🛠️ Development Guides

### Backend Development

**Prerequisites:**
- Java 21
- Maven 3.8+
- PostgreSQL 14+
- Redis 6+
- MinIO (optional for object storage)

**Build Commands:**
```bash
mvn clean install    # Build and test
mvn spring-boot:run  # Run development server
mvn test            # Run tests
```

**Key Locations:**
- Entry Point: `src/main/java/com/neobrutalism/crm/CrmApplication.java`
- Domain Models: `src/main/java/com/neobrutalism/crm/domain/`
- Configurations: `src/main/java/com/neobrutalism/crm/config/`
- Database Migrations: `src/main/resources/db/migration/`
- Application Config: `src/main/resources/application.yml`

---

### Frontend Development

**Prerequisites:**
- Node.js >= 20.x
- pnpm 9.6.0 (preferred package manager)

**Build Commands:**
```bash
npm run dev       # Development server (localhost:3000)
npm run build     # Production build
npm run start     # Start production server
npm run lint      # Lint code
```

**Key Locations:**
- Entry Point: `src/app/layout.tsx`
- Admin Pages: `src/app/admin/`
- Components: `src/components/`
- Custom Hooks: `src/hooks/`
- API Client: `src/lib/api/`
- Types: `src/types/`
- Middleware: `src/middleware.ts`

---

### Integration Testing

**Frontend → Backend Integration:**

1. **API Client** (`src/lib/api/client.ts`)
   - Base URL: `process.env.NEXT_PUBLIC_API_URL`
   - Automatic token management
   - Request retry with exponential backoff

2. **WebSocket** (`src/lib/websocket.ts`)
   - URL: `process.env.NEXT_PUBLIC_WS_URL`
   - STOMP over WebSocket/SockJS
   - Channel: `/user/queue/notifications`

3. **Environment Variables:**
   ```bash
   NEXT_PUBLIC_API_URL=http://localhost:8080
   NEXT_PUBLIC_WS_URL=http://localhost:8080/ws
   JWT_SECRET=<strong-random-secret>
   ```

---

## 📈 Project Statistics

### Overall Metrics

**Backend:**
- Java Files: 307
- Domain Packages: 25
- Entities: 62
- Repositories: 30+
- Services: 30+
- REST Endpoints: 500+
- Database Tables: 60+

**Frontend:**
- TypeScript Files: 200+
- UI Components: 70+
- Feature Components: 70+
- Custom Hooks: 40+
- API Service Modules: 28+
- Type Interfaces: 100+

**Combined:**
- Total Endpoints: 500+ (backend) + 250+ (frontend API calls)
- Lines of Code: 50,000+
- Supported Features: 18+ admin sections

---

## 🔗 Quick Links

### Documentation Files

| Document | Description | Last Updated |
|----------|-------------|--------------|
| [project-overview.md](./project-overview.md) | Project summary and tech stack | 2025-12-07 |
| [architecture-backend.md](./architecture-backend.md) | Backend architecture and domain models | 2025-12-07 |
| [architecture-frontend.md](./architecture-frontend.md) | Frontend architecture and components | 2025-12-07 |
| [api-contracts-backend.md](./api-contracts-backend.md) | REST API reference | 2025-12-07 |
| [security-architecture.md](./security-architecture.md) | Security implementation | 2025-12-07 |

### External Resources

- **Next.js Documentation:** https://nextjs.org/docs
- **Spring Boot Documentation:** https://spring.io/projects/spring-boot
- **Casbin Documentation:** https://casbin.org/docs/overview
- **React Query Documentation:** https://tanstack.com/query/latest
- **Radix UI Documentation:** https://www.radix-ui.com/primitives/docs/overview/introduction

---

## 📝 Documentation Conventions

### File Organization

```
docs/
├── index.md                          # This file - master index
├── project-overview.md               # High-level overview
├── architecture-backend.md           # Backend deep dive
├── architecture-frontend.md          # Frontend deep dive
├── api-contracts-backend.md          # API reference
├── security-architecture.md          # Security details
├── MENU_*.md                         # Feature guides
├── MIGRATION_*.md                    # Performance guides
└── project-scan-report.json          # Scan metadata
```

### Document Status

- ✅ **Complete** - Fully documented with examples
- 🚧 **In Progress** - Partial documentation
- 📋 **Planned** - To be created

**Current Status:** All core documentation is ✅ Complete

---

## 🎯 Next Steps

### For Brownfield Analysis

You have successfully completed the **document-project** workflow with exhaustive scanning. The documentation now provides:

1. ✅ Complete technology stack analysis
2. ✅ Comprehensive architecture documentation (frontend + backend)
3. ✅ Full API catalog (500+ endpoints)
4. ✅ Security architecture and vulnerability assessment
5. ✅ Domain model documentation (62 entities)
6. ✅ Component and hook inventory (140+ components, 40+ hooks)

### Recommended Next Workflows

Based on the BMad Method for brownfield projects:

1. **brainstorm-project** (optional) - Brainstorm improvements and new features
2. **research** (optional) - Research technical decisions
3. **prd** (required) - Extract implemented features into PRD format
4. **validate-prd** (optional) - Validate requirements documentation
5. **create-architecture** (recommended) - Already documented ✅
6. **implementation-readiness** (required) - Validate alignment before next phase

### For Development Teams

**New Team Members:**
1. Read [Project Overview](./project-overview.md)
2. Study [Backend Architecture](./architecture-backend.md) or [Frontend Architecture](./architecture-frontend.md) based on role
3. Review [API Contracts](./api-contracts-backend.md)
4. Check [Security Architecture](./security-architecture.md) for authentication flow

**Technical Leads:**
1. Review identified vulnerabilities in [Security Architecture](./security-architecture.md)
2. Plan security hardening (JWT secret, CSP, cookies)
3. Evaluate architecture patterns for consistency
4. Consider CQRS expansion for performance

**Product Owners:**
1. Review feature inventory in [Project Overview](./project-overview.md)
2. Use documentation for compliance/handoff
3. Plan technical debt reduction
4. Prioritize security improvements

---

## 📧 Support

For questions about this documentation:

1. Check the specific architecture document for detailed information
2. Review existing feature documentation in `/docs/`
3. Consult API contracts for endpoint details
4. Check security architecture for auth/permission flows

---

**Documentation Generated By:** Claude Code (Analyst Agent)
**Scan Level:** Exhaustive (30-120 minutes)
**Scan Date:** 2025-12-07
**Project:** Neobrutalism CRM v1.0.0-SNAPSHOT

---

*This index provides navigation to all comprehensive documentation generated through exhaustive brownfield codebase analysis. All documents are interconnected and cross-referenced for easy navigation.*
