# API Services Migration Guide

## 🎯 Purpose

This guide helps migrate existing API services to align with the new API client pattern and backend endpoints.

## 🚨 Critical Changes

### 1. API Client Response Handling

**OLD Pattern (WRONG):**
```typescript
async getById(id: string): Promise<Organization> {
  const response = await apiClient.get<Organization>(`/organizations/${id}`)
  if (!response.data) {
    throw new Error('No data returned from API')
  }
  return response.data  // ❌ WRONG
}
```

**NEW Pattern (CORRECT):**
```typescript
async getById(id: string): Promise<Organization> {
  return await apiClient.get<Organization>(`/organizations/${id}`)
  // ✅ CORRECT: apiClient already unwraps the response
}
```

### 2. Error Handling

**OLD Pattern:**
```typescript
try {
  const response = await apiClient.get<T>("/endpoint")
  if (!response.data) {
    throw new Error('No data')
  }
  return response.data
} catch (error) {
  console.error('Error:', error)
  throw error
}
```

**NEW Pattern:**
```typescript
// Error handling is done in apiClient
// Just return the direct result
return await apiClient.get<T>("/endpoint")
```

## 📋 Migration Steps

### Step 1: Update API Service Class

```typescript
// BEFORE
class SomeAPI {
  async getAll(): Promise<SomeType[]> {
    const response = await apiClient.get<SomeType[]>("/endpoint")
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }
}

// AFTER
class SomeAPI {
  private readonly BASE_PATH = "/endpoint"

  /**
   * Get all items
   * Backend: GET /api/endpoint
   */
  async getAll(): Promise<SomeType[]> {
    return await apiClient.get<SomeType[]>(this.BASE_PATH)
  }
}
```

### Step 2: Verify Backend Endpoints

Check the actual backend controller to ensure endpoints match:

```java
// Backend: SomeController.java
@GetMapping
public ApiResponse<List<SomeResponse>> getAll() { }

@GetMapping("/{id}")
public ApiResponse<SomeResponse> getById(@PathVariable UUID id) { }

@PostMapping
public ApiResponse<SomeResponse> create(@Valid @RequestBody SomeRequest request) { }
```

Map these to frontend:

```typescript
class SomeAPI {
  async getAll(): Promise<SomeType[]> {
    return await apiClient.get<SomeType[]>("/endpoint")
  }

  async getById(id: string): Promise<SomeType> {
    return await apiClient.get<SomeType>(`/endpoint/${id}`)
  }

  async create(data: SomeRequest): Promise<SomeType> {
    return await apiClient.post<SomeType>("/endpoint", data)
  }
}
```

### Step 3: Update Type Definitions

Ensure types match backend DTOs exactly:

```typescript
// Check backend Response DTO
// OrganizationResponse.java

export interface Organization {
  id: string              // UUID -> string
  name: string           // String
  status: Status         // Enum
  createdAt: string      // Instant -> ISO-8601 string
  createdBy: string      // String
  // ... all fields from backend
}

// Check backend Request DTO
// OrganizationRequest.java

export interface OrganizationRequest {
  name: string
  code: string
  // ... all required/optional fields from backend
}
```

### Step 4: Update React Hooks

```typescript
// BEFORE
export function useSomething(id: string) {
  return useQuery({
    queryKey: ['something', id],
    queryFn: async () => {
      const response = await someAPI.getById(id)
      return response.data  // ❌ WRONG
    },
  })
}

// AFTER
export function useSomething(id: string) {
  return useQuery({
    queryKey: ['something', id],
    queryFn: () => someAPI.getById(id),  // ✅ CORRECT
  })
}
```

## 🔍 Files to Check

For each entity, check these files:

1. **Backend Controller** (Java)
   - `src/main/java/.../controller/XxxController.java`
   - Note all endpoints and HTTP methods

2. **Backend DTOs** (Java)
   - `src/main/java/.../dto/XxxRequest.java`
   - `src/main/java/.../dto/XxxResponse.java`

3. **Frontend API Service** (TypeScript)
   - `src/lib/api/xxx.ts`
   - Update to match backend exactly

4. **Frontend Types** (TypeScript)
   - Ensure types in API service match backend DTOs

5. **React Hooks** (TypeScript)
   - `src/hooks/useXxx.ts`
   - Remove `.data` access

## ✅ Verification Checklist

For each API service:

- [ ] Remove all `.data` property access
- [ ] Remove null checks on response
- [ ] Verify all endpoints match backend
- [ ] Verify HTTP methods are correct
- [ ] Check types match backend DTOs exactly
- [ ] Add JSDoc comments with backend mapping
- [ ] Update related React hooks
- [ ] Add/update unit tests
- [ ] Test in browser console

## 📦 Example: Complete Migration

### Organizations API (Reference Implementation)

See `src/lib/api/organizations.ts` for the reference implementation:

- ✅ Clean API methods without response unwrapping
- ✅ All endpoints verified against backend
- ✅ Types synchronized with backend
- ✅ Proper JSDoc documentation
- ✅ Comprehensive tests

### Other APIs to Migrate

Based on the project structure, these APIs need migration:

1. **Users API** (`src/lib/api/users.ts`)
2. **Roles API** (`src/lib/api/roles.ts`)
3. **Groups API** (`src/lib/api/groups.ts`)
4. **Permissions API** (`src/lib/api/permissions.ts`)
5. **Contacts API** (`src/lib/api/contacts.ts`)
6. **Customers API** (`src/lib/api/customers.ts`)
7. **Activities API** (`src/lib/api/activities.ts`)
8. **Tasks API** (`src/lib/api/tasks.ts`)
9. **Content APIs** (`src/lib/api/content*.ts`)
10. **Course APIs** (`src/lib/api/course*.ts`)
11. **Menu APIs** (`src/lib/api/menu*.ts`)

## 🧪 Testing After Migration

```typescript
// Test in browser console or test file
import { organizationsAPI } from '@/lib/api/organizations'

// Should work without errors
const org = await organizationsAPI.getById('some-id')
console.log(org.name)  // Direct access to properties

// Should handle errors properly
try {
  await organizationsAPI.getById('invalid-id')
} catch (error) {
  console.error(error.message)  // User-friendly message
}
```

## 🎓 Learning Resources

1. **API Client Implementation**: `src/lib/api/client.ts`
2. **Reference Implementation**: `src/lib/api/organizations.ts`
3. **Integration Guide**: `docs/FRONTEND_API_INTEGRATION.md`
4. **Backend Documentation**: `docs/BACKEND_ENHANCEMENTS.md`

## 💡 Tips

1. **Start with one API service** - Use organizations as reference
2. **Check backend first** - Always verify endpoints exist
3. **Test incrementally** - Test each method after changes
4. **Use TypeScript strictly** - Let the compiler catch errors
5. **Write tests** - Ensure nothing breaks

## 🆘 Common Issues

### Issue: `Property 'data' does not exist`

**Cause**: Trying to access `.data` on already unwrapped response

**Solution**: Remove `.data` access
```typescript
// ❌ WRONG
const response = await apiClient.get<T>("/endpoint")
return response.data

// ✅ CORRECT
return await apiClient.get<T>("/endpoint")
```

### Issue: `404 Not Found`

**Cause**: Endpoint doesn't exist in backend

**Solution**: Check backend controller and use correct endpoint
```typescript
// ❌ WRONG (endpoint doesn't exist)
await apiClient.get("/organizations/query/active")

// ✅ CORRECT (actual backend endpoint)
await apiClient.get("/organizations/status/ACTIVE")
```

### Issue: Type mismatch errors

**Cause**: Frontend types don't match backend DTOs

**Solution**: Synchronize types with backend
```typescript
// Check backend DTO and match exactly
export interface Organization {
  // Match all fields from OrganizationResponse.java
}
```

## 📞 Getting Help

If you encounter issues:

1. Check this guide first
2. Review the reference implementation (`organizations.ts`)
3. Verify backend controller and DTOs
4. Check browser network tab for actual responses
5. Add console.log to debug response structure

## 🎉 Success Criteria

Migration is complete when:

- ✅ All API methods work without errors
- ✅ No `.data` property access anywhere
- ✅ All endpoints verified against backend
- ✅ Types match backend DTOs exactly
- ✅ Tests pass
- ✅ No TypeScript errors
- ✅ Application runs without console errors
