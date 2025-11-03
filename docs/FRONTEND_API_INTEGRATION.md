# Frontend API Integration Guide

## 🎯 Overview

This guide documents the **critical alignment** between Frontend API services and Backend endpoints.

## 🚨 Critical Rules

### 1. **API Client Response Unwrapping**

The `apiClient` **automatically unwraps** the backend's `ApiResponse` wrapper:

```typescript
// Backend Response:
{
  success: true,
  message: "Organization created successfully",
  data: {
    id: "123",
    name: "Test Org",
    // ... organization fields
  }
}

// apiClient.get/post/put/delete returns:
{
  id: "123",
  name: "Test Org",
  // ... organization fields (just the data)
}
```

**DO NOT access `.data` property** - it's already unwrapped!

### 2. **Endpoint Alignment**

Always verify frontend endpoints match backend exactly:

| Frontend Method | Backend Endpoint | HTTP Method |
|----------------|------------------|-------------|
| `getAll()` | `/api/organizations` | GET |
| `getById(id)` | `/api/organizations/{id}` | GET |
| `getByCode(code)` | `/api/organizations/code/{code}` | GET |
| `create(data)` | `/api/organizations` | POST |
| `update(id, data)` | `/api/organizations/{id}` | PUT |
| `delete(id)` | `/api/organizations/{id}` | DELETE |
| `activate(id, reason)` | `/api/organizations/{id}/activate` | POST |
| `suspend(id, reason)` | `/api/organizations/{id}/suspend` | POST |
| `archive(id, reason)` | `/api/organizations/{id}/archive` | POST |
| `getByStatus(status)` | `/api/organizations/status/{status}` | GET |

### 3. **Type Synchronization**

Frontend types MUST match backend DTOs:

```typescript
// ✅ CORRECT: Synchronized with Backend
export interface Organization {
  id: string                    // Backend: UUID (converted to string)
  name: string                  // Backend: String
  code: string                  // Backend: String
  description?: string          // Backend: String (nullable)
  email?: string               // Backend: String (nullable)
  phone?: string               // Backend: String (nullable)
  website?: string             // Backend: String (nullable)
  address?: string             // Backend: String (nullable)
  status: OrganizationStatus   // Backend: OrganizationStatus enum
  deleted: boolean             // Backend: Boolean
  createdAt: string            // Backend: Instant (ISO-8601 string)
  createdBy: string            // Backend: String
  updatedAt: string            // Backend: Instant (ISO-8601 string)
  updatedBy: string            // Backend: String
}

// Status enum must match exactly
export type OrganizationStatus = 
  | "DRAFT"      // Backend: DRAFT
  | "ACTIVE"     // Backend: ACTIVE
  | "INACTIVE"   // Backend: INACTIVE
  | "SUSPENDED"  // Backend: SUSPENDED
  | "ARCHIVED"   // Backend: ARCHIVED
```

### 4. **Error Handling Pattern**

```typescript
try {
  const org = await organizationsAPI.getById(id)
  // org is already the Organization object, not wrapped
} catch (error) {
  if (error instanceof ApiError) {
    console.error('API Error:', {
      status: error.status,    // HTTP status code
      code: error.code,        // Backend error code
      message: error.message   // User-friendly message
    })
  }
}
```

## 📦 API Service Structure

```typescript
class OrganizationsAPI {
  private readonly BASE_PATH = "/organizations"

  // CRUD Operations
  async getAll(params?: QueryParams): Promise<PageResponse<T>> { }
  async getById(id: string): Promise<T> { }
  async create(data: TRequest): Promise<T> { }
  async update(id: string, data: TRequest): Promise<T> { }
  async delete(id: string): Promise<void> { }

  // Custom Operations
  async activate(id: string, reason?: string): Promise<T> { }
  
  // Query Operations
  async getByStatus(status: Status): Promise<T[]> { }
  async getActive(): Promise<T[]> { }
}

export const organizationsAPI = new OrganizationsAPI()
```

## 🔍 Common Mistakes to Avoid

### ❌ WRONG: Accessing .data property

```typescript
// ❌ WRONG
const response = await apiClient.get<Organization>("/organizations/123")
return response.data  // Error: data doesn't exist!

// ✅ CORRECT
const organization = await apiClient.get<Organization>("/organizations/123")
return organization  // Already unwrapped!
```

### ❌ WRONG: Wrong endpoint paths

```typescript
// ❌ WRONG: These endpoints don't exist in backend
await apiClient.get("/organizations/query/active")
await apiClient.get("/organizations/query/paginated")

// ✅ CORRECT: Use actual backend endpoints
await apiClient.get("/organizations/status/ACTIVE")
await apiClient.get("/organizations", { page: 0, size: 20 })
```

### ❌ WRONG: Type mismatches

```typescript
// ❌ WRONG: Missing status values
export type OrganizationStatus = "DRAFT" | "ACTIVE" | "ARCHIVED"

// ✅ CORRECT: All backend statuses
export type OrganizationStatus = 
  | "DRAFT" | "ACTIVE" | "INACTIVE" | "SUSPENDED" | "ARCHIVED"
```

## 🧪 Testing Pattern

```typescript
import { describe, it, expect, vi } from 'vitest'
import { organizationsAPI } from '../organizations'
import { apiClient } from '../client'

vi.mock('../client')

describe('OrganizationsAPI', () => {
  it('should unwrap response correctly', async () => {
    // Mock returns unwrapped data
    vi.mocked(apiClient.get).mockResolvedValue(mockOrganization)
    
    const result = await organizationsAPI.getById('123')
    
    // Result is the organization, not wrapped
    expect(result).toEqual(mockOrganization)
  })
})
```

## 📋 Verification Checklist

When creating/updating API services:

- [ ] Verify all endpoints exist in backend controller
- [ ] Check HTTP methods match exactly
- [ ] Ensure types match backend DTOs
- [ ] Test response unwrapping works correctly
- [ ] Add error handling
- [ ] Write unit tests
- [ ] Add JSDoc comments with backend mapping
- [ ] Update this documentation

## 🔗 Related Files

- **API Client**: `src/lib/api/client.ts`
- **Organizations API**: `src/lib/api/organizations.ts`
- **React Hooks**: `src/hooks/useOrganizations.ts`
- **Backend Controller**: `OrganizationController.java`
- **Backend DTOs**: `OrganizationRequest.java`, `OrganizationResponse.java`

## 📚 Additional Resources

- [Backend API Documentation](../../../docs/BACKEND_ENHANCEMENTS.md)
- [Frontend Patterns](../../../docs/FRONTEND_API_PATTERNS.md)
- [Testing Guide](../../../TESTING_GUIDE.md)
