/**
 * Task Types
 * For task management and assignment workflow
 */

export enum TaskStatus {
  TODO = 'TODO',
  IN_PROGRESS = 'IN_PROGRESS',
  IN_REVIEW = 'IN_REVIEW',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  ON_HOLD = 'ON_HOLD',
}

export enum TaskPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT',
  CRITICAL = 'CRITICAL',
}

export enum TaskCategory {
  SALES = 'SALES',
  SUPPORT = 'SUPPORT',
  ONBOARDING = 'ONBOARDING',
  FOLLOW_UP = 'FOLLOW_UP',
  MEETING = 'MEETING',
  RESEARCH = 'RESEARCH',
  PROPOSAL = 'PROPOSAL',
  CONTRACT = 'CONTRACT',
  OTHER = 'OTHER',
}

export interface TaskChecklistItem {
  id: string
  text: string
  isCompleted: boolean
  completedAt?: string
  completedBy?: string
  completedByName?: string
}

export interface TaskComment {
  id: string
  taskId: string
  content: string
  attachments?: string[]
  createdAt: string
  createdBy: string
  createdByName?: string
  author?: string // For display purposes
}

export interface TaskActivity {
  id: string
  taskId: string
  activityType: 'CREATED' | 'UPDATED' | 'STATUS_CHANGED' | 'ASSIGNED' | 'COMMENT_ADDED' | 'CHECKLIST_UPDATED' | 'COMPLETED' | 'CANCELLED'
  description: string
  changes?: Record<string, any>
  performedBy: string
  performedByName?: string
  createdAt: string
}

export interface Task {
  id: string
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  category: TaskCategory
  customerId?: string
  customerName?: string
  contactId?: string
  contactName?: string
  relatedActivityId?: string
  assignedToId?: string
  assignedToName?: string
  dueDate?: string
  startDate?: string
  completedDate?: string
  estimatedHours?: number
  actualHours?: number
  tags?: string[]
  attachments?: string[]
  checklistItems?: TaskChecklistItem[]
  comments?: TaskComment[]
  parentTaskId?: string
  dependencies?: string[]
  reminderDate?: string
  isRecurring: boolean
  recurrencePattern?: string
  organizationId: string
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  version: number
}

export interface CreateTaskRequest {
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  category: TaskCategory
  customerId?: string
  contactId?: string
  relatedActivityId?: string
  assignedToId?: string
  dueDate?: string
  startDate?: string
  estimatedHours?: number
  tags?: string[]
  attachments?: string[]
  checklistItems?: Omit<TaskChecklistItem, 'id' | 'completedAt' | 'completedBy' | 'completedByName'>[]
  parentTaskId?: string
  dependencies?: string[]
  reminderDate?: string
  isRecurring?: boolean
  recurrencePattern?: string
  organizationId: string
}

export interface UpdateTaskRequest {
  title?: string
  description?: string
  status?: TaskStatus
  priority?: TaskPriority
  category?: TaskCategory
  customerId?: string
  contactId?: string
  relatedActivityId?: string
  assignedToId?: string
  dueDate?: string
  startDate?: string
  completedDate?: string
  estimatedHours?: number
  actualHours?: number
  tags?: string[]
  attachments?: string[]
  checklistItems?: TaskChecklistItem[]
  dependencies?: string[]
  reminderDate?: string
  isRecurring?: boolean
  recurrencePattern?: string
}

export interface CreateTaskCommentRequest {
  taskId: string
  content: string
  attachments?: string[]
}

export interface TaskSearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  keyword?: string
  status?: TaskStatus
  priority?: TaskPriority
  category?: TaskCategory
  customerId?: string
  contactId?: string
  assignedToId?: string
  dueDateFrom?: string
  dueDateTo?: string
  isOverdue?: boolean
  organizationId?: string
}

export interface TaskStats {
  total: number
  byStatus: Record<TaskStatus, number>
  byPriority: Record<TaskPriority, number>
  byCategory: Record<TaskCategory, number>
  completedCount: number
  inProgressCount: number
  todoCount: number
  overdueCount: number
  completionRate: number
  averageCompletionTime: number // in hours
  totalEstimatedHours: number
  totalActualHours: number
}

export interface TaskBoard {
  todo: Task[]
  inProgress: Task[]
  inReview: Task[]
  completed: Task[]
}
