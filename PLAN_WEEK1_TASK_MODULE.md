# TUẦN 1-2: KHÓA MODULE TASK - DETAILED PLAN

## 🎯 MỤC TIÊU TỔNG
- Fix 100% placeholders trong Task module
- Build complete Task detail page với đầy đủ UX
- Implement comments, checklist, timeline
- Add bulk operations & advanced filtering
- Achieve 70%+ test coverage cho Task module

---

## 📅 SPRINT 1.1: FIX CRITICAL ISSUES (Ngày 1-3)

### **DAY 1: Fix organizationId & User Context**

#### **Backend Tasks:**

**1.1. Create UserContext Service**
```java
// File: src/main/java/com/neobrutalism/crm/common/security/UserContext.java
package com.neobrutalism.crm.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserContext {

    public Optional<String> getCurrentUserId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getName);
    }

    public Optional<String> getCurrentOrganizationId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(auth -> (String) ((UserDetails) auth.getPrincipal()).getOrganizationId());
    }

    public String getCurrentUserIdOrThrow() {
        return getCurrentUserId()
            .orElseThrow(() -> new UnauthorizedException("No authenticated user"));
    }

    public String getCurrentOrganizationIdOrThrow() {
        return getCurrentOrganizationId()
            .orElseThrow(() -> new UnauthorizedException("No organization context"));
    }
}
```

**1.2. Fix JPA Auditing Config**
```java
// File: src/main/java/com/neobrutalism/crm/config/JpaAuditingConfig.java
// Line 30: Replace TODO with actual implementation

@Bean
public AuditorAware<String> auditorProvider() {
    return () -> {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername());
        }

        return Optional.of(authentication.getName());
    };
}
```

**1.3. Update TaskController to enforce organizationId**
```java
// File: src/main/java/com/neobrutalism/crm/domain/task/controller/TaskController.java

@PostMapping
public ResponseEntity<TaskResponse> createTask(
    @RequestBody @Valid CreateTaskRequest request,
    @AuthenticationPrincipal UserDetails userDetails
) {
    // Enforce organizationId from authenticated user
    String orgId = userDetails.getOrganizationId();
    Task task = taskService.createTask(request, orgId);
    return ResponseEntity.ok(taskMapper.toResponse(task));
}
```

#### **Frontend Tasks:**

**1.4. Create Auth Context Hook**
```typescript
// File: src/hooks/use-current-user.ts

import { useQuery } from '@tanstack/react-query'
import { getCurrentUser } from '@/lib/api/auth'

export function useCurrentUser() {
  return useQuery({
    queryKey: ['current-user'],
    queryFn: getCurrentUser,
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

export function useCurrentOrganization() {
  const { data: user } = useCurrentUser()
  return user?.organizationId
}
```

**1.5. Fix Task Creation - Remove hardcoded organizationId**
```typescript
// File: src/app/admin/tasks/page.tsx
// Line 137-141: Replace with:

const { data: currentUser } = useCurrentUser()

// ...

} else {
  // Remove organizationId - backend will enforce from JWT
  createMutation.mutate(formattedData, {
    onSuccess: () => {
      setIsModalOpen(false)
      refetch()
    },
  })
}
```

**1.6. Load Users from API for Assignee Dropdown**
```typescript
// File: src/app/admin/tasks/page.tsx
// Line 442-446: Replace with:

import { useUsers } from '@/hooks/useUsers'

// In component:
const { data: usersData, isLoading: isLoadingUsers } = useUsers({
  page: 1,
  limit: 100,
  status: 'ACTIVE'
})

// In JSX:
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

**Deliverables Day 1:**
- ✅ UserContext service working
- ✅ JPA Auditing fixed
- ✅ Task creation enforces correct organizationId
- ✅ User dropdown loads from API
- ✅ No more hardcoded "default" values

---

### **DAY 2-3: Build Task Detail Page Foundation**

#### **2.1. Create Task Detail Page Structure**
```typescript
// File: src/app/admin/tasks/[taskId]/page.tsx

import { useParams } from 'next/navigation'
import { useTask } from '@/hooks/use-tasks'
import TaskDetailHeader from '@/components/tasks/task-detail-header'
import TaskDetailSidebar from '@/components/tasks/task-detail-sidebar'
import TaskDetailTabs from '@/components/tasks/task-detail-tabs'
import TaskComments from '@/components/tasks/task-comments'
import TaskChecklist from '@/components/tasks/task-checklist'
import TaskTimeline from '@/components/tasks/task-timeline'
import TaskAttachments from '@/components/tasks/task-attachments'

export default function TaskDetailPage() {
  const params = useParams()
  const taskId = params.taskId as string

  const { data: task, isLoading, error } = useTask(taskId)

  if (isLoading) return <TaskDetailSkeleton />
  if (error) return <ErrorDisplay error={error} />
  if (!task) return <NotFound />

  return (
    <div className="flex h-full">
      {/* Main content area - 70% */}
      <div className="flex-1 overflow-y-auto p-6">
        <TaskDetailHeader task={task} />

        <TaskDetailTabs
          tabs={[
            { id: 'overview', label: 'Overview', content: <TaskOverview task={task} /> },
            { id: 'comments', label: 'Comments', content: <TaskComments taskId={taskId} /> },
            { id: 'checklist', label: 'Checklist', content: <TaskChecklist taskId={taskId} /> },
            { id: 'timeline', label: 'Activity', content: <TaskTimeline taskId={taskId} /> },
            { id: 'attachments', label: 'Attachments', content: <TaskAttachments taskId={taskId} /> },
          ]}
        />
      </div>

      {/* Sidebar - 30% */}
      <TaskDetailSidebar task={task} />
    </div>
  )
}
```

#### **2.2. Create Task Detail Header Component**
```typescript
// File: src/components/tasks/task-detail-header.tsx

import { Task } from '@/types/task'
import { Button } from '@/components/ui/button'
import { ArrowLeft, Edit, Trash, MoreHorizontal } from 'lucide-react'
import TaskStatusBadge from './task-status-badge'
import TaskPriorityBadge from './task-priority-badge'
import { useRouter } from 'next/navigation'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

interface TaskDetailHeaderProps {
  task: Task
}

export default function TaskDetailHeader({ task }: TaskDetailHeaderProps) {
  const router = useRouter()

  const handleEdit = () => {
    // Open edit modal or navigate to edit page
  }

  const handleDelete = () => {
    // Show confirmation dialog
  }

  return (
    <div className="mb-6">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 mb-4 text-sm text-gray-600">
        <button
          onClick={() => router.push('/admin/tasks')}
          className="hover:text-black flex items-center gap-1"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Tasks
        </button>
      </div>

      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-3 mb-2">
            <TaskStatusBadge status={task.status} />
            <TaskPriorityBadge priority={task.priority} />
            {task.category && (
              <span className="px-2 py-1 text-xs bg-gray-100 rounded">
                {task.category}
              </span>
            )}
          </div>

          <h1 className="text-3xl font-bold mb-2">{task.title}</h1>

          {task.description && (
            <p className="text-gray-600">{task.description}</p>
          )}
        </div>

        {/* Actions */}
        <div className="flex gap-2">
          <Button variant="outline" onClick={handleEdit}>
            <Edit className="w-4 h-4 mr-2" />
            Edit
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="icon">
                <MoreHorizontal className="w-4 h-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={handleDelete}>
                <Trash className="w-4 h-4 mr-2" />
                Delete Task
              </DropdownMenuItem>
              <DropdownMenuItem>Duplicate Task</DropdownMenuItem>
              <DropdownMenuItem>Convert to Template</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </div>
  )
}
```

#### **2.3. Create Task Detail Sidebar**
```typescript
// File: src/components/tasks/task-detail-sidebar.tsx

import { Task } from '@/types/task'
import { Calendar, User, Clock, Tag } from 'lucide-react'
import { format } from 'date-fns'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'

interface TaskDetailSidebarProps {
  task: Task
}

export default function TaskDetailSidebar({ task }: TaskDetailSidebarProps) {
  return (
    <div className="w-80 border-l border-gray-200 p-6 overflow-y-auto">
      <h3 className="font-semibold mb-4">Task Details</h3>

      {/* Assignee */}
      <div className="mb-6">
        <label className="text-sm text-gray-600 flex items-center gap-2 mb-2">
          <User className="w-4 h-4" />
          Assigned to
        </label>
        {task.assignedTo ? (
          <div className="flex items-center gap-2">
            <Avatar className="w-8 h-8">
              <AvatarImage src={task.assignedTo.avatar} />
              <AvatarFallback>
                {task.assignedTo.fullName?.charAt(0) || 'U'}
              </AvatarFallback>
            </Avatar>
            <span className="font-medium">{task.assignedTo.fullName}</span>
          </div>
        ) : (
          <span className="text-gray-400">Unassigned</span>
        )}
      </div>

      {/* Due Date */}
      <div className="mb-6">
        <label className="text-sm text-gray-600 flex items-center gap-2 mb-2">
          <Calendar className="w-4 h-4" />
          Due Date
        </label>
        {task.dueDate ? (
          <span className={task.isOverdue ? 'text-red-600 font-medium' : ''}>
            {format(new Date(task.dueDate), 'MMM dd, yyyy')}
          </span>
        ) : (
          <span className="text-gray-400">No due date</span>
        )}
      </div>

      {/* Time Tracking */}
      <div className="mb-6">
        <label className="text-sm text-gray-600 flex items-center gap-2 mb-2">
          <Clock className="w-4 h-4" />
          Time Tracking
        </label>
        <div className="space-y-1 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-600">Estimated:</span>
            <span className="font-medium">{task.estimatedHours || 0}h</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">Logged:</span>
            <span className="font-medium">{task.actualHours || 0}h</span>
          </div>
        </div>
      </div>

      {/* Tags */}
      <div className="mb-6">
        <label className="text-sm text-gray-600 flex items-center gap-2 mb-2">
          <Tag className="w-4 h-4" />
          Tags
        </label>
        {task.tags && task.tags.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {task.tags.map((tag) => (
              <span
                key={tag}
                className="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded"
              >
                {tag}
              </span>
            ))}
          </div>
        ) : (
          <span className="text-gray-400">No tags</span>
        )}
      </div>

      {/* Created/Updated Info */}
      <div className="pt-6 border-t border-gray-200 text-xs text-gray-500 space-y-2">
        <div>
          Created {format(new Date(task.createdAt), 'MMM dd, yyyy HH:mm')}
        </div>
        <div>
          Updated {format(new Date(task.updatedAt), 'MMM dd, yyyy HH:mm')}
        </div>
      </div>
    </div>
  )
}
```

**Deliverables Day 2-3:**
- ✅ Task detail page layout complete
- ✅ Header with breadcrumb, title, actions
- ✅ Sidebar with metadata
- ✅ Tab navigation structure ready
- ✅ Responsive design

---

## 📅 SPRINT 1.2: IMPLEMENT COMMENTS SYSTEM (Ngày 4-5)

### **DAY 4: Backend API for Comments**

#### **4.1. Create Comment Entity**
```java
// File: src/main/java/com/neobrutalism/crm/domain/task/model/TaskComment.java

package com.neobrutalism.crm.domain.task.model;

import com.neobrutalism.crm.common.model.BaseEntity;
import com.neobrutalism.crm.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "task_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private TaskComment parentComment;

    @Column(name = "is_edited")
    private boolean edited = false;

    @Column(name = "is_deleted")
    private boolean deleted = false;
}
```

#### **4.2. Create Flyway Migration for Comments**
```sql
-- File: src/main/resources/db/migration/V201__Create_task_comments_table.sql

CREATE TABLE task_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    parent_comment_id UUID REFERENCES task_comments(id) ON DELETE CASCADE,
    is_edited BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);
CREATE INDEX idx_task_comments_user_id ON task_comments(user_id);
CREATE INDEX idx_task_comments_parent_id ON task_comments(parent_comment_id);
```

#### **4.3. Create Comment Service**
```java
// File: src/main/java/com/neobrutalism/crm/domain/task/service/TaskCommentService.java

package com.neobrutalism.crm.domain.task.service;

import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.security.UserContext;
import com.neobrutalism.crm.domain.task.dto.TaskCommentRequest;
import com.neobrutalism.crm.domain.task.dto.TaskCommentResponse;
import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.model.TaskComment;
import com.neobrutalism.crm.domain.task.repository.TaskCommentRepository;
import com.neobrutalism.crm.domain.task.repository.TaskRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final UserContext userContext;

    @Transactional(readOnly = true)
    public Page<TaskCommentResponse> getComments(UUID taskId, Pageable pageable) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        return commentRepository.findByTaskIdAndDeletedFalse(taskId, pageable)
            .map(this::toResponse);
    }

    @Transactional
    public TaskCommentResponse createComment(UUID taskId, TaskCommentRequest request) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        String userId = userContext.getCurrentUserIdOrThrow();
        User author = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TaskComment comment = TaskComment.builder()
            .task(task)
            .author(author)
            .content(request.getContent())
            .build();

        if (request.getParentCommentId() != null) {
            TaskComment parent = commentRepository.findById(request.getParentCommentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            comment.setParentComment(parent);
        }

        comment = commentRepository.save(comment);

        return toResponse(comment);
    }

    @Transactional
    public TaskCommentResponse updateComment(UUID commentId, TaskCommentRequest request) {
        TaskComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        String userId = userContext.getCurrentUserIdOrThrow();
        if (!comment.getAuthor().getId().toString().equals(userId)) {
            throw new UnauthorizedException("You can only edit your own comments");
        }

        comment.setContent(request.getContent());
        comment.setEdited(true);
        comment = commentRepository.save(comment);

        return toResponse(comment);
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        TaskComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        String userId = userContext.getCurrentUserIdOrThrow();
        if (!comment.getAuthor().getId().toString().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own comments");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    private TaskCommentResponse toResponse(TaskComment comment) {
        return TaskCommentResponse.builder()
            .id(comment.getId())
            .taskId(comment.getTask().getId())
            .author(toUserSummary(comment.getAuthor()))
            .content(comment.getContent())
            .parentCommentId(comment.getParentComment() != null ?
                comment.getParentComment().getId() : null)
            .edited(comment.isEdited())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }
}
```

#### **4.4. Create Comment Controller**
```java
// File: src/main/java/com/neobrutalism/crm/domain/task/controller/TaskCommentController.java

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService commentService;

    @GetMapping
    public ResponseEntity<Page<TaskCommentResponse>> getComments(
        @PathVariable UUID taskId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(commentService.getComments(taskId, pageable));
    }

    @PostMapping
    public ResponseEntity<TaskCommentResponse> createComment(
        @PathVariable UUID taskId,
        @RequestBody @Valid TaskCommentRequest request
    ) {
        return ResponseEntity.ok(commentService.createComment(taskId, request));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<TaskCommentResponse> updateComment(
        @PathVariable UUID commentId,
        @RequestBody @Valid TaskCommentRequest request
    ) {
        return ResponseEntity.ok(commentService.updateComment(commentId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
```

**Deliverables Day 4:**
- ✅ TaskComment entity + migration
- ✅ TaskCommentService with CRUD
- ✅ TaskCommentController with REST API
- ✅ Support for nested comments (replies)
- ✅ Soft delete for comments

### **DAY 5: Frontend Comments UI**

#### **5.1. Create API Client for Comments**
```typescript
// File: src/lib/api/task-comments.ts

import { apiClient } from './client'
import type { TaskComment, CreateTaskCommentRequest, UpdateTaskCommentRequest } from '@/types/task'

export const getTaskComments = async (
  taskId: string,
  params?: { page?: number; size?: number }
): Promise<{ content: TaskComment[]; totalElements: number }> => {
  const response = await apiClient.get(`/tasks/${taskId}/comments`, { params })
  return response.data
}

export const createTaskComment = async (
  taskId: string,
  data: CreateTaskCommentRequest
): Promise<TaskComment> => {
  const response = await apiClient.post(`/tasks/${taskId}/comments`, data)
  return response.data
}

export const updateTaskComment = async (
  commentId: string,
  data: UpdateTaskCommentRequest
): Promise<TaskComment> => {
  const response = await apiClient.put(`/tasks/${taskId}/comments/${commentId}`, data)
  return response.data
}

export const deleteTaskComment = async (commentId: string): Promise<void> => {
  await apiClient.delete(`/tasks/${taskId}/comments/${commentId}`)
}
```

#### **5.2. Create React Query Hook**
```typescript
// File: src/hooks/use-task-comments.ts

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getTaskComments,
  createTaskComment,
  updateTaskComment,
  deleteTaskComment
} from '@/lib/api/task-comments'
import { toast } from 'sonner'

export function useTaskComments(taskId: string) {
  return useQuery({
    queryKey: ['task-comments', taskId],
    queryFn: () => getTaskComments(taskId),
    enabled: !!taskId,
  })
}

export function useCreateTaskComment(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateTaskCommentRequest) => createTaskComment(taskId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['task-comments', taskId] })
      toast.success('Comment added successfully')
    },
    onError: () => {
      toast.error('Failed to add comment')
    },
  })
}

export function useUpdateTaskComment(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ commentId, data }: { commentId: string; data: UpdateTaskCommentRequest }) =>
      updateTaskComment(commentId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['task-comments', taskId] })
      toast.success('Comment updated successfully')
    },
    onError: () => {
      toast.error('Failed to update comment')
    },
  })
}

export function useDeleteTaskComment(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deleteTaskComment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['task-comments', taskId] })
      toast.success('Comment deleted successfully')
    },
    onError: () => {
      toast.error('Failed to delete comment')
    },
  })
}
```

#### **5.3. Build Comments Component**
```typescript
// File: src/components/tasks/task-comments.tsx

import { useState } from 'react'
import { useTaskComments, useCreateTaskComment } from '@/hooks/use-task-comments'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { format } from 'date-fns'
import { MessageSquare, Send } from 'lucide-react'
import TaskCommentItem from './task-comment-item'

interface TaskCommentsProps {
  taskId: string
}

export default function TaskComments({ taskId }: TaskCommentsProps) {
  const [newComment, setNewComment] = useState('')
  const { data, isLoading } = useTaskComments(taskId)
  const createMutation = useCreateTaskComment(taskId)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newComment.trim()) return

    await createMutation.mutateAsync({ content: newComment })
    setNewComment('')
  }

  if (isLoading) {
    return <div>Loading comments...</div>
  }

  const comments = data?.content || []

  return (
    <div className="space-y-6">
      {/* Comment input */}
      <form onSubmit={handleSubmit} className="space-y-3">
        <Textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="Add a comment..."
          className="min-h-24"
        />
        <div className="flex justify-end">
          <Button
            type="submit"
            disabled={!newComment.trim() || createMutation.isPending}
          >
            <Send className="w-4 h-4 mr-2" />
            Post Comment
          </Button>
        </div>
      </form>

      {/* Comments list */}
      <div className="space-y-4">
        {comments.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <MessageSquare className="w-12 h-12 mx-auto mb-2 opacity-50" />
            <p>No comments yet. Be the first to comment!</p>
          </div>
        ) : (
          comments.map((comment) => (
            <TaskCommentItem
              key={comment.id}
              comment={comment}
              taskId={taskId}
            />
          ))
        )}
      </div>
    </div>
  )
}
```

**Deliverables Day 5:**
- ✅ API client for comments
- ✅ React Query hooks
- ✅ Comments UI component
- ✅ Add/edit/delete functionality
- ✅ Real-time updates via query invalidation

---

## 📅 SPRINT 1.3: CHECKLIST & TIMELINE (Ngày 6-8)

### **Backend: Checklist Items**
- Similar pattern as Comments
- Create TaskChecklistItem entity
- CRUD operations
- Progress tracking (% completed)

### **Backend: Activity Timeline**
- Leverage existing Activity entity
- Filter by task entity type
- Show all task changes (status, assignee, due date)

### **Frontend: Checklist Component**
- Checkbox list with add/remove
- Progress bar
- Drag to reorder

### **Frontend: Timeline Component**
- Chronological event list
- Grouping by date
- Icons for different event types

**Deliverables Day 6-8:**
- ✅ Checklist backend + frontend
- ✅ Timeline backend + frontend
- ✅ Full integration testing

---

## 📅 SPRINT 1.4: BULK OPERATIONS & POLISH (Ngày 9-10)

### **Bulk Operations:**
- Multi-select checkboxes on Kanban
- Bulk actions: Assign, Change status, Delete, Add tags
- Confirmation dialogs
- Optimistic updates

### **Polish:**
- Loading states
- Error boundaries
- Empty states
- Keyboard shortcuts
- Accessibility (ARIA labels)

**Deliverables Day 9-10:**
- ✅ Bulk operations working
- ✅ Polished UX
- ✅ No TODO comments left in Task module

---

## 🧪 TESTING (Integrated throughout)

### **Backend Tests:**
```java
// File: src/test/java/com/neobrutalism/crm/domain/task/TaskServiceTest.java
// File: src/test/java/com/neobrutalism/crm/domain/task/TaskCommentServiceTest.java
```

### **Frontend Tests:**
```typescript
// File: src/app/admin/tasks/__tests__/page.test.tsx
// File: src/components/tasks/__tests__/task-comments.test.tsx
```

**Target:** 70%+ coverage for Task module

---

## ✅ DEFINITION OF DONE - TUẦN 1-2

- [ ] All placeholders removed (organizationId, user dropdown)
- [ ] Task detail page fully functional
- [ ] Comments system complete with replies
- [ ] Checklist with progress tracking
- [ ] Activity timeline showing all changes
- [ ] Bulk operations working
- [ ] 70%+ test coverage
- [ ] No TODO comments in Task module code
- [ ] Documentation updated
- [ ] Code reviewed and merged to main
