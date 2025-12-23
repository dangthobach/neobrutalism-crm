# Neobrutalism CRM - Project Overview

**Generated:** 2025-12-07
**Project Type:** Multi-Part Full-Stack Application
**Repository Structure:** Monorepo (Frontend + Backend)

## Executive Summary

Neobrutalism CRM is a comprehensive customer relationship management system built with modern web technologies. The application features a Next.js-based frontend with an extensive component library and a robust Spring Boot backend with advanced security, multi-tenancy, and event-driven architecture.

## Project Structure

This is a **multi-part application** with two distinct but integrated components:

### Part 1: Frontend (Next.js Web Application)
- **Location:** `src/app/`, `src/components/`, `src/hooks/`, `src/lib/`
- **Type:** Single-Page Application with Server-Side Rendering
- **Primary Purpose:** User interface for CRM functionality and admin dashboard

### Part 2: Backend (Spring Boot API)
- **Location:** `src/main/java/`
- **Type:** RESTful API with Domain-Driven Design
- **Primary Purpose:** Business logic, data persistence, and API services

## Technology Stack Summary

### Frontend Technologies

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Framework** | Next.js | 16.0.4 | React framework with SSR/SSG |
| **UI Library** | React | 19.0.0 | Component-based UI |
| **Language** | TypeScript | 5.1.6 | Type-safe development |
| **Styling** | TailwindCSS | 4.0.9 | Utility-first CSS |
| **Component Library** | Radix UI | Various | Accessible UI primitives |
| **Forms** | React Hook Form | 7.51.2 | Form state management |
| **Data Fetching** | TanStack Query | 5.90.5 | Server state management |
| **Validation** | Zod | 3.22.4 | Schema validation |
| **Real-time** | STOMP.js | 7.2.1 | WebSocket client |
| **Tables** | TanStack Table | 8.15.3 | Data grid functionality |
| **Charts** | Recharts | 2.15.3 | Data visualization |

### Backend Technologies

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Framework** | Spring Boot | 3.5.7 | Application framework |
| **Language** | Java | 21 | Programming language |
| **Database** | PostgreSQL | Runtime | Primary database |
| **ORM** | Spring Data JPA | 3.5.7 | Data access layer |
| **Migration** | Flyway | Latest | Database versioning |
| **Security** | Spring Security | 3.5.7 | Authentication & authorization |
| **Authorization** | Casbin (jCasbin) | 1.55.0 | RBAC policy engine |
| **JWT** | jjwt | 0.12.5 | Token generation/validation |
| **Caching** | Caffeine + Redis | Latest | Multi-level caching |
| **Rate Limiting** | Bucket4j | 8.10.1 | API throttling |
| **Metrics** | Micrometer + Prometheus | Latest | Application monitoring |
| **Tracing** | OpenTelemetry | 1.45.0 | Distributed tracing |
| **Object Storage** | MinIO | 8.5.9 | File storage |
| **PDF Generation** | iText | 8.0.5 | Document creation |
| **WebSocket** | Spring WebSocket | 3.5.7 | Real-time notifications |
| **Email** | Spring Mail + Thymeleaf | 3.5.7 | Email templating |
| **Excel Processing** | Apache POI | 5.2.5 | Excel import/export |

## Architecture Patterns

### Frontend Architecture
- **Pattern:** Component-based with Route-based Pages
- **State Management:** React Query for server state, React Context for UI state
- **Routing:** Next.js App Router (file-system based)
- **API Communication:** Centralized API client layer with React Query hooks
- **Real-time:** WebSocket integration via STOMP for live notifications

### Backend Architecture
- **Pattern:** Domain-Driven Design (DDD) with Layered Architecture
- **Layers:**
  - **Domain Layer:** Business entities and domain logic
  - **Application Layer:** Use cases and orchestration
  - **Infrastructure Layer:** Persistence, external services
  - **API Layer:** REST controllers and DTOs
- **Security Model:** Multi-tenant with Casbin RBAC
- **Caching Strategy:** Multi-level (Caffeine L1, Redis L2)
- **Event-Driven:** WebSocket for real-time updates

## Key Features (Based on Directory Structure)

### Admin Dashboard Features
- **User Management:** User administration with role-based access
- **Customer Management:** Customer profiles and relationship tracking
- **Contact Management:** Contact information and interactions
- **Task Management:** Task tracking with comments and checklists
- **Notification System:** Real-time notifications via WebSocket
- **Permission Management:** Fine-grained permission control with Casbin
- **Role Management:** Role definition and assignment
- **Group Management:** User group organization
- **Menu Management:** Dynamic menu configuration
- **Organization Management:** Multi-tenant organization support
- **Content Management:** Content and course management
- **Activity Tracking:** Activity logging and monitoring

### Public Features
- **Blog:** Content publishing system
- **Courses:** Learning management system
- **Documentation:** Technical documentation portal
- **Showcase:** Component showcase and examples

## Development Setup

### Prerequisites
- **Node.js:** >= 20.x
- **Java:** 21
- **Maven:** 3.8+
- **PostgreSQL:** 14+
- **Redis:** 6+
- **MinIO:** Latest (optional for object storage)

### Build Commands

**Frontend:**
```bash
npm run dev       # Development server
npm run build     # Production build
npm run start     # Start production server
npm run lint      # Lint code
```

**Backend:**
```bash
mvn clean install    # Build and test
mvn spring-boot:run  # Run development server
mvn test            # Run tests
```

## Integration Points

The frontend and backend communicate via:
1. **REST API:** HTTP/JSON API calls from frontend to backend
2. **WebSocket:** Real-time bidirectional communication via STOMP
3. **Authentication:** JWT tokens issued by backend, stored in frontend
4. **File Upload:** Direct upload to MinIO via backend proxy

## Documentation Organization

- **[Project Overview](./project-overview.md)** - This document
- **[Architecture - Frontend](./architecture-frontend.md)** - Frontend architecture details *(To be generated)*
- **[Architecture - Backend](./architecture-backend.md)** - Backend architecture details *(To be generated)*
- **[API Contracts](./api-contracts-backend.md)** - REST API documentation *(To be generated)*
- **[Data Models](./data-models-backend.md)** - Database schema and entities *(To be generated)*
- **[Integration Architecture](./integration-architecture.md)** - Frontend-Backend integration *(To be generated)*
- **[Development Guide](./development-guide.md)** - Setup and development workflow *(To be generated)*
- **[Deployment Guide](./deployment-guide.md)** - Deployment instructions *(To be generated)*

## Quick Reference

**Frontend Entry Point:** `src/app/layout.tsx`
**Backend Entry Point:** `src/main/java/com/neobrutalism/crm/CrmApplication.java`
**API Base URL:** Configured via environment variables
**Database Migrations:** `src/main/resources/db/migration/`

## Existing Documentation

This project has extensive existing documentation in the `/docs` folder:
- Implementation plans and roadmaps
- Week-by-week completion summaries
- Technical guides for specific features
- API documentation and migration guides

Refer to the existing documentation for historical context and implementation details.
