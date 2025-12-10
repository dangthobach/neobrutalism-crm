# REST API Contracts - Neobrutalism CRM

**Generated:** 2025-12-07
**Base URL:** Configured via environment variables
**Authentication:** JWT Bearer tokens
**Total Endpoints:** 500+
**Controllers:** 39

---

## Table of Contents

1. [API Overview](#api-overview)
2. [Authentication APIs](#authentication-apis)
3. [User Management APIs](#user-management-apis)
4. [Organization Management APIs](#organization-management-apis)
5. [Role & Group Management APIs](#role--group-management-apis)
6. [Task Management APIs](#task-management-apis)
7. [Customer & Contact APIs](#customer--contact-apis)
8. [Activity Management APIs](#activity-management-apis)
9. [Notification APIs](#notification-apis)
10. [Menu & Permission APIs](#menu--permission-apis)
11. [Content & Course APIs](#content--course-apis)
12. [Common Patterns](#common-patterns)

---

## API Overview

### Response Format

All endpoints return wrapped responses:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* Entity or list */ },
  "errors": null,
  "timestamp": "2025-12-07T10:30:00Z"
}
```

### Standard Query Parameters

Most GET endpoints support:
- `page` (default: 0) - Zero-indexed page number
- `size` (default: 20) - Items per page
- `sortBy` (default: varies) - Field to sort by
- `sortDirection` (default: ASC) - ASC or DESC

### Authentication

- **Bearer Token:** `Authorization: Bearer <accessToken>`
- **Tenant Context:** `X-Tenant-Id` header (optional, defaults to "default")
- **User ID:** `X-User-Id` header (optional, derived from token)

### HTTP Status Codes

- 200: Success
- 201: Created
- 400: Bad Request
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 409: Conflict
- 423: Locked (Account)
- 500: Server Error

---

## Authentication APIs

### Base Path: `/api/auth`

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string",
  "tenantId": "string",
  "rememberMe": boolean
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": "uuid",
    "username": "string",
    "email": "string",
    "roles": ["ADMIN", "USER"]
  }
}
```

#### Refresh Token

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** Same as login response with new tokens

#### Logout

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "message": "Logout successful"
}
```

#### Get Current User

```http
GET /api/auth/me
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "username": "string",
    "email": "string",
    "firstName": "string",
    "lastName": "string",
    "roles": ["ADMIN"],
    "permissions": ["users:read", "users:write"],
    "organizationId": "uuid",
    "branchId": "uuid",
    "tenantId": "string"
  }
}
```

---

## User Management APIs

### Base Path: `/api/users`

#### List Users (Paginated)

```http
GET /api/users?page=0&size=20&sortBy=username&sortDirection=ASC
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "username": "john.doe",
        "email": "john@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "phone": "+1234567890",
        "avatar": "https://...",
        "organizationId": "uuid",
        "status": "ACTIVE",
        "lastLoginAt": "2025-12-07T10:00:00Z",
        "createdAt": "2025-01-01T00:00:00Z"
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "number": 0,
    "size": 20
  }
}
```

#### Get User by ID

```http
GET /api/users/{id}
Authorization: Bearer <token>
```

#### Create User

```http
POST /api/users
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "organizationId": "uuid"
}
```

#### Update User

```http
PUT /api/users/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "email": "newemail@example.com",
  "firstName": "John",
  "lastName": "Smith",
  "phone": "+1234567890"
}
```

#### Status Operations

```http
POST /api/users/{id}/activate?reason=Account%20verified
Authorization: Bearer <token>

POST /api/users/{id}/suspend?reason=Policy%20violation
Authorization: Bearer <token>

POST /api/users/{id}/lock?reason=Security%20concern
Authorization: Bearer <token>

POST /api/users/{id}/unlock
Authorization: Bearer <token>
```

#### Delete User

```http
DELETE /api/users/{id}
Authorization: Bearer <token>
```

#### Advanced Search

```http
POST /api/users/search
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "john",
  "email": "example.com",
  "firstName": "John",
  "status": "ACTIVE",
  "page": 0,
  "size": 20,
  "sortBy": "username",
  "sortDirection": "ASC"
}
```

#### Get User's Menus

```http
GET /api/users/{id}/menus
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "code": "USERS",
      "name": "Users",
      "icon": "👤",
      "route": "/admin/users",
      "permissions": {
        "canView": true,
        "canCreate": true,
        "canEdit": true,
        "canDelete": false
      },
      "children": []
    }
  ]
}
```

---

## Organization Management APIs

### Base Path: `/api/organizations`

#### List Organizations

```http
GET /api/organizations?page=0&size=20
Authorization: Bearer <token>
```

#### Get by Code

```http
GET /api/organizations/code/{code}
Authorization: Bearer <token>
```

#### Create Organization

```http
POST /api/organizations
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Acme Corporation",
  "code": "ACME",
  "description": "Leading provider of...",
  "email": "contact@acme.com",
  "phone": "+1234567890",
  "website": "https://acme.com",
  "address": "123 Main St, City, Country"
}
```

#### Status Operations

```http
POST /api/organizations/{id}/activate?reason=Setup%20complete
POST /api/organizations/{id}/suspend?reason=Payment%20issue
POST /api/organizations/{id}/archive?reason=Business%20closed
```

### Query API (CQRS Read Model)

Base Path: `/api/organizations/query`

```http
GET /api/organizations/query/active
GET /api/organizations/query/status/{status}
GET /api/organizations/query/search?query=acme
GET /api/organizations/query/recent/{days}
GET /api/organizations/query/with-contact
GET /api/organizations/query/statistics
```

---

## Role & Group Management APIs

### Roles - Base Path: `/api/roles`

#### List Roles

```http
GET /api/roles?page=0&size=20
Authorization: Bearer <token>
```

#### Create Role

```http
POST /api/roles
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "MANAGER",
  "name": "Manager",
  "description": "Department manager role",
  "organizationId": "uuid",
  "isSystem": false,
  "priority": 50
}
```

#### Get System Roles

```http
GET /api/roles/system
Authorization: Bearer <token>
```

**Response:** Predefined system roles (ADMIN, USER, GUEST)

### Groups - Base Path: `/api/groups`

#### List Groups

```http
GET /api/groups?page=0&size=20
Authorization: Bearer <token>
```

#### Get Root Groups

```http
GET /api/groups/root
Authorization: Bearer <token>
```

#### Get Child Groups

```http
GET /api/groups/parent/{parentId}
Authorization: Bearer <token>
```

#### Create Group

```http
POST /api/groups
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "SALES_TEAM",
  "name": "Sales Team",
  "description": "Sales department team",
  "parentId": "uuid",
  "organizationId": "uuid"
}
```

---

## Task Management APIs

### Base Path: `/api/tasks`

#### List Tasks

```http
GET /api/tasks?page=0&size=20&sortBy=dueDate&sortDirection=ASC
Authorization: Bearer <token>
```

#### Get Kanban Board

```http
GET /api/tasks/board
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "TODO": [/* tasks */],
    "IN_PROGRESS": [/* tasks */],
    "IN_REVIEW": [/* tasks */],
    "COMPLETED": [/* tasks */]
  }
}
```

#### Create Task

```http
POST /api/tasks
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Follow up with client",
  "description": "Discuss contract renewal",
  "priority": "HIGH",
  "status": "TODO",
  "assignedToId": "uuid",
  "dueDate": "2025-12-15T00:00:00Z",
  "relatedToType": "CUSTOMER",
  "relatedToId": "uuid"
}
```

#### Update Task

```http
PUT /api/tasks/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Updated title",
  "description": "Updated description",
  "priority": "URGENT",
  "dueDate": "2025-12-20T00:00:00Z"
}
```

#### State Transitions

```http
POST /api/tasks/{id}/assign?userId=uuid
POST /api/tasks/{id}/start
POST /api/tasks/{id}/submit-for-review
POST /api/tasks/{id}/complete
POST /api/tasks/{id}/cancel?reason=No%20longer%20needed
```

#### Update Progress

```http
PUT /api/tasks/{id}/progress?percentage=75
Authorization: Bearer <token>
```

#### Query Operations

```http
GET /api/tasks/my-tasks
GET /api/tasks/assigned-by-me
GET /api/tasks/by-status?status=IN_PROGRESS
GET /api/tasks/overdue
GET /api/tasks/upcoming?daysAhead=7
GET /api/tasks/related?relatedToType=CUSTOMER&relatedToId=uuid
```

#### Bulk Operations

```http
POST /api/tasks/bulk/assign
Content-Type: application/json

{
  "taskIds": ["uuid1", "uuid2"],
  "assigneeId": "uuid"
}

POST /api/tasks/bulk/status
Content-Type: application/json

{
  "taskIds": ["uuid1", "uuid2"],
  "status": "COMPLETED"
}

DELETE /api/tasks/bulk
Content-Type: application/json

["uuid1", "uuid2", "uuid3"]
```

### Comments - Base Path: `/api/tasks/{taskId}/comments`

#### Add Comment

```http
POST /api/tasks/{taskId}/comments
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "This is a comment",
  "parentId": "uuid"  // Optional for threaded replies
}
```

#### Get Comments

```http
GET /api/tasks/{taskId}/comments
GET /api/tasks/{taskId}/comments/paginated?page=0&size=10
GET /api/tasks/{taskId}/comments/count
```

#### Update Comment

```http
PUT /api/tasks/comments/{commentId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Updated comment text"
}
```

#### Delete Comment

```http
DELETE /api/tasks/comments/{commentId}
Authorization: Bearer <token>
```

### Checklist - Base Path: `/api/tasks/{taskId}/checklist`

#### Add Checklist Item

```http
POST /api/tasks/{taskId}/checklist
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Review requirements",
  "description": "Check all specs"
}
```

#### Get Checklist

```http
GET /api/tasks/{taskId}/checklist
GET /api/tasks/{taskId}/checklist/progress
```

#### Toggle Item

```http
PUT /api/tasks/checklist/{itemId}/toggle
Authorization: Bearer <token>
```

#### Reorder Items

```http
PUT /api/tasks/{taskId}/checklist/reorder
Authorization: Bearer <token>
Content-Type: application/json

[
  { "id": "uuid1", "order": 0 },
  { "id": "uuid2", "order": 1 }
]
```

---

## Customer & Contact APIs

### Customers - Base Path: `/api/customers`

#### List Customers

```http
GET /api/customers?page=0&size=20
Authorization: Bearer <token>
```

#### Get Statistics

```http
GET /api/customers/stats
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "total": 1234,
    "active": 856,
    "totalRevenue": 5678900,
    "vipCount": 45,
    "byStatus": {
      "PROSPECT": 200,
      "ACTIVE": 856,
      "INACTIVE": 100,
      "CHURNED": 50,
      "BLACKLISTED": 28
    }
  }
}
```

#### Create Customer

```http
POST /api/customers
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "CUST001",
  "companyName": "Acme Inc",
  "legalName": "Acme Incorporated",
  "customerType": "B2B",
  "industry": "TECHNOLOGY",
  "taxId": "123-45-6789",
  "email": "contact@acme.com",
  "phone": "+1234567890",
  "website": "https://acme.com",
  "billingAddress": "123 Main St",
  "city": "New York",
  "country": "USA",
  "ownerId": "uuid"
}
```

#### Status Operations

```http
POST /api/customers/{id}/convert-to-prospect?reason=Qualified%20lead
POST /api/customers/{id}/convert-to-active?reason=Contract%20signed
POST /api/customers/{id}/mark-inactive?reason=No%20activity
POST /api/customers/{id}/mark-churned?reason=Moved%20to%20competitor
POST /api/customers/{id}/blacklist?reason=Fraud%20detected
POST /api/customers/{id}/reactivate?reason=Issue%20resolved
```

#### Query Operations

```http
GET /api/customers/vip
GET /api/customers/search?keyword=acme
GET /api/customers/acquisition?startDate=2025-01-01&endDate=2025-12-31
GET /api/customers/followup?date=2025-12-15
GET /api/customers/tag/{tag}
GET /api/customers/lead-source/{leadSource}
```

### Contacts - Base Path: `/api/contacts`

#### List Contacts

```http
GET /api/contacts?page=0&size=20
Authorization: Bearer <token>
```

#### Get by Customer

```http
GET /api/contacts/customer/{customerId}
GET /api/contacts/customer/{customerId}/primary
Authorization: Bearer <token>
```

#### Create Contact

```http
POST /api/contacts
Authorization: Bearer <token>
Content-Type: application/json

{
  "customerId": "uuid",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@acme.com",
  "workPhone": "+1234567890",
  "mobilePhone": "+9876543210",
  "title": "CTO",
  "department": "Technology",
  "contactRole": "DECISION_MAKER",
  "isPrimary": true
}
```

#### Set as Primary

```http
POST /api/contacts/{id}/set-primary
Authorization: Bearer <token>
```

#### Status Operations

```http
POST /api/contacts/{id}/mark-inactive
POST /api/contacts/{id}/mark-left-company
POST /api/contacts/{id}/mark-do-not-contact
POST /api/contacts/{id}/reactivate
```

#### Query Operations

```http
GET /api/contacts/role/{role}
GET /api/contacts/status/{status}
GET /api/contacts/search?keyword=john
GET /api/contacts/domain/{domain}
GET /api/contacts/tag/{tag}
GET /api/contacts/followup?sinceDate=2025-01-01
GET /api/contacts/email-optouts
```

---

## Activity Management APIs

### Base Path: `/api/activities`

#### List Activities

```http
GET /api/activities?page=0&size=20
Authorization: Bearer <token>
```

#### Create Activity

```http
POST /api/activities
Authorization: Bearer <token>
Content-Type: application/json

{
  "subject": "Client meeting",
  "description": "Discuss Q1 strategy",
  "activityType": "MEETING",
  "priority": "HIGH",
  "scheduledStartAt": "2025-12-15T14:00:00Z",
  "scheduledEndAt": "2025-12-15T15:00:00Z",
  "location": "Conference Room A",
  "ownerId": "uuid",
  "relatedToType": "CUSTOMER",
  "relatedToId": "uuid"
}
```

#### Status Operations

```http
POST /api/activities/{id}/start
POST /api/activities/{id}/complete?outcome=Successfully%20closed%20deal
POST /api/activities/{id}/cancel?reason=Client%20rescheduled
POST /api/activities/{id}/reschedule
Content-Type: application/json

{
  "newStartAt": "2025-12-20T14:00:00Z",
  "newEndAt": "2025-12-20T15:00:00Z",
  "reason": "Conflict in schedule"
}
```

#### Query Operations

```http
GET /api/activities/my-activities
GET /api/activities/upcoming?daysAhead=7
GET /api/activities/overdue
GET /api/activities/by-status?status=PLANNED
GET /api/activities/by-type?type=MEETING
GET /api/activities/related?relatedToType=CUSTOMER&relatedToId=uuid
```

---

## Notification APIs

### Base Path: `/api/notifications`

#### Create Notification

```http
POST /api/notifications
Authorization: Bearer <token>
Content-Type: application/json

{
  "recipientId": "uuid",
  "title": "New task assigned",
  "message": "You have been assigned a new task",
  "notificationType": "TASK_ASSIGNED",
  "priority": "HIGH",
  "actionUrl": "/admin/tasks/uuid",
  "entityType": "TASK",
  "entityId": "uuid"
}
```

#### Get Notifications

```http
GET /api/notifications/me/paginated?page=0&size=20
GET /api/notifications/me/unread
GET /api/notifications/me/unread/count
GET /api/notifications/me/priority
GET /api/notifications/me/recent
```

#### Mark as Read

```http
PUT /api/notifications/{id}/read
PUT /api/notifications/me/read-all
POST /api/notifications/bulk/read
Content-Type: application/json

["uuid1", "uuid2", "uuid3"]
```

#### Delete Notifications

```http
DELETE /api/notifications/{id}
DELETE /api/notifications/me/all
DELETE /api/notifications/bulk
Content-Type: application/json

["uuid1", "uuid2", "uuid3"]
```

---

## Menu & Permission APIs

### Menus - Base Path: `/api/menus`

#### List Menus

```http
GET /api/menus?page=0&size=20
GET /api/menus/root
GET /api/menus/visible
GET /api/menus/parent/{parentId}
```

#### Get Current User's Menus

```http
GET /api/menus/current-user
Authorization: Bearer <token>
```

**Response:** Hierarchical menu tree with permissions

#### Create Menu

```http
POST /api/menus
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "REPORTS",
  "name": "Reports",
  "icon": "📊",
  "parentId": "uuid",
  "route": "/admin/reports",
  "displayOrder": 10,
  "isVisible": true,
  "requiresAuth": true
}
```

### Role Menus - Base Path: `/api/role-menus`

#### Get Role's Menu Permissions

```http
GET /api/role-menus/role/{roleId}
Authorization: Bearer <token>
```

#### Set Permissions

```http
POST /api/role-menus
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleId": "uuid",
  "menuId": "uuid",
  "canView": true,
  "canCreate": true,
  "canEdit": true,
  "canDelete": false,
  "canExport": true,
  "canImport": false
}
```

#### Copy Permissions

```http
POST /api/role-menus/role/{roleId}/copy-from/{sourceRoleId}
Authorization: Bearer <token>
```

### User Roles - Base Path: `/api/user-roles`

#### Get User's Roles

```http
GET /api/user-roles/user/{userId}
GET /api/user-roles/user/{userId}/active
```

#### Assign Role

```http
POST /api/user-roles
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "uuid",
  "roleId": "uuid",
  "isActive": true,
  "expiresAt": "2026-12-31T23:59:59Z"  // Optional
}
```

#### Revoke Role

```http
DELETE /api/user-roles/{id}
DELETE /api/user-roles/user/{userId}/role/{roleId}
```

---

## Content & Course APIs

### Content - Base Path: `/api/contents`

#### Create Content

```http
POST /api/contents
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Getting Started Guide",
  "slug": "getting-started",
  "summary": "Learn the basics",
  "body": "## Introduction\n\nWelcome to...",
  "contentType": "BLOG",
  "categoryIds": ["uuid1", "uuid2"],
  "tagIds": ["uuid1", "uuid2"],
  "tierRequired": "FREE"
}
```

#### Publish Content

```http
POST /api/contents/{id}/publish
Authorization: Bearer <token>
```

#### Get Published Content

```http
GET /api/contents?page=0&size=20
GET /api/contents/{slug}
GET /api/contents/category/{categoryId}
GET /api/contents/tag/{tagId}
GET /api/contents/search?keyword=guide
GET /api/contents/trending?days=7
```

### Courses - Base Path: `/api/courses`

#### Create Course

```http
POST /api/courses
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "INTRO-101",
  "title": "Introduction to CRM",
  "slug": "intro-to-crm",
  "description": "Learn CRM basics",
  "shortDescription": "CRM fundamentals",
  "courseLevel": "BEGINNER",
  "tierRequired": "FREE",
  "price": 0,
  "instructorId": "uuid",
  "categoryId": "uuid",
  "durationHours": 10
}
```

#### Enroll in Course

```http
POST /api/courses/{courseId}/enroll
Authorization: Bearer <token>
Content-Type: application/json

{
  "pricePaid": 0
}
```

#### Get Enrollment

```http
GET /api/courses/{courseId}/enrollment
GET /api/courses/{courseId}/enrollment/check
```

---

## Common Patterns

### Pagination Pattern

**Request:**
```http
GET /api/users?page=0&size=20&sortBy=username&sortDirection=ASC
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [/* items */],
    "totalElements": 100,
    "totalPages": 5,
    "number": 0,
    "size": 20,
    "first": true,
    "last": false
  }
}
```

### Error Response Pattern

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    "Username must be at least 3 characters",
    "Email is required"
  ],
  "timestamp": "2025-12-07T10:30:00Z"
}
```

### Bulk Operation Pattern

**Request:**
```http
POST /api/tasks/bulk/assign
Content-Type: application/json

{
  "taskIds": ["uuid1", "uuid2", "uuid3"],
  "assigneeId": "uuid"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalRequested": 3,
    "successCount": 2,
    "failureCount": 1,
    "successfulTaskIds": ["uuid1", "uuid2"],
    "errors": [
      {
        "taskId": "uuid3",
        "message": "Task not found"
      }
    ]
  }
}
```

### Hierarchical Data Pattern

**Query:**
```http
GET /api/branches/hierarchy
GET /api/branches/{id}/children
GET /api/branches/{id}/ancestors
GET /api/branches/{id}/descendants
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "code": "HQ",
    "name": "Headquarters",
    "level": 0,
    "path": "/HQ",
    "children": [
      {
        "id": "uuid",
        "code": "SALES",
        "name": "Sales Department",
        "level": 1,
        "path": "/HQ/SALES",
        "children": []
      }
    ]
  }
}
```

---

## Summary

This API provides comprehensive endpoints for managing a multi-tenant CRM system with:

- **39 Controllers**
- **500+ Endpoints**
- **11 Functional Areas**
- **Consistent Response Format**
- **JWT Authentication**
- **Role-Based Access Control**
- **Multi-Tenant Support**

For detailed backend architecture, see [architecture-backend.md](./architecture-backend.md).

For security implementation details, see [security-architecture.md](./security-architecture.md).
