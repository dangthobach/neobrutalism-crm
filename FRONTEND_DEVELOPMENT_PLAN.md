# Frontend Development Plan - Neobrutalism CRM

## 📋 Executive Summary

Document này cung cấp roadmap chi tiết để phát triển frontend tích hợp với hệ thống backend CRM đã được triển khai. Backend hiện có 62 controllers với hơn 300+ REST endpoints phục vụ 8 modules chính.

**Tech Stack Backend:**
- Java 21 + Spring Boot 3.3.5
- PostgreSQL (Production) / H2 (Development)
- JWT Authentication + Casbin Authorization
- Multi-tenancy Support
- WebSocket (Real-time Notifications)

**Recommended Tech Stack Frontend:**
- **Framework:** Next.js 14+ (App Router)
- **Language:** TypeScript
- **UI Library:** React 18+
- **Styling:** TailwindCSS + shadcn/ui (Neobrutalism theme)
- **State Management:** React Query (TanStack Query) + Zustand
- **Form Handling:** React Hook Form + Zod
- **API Client:** Axios with interceptors
- **Real-time:** Socket.io-client
- **Charts:** Recharts
- **Date Handling:** date-fns
- **Tables:** TanStack Table

---

## 🏗️ Architecture Overview

### Backend Modules & APIs

#### 1️⃣ **Authentication & Authorization Module**
**Base Path:** `/api/auth`

**Entities:**
- `User` - System users with profile and authentication
- `Role` - RBAC role definitions
- `UserRole` - User-to-role assignments
- `Group` - Hierarchical teams/groups
- `UserGroup` - User-to-group assignments
- `GroupRole` - Group-to-role assignments

**Key Endpoints:**

```typescript
// Authentication
POST   /api/auth/login              // Login with credentials
POST   /api/auth/refresh            // Refresh access token
POST   /api/auth/logout             // Logout user
GET    /api/auth/me                 // Get current user info
GET    /api/auth/status             // Check auth status

// User Management
GET    /api/users                   // List users (paginated)
GET    /api/users/{id}              // Get user by ID
GET    /api/users/username/{username}  // Get by username
GET    /api/users/email/{email}     // Get by email
POST   /api/users                   // Create new user
PUT    /api/users/{id}              // Update user
DELETE /api/users/{id}              // Delete user (soft)
POST   /api/users/{id}/activate     // Activate user
POST   /api/users/{id}/suspend      // Suspend user
POST   /api/users/{id}/lock         // Lock user
POST   /api/users/{id}/unlock       // Unlock user
GET    /api/users/me                // Current user profile
PUT    /api/users/me/profile        // Update own profile
GET    /api/users/me/menus          // Get current user's menus
POST   /api/users/search            // Advanced search

// Role Management
GET    /api/roles                   // List roles
GET    /api/roles/{id}              // Get role details
POST   /api/roles                   // Create role
PUT    /api/roles/{id}              // Update role
DELETE /api/roles/{id}              // Delete role
GET    /api/roles/{id}/users        // Get users with role
GET    /api/roles/{id}/permissions  // Get role permissions

// User Roles Assignment
POST   /api/user-roles              // Assign role to user
DELETE /api/user-roles/{id}         // Remove user role
GET    /api/user-roles/user/{userId}  // Get user's roles

// Group Management
GET    /api/groups                  // List groups
GET    /api/groups/{id}             // Get group details
POST   /api/groups                  // Create group
PUT    /api/groups/{id}             // Update group
DELETE /api/groups/{id}             // Delete group

// User Group Assignment
POST   /api/user-groups             // Add user to group
DELETE /api/user-groups/{id}        // Remove from group
GET    /api/user-groups/user/{userId}  // User's groups
```

---

#### 2️⃣ **Menu & Navigation Module**
**Base Path:** `/api/menus`, `/api/menu-tabs`, `/api/menu-screens`

**Entities:**
- `Menu` - Top-level menu items
- `MenuTab` - Tabs within menus
- `MenuScreen` - Screens/pages accessible from menus
- `ApiEndpoint` - API endpoints for permission control
- `ScreenApiEndpoint` - Screen-to-API mappings
- `RoleMenu` - Role-to-menu permission mappings

**Key Endpoints:**

```typescript
// Menu Management
GET    /api/menus                   // List all menus
GET    /api/menus/{id}              // Get menu details
POST   /api/menus                   // Create menu
PUT    /api/menus/{id}              // Update menu
DELETE /api/menus/{id}              // Delete menu
GET    /api/menus/tree              // Get hierarchical menu tree

// Menu Tabs
GET    /api/menu-tabs               // List tabs
GET    /api/menu-tabs/{id}          // Get tab details
GET    /api/menu-tabs/menu/{menuId}  // Get tabs by menu
POST   /api/menu-tabs               // Create tab
PUT    /api/menu-tabs/{id}          // Update tab
DELETE /api/menu-tabs/{id}          // Delete tab

// Menu Screens
GET    /api/menu-screens            // List screens
GET    /api/menu-screens/{id}       // Get screen details
GET    /api/menu-screens/tab/{tabId}  // Get screens by tab
POST   /api/menu-screens            // Create screen
PUT    /api/menu-screens/{id}       // Update screen
DELETE /api/menu-screens/{id}       // Delete screen

// API Endpoints
GET    /api/api-endpoints           // List API endpoints
POST   /api/api-endpoints           // Register endpoint
PUT    /api/api-endpoints/{id}      // Update endpoint
DELETE /api/api-endpoints/{id}      // Delete endpoint

// Screen-API Mappings
GET    /api/screen-api              // List mappings
POST   /api/screen-api              // Create mapping
DELETE /api/screen-api/{id}         // Delete mapping

// Role Menu Permissions
GET    /api/role-menus              // List role-menu perms
POST   /api/role-menus              // Grant menu to role
DELETE /api/role-menus/{id}         // Revoke menu from role
GET    /api/role-menus/role/{roleId}  // Get role's menus
```

---

#### 3️⃣ **Organization & Branch Module**
**Base Path:** `/api/organizations`, `/api/branches`

**Entities:**
- `Organization` - Multi-tenant organizations (write model)
- `OrganizationReadModel` - Denormalized read model (CQRS)
- `Branch` - Physical/logical branches

**Key Endpoints:**

```typescript
// Organization Management (Command)
POST   /api/organizations           // Create organization
PUT    /api/organizations/{id}      // Update organization
DELETE /api/organizations/{id}      // Delete organization
POST   /api/organizations/{id}/activate    // Activate
POST   /api/organizations/{id}/suspend     // Suspend
POST   /api/organizations/{id}/deactivate  // Deactivate

// Organization Query (Query)
GET    /api/organizations/query     // Search organizations
GET    /api/organizations/query/{id}  // Get org details
GET    /api/organizations/query/active  // List active orgs
GET    /api/organizations/query/search  // Advanced search

// Branch Management
GET    /api/branches                // List branches
GET    /api/branches/{id}           // Get branch details
GET    /api/branches/organization/{orgId}  // Branches by org
POST   /api/branches                // Create branch
PUT    /api/branches/{id}           // Update branch
DELETE /api/branches/{id}           // Delete branch
POST   /api/branches/{id}/activate  // Activate branch
POST   /api/branches/{id}/deactivate  // Deactivate branch
```

---

#### 4️⃣ **CRM - Customer & Contact Module**
**Base Path:** `/api/customers`, `/api/contacts`

**Entities:**
- `Customer` - Customer/Company records
- `Contact` - Individual contact persons
- **Enums:** `CustomerType`, `CustomerStatus`, `Industry`, `ContactRole`, `ContactStatus`

**Key Endpoints:**

```typescript
// Customer Management
GET    /api/customers               // List customers (paginated)
GET    /api/customers/{id}          // Get customer by ID
GET    /api/customers/code/{code}   // Get by code
GET    /api/customers/email/{email}  // Get by email
POST   /api/customers               // Create customer
PUT    /api/customers/{id}          // Update customer
DELETE /api/customers/{id}          // Delete customer
GET    /api/customers/organization/{orgId}  // By organization
GET    /api/customers/owner/{ownerId}  // By owner
GET    /api/customers/branch/{branchId}  // By branch
GET    /api/customers/type/{type}   // By type
GET    /api/customers/status/{status}  // By status
GET    /api/customers/vip           // Get VIP customers
GET    /api/customers/search?keyword  // Search customers
GET    /api/customers/acquisition?startDate&endDate  // By acquisition date
GET    /api/customers/followup?date&status  // Requiring follow-up
GET    /api/customers/tag/{tag}     // By tag
GET    /api/customers/lead-source/{source}  // By lead source

// Customer Status Transitions
POST   /api/customers/{id}/convert-to-prospect  // Lead → Prospect
POST   /api/customers/{id}/convert-to-active    // Prospect → Active
POST   /api/customers/{id}/mark-inactive        // → Inactive
POST   /api/customers/{id}/mark-churned         // → Churned
POST   /api/customers/{id}/blacklist            // → Blacklisted
POST   /api/customers/{id}/reactivate           // Reactivate
POST   /api/customers/{id}/update-contact-date  // Update last contact

// Customer Statistics
GET    /api/customers/stats/by-status?status  // Count by status
GET    /api/customers/stats/by-type?type      // Count by type

// Contact Management
GET    /api/contacts                // List contacts
GET    /api/contacts/{id}           // Get contact by ID
GET    /api/contacts/customer/{customerId}  // By customer
GET    /api/contacts/email/{email}  // By email
POST   /api/contacts                // Create contact
PUT    /api/contacts/{id}           // Update contact
DELETE /api/contacts/{id}           // Delete contact
GET    /api/contacts/primary        // Get primary contacts
GET    /api/contacts/role/{role}    // By role
GET    /api/contacts/status/{status}  // By status
POST   /api/contacts/{id}/set-primary  // Set as primary
POST   /api/contacts/{id}/activate     // Activate
POST   /api/contacts/{id}/deactivate   // Deactivate
```

---

#### 5️⃣ **CMS - Content Management Module**
**Base Path:** `/api/contents`, `/api/content-categories`, `/api/content-tags`, `/api/content-series`

**Entities:**
- `Content` - Main content (write model)
- `ContentReadModel` - Denormalized for fast reads (CQRS)
- `ContentCategory` - Content categories
- `ContentTag` - Tags for content
- `ContentSeries` - Content series/collections
- `ContentView` - View tracking for analytics
- **Enums:** `ContentStatus`, `MemberTier`

**Key Endpoints:**

```typescript
// Content Management (Admin - Write)
POST   /api/contents                // Create content
PUT    /api/contents/{id}           // Update content
DELETE /api/contents/{id}           // Delete content
POST   /api/contents/{id}/publish   // Publish content
POST   /api/contents/{id}/submit-review  // Submit for review
POST   /api/contents/{id}/archive   // Archive content

// Content Consumption (Public - Read)
GET    /api/contents                // List published content
GET    /api/contents/{slug}         // Get by slug
GET    /api/contents/category/{categoryId}  // By category
GET    /api/contents/tag/{tagId}    // By tag
GET    /api/contents/search?keyword  // Search content
GET    /api/contents/trending?days   // Trending content
GET    /api/contents/recent?days     // Recent content
GET    /api/contents/tier/{tier}     // By member tier

// Content Analytics
POST   /api/contents/{id}/view      // Track view
GET    /api/contents/{id}/stats     // View statistics

// Category Management
GET    /api/content-categories      // List categories
GET    /api/content-categories/{id}  // Get category
POST   /api/content-categories      // Create category
PUT    /api/content-categories/{id}  // Update category
DELETE /api/content-categories/{id}  // Delete category
GET    /api/content-categories/with-count  // Categories with content count
GET    /api/content-categories/tree  // Hierarchical tree

// Tag Management
GET    /api/content-tags            // List tags
GET    /api/content-tags/{id}       // Get tag
POST   /api/content-tags            // Create tag
PUT    /api/content-tags/{id}       // Update tag
DELETE /api/content-tags/{id}       // Delete tag
GET    /api/content-tags/popular    // Popular tags
GET    /api/content-tags/with-count  // Tags with content count

// Series Management
GET    /api/content-series          // List series
GET    /api/content-series/{id}     // Get series
POST   /api/content-series          // Create series
PUT    /api/content-series/{id}     // Update series
DELETE /api/content-series/{id}     // Delete series
GET    /api/content-series/with-count  // Series with content count
```

---

#### 6️⃣ **LMS - Learning Management Module**
**Base Path:** `/api/courses`, `/api/lessons`, `/api/quizzes`, `/api/enrollments`

**Entities:**
- `Course` - Online courses
- `CourseModule` - Course modules/chapters
- `Lesson` - Individual lessons
- `Quiz` - Quizzes/assessments
- `QuizQuestion` - Quiz questions
- `QuizAttempt` - User quiz attempts
- `Enrollment` - User course enrollments
- `LessonProgress` - Lesson completion tracking
- `Certificate` - Course completion certificates
- `Achievement` - Gamification achievements
- `UserAchievement` - User-earned achievements
- `CourseReview` - Course reviews/ratings
- **Enums:** `CourseLevel`, `CourseStatus`, `LessonType`, `EnrollmentStatus`

**Key Endpoints:**

```typescript
// Course Management
POST   /api/courses                 // Create course
GET    /api/courses/{courseId}      // Get course details
GET    /api/courses/slug/{slug}     // Get by slug
GET    /api/courses                 // List published courses
GET    /api/courses/search?keyword  // Search courses
GET    /api/courses/instructor/{instructorId}  // By instructor
GET    /api/courses/my-tier?tier    // Courses for tier
POST   /api/courses/{courseId}/publish  // Publish course

// Enrollment
POST   /api/courses/{courseId}/enroll  // Enroll in course
GET    /api/courses/{courseId}/enrollment/check  // Check enrollment
GET    /api/courses/{courseId}/enrollment  // Get enrollment details
GET    /api/enrollments             // List enrollments (admin)
GET    /api/enrollments/{id}        // Get enrollment
POST   /api/enrollments/{id}/complete  // Mark as completed

// Lesson Management
GET    /api/lessons                 // List lessons
GET    /api/lessons/{id}            // Get lesson details
POST   /api/lessons                 // Create lesson
PUT    /api/lessons/{id}            // Update lesson
DELETE /api/lessons/{id}            // Delete lesson
POST   /api/lessons/{id}/complete   // Mark lesson complete

// Quiz Management
GET    /api/quizzes                 // List quizzes
GET    /api/quizzes/{id}            // Get quiz details
POST   /api/quizzes                 // Create quiz
PUT    /api/quizzes/{id}            // Update quiz
DELETE /api/quizzes/{id}            // Delete quiz
POST   /api/quizzes/{id}/submit     // Submit quiz attempt
GET    /api/quizzes/{id}/attempts   // Get user's attempts
```

---

#### 7️⃣ **Notification Module**
**Base Path:** `/api/notifications`

**Entities:**
- `Notification` - User notifications
- **Enums:** `NotificationType`, `NotificationStatus`

**Key Endpoints:**

```typescript
// Notification Management
GET    /api/notifications           // List notifications
GET    /api/notifications/{id}      // Get notification
POST   /api/notifications           // Create notification
PUT    /api/notifications/{id}      // Update notification
DELETE /api/notifications/{id}      // Delete notification
GET    /api/notifications/user/{userId}  // User's notifications
GET    /api/notifications/unread    // Unread notifications
POST   /api/notifications/{id}/mark-read  // Mark as read
POST   /api/notifications/mark-all-read   // Mark all as read
GET    /api/notifications/count/unread    // Unread count
```

**WebSocket Events:**
```typescript
// Real-time notifications via WebSocket
/topic/notifications/{userId}  // Subscribe to user notifications
/app/notification.read         // Send read acknowledgement
```

---

#### 8️⃣ **Attachment & File Management Module**
**Base Path:** `/api/attachments`

**Entities:**
- `Attachment` - File attachments
- **Enums:** `AttachmentType`

**Key Endpoints:**

```typescript
// Attachment Management
POST   /api/attachments/upload      // Upload file
GET    /api/attachments/{id}        // Get attachment metadata
GET    /api/attachments/{id}/download  // Download file
DELETE /api/attachments/{id}        // Delete attachment
GET    /api/attachments/entity/{entityType}/{entityId}  // By entity
GET    /api/attachments/type/{type}  // By type
```

---

## 🎨 Frontend Module Structure

### Recommended Folder Structure

```
src/
├── app/                          # Next.js App Router
│   ├── (auth)/                   # Auth pages layout
│   │   ├── login/
│   │   ├── register/
│   │   └── forgot-password/
│   ├── (dashboard)/             # Main app layout
│   │   ├── layout.tsx           # Dashboard shell
│   │   ├── dashboard/           # Home dashboard
│   │   ├── users/               # User management
│   │   ├── roles/               # Role management
│   │   ├── customers/           # CRM - Customers
│   │   ├── contacts/            # CRM - Contacts
│   │   ├── content/             # CMS - Content
│   │   ├── courses/             # LMS - Courses
│   │   ├── enrollments/         # LMS - Enrollments
│   │   ├── notifications/       # Notifications
│   │   └── settings/            # Settings
│   └── api/                     # API routes (if needed)
├── components/
│   ├── ui/                      # shadcn/ui components
│   ├── layout/                  # Layout components
│   │   ├── navbar.tsx
│   │   ├── sidebar.tsx
│   │   ├── breadcrumb.tsx
│   │   └── footer.tsx
│   ├── auth/                    # Auth components
│   ├── users/                   # User-specific components
│   ├── customers/               # Customer components
│   ├── content/                 # Content components
│   ├── courses/                 # Course components
│   └── shared/                  # Shared/common components
│       ├── data-table/          # Reusable table
│       ├── form-builder/        # Dynamic forms
│       ├── card-grid/
│       └── status-badge/
├── lib/
│   ├── api/                     # API client setup
│   │   ├── client.ts            # Axios instance
│   │   ├── auth.ts              # Auth API calls
│   │   ├── users.ts             # User API calls
│   │   ├── customers.ts         # Customer API calls
│   │   ├── content.ts           # Content API calls
│   │   └── courses.ts           # Course API calls
│   ├── hooks/                   # Custom React hooks
│   │   ├── use-auth.ts
│   │   ├── use-user.ts
│   │   ├── use-customer.ts
│   │   └── use-debounce.ts
│   ├── stores/                  # Zustand stores
│   │   ├── auth-store.ts
│   │   ├── notification-store.ts
│   │   └── ui-store.ts
│   ├── utils/                   # Utility functions
│   │   ├── cn.ts                # Class name merger
│   │   ├── format.ts            # Date, number formatters
│   │   └── validation.ts        # Zod schemas
│   └── constants/               # Constants
│       ├── routes.ts
│       ├── api-endpoints.ts
│       └── enums.ts
├── types/                       # TypeScript types
│   ├── api.ts                   # API response types
│   ├── entities/                # Entity types
│   │   ├── user.ts
│   │   ├── customer.ts
│   │   ├── content.ts
│   │   └── course.ts
│   └── index.ts
└── styles/
    └── globals.css              # Global styles + Tailwind
```

---

## 🚀 Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

#### 1.1 Project Setup
- [ ] Initialize Next.js 14 project with TypeScript
- [ ] Install and configure TailwindCSS
- [ ] Setup shadcn/ui components
- [ ] Configure environment variables (.env.local)
- [ ] Setup ESLint + Prettier

#### 1.2 API Client & Authentication
- [ ] Create Axios instance with interceptors
- [ ] Implement JWT token management (localStorage/cookies)
- [ ] Setup token refresh mechanism
- [ ] Create auth API functions (login, logout, refresh)
- [ ] Implement protected route wrapper
- [ ] Create AuthProvider and useAuth hook

**Files to create:**
```typescript
// lib/api/client.ts
import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle 401, refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const { data } = await axios.post('/api/auth/refresh', { refreshToken });
        localStorage.setItem('accessToken', data.data.accessToken);
        originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Redirect to login
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

```typescript
// lib/api/auth.ts
import apiClient from './client';
import { LoginRequest, LoginResponse } from '@/types/api';

export const authApi = {
  login: async (credentials: LoginRequest) => {
    const { data } = await apiClient.post<ApiResponse<LoginResponse>>(
      '/auth/login',
      credentials
    );
    return data.data;
  },

  logout: async () => {
    await apiClient.post('/auth/logout');
  },

  refresh: async (refreshToken: string) => {
    const { data } = await apiClient.post<ApiResponse<LoginResponse>>(
      '/auth/refresh',
      { refreshToken }
    );
    return data.data;
  },

  getCurrentUser: async () => {
    const { data } = await apiClient.get('/auth/me');
    return data.data;
  },
};
```

#### 1.3 Core UI Components
- [ ] Create dashboard layout (Navbar, Sidebar)
- [ ] Implement responsive sidebar with menu items
- [ ] Create breadcrumb navigation
- [ ] Build DataTable component (reusable)
- [ ] Create FormBuilder for dynamic forms
- [ ] Implement Toast notifications
- [ ] Create Loading states and Error boundaries

---

### Phase 2: User Management Module (Week 3)

#### 2.1 User List & CRUD
- [ ] Users listing page with pagination
- [ ] User detail view
- [ ] Create user form
- [ ] Edit user form
- [ ] Delete user with confirmation
- [ ] User status management (activate, suspend, lock)
- [ ] Search and filter users

#### 2.2 Role Management
- [ ] Roles listing page
- [ ] Create/edit role forms
- [ ] Role permissions management UI
- [ ] Assign roles to users

**Example Component:**
```typescript
// components/users/user-table.tsx
'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/lib/api/users';
import { DataTable } from '@/components/shared/data-table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { User, UserStatus } from '@/types/entities/user';

export function UserTable() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['users', page],
    queryFn: () => userApi.getAll({ page, size: 20 }),
  });

  const deleteMutation = useMutation({
    mutationFn: userApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const columns = [
    { header: 'Username', accessorKey: 'username' },
    { header: 'Email', accessorKey: 'email' },
    { header: 'Name', accessorFn: (row) => `${row.firstName} ${row.lastName}` },
    { 
      header: 'Status', 
      cell: ({ row }) => <StatusBadge status={row.original.status} />
    },
    {
      header: 'Actions',
      cell: ({ row }) => (
        <div className="flex gap-2">
          <Button size="sm" variant="outline">Edit</Button>
          <Button 
            size="sm" 
            variant="destructive"
            onClick={() => deleteMutation.mutate(row.original.id)}
          >
            Delete
          </Button>
        </div>
      ),
    },
  ];

  return (
    <DataTable
      columns={columns}
      data={data?.content || []}
      totalPages={data?.totalPages || 0}
      currentPage={page}
      onPageChange={setPage}
      isLoading={isLoading}
    />
  );
}
```

---

### Phase 3: CRM Module - Customers & Contacts (Week 4-5)

#### 3.1 Customer Management
- [ ] Customer listing with advanced filters
- [ ] Customer detail view (tabs: info, contacts, activities, notes)
- [ ] Create/edit customer forms
- [ ] Customer status transitions (lead → prospect → active)
- [ ] Customer search with autocomplete
- [ ] Customer analytics dashboard (by status, type, owner)
- [ ] VIP customer indicator
- [ ] Tag management for customers

#### 3.2 Contact Management
- [ ] Contact listing by customer
- [ ] Add/edit contact forms
- [ ] Primary contact indicator
- [ ] Contact role badges
- [ ] Email opt-out management
- [ ] Contact hierarchy (reports-to)

#### 3.3 CRM Dashboard
- [ ] Key metrics cards (total customers, conversion rate, etc.)
- [ ] Sales funnel chart
- [ ] Customer acquisition chart (by month)
- [ ] Top customers by revenue
- [ ] Follow-up reminders

---

### Phase 4: CMS Module - Content Management (Week 6-7)

#### 4.1 Content Management (Admin)
- [ ] Content listing with filters (status, category, tag)
- [ ] Rich text editor integration (TipTap/Lexical)
- [ ] Create/edit content forms
- [ ] Content preview
- [ ] Content workflow (draft → review → published)
- [ ] Content versioning display
- [ ] SEO metadata fields
- [ ] Featured image upload
- [ ] Category and tag assignment

#### 4.2 Content Consumption (Public)
- [ ] Public content listing page
- [ ] Content detail page with related content
- [ ] Content search with filters
- [ ] Trending content section
- [ ] Content by category/tag pages
- [ ] Content series navigation
- [ ] Reading progress indicator
- [ ] View tracking (analytics)

#### 4.3 Category & Tag Management
- [ ] Category tree view
- [ ] Drag-and-drop category reordering
- [ ] Tag cloud component
- [ ] Popular tags widget

---

### Phase 5: LMS Module - Learning Management (Week 8-9)

#### 5.1 Course Management
- [ ] Course listing with filters
- [ ] Course detail page
- [ ] Course creation wizard (multi-step form)
- [ ] Module and lesson management
- [ ] Course preview mode
- [ ] Publish/unpublish course
- [ ] Course pricing and tier settings
- [ ] Course analytics (enrollments, completion rate)

#### 5.2 Course Consumption (Student)
- [ ] Course catalog page
- [ ] Course detail with curriculum
- [ ] Enroll in course flow
- [ ] My courses dashboard
- [ ] Course player (video/content display)
- [ ] Lesson navigation (prev/next)
- [ ] Progress tracking (percentage completed)
- [ ] Certificate display and download

#### 5.3 Quiz & Assessment
- [ ] Quiz taking interface
- [ ] Question types support (multiple choice, etc.)
- [ ] Timer display
- [ ] Submit quiz confirmation
- [ ] Quiz results page
- [ ] Attempt history
- [ ] Score display

---

### Phase 6: Notification & Real-time Features (Week 10)

#### 6.1 Notification System
- [ ] Notification bell icon with unread count
- [ ] Notification dropdown panel
- [ ] Notification list page
- [ ] Mark as read functionality
- [ ] Notification types with icons
- [ ] Real-time WebSocket integration
- [ ] Browser notification permissions

#### 6.2 Real-time Updates
- [ ] Setup Socket.io client
- [ ] Subscribe to user notification channel
- [ ] Handle incoming notifications
- [ ] Toast notifications for real-time events
- [ ] Connection status indicator

**Example WebSocket Integration:**
```typescript
// lib/websocket/client.ts
import io from 'socket.io-client';

export const socket = io(process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080', {
  autoConnect: false,
  auth: {
    token: localStorage.getItem('accessToken'),
  },
});

export const subscribeToNotifications = (userId: string, callback: (notification: any) => void) => {
  socket.on(`/topic/notifications/${userId}`, callback);
};

export const unsubscribeFromNotifications = (userId: string) => {
  socket.off(`/topic/notifications/${userId}`);
};
```

---

### Phase 7: Advanced Features & Polish (Week 11-12)

#### 7.1 Search & Filtering
- [ ] Global search (across all modules)
- [ ] Advanced filter panels
- [ ] Saved filters
- [ ] Export filtered data (CSV, Excel)

#### 7.2 Settings & Preferences
- [ ] User profile settings
- [ ] Password change
- [ ] Notification preferences
- [ ] Theme toggle (light/dark mode)
- [ ] Language selection

#### 7.3 Responsive & Accessibility
- [ ] Mobile responsive design for all pages
- [ ] Touch-friendly interactions
- [ ] Keyboard navigation support
- [ ] ARIA labels
- [ ] Color contrast compliance

#### 7.4 Performance Optimization
- [ ] Implement React Query with proper caching
- [ ] Image optimization (Next.js Image)
- [ ] Code splitting by routes
- [ ] Lazy loading for heavy components
- [ ] Debounced search inputs
- [ ] Virtualized lists for large datasets

---

## 📊 Data Flow & State Management

### React Query (TanStack Query)

**Query Configuration:**
```typescript
// lib/query-client.ts
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000, // 1 minute
      cacheTime: 5 * 60 * 1000, // 5 minutes
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});
```

**Query Hooks Pattern:**
```typescript
// lib/hooks/use-users.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/lib/api/users';

export function useUsers(page: number, size: number = 20) {
  return useQuery({
    queryKey: ['users', page, size],
    queryFn: () => userApi.getAll({ page, size }),
  });
}

export function useUser(id: string) {
  return useQuery({
    queryKey: ['users', id],
    queryFn: () => userApi.getById(id),
    enabled: !!id,
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: userApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });
}
```

### Zustand for Global State

**Auth Store:**
```typescript
// lib/stores/auth-store.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  login: (user: User, accessToken: string, refreshToken: string) => void;
  logout: () => void;
  updateUser: (user: User) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      login: (user, accessToken, refreshToken) => {
        set({ user, accessToken, refreshToken, isAuthenticated: true });
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
      },
      logout: () => {
        set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
      },
      updateUser: (user) => set({ user }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ user: state.user }),
    }
  )
);
```

---

## 🎨 UI/UX Design Guidelines

### Neobrutalism Theme

**Color Palette:**
```css
/* Primary Colors */
--brutalism-black: #000000;
--brutalism-white: #FFFFFF;
--brutalism-yellow: #FFD700;
--brutalism-red: #FF0000;
--brutalism-blue: #0000FF;
--brutalism-green: #00FF00;

/* Border & Shadow */
--border-thick: 4px solid #000000;
--shadow-brutal: 8px 8px 0 #000000;
```

**Component Style:**
- Bold, thick borders (3-5px)
- High contrast colors
- Drop shadows (not blur, solid offset)
- Chunky buttons
- Monospace fonts for code
- Grid-based layouts

**Example Button:**
```tsx
<Button className="
  bg-brutalism-yellow 
  text-black 
  border-4 
  border-black 
  shadow-[8px_8px_0_0_#000] 
  hover:shadow-[4px_4px_0_0_#000] 
  hover:translate-x-1 
  hover:translate-y-1 
  font-bold 
  uppercase 
  tracking-wider
">
  Click Me
</Button>
```

---

## 🧪 Testing Strategy

### Unit Tests (Vitest)
- Test utility functions
- Test custom hooks
- Test API client functions

### Component Tests (React Testing Library)
- Test UI components in isolation
- Test user interactions
- Test conditional rendering

### E2E Tests (Playwright)
- Test critical user flows
- Test authentication flow
- Test CRUD operations

---

## 📦 Dependencies

**Core:**
```json
{
  "next": "^14.2.0",
  "react": "^18.3.0",
  "react-dom": "^18.3.0",
  "typescript": "^5.5.0"
}
```

**UI & Styling:**
```json
{
  "tailwindcss": "^3.4.0",
  "@radix-ui/react-*": "latest",
  "class-variance-authority": "^0.7.0",
  "clsx": "^2.1.0",
  "tailwind-merge": "^2.3.0",
  "lucide-react": "^0.395.0"
}
```

**State & Data Fetching:**
```json
{
  "@tanstack/react-query": "^5.45.0",
  "zustand": "^4.5.0",
  "axios": "^1.7.0"
}
```

**Forms & Validation:**
```json
{
  "react-hook-form": "^7.51.0",
  "zod": "^3.23.0",
  "@hookform/resolvers": "^3.6.0"
}
```

**Utilities:**
```json
{
  "date-fns": "^3.6.0",
  "socket.io-client": "^4.7.0",
  "recharts": "^2.12.0"
}
```

---

## 🔐 Security Considerations

1. **Authentication:**
   - Store JWT in httpOnly cookies (preferred) or localStorage
   - Implement token refresh before expiration
   - Clear tokens on logout

2. **Authorization:**
   - Check user permissions before rendering UI
   - Validate permissions on backend (never trust client)
   - Hide/disable actions user can't perform

3. **Input Validation:**
   - Validate all user inputs with Zod schemas
   - Sanitize HTML content (XSS prevention)
   - Use parameterized queries (SQL injection prevention)

4. **HTTPS:**
   - Always use HTTPS in production
   - Set secure flags on cookies

---

## 📈 Performance Targets

- **Initial Load:** < 2s (3G)
- **Time to Interactive:** < 3s
- **First Contentful Paint:** < 1.5s
- **Lighthouse Score:** > 90

**Optimization Techniques:**
- Server-side rendering for initial pages
- Static generation for public content
- Image optimization (WebP, lazy loading)
- Code splitting by routes
- Tree shaking unused code
- Minification and compression (Gzip/Brotli)

---

## 🚀 Deployment

**Recommended Platforms:**
- **Vercel** (Next.js optimized)
- **Netlify**
- **AWS Amplify**
- **Self-hosted** (Docker + Nginx)

**Environment Variables:**
```env
NEXT_PUBLIC_API_URL=https://api.example.com
NEXT_PUBLIC_WS_URL=wss://api.example.com
NEXT_PUBLIC_APP_NAME=Neobrutalism CRM
```

---

## 📚 Documentation

**To be created:**
- [ ] API Integration Guide
- [ ] Component Library Documentation
- [ ] Deployment Guide
- [ ] Contributing Guidelines
- [ ] User Manual

---

## ✅ Success Criteria

- [ ] All 8 modules implemented
- [ ] Responsive on mobile, tablet, desktop
- [ ] < 2s page load time
- [ ] 100% TypeScript coverage
- [ ] Comprehensive error handling
- [ ] Accessibility (WCAG AA)
- [ ] 80%+ test coverage
- [ ] Production deployment

---

## 🤝 Team & Timeline

**Estimated Team Size:** 2-3 Frontend Developers  
**Estimated Timeline:** 12 weeks (3 months)

**Breakdown:**
- Week 1-2: Foundation & Setup
- Week 3: User Management
- Week 4-5: CRM Module
- Week 6-7: CMS Module
- Week 8-9: LMS Module
- Week 10: Notifications & Real-time
- Week 11-12: Polish & Launch

---

## 📞 Next Steps

1. **Review** this plan with team
2. **Setup** development environment
3. **Create** project repository
4. **Initialize** Next.js project
5. **Start** Phase 1 implementation

---

**Document Version:** 1.0  
**Last Updated:** November 2, 2025  
**Prepared By:** AI Assistant  

---

## Appendix: Full API Endpoint Reference

See individual module sections above for complete endpoint listings. Total: **300+ REST endpoints** across 62 controllers.

**Key Stats:**
- 8 major modules
- 40+ entities
- 15+ enums
- Multi-tenancy support
- CQRS pattern (Content, Organization)
- Real-time notifications (WebSocket)
- File upload/download
- Advanced search & filtering
- Pagination & sorting
- Soft delete support
- Audit trail (createdBy, updatedBy, timestamps)
