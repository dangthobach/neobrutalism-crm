# 🚀 Frontend API Quick Reference

## ⚡ TL;DR

```typescript
// ✅ DO THIS
const org = await organizationsAPI.getById(id)
console.log(org.name)

// ❌ DON'T DO THIS
const response = await organizationsAPI.getById(id)
console.log(response.data.name)  // Error: data doesn't exist
```

---

## 📖 Common Patterns

### GET Request

```typescript
// Single item
async getById(id: string): Promise<T> {
  return await apiClient.get<T>(`${BASE_PATH}/${id}`)
}

// List
async getAll(): Promise<T[]> {
  return await apiClient.get<T[]>(BASE_PATH)
}

// Paginated
async getAllPaged(params?: QueryParams): Promise<PageResponse<T>> {
  return await apiClient.get<PageResponse<T>>(BASE_PATH, params)
}
```

### POST Request

```typescript
// Create
async create(data: TRequest): Promise<T> {
  return await apiClient.post<T>(BASE_PATH, data)
}

// Action with body
async activate(id: string, reason?: string): Promise<T> {
  const params = reason ? { reason } : undefined
  return await apiClient.post<T>(`${BASE_PATH}/${id}/activate`, params)
}
```

### PUT Request

```typescript
// Update
async update(id: string, data: TRequest): Promise<T> {
  return await apiClient.put<T>(`${BASE_PATH}/${id}`, data)
}
```

### DELETE Request

```typescript
// Delete
async delete(id: string): Promise<void> {
  await apiClient.delete<void>(`${BASE_PATH}/${id}`)
}
```

---

## 🎯 API Service Template

```typescript
/**
 * [Entity] API Service
 * Backend: [Controller].java
 */

import { apiClient, PageResponse } from './client'

// Types matching backend DTOs
export interface [Entity] {
  id: string
  // ... match backend Response DTO exactly
}

export interface [Entity]Request {
  // ... match backend Request DTO exactly
}

export interface [Entity]QueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

class [Entity]API {
  private readonly BASE_PATH = "/[entities]"

  /**
   * Get all with pagination
   * Backend: GET /api/[entities]
   */
  async getAll(params?: [Entity]QueryParams): Promise<PageResponse<[Entity]>> {
    return await apiClient.get<PageResponse<[Entity]>>(this.BASE_PATH, params)
  }

  /**
   * Get by ID
   * Backend: GET /api/[entities]/{id}
   */
  async getById(id: string): Promise<[Entity]> {
    return await apiClient.get<[Entity]>(`${this.BASE_PATH}/${id}`)
  }

  /**
   * Create
   * Backend: POST /api/[entities]
   */
  async create(data: [Entity]Request): Promise<[Entity]> {
    return await apiClient.post<[Entity]>(this.BASE_PATH, data)
  }

  /**
   * Update
   * Backend: PUT /api/[entities]/{id}
   */
  async update(id: string, data: [Entity]Request): Promise<[Entity]> {
    return await apiClient.put<[Entity]>(`${this.BASE_PATH}/${id}`, data)
  }

  /**
   * Delete
   * Backend: DELETE /api/[entities]/{id}
   */
  async delete(id: string): Promise<void> {
    await apiClient.delete<void>(`${this.BASE_PATH}/${id}`)
  }
}

export const [entities]API = new [Entity]API()
```

---

## 🪝 React Hook Template

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { [entities]API, [Entity], [Entity]Request } from '@/lib/api/[entities]'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const QUERY_KEY = '[entities]'

// Fetch list
export function use[Entities](params?: QueryParams) {
  return useQuery({
    queryKey: [QUERY_KEY, params],
    queryFn: () => [entities]API.getAll(params),
    staleTime: 5 * 60 * 1000,
  })
}

// Fetch single
export function use[Entity](id: string) {
  return useQuery({
    queryKey: [QUERY_KEY, id],
    queryFn: () => [entities]API.getById(id),
    enabled: !!id,
  })
}

// Create
export function useCreate[Entity]() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (data: [Entity]Request) => [entities]API.create(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('[Entity] created successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to create [entity]', {
        description: error.message,
      })
    },
  })
}

// Update
export function useUpdate[Entity]() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: [Entity]Request }) =>
      [entities]API.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('[Entity] updated successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update [entity]', {
        description: error.message,
      })
    },
  })
}

// Delete
export function useDelete[Entity]() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => [entities]API.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('[Entity] deleted successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to delete [entity]', {
        description: error.message,
      })
    },
  })
}
```

---

## 🧪 Test Template

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { [entities]API } from '../[entities]'
import { apiClient } from '../client'

vi.mock('../client')

describe('[Entity]API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const mock[Entity] = {
    id: '123',
    name: 'Test',
    // ...
  }

  it('should get by id', async () => {
    vi.mocked(apiClient.get).mockResolvedValue(mock[Entity])
    
    const result = await [entities]API.getById('123')
    
    expect(apiClient.get).toHaveBeenCalledWith('/[entities]/123')
    expect(result).toEqual(mock[Entity])
  })

  it('should create', async () => {
    vi.mocked(apiClient.post).mockResolvedValue(mock[Entity])
    
    const result = await [entities]API.create({ name: 'Test' })
    
    expect(apiClient.post).toHaveBeenCalledWith('/[entities]', { name: 'Test' })
    expect(result).toEqual(mock[Entity])
  })
})
```

---

## ⚠️ Common Mistakes

### ❌ Mistake 1: Accessing .data

```typescript
// ❌ WRONG
const response = await apiClient.get<T>("/endpoint")
return response.data

// ✅ CORRECT
return await apiClient.get<T>("/endpoint")
```

### ❌ Mistake 2: Null checks on unwrapped response

```typescript
// ❌ WRONG
const response = await apiClient.get<T>("/endpoint")
if (!response.data) {
  throw new Error('No data')
}
return response.data

// ✅ CORRECT
return await apiClient.get<T>("/endpoint")
// Throws error automatically if response is invalid
```

### ❌ Mistake 3: Wrong endpoints

```typescript
// ❌ WRONG - Check backend first!
await apiClient.get("/organizations/query/active")

// ✅ CORRECT - Use actual backend endpoint
await apiClient.get("/organizations/status/ACTIVE")
```

### ❌ Mistake 4: Type mismatches

```typescript
// ❌ WRONG - Check backend DTO!
export interface Organization {
  id: string
  name: string
  // Missing fields...
}

// ✅ CORRECT - Match backend exactly
export interface Organization {
  id: string
  name: string
  code: string
  status: OrganizationStatus
  createdAt: string
  createdBy: string
  // ... all fields from backend
}
```

---

## 🔍 Debugging Tips

### Check API Response in Browser

```javascript
// Open browser console
const org = await organizationsAPI.getById('some-id')
console.log(org)  // Should be the object directly

// Check raw response
fetch('http://localhost:8080/api/organizations/some-id', {
  headers: { 'Authorization': 'Bearer ' + localStorage.getItem('access_token') }
})
.then(r => r.json())
.then(console.log)
```

### Verify Backend Endpoint

```bash
# Check if endpoint exists
curl -X GET http://localhost:8080/api/organizations \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Check TypeScript Errors

```typescript
// If you see errors, check:
1. Is the endpoint correct?
2. Do types match backend?
3. Are you accessing .data?
4. Is apiClient mocked correctly in tests?
```

---

## 📚 Quick Links

- **Full Guide**: [docs/FRONTEND_API_INTEGRATION.md](./FRONTEND_API_INTEGRATION.md)
- **Migration Guide**: [docs/API_MIGRATION_GUIDE.md](./API_MIGRATION_GUIDE.md)
- **Reference Implementation**: `src/lib/api/organizations.ts`
- **Backend Code**: `src/main/java/.../controller/`

---

## 💡 Pro Tips

1. **Always check backend controller first** before writing frontend code
2. **Use the templates** - Copy from organizations.ts
3. **Let TypeScript help** - Fix all type errors
4. **Write tests** - Catch issues early
5. **Document endpoints** - Add JSDoc comments with backend mapping

---

## 🎯 Checklist

When creating/updating API service:

- [ ] Check backend controller for actual endpoints
- [ ] Match types with backend DTOs exactly
- [ ] Remove all `.data` property access
- [ ] Add JSDoc comments with backend mapping
- [ ] Write unit tests
- [ ] Update React hooks
- [ ] Test in browser
- [ ] No TypeScript errors

---

**Remember**: The `apiClient` automatically unwraps responses. Just use the methods directly! 🚀
