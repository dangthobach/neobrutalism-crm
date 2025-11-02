# Frontend API Patterns and Hook Conventions

## Overview

This document defines the standardized patterns and conventions for API services and React Query hooks in the Neobrutalism CRM application.

**Last Updated:** October 29, 2025
**Status:** ✅ Complete

---

## Table of Contents

1. [API Service Layer](#api-service-layer)
2. [React Query Hooks](#react-query-hooks)
3. [Query Key Conventions](#query-key-conventions)
4. [Error Handling](#error-handling)
5. [Cache Management](#cache-management)
6. [Best Practices](#best-practices)
7. [Examples](#examples)

---

## API Service Layer

### File Structure

```
src/lib/api/
├── client.ts          # API client with auth, error handling, retries
├── index.ts           # Central export point
├── users.ts           # User API service
├── roles.ts           # Role API service
├── menus.ts           # Menu API service
├── menu-tabs.ts       # Menu Tab API service
├── menu-screens.ts    # Menu Screen API service
├── api-endpoints.ts   # API Endpoint API service
└── ...
```

### API Service Pattern

**All API services MUST:**
1. Import and use `apiClient` from `./client`
2. Export TypeScript interfaces for entities and requests
3. Implement a class with methods for CRUD operations
4. Export a singleton instance
5. Follow consistent naming conventions

**Template:**

```typescript
/**
 * [Entity Name] API Service
 * Handles all [entity]-related API calls
 */

import { apiClient, ApiResponse, PageResponse } from './client'

// Entity interface
export interface MyEntity {
  id: string
  name: string
  // ... other fields
  createdAt: string
  updatedAt: string
}

// Query params interface
export interface MyEntityQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
  // ... custom filters
}

// Request interfaces
export interface CreateMyEntityRequest {
  name: string
  // ... required fields
}

export interface UpdateMyEntityRequest {
  name?: string
  // ... optional fields for update
}

export class MyEntityApi {
  /**
   * Get all entities with pagination
   */
  async getAll(params?: MyEntityQueryParams): Promise<PageResponse<MyEntity>> {
    const response = await apiClient.get<PageResponse<MyEntity>>('/my-entities', params)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get entity by ID
   */
  async getById(id: string): Promise<MyEntity> {
    const response = await apiClient.get<MyEntity>(`/my-entities/${id}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Create new entity
   */
  async create(request: CreateMyEntityRequest): Promise<MyEntity> {
    const response = await apiClient.post<MyEntity>('/my-entities', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update entity
   */
  async update(id: string, request: UpdateMyEntityRequest): Promise<MyEntity> {
    const response = await apiClient.put<MyEntity>(`/my-entities/${id}`, request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Delete entity (soft delete)
   */
  async delete(id: string): Promise<void> {
    await apiClient.delete(`/my-entities/${id}`)
  }
}

// Export singleton instance
export const myEntityApi = new MyEntityApi()
```

### API Client Features

The `apiClient` from [src/lib/api/client.ts](../src/lib/api/client.ts) provides:

1. **Authentication:** Automatic JWT token injection
2. **Token Refresh:** Auto-retry with refreshed token on 401
3. **Error Handling:** Converts HTTP errors to `ApiError` instances
4. **Type Safety:** Generic type support for responses
5. **Consistent Interface:** Standardized methods (get, post, put, delete)

**Available Methods:**

```typescript
// GET request
apiClient.get<T>(endpoint: string, params?: Record<string, any>): Promise<ApiResponse<T>>

// POST request
apiClient.post<T>(endpoint: string, body?: any): Promise<ApiResponse<T>>

// PUT request
apiClient.put<T>(endpoint: string, body?: any): Promise<ApiResponse<T>>

// DELETE request
apiClient.delete<T>(endpoint: string): Promise<ApiResponse<T>>

// Token management
apiClient.setAccessToken(token: string | null): void
apiClient.getAccessToken(): string | null
```

---

## React Query Hooks

### File Structure

```
src/hooks/
├── useUsers.ts
├── useRoles.ts
├── useMenus.ts
├── useMenuTabs.ts
├── useMenuScreens.ts
├── useApiEndpoints.ts
└── ...
```

### Hook Pattern

**All hooks MUST:**
1. Import from `@tanstack/react-query`
2. Import the corresponding API service
3. Use consistent query key patterns
4. Implement proper cache invalidation
5. Show toast notifications for mutations
6. Handle loading and error states

**Template:**

```typescript
/**
 * React Query hooks for [Entity] management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  myEntityApi,
  MyEntity,
  MyEntityQueryParams,
  CreateMyEntityRequest,
  UpdateMyEntityRequest
} from '@/lib/api/my-entities'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const MY_ENTITIES_QUERY_KEY = 'my-entities'

/**
 * Fetch all entities with pagination
 */
export function useMyEntities(params?: MyEntityQueryParams) {
  return useQuery({
    queryKey: [MY_ENTITIES_QUERY_KEY, params],
    queryFn: () => myEntityApi.getAll(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Fetch entity by ID
 */
export function useMyEntity(id: string) {
  return useQuery({
    queryKey: [MY_ENTITIES_QUERY_KEY, id],
    queryFn: () => myEntityApi.getById(id),
    enabled: !!id,
  })
}

/**
 * Create new entity
 */
export function useCreateMyEntity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateMyEntityRequest) => myEntityApi.create(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [MY_ENTITIES_QUERY_KEY] })
      toast.success('Entity created successfully', {
        description: `${data.name} has been created.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to create entity', {
        description: error.message,
      })
    },
  })
}

/**
 * Update entity
 */
export function useUpdateMyEntity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateMyEntityRequest }) =>
      myEntityApi.update(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: [MY_ENTITIES_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [MY_ENTITIES_QUERY_KEY, variables.id] })
      toast.success('Entity updated successfully', {
        description: `${data.name} has been updated.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update entity', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete entity
 */
export function useDeleteMyEntity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => myEntityApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MY_ENTITIES_QUERY_KEY] })
      toast.success('Entity deleted successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to delete entity', {
        description: error.message,
      })
    },
  })
}
```

---

## Query Key Conventions

### Key Structure

Query keys follow a hierarchical structure:

```typescript
[ENTITY_QUERY_KEY, ...identifiers, ...filters]
```

**Examples:**

```typescript
// List queries
['users']                          // All users
['users', { page: 0, size: 20 }]  // Users with pagination

// Detail queries
['users', userId]                  // Single user

// Filtered queries
['users', 'role', roleId]          // Users by role
['users', 'group', groupId]        // Users by group

// Nested queries
['menus', 'root']                  // Root menus
['menus', 'parent', parentId]      // Child menus
['menu-tabs', 'menu', menuId]      // Tabs by menu
```

### Query Key Constants

**Always define a constant for the base query key:**

```typescript
const USERS_QUERY_KEY = 'users'
const ROLES_QUERY_KEY = 'roles'
const MENUS_QUERY_KEY = 'menus'
const MENU_TABS_QUERY_KEY = 'menu-tabs'
const MENU_SCREENS_QUERY_KEY = 'menu-screens'
const API_ENDPOINTS_QUERY_KEY = 'api-endpoints'
```

### Query Key Patterns

| Pattern | Example | Use Case |
|---------|---------|----------|
| `[KEY]` | `['users']` | List all |
| `[KEY, params]` | `['users', { page: 0 }]` | List with filters |
| `[KEY, id]` | `['users', '123']` | Get by ID |
| `[KEY, 'code', code]` | `['roles', 'code', 'ADMIN']` | Get by code |
| `[KEY, 'parent', parentId]` | `['menus', 'parent', '456']` | Get children |
| `[KEY, 'type', type]` | `['api-endpoints', 'method', 'GET']` | Get by type |

### Stale Time Guidelines

| Data Type | Stale Time | Rationale |
|-----------|------------|-----------|
| Static Configuration | 30 minutes | Rarely changes (roles, permissions) |
| Reference Data | 5 minutes | Changes occasionally (menus, API endpoints) |
| Transactional Data | 1 minute | Changes frequently (users, organizations) |
| Real-time Data | 0 (always refetch) | Must be current (dashboard stats) |

**Example:**

```typescript
export function useMenus() {
  return useQuery({
    queryKey: [MENUS_QUERY_KEY],
    queryFn: () => menuApi.getAll(),
    staleTime: 5 * 60 * 1000, // 5 minutes - reference data
  })
}

export function useDashboardStats() {
  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: () => dashboardApi.getStats(),
    staleTime: 0, // Always refetch - real-time data
  })
}
```

---

## Error Handling

### API Error Structure

```typescript
export class ApiError extends Error {
  constructor(
    public status: number,     // HTTP status code
    public code: string,        // Application error code
    message: string,            // Human-readable message
    public data?: any          // Additional error data
  ) {
    super(message)
    this.name = 'ApiError'
  }
}
```

### Handling Errors in Hooks

```typescript
export function useCreateUser() {
  return useMutation({
    mutationFn: (data) => userApi.create(data),
    onError: (error: ApiError) => {
      // Generic error toast
      toast.error('Failed to create user', {
        description: error.message,
      })

      // Handle specific error codes
      if (error.code === 'USER_ALREADY_EXISTS') {
        // Show specific message
      }

      // Log for debugging
      console.error('Create user error:', error)
    },
  })
}
```

### Handling Errors in Components

```typescript
function UserForm() {
  const createMutation = useCreateUser()

  async function handleSubmit(data: CreateUserRequest) {
    try {
      await createMutation.mutateAsync(data)
      router.push('/admin/users')
    } catch (error) {
      // Error already handled in mutation's onError
      // Additional component-specific handling if needed
      if (error instanceof ApiError) {
        if (error.code === 'VALIDATION_ERROR') {
          // Show form validation errors
          setFormErrors(error.data)
        }
      }
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      {/* ... */}
      {createMutation.isError && (
        <Alert variant="destructive">
          Error: {createMutation.error.message}
        </Alert>
      )}
    </form>
  )
}
```

---

## Cache Management

### Cache Invalidation Strategies

#### 1. Invalidate All Queries for Entity

Use when: Creating, deleting, or bulk updates

```typescript
queryClient.invalidateQueries({ queryKey: [MY_ENTITIES_QUERY_KEY] })
```

#### 2. Invalidate Specific Query

Use when: Updating a single entity

```typescript
queryClient.invalidateQueries({ queryKey: [MY_ENTITIES_QUERY_KEY, entityId] })
```

#### 3. Invalidate Related Queries

Use when: Changes affect multiple entities

```typescript
// User role assignment affects both users and roles
queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
queryClient.invalidateQueries({ queryKey: [ROLES_QUERY_KEY] })
```

#### 4. Optimistic Updates

Use when: Immediate UI feedback needed

```typescript
export function useUpdateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }) => userApi.update(id, data),
    onMutate: async ({ id, data }) => {
      // Cancel outgoing queries
      await queryClient.cancelQueries({ queryKey: [USERS_QUERY_KEY, id] })

      // Snapshot previous value
      const previousUser = queryClient.getQueryData([USERS_QUERY_KEY, id])

      // Optimistically update
      queryClient.setQueryData([USERS_QUERY_KEY, id], (old) => ({
        ...old,
        ...data
      }))

      return { previousUser }
    },
    onError: (err, variables, context) => {
      // Rollback on error
      queryClient.setQueryData(
        [USERS_QUERY_KEY, variables.id],
        context.previousUser
      )
    },
    onSettled: (data, error, variables) => {
      // Always refetch after mutation
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY, variables.id] })
    },
  })
}
```

### Cache Prefetching

**Prefetch on hover:**

```typescript
function UserList() {
  const queryClient = useQueryClient()

  function handleHover(userId: string) {
    queryClient.prefetchQuery({
      queryKey: [USERS_QUERY_KEY, userId],
      queryFn: () => userApi.getById(userId),
    })
  }

  return (
    <div>
      {users.map(user => (
        <div onMouseEnter={() => handleHover(user.id)}>
          {user.name}
        </div>
      ))}
    </div>
  )
}
```

### Cache Persistence

**Using React Query Persist:**

```typescript
import { persistQueryClient } from '@tanstack/react-query-persist-client'
import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister'

const persister = createSyncStoragePersister({
  storage: window.localStorage,
})

persistQueryClient({
  queryClient,
  persister,
  maxAge: 1000 * 60 * 60 * 24, // 24 hours
})
```

---

## Best Practices

### 1. Always Use apiClient

✅ **DO:**
```typescript
const response = await apiClient.get<User>('/users/123')
```

❌ **DON'T:**
```typescript
const response = await fetch('/api/users/123')
```

### 2. Define Query Keys as Constants

✅ **DO:**
```typescript
const USERS_QUERY_KEY = 'users'
queryKey: [USERS_QUERY_KEY, userId]
```

❌ **DON'T:**
```typescript
queryKey: ['users', userId] // Typo risk
```

### 3. Always Check response.data

✅ **DO:**
```typescript
const response = await apiClient.get<User>('/users/123')
if (!response.data) {
  throw new Error('No data returned')
}
return response.data
```

❌ **DON'T:**
```typescript
return response.data // Might be undefined
```

### 4. Invalidate Caches After Mutations

✅ **DO:**
```typescript
onSuccess: (data, variables) => {
  queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
  queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY, variables.id] })
}
```

❌ **DON'T:**
```typescript
onSuccess: () => {
  // No cache invalidation - stale data!
}
```

### 5. Use Descriptive Toast Messages

✅ **DO:**
```typescript
toast.success('User created successfully', {
  description: `${data.fullName} has been added to the system.`,
})
```

❌ **DON'T:**
```typescript
toast.success('Success')
```

### 6. Enable Queries Conditionally

✅ **DO:**
```typescript
export function useUser(id: string | null) {
  return useQuery({
    queryKey: [USERS_QUERY_KEY, id],
    queryFn: () => userApi.getById(id!),
    enabled: !!id, // Only run when id exists
  })
}
```

❌ **DON'T:**
```typescript
export function useUser(id: string | null) {
  return useQuery({
    queryKey: [USERS_QUERY_KEY, id],
    queryFn: () => userApi.getById(id!), // Crashes if id is null
  })
}
```

### 7. Use Mutation Variables Properly

✅ **DO:**
```typescript
mutationFn: ({ id, data }: { id: string; data: UpdateRequest }) =>
  api.update(id, data)
```

❌ **DON'T:**
```typescript
mutationFn: (id, data) => api.update(id, data) // Type unsafe
```

---

## Examples

### Complete Example: User Management

**API Service ([src/lib/api/users.ts](../src/lib/api/users.ts)):**

```typescript
import { apiClient, PageResponse } from './client'

export interface User {
  id: string
  username: string
  fullName: string
  email: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
}

export interface CreateUserRequest {
  username: string
  fullName: string
  email: string
  password: string
}

export class UserApi {
  async getAll(params?: { page?: number; size?: number }): Promise<PageResponse<User>> {
    const response = await apiClient.get<PageResponse<User>>('/users', params)
    if (!response.data) throw new Error('No data returned')
    return response.data
  }

  async create(data: CreateUserRequest): Promise<User> {
    const response = await apiClient.post<User>('/users', data)
    if (!response.data) throw new Error('No data returned')
    return response.data
  }
}

export const userApi = new UserApi()
```

**Hooks ([src/hooks/useUsers.ts](../src/hooks/useUsers.ts)):**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userApi, User, CreateUserRequest } from '@/lib/api/users'
import { toast } from 'sonner'

const USERS_QUERY_KEY = 'users'

export function useUsers(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: [USERS_QUERY_KEY, params],
    queryFn: () => userApi.getAll(params),
    staleTime: 1 * 60 * 1000, // 1 minute
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateUserRequest) => userApi.create(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
      toast.success('User created', {
        description: `${data.fullName} has been added.`,
      })
    },
    onError: (error) => {
      toast.error('Failed to create user', {
        description: error.message,
      })
    },
  })
}
```

**Component:**

```typescript
function UsersPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useUsers({ page, size: 20 })
  const createMutation = useCreateUser()

  async function handleCreate(data: CreateUserRequest) {
    await createMutation.mutateAsync(data)
  }

  if (isLoading) return <div>Loading...</div>

  return (
    <div>
      <UserForm onSubmit={handleCreate} />
      <UserTable users={data?.content || []} />
    </div>
  )
}
```

---

## Summary

### Key Takeaways

1. **Always use `apiClient`** for API calls
2. **Define query keys as constants** at the top of hook files
3. **Invalidate caches** after successful mutations
4. **Show toast notifications** for user feedback
5. **Check `response.data`** existence before returning
6. **Use TypeScript** for type safety
7. **Follow naming conventions** consistently
8. **Document your code** with JSDoc comments

### Checklist for New API Service

- [ ] Import `apiClient` from `./client`
- [ ] Define TypeScript interfaces (Entity, Request, Params)
- [ ] Implement API class with CRUD methods
- [ ] Export singleton instance
- [ ] Check `response.data` in all methods
- [ ] Export from `index.ts`

### Checklist for New Hook

- [ ] Import from `@tanstack/react-query`
- [ ] Define query key constant
- [ ] Implement query hooks with proper keys
- [ ] Implement mutation hooks with cache invalidation
- [ ] Add toast notifications for success/error
- [ ] Handle ApiError in onError callbacks
- [ ] Use `enabled` option when needed

---

**For questions or updates, contact the frontend team or update this document.**
