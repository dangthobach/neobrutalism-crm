# 📋 WEEK 7-8: Task & Notification Frontend - Implementation Plan

**Target Dates:** Week 7-8 (Next Phase)  
**Estimated Effort:** 24-32 hours  
**Priority:** High (Backend ready, frontend missing)

---

## 🎯 Objectives

### Week 7: Task Management Frontend
- Create Task Board (Kanban-style) UI
- Implement Task CRUD with React Query
- Add task status transitions (TODO → IN_PROGRESS → DONE)
- Add task assignment to users
- Add permission guards for task management

### Week 8: Notification System
- Create Notification Bell component (header)
- Implement real-time notifications with WebSocket
- Add notification dropdown with unread count
- Implement mark as read/unread functionality
- Add notification preferences

---

## ✅ Backend Status (Already Complete)

### Task Backend ✅
**Entity:** `src/main/java/com/neobrutalism/crm/domain/task/`
- ✅ `Task.java` - Entity with status, priority, assignee
- ✅ `TaskController.java` - REST endpoints
- ✅ `TaskService.java` - Business logic
- ✅ `TaskRepository.java` - Data access

**Endpoints Available:**
```
GET    /api/tasks              - Get all (paginated)
GET    /api/tasks/{id}         - Get by ID
POST   /api/tasks              - Create
PUT    /api/tasks/{id}         - Update
DELETE /api/tasks/{id}         - Delete
GET    /api/tasks/user/{id}    - Get by assigned user
GET    /api/tasks/status/{status} - Get by status
```

### Notification Backend ✅
**Entity:** `src/main/java/com/neobrutalism/crm/domain/notification/`
- ✅ `Notification.java` - Entity with type, read status
- ✅ `NotificationController.java` - REST endpoints
- ✅ `NotificationService.java` - Business logic
- ✅ `NotificationRepository.java` - Data access

**Endpoints Available:**
```
GET    /api/notifications           - Get all (paginated)
GET    /api/notifications/{id}      - Get by ID
POST   /api/notifications           - Create
DELETE /api/notifications/{id}      - Delete
GET    /api/notifications/user/{id} - Get by user
POST   /api/notifications/{id}/read - Mark as read
GET    /api/notifications/unread    - Get unread count
```

### WebSocket Support ✅
**Configuration:** `src/main/java/com/neobrutalism/crm/config/WebSocketConfig.java`
- ✅ WebSocket enabled
- ✅ STOMP messaging configured
- ✅ Real-time notification broadcasting ready

---

## 📋 Week 7: Task Management - Detailed Plan

### Step 1: Create Task API Client & Hooks

**File:** `src/lib/api/tasks.ts`

```typescript
/**
 * Task API Service
 */

import { apiClient, PageResponse } from './client'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface Task {
  id: string
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  assignedToId?: string
  assignedToName?: string
  dueDate?: string
  createdAt: string
  createdBy: string
  updatedAt: string
  deleted: boolean
}

export interface TaskQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
  status?: TaskStatus
  priority?: TaskPriority
  assignedToId?: string
}

export interface CreateTaskRequest {
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  assignedToId?: string
  dueDate?: string
}

export interface UpdateTaskRequest extends Partial<CreateTaskRequest> {}

class TasksAPI {
  async getTasks(params?: TaskQueryParams): Promise<PageResponse<Task>> {
    return apiClient.get<PageResponse<Task>>('/tasks', params)
  }

  async getTaskById(id: string): Promise<Task> {
    return apiClient.get<Task>(`/tasks/${id}`)
  }

  async getTasksByUser(userId: string): Promise<Task[]> {
    return apiClient.get<Task[]>(`/tasks/user/${userId}`)
  }

  async getTasksByStatus(status: TaskStatus): Promise<Task[]> {
    return apiClient.get<Task[]>(`/tasks/status/${status}`)
  }

  async createTask(data: CreateTaskRequest): Promise<Task> {
    return apiClient.post<Task>('/tasks', data)
  }

  async updateTask(id: string, data: UpdateTaskRequest): Promise<Task> {
    return apiClient.put<Task>(`/tasks/${id}`, data)
  }

  async deleteTask(id: string): Promise<void> {
    await apiClient.delete(`/tasks/${id}`)
  }

  async updateStatus(id: string, status: TaskStatus): Promise<Task> {
    return apiClient.put<Task>(`/tasks/${id}`, { status })
  }
}

export const tasksAPI = new TasksAPI()
```

**File:** `src/hooks/useTasks.ts`

```typescript
/**
 * React Query hooks for Task management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { tasksAPI, Task, TaskQueryParams, CreateTaskRequest, UpdateTaskRequest, TaskStatus } from '@/lib/api/tasks'
import { toast } from 'sonner'

const TASKS_QUERY_KEY = 'tasks'

export function useTasks(params?: TaskQueryParams) {
  return useQuery({
    queryKey: [TASKS_QUERY_KEY, params],
    queryFn: () => tasksAPI.getTasks(params),
    staleTime: 2 * 60 * 1000, // 2 minutes (tasks change frequently)
  })
}

export function useTask(id: string) {
  return useQuery({
    queryKey: [TASKS_QUERY_KEY, id],
    queryFn: () => tasksAPI.getTaskById(id),
    enabled: !!id,
  })
}

export function useTasksByUser(userId: string) {
  return useQuery({
    queryKey: [TASKS_QUERY_KEY, 'user', userId],
    queryFn: () => tasksAPI.getTasksByUser(userId),
    enabled: !!userId,
  })
}

export function useTasksByStatus(status: TaskStatus) {
  return useQuery({
    queryKey: [TASKS_QUERY_KEY, 'status', status],
    queryFn: () => tasksAPI.getTasksByStatus(status),
  })
}

export function useCreateTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateTaskRequest) => tasksAPI.createTask(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [TASKS_QUERY_KEY] })
      toast.success('Task created successfully', {
        description: `${data.title} has been created.`,
      })
    },
    onError: (error: any) => {
      toast.error('Failed to create task', {
        description: error.message,
      })
    },
  })
}

export function useUpdateTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTaskRequest }) =>
      tasksAPI.updateTask(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: [TASKS_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [TASKS_QUERY_KEY, variables.id] })
      toast.success('Task updated successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to update task', {
        description: error.message,
      })
    },
  })
}

export function useDeleteTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => tasksAPI.deleteTask(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [TASKS_QUERY_KEY] })
      toast.success('Task deleted successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to delete task', {
        description: error.message,
      })
    },
  })
}

export function useUpdateTaskStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: TaskStatus }) =>
      tasksAPI.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [TASKS_QUERY_KEY] })
      toast.success('Task status updated')
    },
    onError: (error: any) => {
      toast.error('Failed to update status', {
        description: error.message,
      })
    },
  })
}
```

### Step 2: Create Task Board UI Component

**File:** `src/components/tasks/task-board.tsx`

**Features:**
- 3-column Kanban board (TODO, IN_PROGRESS, DONE)
- Drag & drop task cards between columns
- Task card shows: title, priority, assignee, due date
- Click card to open detail dialog
- Add task button per column
- Filter by assignee, priority, due date

**Libraries to use:**
- `@dnd-kit/core` - Drag and drop
- `@dnd-kit/sortable` - Sortable lists
- Existing shadcn/ui components

### Step 3: Create Task Pages

**File:** `src/app/admin/tasks/page.tsx`

**Sections:**
- Header with "Add Task" button (PermissionGuard)
- Task Board (Kanban view)
- Filter bar (assignee, priority, status)
- Statistics (total, by status, overdue)

**File:** `src/app/admin/tasks/[taskId]/page.tsx` (Optional detail page)

---

## 🔔 Week 8: Notification System - Detailed Plan

### Step 1: Create Notification API Client & Hooks

**File:** `src/lib/api/notifications.ts`

```typescript
/**
 * Notification API Service
 */

import { apiClient, PageResponse } from './client'

export type NotificationType = 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS'

export interface Notification {
  id: string
  userId: string
  type: NotificationType
  title: string
  message: string
  isRead: boolean
  createdAt: string
}

export interface NotificationQueryParams {
  page?: number
  size?: number
  isRead?: boolean
}

export interface CreateNotificationRequest {
  userId: string
  type: NotificationType
  title: string
  message: string
}

class NotificationsAPI {
  async getNotifications(params?: NotificationQueryParams): Promise<PageResponse<Notification>> {
    return apiClient.get<PageResponse<Notification>>('/notifications', params)
  }

  async getUnreadCount(): Promise<number> {
    const response = await apiClient.get<{ count: number }>('/notifications/unread')
    return response.count
  }

  async markAsRead(id: string): Promise<void> {
    await apiClient.post(`/notifications/${id}/read`)
  }

  async markAllAsRead(): Promise<void> {
    await apiClient.post('/notifications/read-all')
  }

  async deleteNotification(id: string): Promise<void> {
    await apiClient.delete(`/notifications/${id}`)
  }
}

export const notificationsAPI = new NotificationsAPI()
```

**File:** `src/hooks/useNotifications.ts`

```typescript
/**
 * React Query hooks for Notification management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { notificationsAPI, Notification, NotificationQueryParams } from '@/lib/api/notifications'
import { toast } from 'sonner'

const NOTIFICATIONS_QUERY_KEY = 'notifications'

export function useNotifications(params?: NotificationQueryParams) {
  return useQuery({
    queryKey: [NOTIFICATIONS_QUERY_KEY, params],
    queryFn: () => notificationsAPI.getNotifications(params),
    refetchInterval: 30000, // Poll every 30 seconds
  })
}

export function useUnreadCount() {
  return useQuery({
    queryKey: [NOTIFICATIONS_QUERY_KEY, 'unread-count'],
    queryFn: () => notificationsAPI.getUnreadCount(),
    refetchInterval: 10000, // Poll every 10 seconds
  })
}

export function useMarkAsRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => notificationsAPI.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [NOTIFICATIONS_QUERY_KEY] })
    },
  })
}

export function useMarkAllAsRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => notificationsAPI.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [NOTIFICATIONS_QUERY_KEY] })
      toast.success('All notifications marked as read')
    },
  })
}

export function useDeleteNotification() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => notificationsAPI.deleteNotification(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [NOTIFICATIONS_QUERY_KEY] })
      toast.success('Notification deleted')
    },
  })
}
```

### Step 2: Create Notification Bell Component

**File:** `src/components/notifications/notification-bell.tsx`

**Features:**
- Bell icon with unread count badge
- Click to open dropdown
- Dropdown shows last 5 notifications
- "View All" link to full notification page
- "Mark all as read" button
- Real-time updates via WebSocket

**File:** `src/components/notifications/notification-dropdown.tsx`

**Features:**
- List of notifications (title, message, time ago)
- Unread indicator (bold text, blue dot)
- Click notification to mark as read
- Delete button per notification
- Empty state when no notifications

### Step 3: WebSocket Integration

**File:** `src/lib/websocket.ts`

```typescript
/**
 * WebSocket connection for real-time notifications
 */

import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export class NotificationWebSocket {
  private client: Client | null = null
  private onMessageCallback: ((notification: any) => void) | null = null

  connect(userId: string) {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        console.log('WebSocket connected')
        this.client?.subscribe(`/user/${userId}/notifications`, (message) => {
          const notification = JSON.parse(message.body)
          this.onMessageCallback?.(notification)
        })
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected')
      },
    })

    this.client.activate()
  }

  disconnect() {
    this.client?.deactivate()
  }

  onMessage(callback: (notification: any) => void) {
    this.onMessageCallback = callback
  }
}

export const notificationWS = new NotificationWebSocket()
```

### Step 4: Integrate Bell into Layout

**File:** `src/app/admin/layout.tsx`

**Add NotificationBell to header:**
```tsx
<header>
  {/* ... existing header content ... */}
  <NotificationBell />
  {/* User menu */}
</header>
```

---

## 📦 Dependencies to Add

```bash
pnpm add @dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities
pnpm add @stomp/stompjs sockjs-client
pnpm add date-fns  # If not already installed
```

---

## 🎨 UI/UX Patterns

### Task Board Colors (by Priority)
- 🔴 **URGENT:** Red border + red badge
- 🟠 **HIGH:** Orange border + orange badge
- 🟡 **MEDIUM:** Yellow border + yellow badge
- 🟢 **LOW:** Green border + green badge

### Notification Types
- ℹ️ **INFO:** Blue icon + blue border
- ⚠️ **WARNING:** Yellow icon + yellow border
- ❌ **ERROR:** Red icon + red border
- ✅ **SUCCESS:** Green icon + green border

### Time Display
- Use `date-fns` with `formatDistanceToNow`
- "2 minutes ago", "1 hour ago", "Yesterday"

---

## 🧪 Testing Checklist

### Task Management Tests
- [ ] Create new task
- [ ] Drag task between columns (TODO → IN_PROGRESS → DONE)
- [ ] Edit task details
- [ ] Delete task
- [ ] Assign task to user
- [ ] Filter by status, priority, assignee
- [ ] Test permission guards (only authorized users can CRUD)

### Notification Tests
- [ ] View unread count in bell icon
- [ ] Click bell to open dropdown
- [ ] Mark single notification as read
- [ ] Mark all as read
- [ ] Delete notification
- [ ] Real-time notification appears (via WebSocket)
- [ ] Navigate to "All Notifications" page
- [ ] Test with 0, 5, 50+ notifications

---

## 📊 Estimated Breakdown

### Week 7: Task Management (16-20 hours)
- API Client + Hooks: 2-3 hours
- Task Board Component: 6-8 hours
- Task Pages: 4-5 hours
- Permission Guards: 1-2 hours
- Testing: 3-4 hours

### Week 8: Notification System (8-12 hours)
- API Client + Hooks: 1-2 hours
- Notification Bell + Dropdown: 3-4 hours
- WebSocket Integration: 2-3 hours
- Layout Integration: 1 hour
- Testing: 2-3 hours

---

## 🚀 Quick Start Commands

```bash
# Install dependencies
pnpm add @dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities @stomp/stompjs sockjs-client

# Create API files
touch src/lib/api/tasks.ts
touch src/lib/api/notifications.ts
touch src/lib/websocket.ts

# Create hooks
touch src/hooks/useTasks.ts
touch src/hooks/useNotifications.ts

# Create components
mkdir -p src/components/tasks
touch src/components/tasks/task-board.tsx
touch src/components/tasks/task-card.tsx

mkdir -p src/components/notifications
touch src/components/notifications/notification-bell.tsx
touch src/components/notifications/notification-dropdown.tsx

# Create pages
mkdir -p src/app/admin/tasks
touch src/app/admin/tasks/page.tsx

mkdir -p src/app/admin/notifications
touch src/app/admin/notifications/page.tsx

# Start backend
cd backend && ./mvnw spring-boot:run

# Start frontend
pnpm dev
```

---

**Status:** 📋 PLANNED  
**Next Action:** Begin Week 7.1 - Create Task API Client & Hooks
