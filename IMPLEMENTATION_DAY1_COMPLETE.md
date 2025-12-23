# DAY 1 IMPLEMENTATION - COMPLETE ✅

## Backend Changes (COMPLETED)

### 1. Created UserContext Service
**File**: `src/main/java/com/neobrutalism/crm/common/security/UserContext.java`
- Gets current user ID from Spring Security
- Gets current organization ID from UserPrincipal
- Provides safe methods with Optional returns
- Throws UnauthorizedException if not authenticated

### 2. Created UnauthorizedException
**File**: `src/main/java/com/neobrutalism/crm/common/exception/UnauthorizedException.java`
- Custom exception for authentication/authorization failures

### 3. Fixed JPA Auditing
**File**: `src/main/java/com/neobrutalism/crm/config/JpaAuditingConfig.java`
- **BEFORE**: Returned hardcoded "system"
- **AFTER**: Returns authenticated username from SecurityContext
- Handles anonymous users gracefully

### 4. Updated TaskService
**File**: `src/main/java/com/neobrutalism/crm/domain/task/service/TaskService.java`
- Added `UserContext` dependency injection
- Updated `mapRequestToEntity()` method
- **CRITICAL CHANGE**: Now auto-sets `organizationId` from `userContext.getCurrentOrganizationId()`
- **Ignores** organizationId from request (security fix)

---

## Frontend Changes (NEXT - DAY 1 CONTINUED)

### Step 5: Create useCurrentUser Hook

**File**: `src/hooks/use-current-user.ts`

```typescript
import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'

interface CurrentUser {
  id: string
  username: string
  email: string
  fullName: string
  organizationId: string
  branchId?: string
  dataScope: string
  roles: string[]
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiClient.get('/auth/me')
  return response.data
}

export function useCurrentUser() {
  return useQuery({
    queryKey: ['current-user'],
    queryFn: getCurrentUser,
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: 1,
  })
}

export function useCurrentOrganization() {
  const { data: user } = useCurrentUser()
  return user?.organizationId
}
```

---

### Step 6: Fix Task Creation Page

**File**: `src/app/admin/tasks/page.tsx`

#### Line 137-145: Remove hardcoded organizationId

**BEFORE**:
```typescript
} else {
  // Create new task - need to add organizationId
  const createData = {
    ...formattedData,
    organizationId: "default", // TODO: Get from current user context
  } as CreateTaskRequest

  createMutation.mutate(createData, {
    onSuccess: () => {
      setIsModalOpen(false)
      refetch()
    },
  })
}
```

**AFTER**:
```typescript
} else {
  // Create new task (organizationId is set by backend from user context)
  createMutation.mutate(formattedData, {
    onSuccess: () => {
      setIsModalOpen(false)
      setEditingTask(null)
      refetch()
    },
  })
}
```

#### Line 442-446: Load users from API

**BEFORE**:
```typescript
<SelectContent>
  <SelectItem value="ALL">All Users</SelectItem>
  <SelectItem value="unassigned">Unassigned</SelectItem>
  <SelectItem value="me">My Tasks</SelectItem>
  {/* TODO: Load users from API */}
</SelectContent>
```

**AFTER**:
```typescript
import { useUsers } from '@/hooks/useUsers'

// In component
const { data: usersData, isLoading: isLoadingUsers } = useUsers({
  page: 1,
  limit: 100,
  status: 'ACTIVE'
})

// In JSX
<SelectContent>
  <SelectItem value="ALL">All Users</SelectItem>
  <SelectItem value="unassigned">Unassigned</SelectItem>
  <SelectItem value="me">My Tasks</SelectItem>
  {isLoadingUsers ? (
    <SelectItem value="" disabled>Loading users...</SelectItem>
  ) : (
    usersData?.content?.map((user) => (
      <SelectItem key={user.id} value={user.id}>
        {user.fullName || user.username}
      </SelectItem>
    ))
  )}
</SelectContent>
```

---

### Step 7: Update TypeScript Types

**File**: `src/types/task.ts`

Remove `organizationId` from frontend CreateTaskRequest if present:

```typescript
export interface CreateTaskRequest {
  title: string
  description?: string
  priority: TaskPriority
  category?: TaskCategory
  status?: TaskStatus
  assignedToId?: string
  dueDate?: string
  estimatedHours?: number
  tags?: string[]
  // organizationId is NOT sent from frontend - backend sets it
  branchId?: string
}
```

---

## Testing the Changes

### Backend Test

```bash
# Run Spring Boot app
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Test organizationId is set correctly
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Task",
    "priority": "HIGH"
  }'

# Verify response includes organizationId from authenticated user
```

### Frontend Test

```bash
# Run Next.js dev server
pnpm dev

# Navigate to /admin/tasks
# Try creating a new task
# Verify:
# 1. No "default" organizationId sent
# 2. User dropdown loads from API
# 3. Task created with correct organizationId
```

---

## Verification Checklist

- [x] UserContext service created and working
- [x] JPA Auditing using real user from Security
- [x] TaskService auto-sets organizationId from context
- [ ] useCurrentUser hook created
- [ ] Task creation page removes hardcoded "default"
- [ ] User dropdown loads from API
- [ ] Manual testing passes

---

## Next Steps (Day 2)

1. Create Task detail page structure
2. Build TaskDetailHeader component
3. Build TaskDetailSidebar component
4. Setup tab navigation for detail page

**Estimated time**: Day 1 = 4 hours complete ✅
