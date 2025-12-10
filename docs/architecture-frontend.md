# Frontend Architecture - Neobrutalism CRM

**Generated:** 2025-12-07
**Project:** Neobrutalism CRM
**Technology Stack:** Next.js 16.0.4 + React 19.0.0 + TypeScript 5.1.6

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [API Client Architecture](#api-client-architecture)
5. [State Management](#state-management)
6. [Component Architecture](#component-architecture)
7. [Authentication & Authorization](#authentication--authorization)
8. [Real-Time Features](#real-time-features)
9. [Performance Optimizations](#performance-optimizations)

---

## Architecture Overview

The frontend follows a **component-based architecture** with clear separation between UI, business logic, and data fetching:

```
┌─────────────────────────────────────────┐
│         Pages/Routes (App Router)       │
│         (Next.js 16 App Directory)      │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      Feature Components Layer           │
│    (Task, Customer, Contact, etc.)      │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         Custom Hooks Layer              │
│   (Data Fetching, State Management)     │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         API Client Layer                │
│    (REST API, WebSocket, Caching)       │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│       UI Components Library             │
│    (Shadcn/ui + Radix UI primitives)    │
└─────────────────────────────────────────┘
```

### Key Architectural Principles

1. **Component Composition** - Build complex UIs from small, reusable components
2. **Hook-Based Logic** - Centralize data fetching and state in custom hooks
3. **Type Safety** - Comprehensive TypeScript coverage
4. **Server-First** - Leverage Next.js App Router for server components
5. **Client Interactivity** - Strategic use of client components for dynamic UI
6. **Performance** - Code splitting, lazy loading, and optimistic updates

---

## Technology Stack

### Core Framework

| Technology | Version | Purpose |
|------------|---------|---------|
| **Next.js** | 16.0.4 | React framework with App Router |
| **React** | 19.0.0 | Component-based UI library |
| **TypeScript** | 5.1.6 | Type-safe development |
| **TailwindCSS** | 4.0.9 | Utility-first CSS framework |

### State Management & Data Fetching

| Technology | Version | Purpose |
|------------|---------|---------|
| **TanStack Query** | 5.90.5 | Server state management |
| **TanStack Table** | 8.15.3 | Data grid/table functionality |
| **React Hook Form** | 7.51.2 | Form state management |
| **Zod** | 3.22.4 | Schema validation |

### UI Components

| Technology | Version | Purpose |
|------------|---------|---------|
| **Radix UI** | Various | Accessible UI primitives |
| **Lucide React** | 0.477.0 | Icon library |
| **Sonner** | 2.0.1 | Toast notifications |
| **Recharts** | 2.15.3 | Data visualization |

### Real-Time Communication

| Technology | Version | Purpose |
|------------|---------|---------|
| **STOMP.js** | 7.2.1 | WebSocket client (STOMP protocol) |
| **SockJS Client** | 1.6.1 | WebSocket fallback to HTTP long-polling |

---

## Project Structure

```
src/
├── app/                          # Next.js App Router pages
│   ├── admin/                    # Admin interface routes
│   │   ├── layout.tsx           # Admin layout with sidebar
│   │   ├── page.tsx             # Dashboard
│   │   ├── users/               # User management
│   │   ├── roles/               # Role management
│   │   ├── groups/              # Group management
│   │   ├── organizations/       # Organization management
│   │   ├── permissions/         # Permission management
│   │   ├── menus/               # Menu management
│   │   ├── customers/           # Customer management
│   │   ├── contacts/            # Contact management
│   │   ├── tasks/               # Task management
│   │   └── notifications/       # Notification center
│   ├── login/                    # Login page
│   └── layout.tsx               # Root layout
├── components/                   # React components
│   ├── ui/                      # Base UI components (70+)
│   │   ├── button.tsx
│   │   ├── card.tsx
│   │   ├── dialog.tsx
│   │   ├── data-table.tsx
│   │   └── ...
│   ├── activity/                # Activity components
│   ├── contact/                 # Contact components
│   ├── customer/                # Customer components
│   ├── task/                    # Task components
│   ├── notifications/           # Notification components
│   ├── auth/                    # Auth components
│   │   ├── permission-guard.tsx
│   │   └── protected-route.tsx
│   ├── app/                     # App-level components
│   │   ├── admin-sidebar.tsx
│   │   ├── navbar.tsx
│   │   └── theme-provider.tsx
│   └── providers/               # Context providers
│       ├── query-provider.tsx
│       └── websocket-provider.tsx
├── hooks/                        # Custom React hooks (40+)
│   ├── useUsers.ts
│   ├── useRoles.ts
│   ├── useTasks.ts
│   ├── useCustomers.ts
│   ├── useNotifications.ts
│   ├── usePermission.tsx
│   ├── useWebSocketNotifications.ts
│   └── ...
├── lib/                          # Utilities and libraries
│   ├── api/                     # API client modules (28+)
│   │   ├── client.ts           # Core API client
│   │   ├── users.ts
│   │   ├── roles.ts
│   │   ├── tasks.ts
│   │   ├── customers.ts
│   │   └── ...
│   ├── validations/             # Zod schemas
│   │   ├── user.ts
│   │   ├── customer.ts
│   │   ├── role.ts
│   │   └── ...
│   ├── websocket.ts            # WebSocket manager
│   └── utils.ts                # Utility functions
├── types/                        # TypeScript types
│   ├── task.ts
│   ├── customer.ts
│   ├── permission.ts
│   └── ...
├── contexts/                     # React contexts
│   └── auth-context.tsx        # Authentication context
└── middleware.ts                # Next.js middleware (route protection)
```

---

## API Client Architecture

### Core API Client

**Location:** `src/lib/api/client.ts`

**Pattern:** Singleton with automatic token management

**Key Features:**

1. **Automatic Response Unwrapping**
   - Backend sends: `{ success: true, data: {...}, message: "..." }`
   - Client extracts: `data` object automatically

2. **JWT Token Management**
   - Access token: In-memory + localStorage + cookie
   - Refresh token: localStorage (persistent)
   - Automatic refresh on 401 responses
   - Token expiration tracking

3. **Request Retry Logic**
   - Max 3 retries with exponential backoff
   - Retries on network errors and 5xx responses
   - Configurable retry delays

4. **Caching**
   - Built-in TTL cache (1 minute default)
   - Cache key: `${method}:${endpoint}:${JSON.stringify(params)}`
   - Bypass cache option for mutations

5. **Error Handling**
   - Custom `ApiError` class with status, code, message, data
   - Network error detection (status 0)
   - Server error detection (5xx)
   - Timeout handling (30s default)

**Core Methods:**
```typescript
class ApiClient {
  get<T>(endpoint: string, params?: Record<string, any>, useCache = true): Promise<T>
  post<T>(endpoint: string, body?: any): Promise<T>
  put<T>(endpoint: string, body?: any): Promise<T>
  patch<T>(endpoint: string, body?: any): Promise<T>
  delete<T>(endpoint: string): Promise<T>

  setAccessToken(token: string | null): void
  getAccessToken(): string | null
  clearRefreshToken(): void
}
```

**Usage Example:**
```typescript
const users = await apiClient.get<User[]>('/api/users', { page: 0, size: 20 });
const newUser = await apiClient.post<User>('/api/users', { username: 'john', email: 'john@example.com' });
```

### API Service Modules

**Count:** 28+ specialized service modules

**Pattern:** Object-based exports with typed functions

**Examples:**

#### Users API (`src/lib/api/users.ts`)

**Endpoints:** 20+ operations
- CRUD: `getUsers`, `getUserById`, `createUser`, `updateUser`, `deleteUser`
- Lookups: `getUserByUsername`, `getUserByEmail`
- Filtering: `getUsersByOrganization`, `getUsersByStatus`
- Status: `activateUser`, `suspendUser`, `lockUser`, `unlockUser`
- Search: `searchUsers`, `checkUsername`, `checkEmail`
- Profile: `getCurrentUserProfile`, `updateCurrentUserProfile`
- Menus: `getCurrentUserMenus`, `getUserMenus`

#### Tasks API (`src/lib/api/tasks.ts`)

**Endpoints:** 30+ operations
- CRUD: `getAll`, `getById`, `create`, `update`, `delete`
- Filtered: `getByCustomer`, `getByContact`, `getByAssignee`, `getMyTasks`
- Time-based: `getOverdue`, `getDueToday`, `getDueThisWeek`
- Board: `getBoard` (Kanban view)
- Status: `changeStatus`, `changePriority`, `assign`, `complete`, `cancel`
- Comments: `getComments`, `addComment`, `deleteComment`
- Analytics: `getStats`, `search`

#### Organizations API (`src/lib/api/organizations.ts`)

**Features:**
- Input validation (name: 2-200 chars, code: 2-50 chars uppercase)
- UUID format validation
- Comprehensive CRUD with status operations
- Read model queries via separate endpoints

**All Service Modules:**
1. users.ts, roles.ts, groups.ts, organizations.ts
2. tasks.ts, customers.ts, contacts.ts, activities.ts
3. menus.ts, menu-tabs.ts, menu-screens.ts, api-endpoints.ts
4. notifications.ts, notification-preferences.ts
5. courses.ts, course-modules.ts, course-lessons.ts, course-enrollments.ts
6. content.ts, content-categories.ts, content-tags.ts, content-series.ts
7. branches.ts, user-roles.ts, user-groups.ts, group-roles.ts, role-menus.ts

---

## State Management

### React Query Integration

**Provider:** `src/components/providers/query-provider.tsx`

**Configuration:**
```typescript
queries: {
  staleTime: 60 * 1000           // 1 minute
  gcTime: 5 * 60 * 1000          // 5 minutes
  retry: 1                        // Retry once
  retryDelay: 1000               // 1 second
  refetchOnWindowFocus: false
  refetchOnReconnect: true
  refetchOnMount: true
}

mutations: {
  retry: 1
  retryDelay: 1000
}
```

### Custom Hooks Pattern

**Count:** 40+ custom hooks

**Pattern:** Query keys factory + React Query hooks

**Example:** `src/hooks/useRoles.ts`

```typescript
// Query keys factory
export const roleKeys = {
  all: ['roles'] as const,
  lists: () => [...roleKeys.all, 'list'] as const,
  list: (params: RoleQueryParams) => [...roleKeys.lists(), params] as const,
  details: () => [...roleKeys.all, 'detail'] as const,
  detail: (id: string) => [...roleKeys.details(), id] as const,
  byCode: (code: string) => [...roleKeys.all, 'code', code] as const,
}

// Query hooks
export function useRoles(params?: RoleQueryParams) {
  return useQuery({
    queryKey: roleKeys.list(params || {}),
    queryFn: () => rolesApi.getRoles(params),
    staleTime: 5 * 60 * 1000,
  });
}

export function useRole(id: string) {
  return useQuery({
    queryKey: roleKeys.detail(id),
    queryFn: () => rolesApi.getRoleById(id),
    enabled: !!id,
  });
}

// Mutation hooks
export function useCreateRole() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: rolesApi.createRole,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: roleKeys.all });
      toast.success('Role created successfully');
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to create role');
    },
  });
}
```

### Stale Times by Query Type

| Query Type | Stale Time | Notes |
|------------|------------|-------|
| List queries | 5 minutes | User lists, role lists, etc. |
| Detail queries | Default (1 min) | Single entity fetches |
| Notifications | 10-30 seconds | Adaptive polling |
| Unread count | 10 seconds | Real-time feel |
| Stats | 30 seconds | Dashboard metrics |

### Optimistic Updates

**Implemented in:**
- Notification hooks (mark as read, delete)
- Comment hooks (add, update, delete)
- Bulk operations (assign, status change)

**Pattern:**
```typescript
useMutation({
  mutationFn: async (id: string) => await api.markAsRead(id),
  onMutate: async (id) => {
    // Cancel queries
    await queryClient.cancelQueries({ queryKey: notificationKeys.all });

    // Get previous data
    const previous = queryClient.getQueryData(notificationKeys.all);

    // Update optimistically
    queryClient.setQueryData(notificationKeys.all, (old: Notification[]) =>
      old.map(n => n.id === id ? { ...n, isRead: true } : n)
    );

    return { previous };
  },
  onError: (error, variables, context) => {
    // Rollback on error
    queryClient.setQueryData(notificationKeys.all, context.previous);
  },
});
```

---

## Component Architecture

### UI Component Library

**Count:** 70+ base components

**Source:** Shadcn/ui (Radix UI primitives)

**Categories:**

1. **Form Components**
   - Button, Input, Label, Checkbox, Radio, Switch
   - Select, Combobox, DatePicker
   - Form (React Hook Form integration)

2. **Layout Components**
   - Card, Dialog, Drawer, Popover
   - Tabs, Accordion, Collapsible
   - Sidebar, NavigationMenu

3. **Data Display**
   - Table, DataTable (generic), GenericDataTable
   - Badge, Avatar, Alert
   - Skeleton (loading states)
   - ProgressBar, Carousel

4. **Feedback**
   - Toast (Sonner)
   - AlertDialog
   - ContextMenu, DropdownMenu

5. **Advanced**
   - Chart (Recharts integration)
   - ResizablePanel
   - ScrollArea
   - Command (cmdk)

**Location:** `src/components/ui/`

### Feature Components

**Count:** 70+ business components

**Organization:** By domain

#### Task Components (`src/components/task/`)

- `TaskCard` - Display task card
- `TaskBoard` - Kanban board view
- `TaskForm` - Create/edit form
- `TaskComments` - Comment section with threading
- `Checklist`, `ChecklistItem` - Task checklist
- `CommentList`, `CommentItem`, `AddComment` - Comment UI
- `ActivityTimeline`, `ActivityItem` - Audit trail
- `TaskStatusBadge`, `TaskPriorityBadge`, `TaskCategoryBadge`
- `BulkActionToolbar` - Multi-select operations
- `TaskEditModal` - Edit in modal

#### Customer Components (`src/components/customer/`)

- `CustomerCard` - Display card
- `CustomerForm` - Create/edit form
- `CustomerTable` - Paginated table
- `CustomerStatusBadge` - Status indicator

#### Contact Components (`src/components/contact/`)

- `ContactCard` - Display card
- `ContactForm` - Create/edit form
- `ContactTable` - Paginated table
- `ContactStatusBadge`, `ContactRoleBadge` - Indicators

#### Notification Components (`src/components/notifications/`)

- `NotificationBell` - Badge with unread count
- `NotificationDropdown` - Dropdown list
- `NotificationItem` - Single notification
- `NotificationList` - Paginated list
- `NotificationFilters` - Filter UI
- `WebSocketStatus` - Connection indicator

**All Feature Component Directories:**
- activity/, contact/, contacts/, customer/, customers/
- task/, tasks/, course/, content/
- notifications/, app/, auth/, errors/

---

## Authentication & Authorization

### Auth Context

**Location:** `src/contexts/auth-context.tsx`

**Features:**
- JWT token-based authentication
- Automatic token refresh (60s before expiry)
- User profile caching
- SSR-safe localStorage access

**API:**
```typescript
interface User {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  fullName?: string
  avatar?: string
  organizationId: string
  roles: string[]
  permissions: string[]
}

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login(credentials: LoginRequest): Promise<void>
  logout(): void
  refreshToken(): Promise<void>
}
```

**Usage:**
```typescript
const { user, isAuthenticated, login, logout } = useAuth();

if (!isAuthenticated) {
  return <LoginForm onSubmit={login} />;
}
```

### Permission System

**Hook:** `src/hooks/usePermission.tsx`

**API:**
```typescript
usePermission() returns {
  hasPermission(routeOrCode: string, action: PermissionAction | MenuPermission): boolean
  canView(routeOrCode: string): boolean
  canCreate(routeOrCode: string): boolean
  canEdit(routeOrCode: string): boolean
  canDelete(routeOrCode: string): boolean
  canExport(routeOrCode: string): boolean
  canImport(routeOrCode: string): boolean
  getPermissions(routeOrCode: string): MenuPermissions
  userMenus: UserMenu[]
}
```

**Permission Types:**
```typescript
type PermissionAction = 'READ' | 'WRITE' | 'DELETE' | 'EXECUTE'
type MenuPermission = 'canView' | 'canCreate' | 'canEdit' | 'canDelete' | 'canExport' | 'canImport'
```

**Components:** `src/components/auth/permission-guard.tsx`

```typescript
// Single permission
<PermissionGuard route="/users" permission="canCreate">
  <Button>Create User</Button>
</PermissionGuard>

// All permissions required
<PermissionGuardAll route="/users" permissions={['canView', 'canCreate']}>
  <UserManagement />
</PermissionGuardAll>

// Any permission required
<PermissionGuardAny route="/users" permissions={['canCreate', 'canEdit']}>
  <ModifyButton />
</PermissionGuardAny>
```

**HOC Wrapper:**
```typescript
const ProtectedComponent = withPermission(Component, '/users', 'canDelete');
```

### Route Protection

**Middleware:** `src/middleware.ts`

**Logic:**
```typescript
Public Routes: /login, /register, /forgot-password
Protected Routes: /admin/**

Flow:
  Has access_token?
    Yes + /login → Redirect to /admin
    Yes + /admin → Allow
    No + /admin → Redirect to /login?returnUrl=...
    No + public → Allow
```

---

## Real-Time Features

### WebSocket Integration

**Manager:** `src/lib/websocket.ts`

**Architecture:**
- STOMP over WebSocket
- SockJS fallback to HTTP long-polling
- Singleton pattern for connection sharing
- Automatic reconnection with exponential backoff

**Configuration:**
```typescript
interface WebSocketConfig {
  url: string                        // Default: process.env.NEXT_PUBLIC_WS_URL
  reconnectDelay: number             // 2000ms start
  maxReconnectDelay: number          // 30000ms max
  heartbeatIncoming: number          // 10000ms
  heartbeatOutgoing: number          // 10000ms
  debug: boolean
}
```

**Core API:**
```typescript
class WebSocketManager {
  connect(): void
  disconnect(): void
  onNotification(callback: (notification: Notification) => void): () => void
  onConnect(callback: () => void): () => void
  onError(callback: (error: any) => void): () => void
  send(destination: string, body: any): void
  isConnected(): boolean
  getState(): 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED'
}
```

**Subscription:** `/user/queue/notifications`

### WebSocket Hook

**Location:** `src/hooks/useWebSocketNotifications.ts`

**Features:**
- Auto-connect/disconnect based on userId
- Real-time notification delivery
- Unread count updates
- React Query cache invalidation
- Toast notifications
- Reconnection handling

**API:**
```typescript
useWebSocketNotifications(options: {
  userId?: string
  enabled?: boolean
  onNotification?: (notification: NotificationMessage) => void
  onUnreadCountUpdate?: (count: number) => void
  showToast?: boolean
}) returns {
  isConnected: boolean
  isReconnecting: boolean
  reconnectAttempt: number
  error: Error | null
  unreadCount: number
  connect(): Promise<void>
  disconnect(): Promise<void>
  reconnect(): Promise<void>
}
```

**Usage:**
```typescript
const { isConnected, unreadCount } = useWebSocketNotifications({
  userId: user?.id,
  enabled: isAuthenticated,
  showToast: true,
});
```

---

## Performance Optimizations

### Implemented Optimizations

1. **Request Caching**
   - TTL-based cache (1 minute default)
   - Request deduplication
   - Stale-while-revalidate pattern

2. **React Query Optimizations**
   - Query key factories for consistent cache keys
   - Stale time configuration per query type
   - Garbage collection (5 minutes)
   - Optimistic updates for instant feedback

3. **Adaptive Polling**
   - Notifications: 30s when unread → 120s when empty
   - Exponential backoff for reconnection
   - Smart refetch on window focus (disabled globally)

4. **Code Splitting**
   - Next.js automatic code splitting
   - Dynamic imports for heavy components
   - Route-based splitting via App Router

5. **Image Optimization**
   - Next.js Image component
   - Automatic WebP conversion
   - Lazy loading

6. **WebSocket Optimizations**
   - Single shared connection
   - Connection pooling/reuse
   - Heartbeat monitoring
   - SockJS fallback for compatibility

7. **Memory Management**
   - Query cache garbage collection
   - WebSocket cleanup on unmount
   - Event listener cleanup
   - AbortController for request cancellation

---

## Form Validation

### Zod Schemas

**Location:** `src/lib/validations/`

**Available Schemas:**

1. **User Schema** (`user.ts`)
   - UserSchema, CreateUserSchema, UpdateUserSchema
   - ChangePasswordSchema (8+ chars, uppercase, lowercase, digits)
   - UserStatusSchema

2. **Customer Schema** (`customer.ts`)
   - CustomerSchema, CreateCustomerSchema, UpdateCustomerSchema
   - CustomerTypeSchema, CustomerStatusSchema

3. **Contact Schema** (`contact.ts`)
   - ContactSchema, CreateContactSchema, UpdateContactSchema
   - ContactTypeSchema

4. **Role Schema** (`role.ts`)
   - RoleSchema, CreateRoleSchema, UpdateRoleSchema
   - RoleStatusSchema, PermissionSchema
   - SYSTEM_ROLES, DEFAULT_PERMISSIONS

5. **Group Schema** (`group.ts`)
   - GroupSchema, CreateGroupSchema, UpdateGroupSchema
   - UserGroupSchema, AddUserToGroupSchema

6. **Branch Schema** (`branch.ts`)
   - BranchSchema, CreateBranchSchema, UpdateBranchSchema
   - BranchTypeSchema, BranchStatusSchema

**Pattern:**
```typescript
import { z } from 'zod';

export const CreateUserSchema = z.object({
  username: z.string().min(3).max(50),
  email: z.string().email(),
  password: z.string()
    .min(8)
    .regex(/[A-Z]/, 'Must contain uppercase')
    .regex(/[a-z]/, 'Must contain lowercase')
    .regex(/[0-9]/, 'Must contain digits'),
  firstName: z.string().min(1).max(100),
  lastName: z.string().min(1).max(100),
});

export type CreateUserRequest = z.infer<typeof CreateUserSchema>;
```

**Integration with React Hook Form:**
```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

const form = useForm<CreateUserRequest>({
  resolver: zodResolver(CreateUserSchema),
  defaultValues: {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  },
});
```

---

## Utility Functions

**Location:** `src/lib/utils.ts`

**Functions:**

```typescript
// Class name merging (Tailwind)
cn(...inputs: ClassValue[]): string

// String transformations
addSpaces(name: string): string                    // camelCase → camel Case
transformToSlug(input: string): string             // String to URL slug
transformToName(input: string): string             // slug-name → Slug Name
transformToPascalCase(input: string): string       // string input → StringInput

// Formatting
formatCurrency(value: number, currency?: string): string  // $1,234
formatDate(dateString: string, options?): string          // Jan 1, 2024
formatDateTime(dateString: string): string                // Jan 1, 2024, 02:30 PM
formatRelativeTime(dateString: string): string            // 2 minutes ago
```

---

## Development Tools

1. **React Query DevTools** - Development mode only, bottom-right position
2. **API Request Logging** - Development mode console logging
3. **WebSocket Debug Logging** - Development mode WebSocket messages
4. **Toast Notifications** - User feedback via Sonner
5. **TypeScript Strict Mode** - Full type safety
6. **Zod Runtime Validation** - Schema validation

---

## Summary Statistics

| Category | Count |
|----------|-------|
| UI Components | 70+ |
| Feature Components | 70+ |
| Custom Hooks | 40+ |
| API Service Modules | 28+ |
| Total Endpoints | 250+ |
| Validation Schemas | 6 major schemas |
| TypeScript Interfaces | 100+ |

---

## Next Steps

For admin interface details, see the admin interface documentation in the comprehensive scan results.

For API contracts, see [api-contracts-backend.md](./api-contracts-backend.md).

For security implementation, see [security-architecture.md](./security-architecture.md).
